package com.scanner.barcodescanner.qrgenrater.qr_code_reader

import android.content.Context
import android.graphics.Point
import android.os.Build
import android.view.WindowManager

object DisplayUtil {
    fun getScreenResolution(context: Context): Point {
        val defaultDisplay =
            (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        val point = Point()
        if (Build.VERSION.SDK_INT >= 13) {
            defaultDisplay.getSize(point)
        } else {
            point[defaultDisplay.width] = defaultDisplay.height
        }
        return point
    }

    fun getScreenOrientation(context: Context): Int {
        val defaultDisplay =
            (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        if (defaultDisplay.width == defaultDisplay.height) {
            return 3
        }
        return if (defaultDisplay.width < defaultDisplay.height) 1 else 2
    }
}
