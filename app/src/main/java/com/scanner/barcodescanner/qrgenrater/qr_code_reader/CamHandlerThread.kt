package com.scanner.barcodescanner.qrgenrater.qr_code_reader

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import com.scanner.barcodescanner.qrgenrater.qr_code_reader.CamUtils.getCameraInstance

class CamHandlerThread(var mScannerView: BarcodeScannerView) :
    HandlerThread("CameraHandlerThread") {
    init {
        start()
    }

    fun startCamera(i: Int) {
        Handler(getLooper()).post {
            val cameraInstance = getCameraInstance(i)
            Handler(Looper.getMainLooper()).post {
                mScannerView.setupCameraPreview(
                    CamWrapper.getWrapper(
                        cameraInstance!!, i
                    )
                )
            }
        }
    }
}
