package com.example.welive.deviceowner

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.example.welive.settings.AppSettings

class DeviceOwnerPolicyController(
    private val context: Context
) {
    private val devicePolicyManager = context.getSystemService(DevicePolicyManager::class.java)
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val adminComponent = ComponentName(context, UnreelDeviceAdminReceiver::class.java)

    fun status(settings: AppSettings, nowMillis: Long = System.currentTimeMillis()): UninstallProtectionStatus {
        val canControl = canControlUninstallBlock()
        return UninstallProtectionStatus(
            isDeviceOwner = isDeviceOwnerApp(),
            isDeviceAdminActive = isDeviceAdminActive(),
            canControlUninstallBlock = canControl,
            protectionEnabled = settings.protectAppUninstall,
            uninstallBlocked = if (canControl) isSelfUninstallBlocked() else false,
            uninstallBypassActive = settings.isUninstallBypassActive(nowMillis)
        )
    }

    fun syncPolicies(settings: AppSettings, nowMillis: Long = System.currentTimeMillis()) {
        if (!canControlUninstallBlock()) {
            cancelRestoreAlarm()
            return
        }
        when {
            !settings.protectAppUninstall -> {
                cancelRestoreAlarm()
                setSelfUninstallBlocked(false)
            }
            settings.isUninstallBypassActive(nowMillis) -> {
                setSelfUninstallBlocked(false)
                scheduleRestoreAlarm(settings.uninstallBypassUntilMillis)
            }
            else -> {
                cancelRestoreAlarm()
                setSelfUninstallBlocked(true)
            }
        }
    }

    fun isDeviceOwnerApp(): Boolean {
        return devicePolicyManager?.isDeviceOwnerApp(context.packageName) == true
    }

    fun isDeviceAdminActive(): Boolean {
        return devicePolicyManager?.isAdminActive(adminComponent) == true
    }

    fun canControlUninstallBlock(): Boolean {
        return isDeviceOwnerApp() || devicePolicyManager?.isProfileOwnerApp(context.packageName) == true
    }

    fun isSelfUninstallBlocked(): Boolean {
        val manager = devicePolicyManager ?: return false
        return runCatching {
            manager.isUninstallBlocked(adminComponent, context.packageName)
        }.getOrDefault(false)
    }

    fun setSelfUninstallBlocked(blocked: Boolean): Boolean {
        val manager = devicePolicyManager ?: return false
        if (!canControlUninstallBlock()) return false
        return runCatching {
            manager.setUninstallBlocked(adminComponent, context.packageName, blocked)
            true
        }.getOrDefault(false)
    }

    fun createAppInfoIntent(): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun scheduleRestoreAlarm(triggerAtMillis: Long) {
        val manager = alarmManager ?: return
        manager.cancel(restorePendingIntent())
        manager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis.coerceAtLeast(System.currentTimeMillis() + 1_000L),
            restorePendingIntent()
        )
    }

    private fun cancelRestoreAlarm() {
        alarmManager?.cancel(restorePendingIntent())
    }

    private fun restorePendingIntent(): PendingIntent {
        val intent = Intent(context, UninstallProtectionResyncReceiver::class.java).apply {
            action = UninstallProtectionResyncReceiver.ACTION_RESYNC_UNINSTALL_PROTECTION
            `package` = context.packageName
        }
        return PendingIntent.getBroadcast(
            context,
            701,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

data class UninstallProtectionStatus(
    val isDeviceOwner: Boolean,
    val isDeviceAdminActive: Boolean,
    val canControlUninstallBlock: Boolean,
    val protectionEnabled: Boolean,
    val uninstallBlocked: Boolean,
    val uninstallBypassActive: Boolean
)
