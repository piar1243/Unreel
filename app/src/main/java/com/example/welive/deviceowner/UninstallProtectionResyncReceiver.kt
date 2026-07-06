package com.example.welive.deviceowner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.welive.settings.UserRulesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class UninstallProtectionResyncReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching {
                val applicationContext = context.applicationContext
                val settings = UserRulesRepository(applicationContext).settings.first()
                DeviceOwnerPolicyController(applicationContext).syncPolicies(settings)
            }
            pendingResult.finish()
        }
    }

    companion object {
        const val ACTION_RESYNC_UNINSTALL_PROTECTION =
            "com.example.welive.action.RESYNC_UNINSTALL_PROTECTION"
    }
}
