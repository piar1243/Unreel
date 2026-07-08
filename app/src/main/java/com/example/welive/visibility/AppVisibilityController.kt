package com.example.welive.visibility

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import com.example.welive.MainActivity

class AppVisibilityController(
    private val context: Context
) {
    private val packageManager = context.packageManager
    private val launcherAlias = ComponentName(context, LAUNCHER_ALIAS_CLASS_NAME)

    fun syncLauncherVisibility(hidden: Boolean) {
        // Keep Android Studio debug installs launchable even when the user-facing
        // hide-icon setting is enabled. Release builds retain the locked-down behavior.
        val isDebuggable =
            context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        val shouldHideLauncher = hidden && !isDebuggable
        val targetState = if (shouldHideLauncher) {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        }
        val currentState = packageManager.getComponentEnabledSetting(launcherAlias)
        if (currentState == targetState) return
        packageManager.setComponentEnabledSetting(
            launcherAlias,
            targetState,
            PackageManager.DONT_KILL_APP
        )
    }

    fun createReopenIntent(): Intent {
        return Intent(Intent.ACTION_VIEW, Uri.parse(REOPEN_DEEP_LINK)).apply {
            setPackage(context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
    }

    companion object {
        const val REOPEN_DEEP_LINK = "unreel://open"
        private const val LAUNCHER_ALIAS_CLASS_NAME = "com.example.welive.LauncherAlias"
    }
}
