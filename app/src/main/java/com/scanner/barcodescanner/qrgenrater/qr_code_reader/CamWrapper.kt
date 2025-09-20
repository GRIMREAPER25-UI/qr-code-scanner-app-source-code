package com.scanner.barcodescanner.qrgenrater.qr_code_reader

import android.hardware.Camera

class CamWrapper private constructor(camera: Camera?, i: Int) {

    var mCamera: Camera? = null
    var mCameraId = 0

    init {
        if (camera != null) {
            mCamera = camera
            mCameraId = i
        } else {
            throw NullPointerException("Camera cannot be null")
        }
    }

    companion object {
        fun getWrapper(camera: Camera, i: Int): CamWrapper {
            return CamWrapper(camera, i)
        }
    }
}
