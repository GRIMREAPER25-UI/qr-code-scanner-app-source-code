package com.scanner.barcodescanner.qrgenrater.qr_code_maker

import java.util.regex.Pattern

class MECARDContactEncoder : ContactEncoder() {

    companion object {
        private const val TERMINATOR = ';'
    }

    override fun encode(
        names: List<String>,
        organization: String,
        addresses: List<String>,
        phones: List<String>,
        phoneTypes: List<String>,
        emails: List<String>,
        urls: List<String>,
        note: String?
    ): Array<String> {
        val newContents = StringBuilder(100)
        newContents.append("MECARD:")

        val newDisplayContents = StringBuilder(100)

        val fieldFormatter = MECARDFieldFormatter()

        appendUpToUnique(newContents, newDisplayContents, "N", names, 1, MECARDNameDisplayFormatter(), fieldFormatter, TERMINATOR)

        append(newContents, newDisplayContents, "ORG", organization, fieldFormatter, TERMINATOR)

        appendUpToUnique(newContents, newDisplayContents, "ADR", addresses, 1, null, fieldFormatter, TERMINATOR)

        appendUpToUnique(newContents, newDisplayContents, "TEL", phones, Int.MAX_VALUE, MECARDTelDisplayFormatter(), fieldFormatter, TERMINATOR)

        appendUpToUnique(newContents, newDisplayContents, "EMAIL", emails, Int.MAX_VALUE, null, fieldFormatter, TERMINATOR)

        appendUpToUnique(newContents, newDisplayContents, "URL", urls, Int.MAX_VALUE, null, fieldFormatter, TERMINATOR)

        append(newContents, newDisplayContents, "NOTE", note, fieldFormatter, TERMINATOR)

        newContents.append(';')

        return arrayOf(newContents.toString(), newDisplayContents.toString())
    }


    private class  MECARDFieldFormatter : Formatter {
        private val RESERVED_MECARD_CHARS = Pattern.compile("([\\\\:;])")
        private val NEWLINE = Pattern.compile("\\n")
        override fun format(value: CharSequence, index: Int): CharSequence {
            return ':' + NEWLINE.matcher(RESERVED_MECARD_CHARS.matcher(value).replaceAll("\\\\$1")).replaceAll("")
        }


    }

    private class MECARDTelDisplayFormatter : Formatter {
        private val NOT_DIGITS_OR_PLUS = Pattern.compile("[^0-9+]+")
        override fun format(value: CharSequence, index: Int): CharSequence {
            return NOT_DIGITS_OR_PLUS.matcher(formatPhone(value.toString())).replaceAll("")
        }
    }

    private class MECARDNameDisplayFormatter : Formatter {
        private val COMMA = Pattern.compile(",")
        override fun format(value: CharSequence, index: Int): CharSequence {
            return COMMA.matcher(value).replaceAll("")
        }
    }




}






