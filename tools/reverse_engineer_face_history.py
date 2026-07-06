import argparse
import csv
import itertools
import math
from collections import Counter, defaultdict

import numpy as np


FEATURES = [
    "faceShape",
    "skinTone",
    "hair",
    "eyebrows",
    "eyes",
    "nose",
    "mouth",
    "glasses",
    "mole",
    "accessory",
]


def as_float(row, key):
    return float(row[key])


def as_int(row, key):
    return int(float(row[key]))


def read_rows(path):
    with open(path, newline="", encoding="utf-8-sig") as handle:
        rows = list(csv.DictReader(handle))

    for row in rows:
        for feature in FEATURES:
            row[feature] = as_int(row, feature)
        row["queryIndex"] = as_int(row, "queryIndex")
        row["probability"] = as_float(row, "probability")
        row["percentCorrect"] = row["probability"] * 100.0
        row["charm"] = as_float(row, "charm")
        row["editDistance"] = as_int(row, "editDistance")
        row["sync"] = row.get("sync", "").strip().lower() == "true"
    return rows


def combo_key(row):
    return tuple(row[feature] for feature in FEATURES)


def hamming(a, b):
    return sum(left != right for left, right in zip(a, b))


def format_combo(combo):
    return ", ".join(f"{feature}={value}" for feature, value in zip(FEATURES, combo))


def print_rows(title, rows, limit=10):
    print(f"\n{title}")
    if not rows:
        print("  none")
        return
    for row in rows[:limit]:
        combo = combo_key(row)
        print(
            f"  query={row['queryIndex']:>4}  pct={row['percentCorrect']:>8.3f}%"
            f"  charm={row['charm']:>5.2f}  dist={row['editDistance']}"
            f"  sync={str(row.get('sync', False)).lower()}"
            f"  {format_combo(combo)}"
        )
        if row.get("reviewerComment"):
            print(f"        comment: {row['reviewerComment']}")


def find_baseline(rows):
    zero_rows = [row for row in rows if row["editDistance"] == 0]
    if not zero_rows:
        counts = Counter(combo_key(row) for row in rows)
        return counts.most_common(1)[0][0]
    counts = Counter(combo_key(row) for row in zero_rows)
    return counts.most_common(1)[0][0]


def observed_domains(rows):
    return {feature: sorted({row[feature] for row in rows}) for feature in FEATURES}


def aggregate_by_combo(rows):
    by_combo = defaultdict(list)
    for row in rows:
        by_combo[combo_key(row)].append(row)

    aggregated = []
    for combo, group in by_combo.items():
        item = {feature: value for feature, value in zip(FEATURES, combo)}
        item["queryIndex"] = min(row["queryIndex"] for row in group)
        item["probability"] = sum(row["probability"] for row in group) / len(group)
        item["percentCorrect"] = item["probability"] * 100.0
        item["charm"] = sum(row["charm"] for row in group) / len(group)
        item["editDistance"] = min(row["editDistance"] for row in group)
        item["sync"] = any(row.get("sync", False) for row in group)
        item["reviewerComment"] = "; ".join(sorted({row["reviewerComment"] for row in group}))
        aggregated.append(item)
    return aggregated


def logit(probability):
    probability = min(max(probability, 1e-6), 1.0 - 1e-6)
    return math.log(probability / (1.0 - probability))


def sigmoid(value):
    if value >= 0:
        z = math.exp(-value)
        return 1.0 / (1.0 + z)
    z = math.exp(value)
    return z / (1.0 + z)


def generate_close_combos(baseline, domains, max_distance):
    baseline = tuple(baseline)
    yield baseline
    indices = range(len(FEATURES))
    for distance in range(1, max_distance + 1):
        for changed_indices in itertools.combinations(indices, distance):
            value_lists = []
            for idx in changed_indices:
                feature = FEATURES[idx]
                value_lists.append([value for value in domains[feature] if value != baseline[idx]])
            for replacement_values in itertools.product(*value_lists):
                combo = list(baseline)
                for idx, value in zip(changed_indices, replacement_values):
                    combo[idx] = value
                yield tuple(combo)


