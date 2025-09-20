package com.scanner.barcodescanner.qrgenrater.qr_code_maker

import android.content.Intent

object Intents {
    private object Scan {
        const val ACTION = "com.google.zxing.client.android.SCAN"
        const val MODE = "SCAN_MODE"
        const val PRODUCT_MODE = "PRODUCT_MODE"
        const val ONE_D_MODE = "ONE_D_MODE"
        const val QR_CODE_MODE = "QR_CODE_MODE"
        const val DATA_MATRIX_MODE = "DATA_MATRIX_MODE"
        const val AZTEC_MODE = "AZTEC_MODE"
        const val PDF417_MODE = "PDF417_MODE"
        const val FORMATS = "SCAN_FORMATS"
        const val CAMERA_ID = "SCAN_CAMERA_ID"
        const val CHARACTER_SET = "CHARACTER_SET"
        const val WIDTH = "SCAN_WIDTH"
        const val HEIGHT = "SCAN_HEIGHT"
        const val RESULT_DISPLAY_DURATION_MS = "RESULT_DISPLAY_DURATION_MS"
        const val PROMPT_MESSAGE = "PROMPT_MESSAGE"
        const val RESULT = "SCAN_RESULT"
        const val RESULT_FORMAT = "SCAN_RESULT_FORMAT"
        const val RESULT_UPC_EAN_EXTENSION = "SCAN_RESULT_UPC_EAN_EXTENSION"
        const val RESULT_BYTES = "SCAN_RESULT_BYTES"
        const val RESULT_ORIENTATION = "SCAN_RESULT_ORIENTATION"
        const val RESULT_ERROR_CORRECTION_LEVEL = "SCAN_RESULT_ERROR_CORRECTION_LEVEL"
        const val RESULT_BYTE_SEGMENTS_PREFIX = "SCAN_RESULT_BYTE_SEGMENTS_"
        const val SAVE_HISTORY = "SAVE_HISTORY"
    }

    object History {
        const val ITEM_NUMBER = "ITEM_NUMBER"
    }

    object Encode {
        const val ACTION = "com.google.zxing.client.android.ENCODE"
        const val DATA = "ENCODE_DATA"
        const val TYPE = "ENCODE_TYPE"
        const val FORMAT = "ENCODE_FORMAT"
        const val SHOW_CONTENTS = "ENCODE_SHOW_CONTENTS"
    }

    object SearchBookContents {
        const val ACTION = "com.google.zxing.client.android.SEARCH_BOOK_CONTENTS"
        const val ISBN = "ISBN"
        const val QUERY = "QUERY"
    }

    object WifiConnect {
        const val ACTION = "com.google.zxing.client.android.WIFI_CONNECT"
        const val SSID = "SSID"
        const val TYPE = "TYPE"
        const val PASSWORD = "PASSWORD"
    }

    object Share {
        const val ACTION = "com.google.zxing.client.android.SHARE"
    }

    const val FLAG_NEW_DOC = Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET
}