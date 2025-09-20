package com.scanner.barcodescanner.qrgenrater.qr_code_reader

import android.content.Context
import android.graphics.Rect
import android.hardware.Camera
import android.hardware.Camera.PreviewCallback
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.core.view.ViewCompat
import com.scanner.barcodescanner.qrgenrater.R
import com.scanner.barcodescanner.qrgenrater.qr_code_reader.CamUtils.getDefaultCameraId
import com.scanner.barcodescanner.qrgenrater.qr_code_reader.CamUtils.isFlashSupported

abstract class BarcodeScannerView : FrameLayout, PreviewCallback {
    private var mAspectTolerance = 0.1f
    private var mAutofocusState = true
    private var mBorderAlpha = 1.0f
    private var mBorderColor = resources.getColor(R.color.viewfinder_border)
    private var mBorderLength = resources.getInteger(R.integer.viewfinder_border_length)
    private var mBorderWidth = resources.getInteger(R.integer.viewfinder_border_width)
    private var mCameraHandlerThread: CamHandlerThread? = null
    private var mCameraWrapper: CamWrapper? = null
    private var mCornerRadius = 0
    private var mFlashState: Boolean? = null
    private var mFramingRectInPreview: Rect? = null
    private var mIsLaserEnabled = true
    private var mLaserColor = resources.getColor(R.color.viewfinder_laser)
    private var mMaskColor = resources.getColor(R.color.viewfinder_mask)
    private var mPreview: CamPreview? = null
    private var mRoundedCorner = false
    private var mShouldScaleToFill = true
    private var mSquaredFinder = false
    private var mViewFinderOffset = 0
    private var mViewFinderView: IViewFinder? = null

    constructor(context: Context?) : super(context!!) {
        init()
    }

    constructor(context: Context, attributeSet: AttributeSet?) : super(context, attributeSet) {
        val obtainStyledAttributes =
            context.theme.obtainStyledAttributes(attributeSet, R.styleable.BarcodeScannerView, 0, 0)
        try {
            setShouldScaleToFill(
                obtainStyledAttributes.getBoolean(
                    R.styleable.BarcodeScannerView_shouldScaleToFill,
                    true
                )
            )
            mIsLaserEnabled = obtainStyledAttributes.getBoolean(
                R.styleable.BarcodeScannerView_laserEnabled,
                mIsLaserEnabled
            )
            mLaserColor = obtainStyledAttributes.getColor(
                R.styleable.BarcodeScannerView_laserColor,
                mLaserColor
            )
            mBorderColor = obtainStyledAttributes.getColor(
                R.styleable.BarcodeScannerView_borderColor,
                mBorderColor
            )
            mMaskColor = obtainStyledAttributes.getColor(
                R.styleable.BarcodeScannerView_maskColor,
                mMaskColor
            )
            mBorderWidth = obtainStyledAttributes.getDimensionPixelSize(
                R.styleable.BarcodeScannerView_borderWidth,
                mBorderWidth
            )
            mBorderLength = obtainStyledAttributes.getDimensionPixelSize(
                R.styleable.BarcodeScannerView_borderLength,
                mBorderLength
            )
            mRoundedCorner = obtainStyledAttributes.getBoolean(
                R.styleable.BarcodeScannerView_roundedCorner,
                mRoundedCorner
            )
            mCornerRadius = obtainStyledAttributes.getDimensionPixelSize(
                R.styleable.BarcodeScannerView_cornerRadius,
                mCornerRadius
            )
            mSquaredFinder = obtainStyledAttributes.getBoolean(
                R.styleable.BarcodeScannerView_squaredFinder,
                mSquaredFinder
            )
            mBorderAlpha = obtainStyledAttributes.getFloat(
                R.styleable.BarcodeScannerView_barcode_borderAlpha,
                mBorderAlpha
            )
            mViewFinderOffset = obtainStyledAttributes.getDimensionPixelSize(
                R.styleable.BarcodeScannerView_finderOffset,
                mViewFinderOffset
            )
            obtainStyledAttributes.recycle()
            init()
        } catch (th: Throwable) {
            obtainStyledAttributes.recycle()
            throw th
        }
    }

    private fun init() {
        mViewFinderView = createViewFinderView(context)
    }

