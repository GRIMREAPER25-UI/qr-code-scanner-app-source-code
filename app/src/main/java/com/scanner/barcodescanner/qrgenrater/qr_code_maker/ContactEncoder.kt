package com.scanner.barcodescanner.qrgenrater.qr_code_maker

import android.telephony.PhoneNumberUtils

abstract class ContactEncoder {

    abstract fun encode(
        names: List<String>,
        organization: String,
        addresses: List<String>,
        phones: List<String>,
        phoneTypes: List<String>,
        emails: List<String>,
        urls: List<String>,
        note: String?
    ): Array<String>

    companion object {
        fun trim(s: String?): String? {
            return s?.trim().takeUnless { it.isNullOrEmpty() }
        }

        fun append(
            newContents: StringBuilder,
            newDisplayContents: StringBuilder,
            prefix: String,
            value: String?,
            fieldFormatter: Formatter,
            terminator: Char
        ) {
            trim(value)?.let {
                newContents.append(prefix).append(fieldFormatter.format(it, 0)).append(terminator)
                newDisplayContents.append(it).append('\n')
            }
        }

        fun appendUpToUnique(
            newContents: StringBuilder,
            newDisplayContents: StringBuilder,
            prefix: String,
            values: List<String>?,
            max: Int,
            displayFormatter: Formatter?,
            fieldFormatter: Formatter,
            terminator: Char
        ) {
            values?.mapNotNull { trim(it) }
                .orEmpty()
                .distinctBy { it }
                .take(max)
                .forEach { value ->
                    newContents.append(prefix).append(fieldFormatter.format(value, values!!.indexOf(value))).append(terminator)
                    val display = displayFormatter?.format(value, values.indexOf(value)) ?: value
                    newDisplayContents.append(display).append('\n')
                }
        }

        @Suppress("DEPRECATION")
        fun formatPhone(phoneData: String): String {
            return PhoneNumberUtils.formatNumber(phoneData)
        }
    }
}