package com.scanner.barcodescanner.qrgenrater.qr_code_reader

import android.content.Context
import android.graphics.Point
import android.hardware.Camera
import android.hardware.Camera.AutoFocusCallback
import android.hardware.Camera.CameraInfo
import android.hardware.Camera.PreviewCallback
import android.os.Handler
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import com.scanner.barcodescanner.qrgenrater.qr_code_reader.DisplayUtil.getScreenOrientation
import org.bouncycastle.crypto.tls.CipherSuite
import kotlin.math.abs

class CamPreview : SurfaceView, SurfaceHolder.Callback {
    var TAG = "CameraPreview"
    var autoFocusCB = AutoFocusCallback { isFocus: Boolean, camera: Camera? -> scheduleAutoFocus() }
    private val doAutoFocus = Runnable {
        if (mCameraWrapper != null && mPreviewing && mAutoFocus && mSurfaceCreated) {
            safeAutoFocus()
        }
    }
    private var mAspectTolerance = 0.1f
    var mAutoFocus = true
    private var mAutoFocusHandler: Handler? = null
    var mCameraWrapper: CamWrapper? = null
    private var mPreviewCallback: PreviewCallback? = null
    var mPreviewing = true
    private var mShouldScaleToFill = true
    var mSurfaceCreated = false

    constructor(
        context: Context?,
        cameraWrapper: CamWrapper?,
        previewCallback: PreviewCallback?
    ) : super(context) {
        init(cameraWrapper, previewCallback)
    }

    constructor(
        context: Context?,
        attributeSet: AttributeSet?,
        cameraWrapper: CamWrapper?,
        previewCallback: PreviewCallback?
    ) : super(context, attributeSet) {
        init(cameraWrapper, previewCallback)
    }

    fun init(cameraWrapper: CamWrapper?, previewCallback: PreviewCallback?) {
        setCamera(cameraWrapper, previewCallback)
        mAutoFocusHandler = Handler()
        holder.addCallback(this)
        holder.setType(3)
    }

    fun setCamera(cameraWrapper: CamWrapper?, previewCallback: PreviewCallback?) {
        mCameraWrapper = cameraWrapper
        mPreviewCallback = previewCallback
    }

    fun setShouldScaleToFill(z: Boolean) {
        mShouldScaleToFill = z
    }

    fun setAspectTolerance(f: Float) {
        mAspectTolerance = f
    }