    fun setupLayout(cameraWrapper: CamWrapper?) {
        removeAllViews()
        mPreview = CamPreview(context, cameraWrapper, this)
        mPreview!!.setAspectTolerance(mAspectTolerance)
        mPreview!!.setShouldScaleToFill(mShouldScaleToFill)
        if (!mShouldScaleToFill) {
            val relativeLayout = RelativeLayout(context)
            relativeLayout.gravity = 17
            relativeLayout.setBackgroundColor(ViewCompat.MEASURED_STATE_MASK)
            relativeLayout.addView(mPreview)
            addView(relativeLayout)
        } else {
            addView(mPreview)
        }
        if (mViewFinderView is View) {
            addView(mViewFinderView as View?)

        } else {
            throw IllegalArgumentException("IViewFinder object returned by 'createViewFinderView()' should be instance of android.view.View")
        }
    }

    fun createViewFinderView(context: Context?): IViewFinder {
        val viewFinderView = ViewFinderView(context)
        viewFinderView.setBorderColor(mBorderColor)
        viewFinderView.setLaserColor(mLaserColor)
        viewFinderView.setLaserEnabled(mIsLaserEnabled)
        viewFinderView.setBorderStrokeWidth(mBorderWidth)
        viewFinderView.setBorderLineLength(mBorderLength)
        viewFinderView.setMaskColor(mMaskColor)
        viewFinderView.setBorderCornerRounded(mRoundedCorner)
        viewFinderView.setBorderCornerRadius(mCornerRadius)
        viewFinderView.setSquareViewFinder(mSquaredFinder)
        viewFinderView.setViewFinderOffset(mViewFinderOffset)
        return viewFinderView
    }

    fun setLaserColor(i: Int) {
        mLaserColor = i
        mViewFinderView!!.setLaserColor(mLaserColor)
        mViewFinderView!!.setupViewFinder()
    }

    fun setMaskColor(i: Int) {
        mMaskColor = i
        mViewFinderView!!.setMaskColor(mMaskColor)
        mViewFinderView!!.setupViewFinder()
    }

    fun setBorderColor(i: Int) {
        mBorderColor = i
        mViewFinderView!!.setBorderColor(mBorderColor)
        mViewFinderView!!.setupViewFinder()
    }

    fun setBorderStrokeWidth(i: Int) {
        mBorderWidth = i
        mViewFinderView!!.setBorderStrokeWidth(mBorderWidth)
        mViewFinderView!!.setupViewFinder()
    }

    fun setBorderLineLength(i: Int) {
        mBorderLength = i
        mViewFinderView!!.setBorderLineLength(mBorderLength)
        mViewFinderView!!.setupViewFinder()
    }

    fun setLaserEnabled(isLaserEnabled: Boolean) {
        mIsLaserEnabled = isLaserEnabled
        mViewFinderView!!.setLaserEnabled(mIsLaserEnabled)
        mViewFinderView!!.setupViewFinder()
    }

    fun setIsBorderCornerRounded(isRoundedCorner: Boolean) {
        mRoundedCorner = isRoundedCorner
        mViewFinderView!!.setBorderCornerRounded(mRoundedCorner)
        mViewFinderView!!.setupViewFinder()
    }

    fun setBorderCornerRadius(i: Int) {
        mCornerRadius = i
        mViewFinderView!!.setBorderCornerRadius(mCornerRadius)
        mViewFinderView!!.setupViewFinder()
    }

    fun setSquareViewFinder(isViewFinder: Boolean) {
        mSquaredFinder = isViewFinder
        mViewFinderView!!.setSquareViewFinder(mSquaredFinder)
        mViewFinderView!!.setupViewFinder()
    }

    fun setBorderAlpha(borderAlpha: Float) {
        mBorderAlpha = borderAlpha
        mViewFinderView!!.setBorderAlpha(mBorderAlpha)
        mViewFinderView!!.setupViewFinder()
    }

    @JvmOverloads // getDefaultCameraId leave it
    fun startCamera(i: Int = getDefaultCameraId()) {
        if (mCameraHandlerThread == null) {
            mCameraHandlerThread = CamHandlerThread(this)
        }
        mCameraHandlerThread!!.startCamera(i)
    }

    fun setupCameraPreview(cameraWrapper: CamWrapper?) {
        mCameraWrapper = cameraWrapper
        if (mCameraWrapper != null) {
            setupLayout(mCameraWrapper)
            mViewFinderView!!.setupViewFinder()
            val bool = mFlashState
            if (bool != null) {
                flash = bool
            }
            setAutoFocus(mAutofocusState)
        }
    }

