package com.scanner.barcodescanner.qrgenrater.qr_code_reader

import android.content.Context
import android.graphics.Canvas
import android.graphics.CornerPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import com.scanner.barcodescanner.qrgenrater.R
import com.scanner.barcodescanner.qrgenrater.qr_code_reader.DisplayUtil.getScreenOrientation

class ViewFinderView : View, IViewFinder {

    companion object {
        private const val ANIMATION_DELAY: Long = 80
        private const val DEFAULT_SQUARE_DIMENSION_RATIO = 0.625f
        private const val LANDSCAPE_HEIGHT_RATIO = 0.625f
        private const val LANDSCAPE_WIDTH_HEIGHT_RATIO = 1.4f
        private const val MIN_DIMENSION_DIFF = 50
        private const val POINT_SIZE = 10
        private const val PORTRAIT_WIDTH_HEIGHT_RATIO = 0.75f
        private const val PORTRAIT_WIDTH_RATIO = 0.75f
        private val SCANNER_ALPHA = intArrayOf(0, 64, 128, 192, 255, 192, 128, 64)
        private const val TAG = "ViewFinderView"
    }

    protected var mBorderLineLength = 0
    protected var mBorderPaint: Paint? = null
    protected var mBordersAlpha = 0f
    private val mDefaultBorderColor = resources.getColor(R.color.viewfinder_border)
    private val mDefaultBorderLineLength = resources.getInteger(R.integer.viewfinder_border_length)
    private val mDefaultBorderStrokeWidth = resources.getInteger(R.integer.viewfinder_border_width)
    private val mDefaultLaserColor = resources.getColor(R.color.viewfinder_laser)
    private val mDefaultMaskColor = resources.getColor(R.color.viewfinder_mask)
    protected var mFinderMaskPaint: Paint? = null

    override var framingRect: Rect? = null

