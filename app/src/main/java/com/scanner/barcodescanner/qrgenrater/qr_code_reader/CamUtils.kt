package com.scanner.barcodescanner.qrgenrater.qr_code_reader

import android.hardware.Camera

object CamUtils {

    fun getCameraInstance(): Camera? {
        return getCameraInstance(getDefaultCameraId())
    }

    fun getDefaultCameraId(): Int {
        val numberOfCameras = Camera.getNumberOfCameras()
        val cameraInfo = Camera.CameraInfo()
        var defaultId = -1
        for (i in 0 until numberOfCameras) {
            Camera.getCameraInfo(i, cameraInfo)
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                return i
            }
            defaultId = i
        }
        return defaultId
    }

    fun getCameraInstance(id: Int): Camera? {
        return if (id != -1) {
            try {
                Camera.open(id)
            } catch (e: Exception) {
                null
            }
        } else {
            try {
                Camera.open()
            } catch (e: Exception) {
                null
            }
        }
    }

    fun isFlashSupported(camera: Camera?): Boolean {
        return camera?.let {
            val parameters = it.parameters
            val flashMode = parameters.flashMode
            val supportedFlashModes = parameters.supportedFlashModes
            supportedFlashModes != null && supportedFlashModes.isNotEmpty() && (supportedFlashModes.size != 1 || supportedFlashModes[0] != Camera.Parameters.FLASH_MODE_OFF)
        } ?: false
    }
}