    fun stopCamera() {
        if (mCameraWrapper != null) {
            mPreview!!.stopCameraPreview()
            mPreview!!.setCamera(null as CamWrapper?, null as PreviewCallback?)
            mCameraWrapper!!.mCamera!!.release()
            mCameraWrapper = null
        }
        if (mCameraHandlerThread != null) {
            mCameraHandlerThread!!.quit()
            mCameraHandlerThread = null
        }
    }

    fun stopCameraPreview() {
        if (mPreview != null) {
            mPreview!!.stopCameraPreview()
        }
    }

    fun resumeCameraPreview() {
        if (mPreview != null) {
            mPreview!!.showCameraPreview()
        }
    }

    @Synchronized
    fun getFramingRectInPreview(i: Int, i2: Int): Rect? {
        if (mFramingRectInPreview == null) {
            val framingRect = mViewFinderView!!.framingRect
            val width = width
            val height = height
            if (!(framingRect == null || width == 0)) {
                if (height != 0) {
                    val rect = Rect(framingRect)
                    if (i < width) {
                        rect.left = rect.left * i / width
                        rect.right = rect.right * i / width
                    }
                    if (i2 < height) {
                        rect.top = rect.top * i2 / height
                        rect.bottom = rect.bottom * i2 / height
                    }
                    mFramingRectInPreview = rect
                }
            }
            return null
        }
        return mFramingRectInPreview
    }

    var flash: Boolean
        get() = if (mCameraWrapper == null || !isFlashSupported(
                mCameraWrapper!!.mCamera
            ) || mCameraWrapper!!.mCamera!!.getParameters().flashMode != "torch"
        ) {
            false
        } else true
        set(z) {
            mFlashState = z
            if (mCameraWrapper != null && isFlashSupported(
                    mCameraWrapper!!.mCamera
                )
            ) {
                val parameters = mCameraWrapper!!.mCamera!!.getParameters()
                if (z) {
                    if (parameters.flashMode != "torch") {
                        parameters.flashMode = "torch"
                    } else {
                        return
                    }
                } else if (parameters.flashMode != Camera.Parameters.FLASH_MODE_OFF) {
                    parameters.flashMode = Camera.Parameters.FLASH_MODE_OFF
                } else {
                    return
                }
                mCameraWrapper!!.mCamera!!.setParameters(parameters)
            }
        }

    fun toggleFlash() {
        if (mCameraWrapper != null && isFlashSupported(
                mCameraWrapper!!.mCamera
            )
        ) {
            val parameters = mCameraWrapper!!.mCamera!!.getParameters()
            if (parameters.flashMode == "torch") {
                parameters.flashMode = Camera.Parameters.FLASH_MODE_OFF
            } else {
                parameters.flashMode = "torch"
            }
            mCameraWrapper!!.mCamera!!.setParameters(parameters)
        }
    }

    fun setAutoFocus(isFocus: Boolean) {
        mAutofocusState = isFocus
        if (mPreview != null) {
            mPreview!!.setAutoFocus(isFocus)
        }
    }

    fun setShouldScaleToFill(isFill: Boolean) {
        mShouldScaleToFill = isFill
    }

    fun setAspectTolerance(f: Float) {
        mAspectTolerance = f
    }

    fun getRotatedData(bArr: ByteArray, camera: Camera): ByteArray {
        val previewSize = camera.getParameters().getPreviewSize()
        var i = previewSize.width
        val i2 = previewSize.height
        val rotationCount = rotationCount
        if (rotationCount != 1 && rotationCount != 3) {
            return bArr
        }
        var i3 = i2
        var bArr2 = bArr
        var i4 = 0
        while (i4 < rotationCount) {
            val bArr3 = ByteArray(bArr2.size)
            for (i5 in 0 until i3) {
                for (i6 in 0 until i) {
                    bArr3[i6 * i3 + i3 - i5 - 1] = bArr2[i5 * i + i6]
                }
            }
            i4++
            bArr2 = bArr3
            val i7 = i3
            i3 = i
            i = i7
        }
        return bArr2
    }

    val rotationCount: Int
        get() = mPreview!!.displayOrientation / 90
}