    private var mIsLaserEnabled = false
    protected var mLaserPaint: Paint? = null
    protected var mSquareViewFinder = false
    private var mViewFinderOffset = 0
    private var scannerAlpha = 0

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attributeSet: AttributeSet?) : super(context, attributeSet) {
        init()
    }

    private fun init() {
        mLaserPaint = Paint()
        mLaserPaint!!.setColor(mDefaultLaserColor)
        mLaserPaint!!.style = Paint.Style.FILL
        mFinderMaskPaint = Paint()
        mFinderMaskPaint!!.setColor(mDefaultMaskColor)
        mBorderPaint = Paint()
        mBorderPaint!!.setColor(mDefaultBorderColor)
        mBorderPaint!!.style = Paint.Style.STROKE
        mBorderPaint!!.strokeWidth = mDefaultBorderStrokeWidth.toFloat()
        mBorderPaint!!.isAntiAlias = true
        mBorderLineLength = mDefaultBorderLineLength
    }

    override fun setLaserColor(i: Int) {
        mLaserPaint!!.setColor(i)
    }

    override fun setMaskColor(i: Int) {
        mFinderMaskPaint!!.setColor(i)
    }

    override fun setBorderColor(i: Int) {
        mBorderPaint!!.setColor(i)
    }

    override fun setBorderStrokeWidth(i: Int) {
        mBorderPaint!!.strokeWidth = i.toFloat()
    }

    override fun setBorderLineLength(i: Int) {
        mBorderLineLength = i
    }

    override fun setLaserEnabled(z: Boolean) {
        mIsLaserEnabled = z
    }

    override fun setBorderCornerRounded(isCornerRounded: Boolean) {
        if (isCornerRounded) {
            mBorderPaint!!.strokeJoin = Paint.Join.ROUND
        } else {
            mBorderPaint!!.strokeJoin = Paint.Join.BEVEL
        }
    }

    override fun setBorderAlpha(f: Float) {
        mBordersAlpha = f
        mBorderPaint!!.setAlpha((255.0f * f).toInt())
    }

    override fun setBorderCornerRadius(i: Int) {
        mBorderPaint!!.setPathEffect(CornerPathEffect(i.toFloat()))
    }

    override fun setViewFinderOffset(i: Int) {
        mViewFinderOffset = i
    }

    override fun setSquareViewFinder(isViewFinder: Boolean) {
        mSquareViewFinder = isViewFinder
    }

    override fun setupViewFinder() {
        updateFramingRect()
        invalidate()
    }

    public override fun onDraw(canvas: Canvas) {
        if (framingRect != null) {
            drawViewFinderMask(canvas)
            drawViewFinderBorder(canvas)
            if (mIsLaserEnabled) {
                drawLaser(canvas)
            }
        }
    }

    fun drawViewFinderMask(canvas: Canvas) {
        val width = canvas.width
        val height = canvas.height
        val framingRect = framingRect!!
        val f = width.toFloat()
        canvas.drawRect(0.0f, 0.0f, f, framingRect.top.toFloat(), mFinderMaskPaint!!)
        canvas.drawRect(
            0.0f,
            framingRect.top.toFloat(),
            framingRect.left.toFloat(),
            (framingRect.bottom + 1).toFloat(),
            mFinderMaskPaint!!
        )
        canvas.drawRect(
            (framingRect.right + 1).toFloat(),
            framingRect.top.toFloat(),
            f,
            (framingRect.bottom + 1).toFloat(),
            mFinderMaskPaint!!
        )
        canvas.drawRect(
            0.0f,
            (framingRect.bottom + 1).toFloat(),
            f,
            height.toFloat(),
            mFinderMaskPaint!!
        )
    }

    fun drawViewFinderBorder(canvas: Canvas) {
        val framingRect = framingRect!!
        val path = Path()
        path.moveTo(framingRect.left.toFloat(), (framingRect.top + mBorderLineLength).toFloat())
        path.lineTo(framingRect.left.toFloat(), framingRect.top.toFloat())
        path.lineTo((framingRect.left + mBorderLineLength).toFloat(), framingRect.top.toFloat())
        canvas.drawPath(path, mBorderPaint!!)
        path.moveTo(framingRect.right.toFloat(), (framingRect.top + mBorderLineLength).toFloat())
        path.lineTo(framingRect.right.toFloat(), framingRect.top.toFloat())
        path.lineTo((framingRect.right - mBorderLineLength).toFloat(), framingRect.top.toFloat())
        canvas.drawPath(path, mBorderPaint!!)
        path.moveTo(framingRect.right.toFloat(), (framingRect.bottom - mBorderLineLength).toFloat())
        path.lineTo(framingRect.right.toFloat(), framingRect.bottom.toFloat())
        path.lineTo((framingRect.right - mBorderLineLength).toFloat(), framingRect.bottom.toFloat())
        canvas.drawPath(path, mBorderPaint!!)
        path.moveTo(framingRect.left.toFloat(), (framingRect.bottom - mBorderLineLength).toFloat())
        path.lineTo(framingRect.left.toFloat(), framingRect.bottom.toFloat())
        path.lineTo((framingRect.left + mBorderLineLength).toFloat(), framingRect.bottom.toFloat())
        canvas.drawPath(path, mBorderPaint!!)
    }

    fun drawLaser(canvas: Canvas) {
        val framingRect = framingRect!!
        mLaserPaint!!.setAlpha(SCANNER_ALPHA[scannerAlpha])
        scannerAlpha = (scannerAlpha + 1) % SCANNER_ALPHA.size
        val height = framingRect.height() / 2 + framingRect.top
        canvas.drawRect(
            (framingRect.left + 2).toFloat(),
            (height - 1).toFloat(),
            (framingRect.right - 1).toFloat(),
            (height + 2).toFloat(),
            mLaserPaint!!
        )
        postInvalidateDelayed(
            ANIMATION_DELAY,
            framingRect.left - 10,
            framingRect.top - 10,
            framingRect.right + 10,
            framingRect.bottom + 10
        )
    }

    public override fun onSizeChanged(i: Int, i2: Int, i3: Int, i4: Int) {
        updateFramingRect()
    }

    @Synchronized
    fun updateFramingRect() {
        var i: Int
        var i2: Int
        val i3: Int
        val point = Point(getWidth(), getHeight())
        val screenOrientation = getScreenOrientation(context)
        if (mSquareViewFinder) {
            i3 = if (screenOrientation != 1) {
                getHeight()
            } else {
                getWidth()
            }
            i2 = (i3.toFloat() * 0.625f).toInt()
            i = i2
        } else if (screenOrientation != 1) {
            val height = (getHeight().toFloat() * 0.625f).toInt()
            i = height
            i2 = (height.toFloat() * LANDSCAPE_WIDTH_HEIGHT_RATIO).toInt()
        } else {
            i2 = (getWidth().toFloat() * 0.75f).toInt()
            i = (i2.toFloat() * 0.75f).toInt()
        }
        if (i2 > getWidth()) {
            i2 = getWidth() - 50
        }
        if (i > getHeight()) {
            i = getHeight() - 50
        }
        val i4 = (point.x - i2) / 2
        val i5 = (point.y - i) / 2
        framingRect = Rect(
            mViewFinderOffset + i4,
            mViewFinderOffset + i5,
            i4 + i2 - mViewFinderOffset,
            i5 + i - mViewFinderOffset
        )
    }
}