    override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
        mSurfaceCreated = true
    }

    override fun surfaceChanged(surfaceHolder: SurfaceHolder, i: Int, i2: Int, i3: Int) {
        if (surfaceHolder.surface != null) {
            stopCameraPreview()
            showCameraPreview()
        }
    }

    override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {
        mSurfaceCreated = false
        stopCameraPreview()
    }

    fun showCameraPreview() {
        if (mCameraWrapper != null) {
            try {
                holder.addCallback(this)
                mPreviewing = true
                setupCameraParameters()
                mCameraWrapper!!.mCamera!!.setPreviewDisplay(holder)
                mCameraWrapper!!.mCamera!!.setDisplayOrientation(displayOrientation)
                mCameraWrapper!!.mCamera!!.setOneShotPreviewCallback(mPreviewCallback)
                mCameraWrapper!!.mCamera!!.startPreview()
                if (!mAutoFocus) {
                    return
                }
                if (mSurfaceCreated) {
                    safeAutoFocus()
                } else {
                    scheduleAutoFocus()
                }
            } catch (e: Exception) {
                Log.e(TAG, e.toString(), e)
            }
        }
    }

    fun safeAutoFocus() {
        try {
            mCameraWrapper!!.mCamera!!.autoFocus(autoFocusCB)
        } catch (exception: RuntimeException) {
            scheduleAutoFocus()
        }
    }

    fun stopCameraPreview() {
        if (mCameraWrapper != null) {
            try {
                mPreviewing = false
                holder.removeCallback(this)
                mCameraWrapper!!.mCamera!!.cancelAutoFocus()
                mCameraWrapper!!.mCamera!!.setOneShotPreviewCallback(null as PreviewCallback?)
                mCameraWrapper!!.mCamera!!.stopPreview()
            } catch (e: Exception) {
                Log.e(TAG, e.toString(), e)
            }
        }
    }

    fun setupCameraParameters() {
        val optimalPreviewSize = optimalPreviewSize
        val parameters = mCameraWrapper!!.mCamera!!.getParameters()
        parameters.setPreviewSize(optimalPreviewSize!!.width, optimalPreviewSize.height)
        mCameraWrapper!!.mCamera!!.setParameters(parameters)
        adjustViewSize(optimalPreviewSize)
    }

    private fun adjustViewSize(size: Camera.Size?) {
        val convertSizeToLandscapeOrientation = convertSizeToLandscapeOrientation(
            Point(
                width, height
            )
        )
        val f = size!!.width.toFloat() / size.height.toFloat()
        if (convertSizeToLandscapeOrientation.x.toFloat() / convertSizeToLandscapeOrientation.y.toFloat() > f) {
            setViewSize(
                (convertSizeToLandscapeOrientation.y.toFloat() * f).toInt(),
                convertSizeToLandscapeOrientation.y
            )
        } else {
            setViewSize(
                convertSizeToLandscapeOrientation.x,
                (convertSizeToLandscapeOrientation.x.toFloat() / f).toInt()
            )
        }
    }

    private fun convertSizeToLandscapeOrientation(point: Point): Point {
        return if (displayOrientation % CipherSuite.TLS_DHE_PSK_WITH_NULL_SHA256 == 0) {
            point
        } else Point(point.y, point.x)
    }

    private fun setViewSize(i: Int, i2: Int) {
        var i = i
        var i2 = i2
        val layoutParams = layoutParams
        if (displayOrientation % CipherSuite.TLS_DHE_PSK_WITH_NULL_SHA256 != 0) {
            val i3 = i2
            i2 = i
            i = i3
        }
        if (mShouldScaleToFill) {
            val f = i.toFloat()
            var width = (parent as View).width.toFloat() / f
            val f2 = i2.toFloat()
            val height = (parent as View).height.toFloat() / f2
            if (width <= height) {
                width = height
            }
            i = Math.round(f * width)
            i2 = Math.round(f2 * width)
        }
        layoutParams.width = i
        layoutParams.height = i2
        setLayoutParams(layoutParams)
    }

    val displayOrientation: Int
        get() {
            var i = 0
            if (mCameraWrapper == null) {
                return 0
            }
            val cameraInfo = CameraInfo()
            if (mCameraWrapper!!.mCameraId == -1) {
                Camera.getCameraInfo(0, cameraInfo)
            } else {
                Camera.getCameraInfo(mCameraWrapper!!.mCameraId, cameraInfo)
            }
            val rotation =
                (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation
            if (rotation != 0) {
                if (rotation == 1) {
                    i = 90
                } else if (rotation == 2) {
                    i = CipherSuite.TLS_DHE_PSK_WITH_NULL_SHA256
                } else if (rotation == 3) {
                    i = 270
                }
            }
            return if (cameraInfo.facing == 1) {
                (360 - (cameraInfo.orientation + i) % 360) % 360
            } else (cameraInfo.orientation - i + 360) % 360
        }
    private val optimalPreviewSize: Camera.Size?
        private get() {
            var size: Camera.Size? = null
            if (mCameraWrapper == null) {
                return null
            }
            val supportedPreviewSizes =
                mCameraWrapper!!.mCamera!!.getParameters().getSupportedPreviewSizes()
            var width = width
            var height = height
            if (getScreenOrientation(context) == 1) {
                val i = height
                height = width
                width = i
            }
            val d = width.toDouble() / height.toDouble()
            if (supportedPreviewSizes == null) {
                return null
            }
            var d2 = Double.MAX_VALUE
            var d3 = Double.MAX_VALUE
            for (next in supportedPreviewSizes) {
                if (abs(next.width.toDouble() / next.height.toDouble() - d) <= mAspectTolerance.toDouble() && abs(
                        (next.height - height).toDouble()
                    ) < d3
                ) {
                    d3 = abs((next.height - height).toDouble())
                    size = next
                }
            }
            if (size == null) {
                for (next2 in supportedPreviewSizes) {
                    if (abs((next2.height - height).toDouble()) < d2) {
                        size = next2
                        d2 = abs((next2.height - height).toDouble())
                    }
                }
            }
            return size
        }

    fun setAutoFocus(z: Boolean) {
        if (mCameraWrapper != null && mPreviewing && z != mAutoFocus) {
            mAutoFocus = z
            if (!mAutoFocus) {
                Log.v(TAG, "Cancelling autofocus")
                mCameraWrapper!!.mCamera!!.cancelAutoFocus()
            } else if (mSurfaceCreated) {
                Log.v(TAG, "Starting autofocus")
                safeAutoFocus()
            } else {
                scheduleAutoFocus()
            }
        }
    }

    fun scheduleAutoFocus() {
        mAutoFocusHandler!!.postDelayed(doAutoFocus, 1000)
    }
}
