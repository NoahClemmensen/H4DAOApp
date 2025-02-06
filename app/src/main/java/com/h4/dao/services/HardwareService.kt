package com.h4.dao.services

import android.content.Context
import android.content.pm.PackageManager

class HardwareService {
    /** Check if this device has a camera */
    fun CheckCameraHardware(context: Context): Boolean {
        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            // this device has a camera
            return true
        } else {
            // no camera on this device
            return false
        }
    }
}