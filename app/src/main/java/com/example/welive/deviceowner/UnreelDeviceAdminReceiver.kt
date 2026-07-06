package com.example.welive.deviceowner

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

class UnreelDeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return "Unreel uses device management to keep uninstall protection active."
    }
}