def generate_local_candidate_combos(baseline, domains, rows, max_distance, seed_count=80):
    candidates = set()
    observed_combos = {combo_key(row) for row in rows}
    for combo in observed_combos:
        if hamming(combo, baseline) <= max_distance:
            candidates.add(combo)

    high_signal_rows = sorted(
        rows,
        key=lambda row: (row["probability"], row["charm"], -row["editDistance"]),
        reverse=True,
    )[:seed_count]
    high_charm_rows = sorted(
        rows,
        key=lambda row: (row["charm"], row["probability"], -row["editDistance"]),
        reverse=True,
    )[: seed_count // 2]

    for row in high_signal_rows + high_charm_rows:
        combo = combo_key(row)
        changed = [idx for idx, value in enumerate(combo) if value != baseline[idx]]
        for size in range(0, min(max_distance, len(changed)) + 1):
            for subset in itertools.combinations(changed, size):
                projected = list(baseline)
                for idx in subset:
                    projected[idx] = combo[idx]
                candidates.add(tuple(projected))

    close_rows = sorted(
        [row for row in rows if row["editDistance"] <= max_distance],
        key=lambda row: (row["probability"], row["charm"], -row["editDistance"]),
        reverse=True,
    )[:seed_count]
    for row in close_rows:
        combo = combo_key(row)
        candidates.add(combo)
        for idx, feature in enumerate(FEATURES):
            for value in domains[feature]:
                mutated = list(combo)
                mutated[idx] = value
                mutated = tuple(mutated)
                if hamming(mutated, baseline) <= max_distance:
                    candidates.add(mutated)

    return candidates


def build_encoder(rows, baseline):
    columns = [("intercept", None)]
    for idx, feature in enumerate(FEATURES):
        for value in sorted({row[feature] for row in rows}):
            if value != baseline[idx]:
                columns.append((feature, value))
    return columns


def encode_combo(combo, columns):
    values = np.zeros(len(columns), dtype=float)
    values[0] = 1.0
    by_feature = dict(zip(FEATURES, combo))
    for col_idx, (feature, value) in enumerate(columns[1:], start=1):
        values[col_idx] = 1.0 if by_feature[feature] == value else 0.0
    return values


def fit_ridge(rows, baseline, target_key, alpha):
    columns = build_encoder(rows, baseline)
    x = np.vstack([encode_combo(combo_key(row), columns) for row in rows])
    y = np.array([row[target_key] for row in rows], dtype=float)

    penalty = np.eye(x.shape[1]) * alpha
    penalty[0, 0] = 0.0
    beta = np.linalg.solve(x.T @ x + penalty, x.T @ y)
    prediction = x @ beta
    rmse = float(np.sqrt(np.mean((prediction - y) ** 2)))
    return columns, beta, rmse


def nearest_observed(combo, observed_combos):
    nearest = min(observed_combos, key=lambda item: hamming(combo, item))
    return hamming(combo, nearest), nearest


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("csv_path")
    parser.add_argument("--target-probability", type=float, default=0.999)
    parser.add_argument("--target-charm", type=float, default=3.0)
    parser.add_argument("--max-distance", type=int, default=4)
    parser.add_argument("--top", type=int, default=15)
    parser.add_argument(
        "--require-sync",
        action="store_true",
        help="Only use rows where sync is true.",
    )
    parser.add_argument(
        "--full-grid",
        action="store_true",
        help="Score every observed-domain combination within max distance. Slower.",
    )
    args = parser.parse_args()

    all_rows = read_rows(args.csv_path)
    baseline = find_baseline(all_rows)
    rows = [row for row in all_rows if row["sync"]] if args.require_sync else all_rows
    if not rows:
        raise SystemExit("No rows left after filtering.")
    aggregated = aggregate_by_combo(rows)
    domains = observed_domains(rows)

    print(f"Loaded rows: {len(all_rows)}")
    print(f"Rows used: {len(rows)}")
    print(f"Sync filter: {'required' if args.require_sync else 'not required'}")
    print(f"Sync=true rows in file: {sum(1 for row in all_rows if row['sync'])}")
    print(f"Unique combinations: {len(aggregated)}")
    print(f"Baseline inferred from editDistance=0: {format_combo(baseline)}")

    mismatches = [row for row in rows if hamming(combo_key(row), baseline) != row["editDistance"]]
    print(f"Rows where inferred Hamming distance disagrees with editDistance: {len(mismatches)}")

    exact_hits = [
        row
        for row in aggregated
        if row["probability"] >= args.target_probability
        and row["charm"] > args.target_charm
        and row["editDistance"] <= args.max_distance
    ]
    exact_hits.sort(key=lambda row: (row["probability"], row["charm"], -row["editDistance"]), reverse=True)
    print_rows("Existing rows that satisfy all constraints", exact_hits, args.top)

    strict_rows = [
        row
        for row in aggregated
        if row["charm"] > args.target_charm and row["editDistance"] <= args.max_distance
    ]
    strict_rows.sort(key=lambda row: (row["probability"], row["charm"], -row["editDistance"]), reverse=True)
    print_rows("Best existing rows with charm > target and distance within target", strict_rows, args.top)

    close_charm_rows = [
        row
        for row in aggregated
        if row["charm"] > args.target_charm - 0.15 and row["editDistance"] <= args.max_distance
    ]
    close_charm_rows.sort(key=lambda row: (row["probability"], row["charm"], -row["editDistance"]), reverse=True)
    print_rows("Best existing rows with charm near target and distance within target", close_charm_rows, args.top)

    model_rows = []
    for row in aggregated:
        modeled = dict(row)
        modeled["logitProbability"] = logit(row["probability"])
        model_rows.append(modeled)

    probability_columns, probability_beta, probability_rmse = fit_ridge(
        model_rows,
        baseline,
        "logitProbability",
        alpha=2.0,
    )
    charm_columns, charm_beta, charm_rmse = fit_ridge(
        model_rows,
        baseline,
        "charm",
        alpha=2.0,
    )

    scored = []
    observed = {combo_key(row) for row in aggregated}
    if args.full_grid:
        candidates = generate_close_combos(baseline, domains, args.max_distance)
        print("\nScoring candidate source: full observed-domain grid")
    else:
        local_candidates = generate_local_candidate_combos(
            baseline,
            domains,
            aggregated,
            args.max_distance,
        )
        candidates = sorted(local_candidates)
        print(f"\nScoring candidate source: evidence-backed local search ({len(candidates)} combos)")

    for combo in candidates:
        probability_logit = float(encode_combo(combo, probability_columns) @ probability_beta)
        probability = sigmoid(probability_logit)
        charm = float(encode_combo(combo, charm_columns) @ charm_beta)
        prediction = {
            "probability": probability,
            "percentCorrect": probability * 100.0,
            "charm": charm,
        }
        distance = hamming(combo, baseline)
        score = (
            prediction["probability"],
            prediction["charm"],
            -distance,
        )
        scored.append((score, combo, prediction, combo in observed))

    satisfying_predictions = [
        item
        for item in scored
        if item[2]["probability"] >= args.target_probability and item[2]["charm"] > args.target_charm
    ]
    satisfying_predictions.sort(reverse=True)
    print(
        "\nRidge surrogate candidates that meet all constraints"
        f" (probability RMSE logit={probability_rmse:.3f}, charm RMSE={charm_rmse:.3f})"
    )
    if not satisfying_predictions:
        print("  none")
    for _, combo, prediction, was_observed in satisfying_predictions[: args.top]:
        support_distance, support_combo = nearest_observed(combo, observed)
        print(
            f"  pred_pct={prediction['percentCorrect']:>8.3f}%"
            f"  pred_charm={prediction['charm']:>5.2f}"
            f"  dist={hamming(combo, baseline)}"
            f"  observed={'yes' if was_observed else 'no'}"
            f"  nearest_observed_dist={support_distance}"
            f"  {format_combo(combo)}"
        )
        print(f"        nearest observed: {format_combo(support_combo)}")

    scored.sort(reverse=True)
    print("\nTop ridge surrogate candidates within distance limit")
    top_scored = [item for item in scored if item[2]["charm"] > args.target_charm]
    if not top_scored:
        top_scored = scored
    for _, combo, prediction, was_observed in top_scored[: args.top]:
        support_distance, support_combo = nearest_observed(combo, observed)
        print(
            f"  pred_pct={prediction['percentCorrect']:>8.3f}%"
            f"  pred_charm={prediction['charm']:>5.2f}"
            f"  dist={hamming(combo, baseline)}"
            f"  observed={'yes' if was_observed else 'no'}"
            f"  nearest_observed_dist={support_distance}"
            f"  {format_combo(combo)}"
        )
        print(f"        nearest observed: {format_combo(support_combo)}")


if __name__ == "__main__":
    main()
