package com.scanner.barcodescanner.qrgenrater.qr_code_reader

import android.graphics.Rect


interface IViewFinder {

    val framingRect: Rect?

    fun setBorderAlpha(borderAlpha: Float)
    fun setBorderColor(i: Int)
    fun setBorderCornerRadius(i: Int)
    fun setBorderCornerRounded(isCornerRounded: Boolean)
    fun setBorderLineLength(i: Int)
    fun setBorderStrokeWidth(i: Int)
    fun setLaserColor(i: Int)
    fun setLaserEnabled(isLaserEnabled: Boolean)
    fun setMaskColor(i: Int)
    fun setSquareViewFinder(isViewFinder: Boolean)
    fun setViewFinderOffset(i: Int)
    fun setupViewFinder()
}
