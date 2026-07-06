# Unreel Device-Owner Setup

Unreel can only enforce uninstall blocking when Android recognizes it as a device owner (or profile owner). A normal app install cannot grant that power.

## What this enables

- Block uninstall of Unreel itself
- Temporarily lift uninstall protection only after a phone credential check
- Re-apply uninstall blocking after the temporary uninstall window expires or after reboot

## Important limitation

Device-owner provisioning is usually done on a fresh or reset test device during setup. If the phone is already in normal personal use, Android may reject device-owner enrollment.

## Typical adb test flow

1. Install the debug build.
2. Enable device admin / managed-device provisioning on a test phone.
3. Run:

```powershell
adb shell dpm set-device-owner com.example.welive/.deviceowner.UnreelDeviceAdminReceiver
```

If Android rejects the command, the device likely needs to be reset or reprovisioned for managed-device use first.

## After provisioning

- Open Unreel
- Go to `App lock`
- Leave `Block Uninstall` enabled
- Use `Allow Uninstall with Phone Credential` only when you intentionally want a short uninstall window

## Current uninstall flow

Unreel does not inject a PIN prompt into the Android uninstall dialog itself. Instead:

1. Uninstall is blocked by device policy
2. Inside Unreel, the user proves identity with the phone's credential screen
3. Unreel opens a short uninstall window
4. Android app info opens so the uninstall can be completed
