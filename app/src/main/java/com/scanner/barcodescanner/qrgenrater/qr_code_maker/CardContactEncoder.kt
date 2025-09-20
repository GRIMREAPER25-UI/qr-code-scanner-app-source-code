package com.scanner.barcodescanner.qrgenrater.qr_code_maker

import android.provider.ContactsContract

class CardContactEncoder : ContactEncoder() {
    private companion object {
        private const val TERMINATOR = '\n'
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
        newContents.append("BEGIN:VCARD").append(TERMINATOR)
        newContents.append("VERSION:3.0").append(TERMINATOR)

        val newDisplayContents = StringBuilder(100)

        val fieldFormatter = CardFieldFormatter()

        appendUpToUnique(
            newContents, newDisplayContents, "N", names, 1, null, fieldFormatter, TERMINATOR
        )

        append(newContents, newDisplayContents, "ORG", organization, fieldFormatter, TERMINATOR)

        appendUpToUnique(
            newContents, newDisplayContents, "ADR", addresses, 1, null, fieldFormatter, TERMINATOR
        )

        val phoneMetadata = buildPhoneMetadata(phones, phoneTypes!!)
        appendUpToUnique(
            newContents, newDisplayContents, "TEL", phones, Int.MAX_VALUE,
            CardTelDisplayFormatter(phoneMetadata),
            CardFieldFormatter(phoneMetadata), TERMINATOR
        )

        appendUpToUnique(
            newContents, newDisplayContents, "EMAIL", emails, Int.MAX_VALUE, null,
            fieldFormatter, TERMINATOR
        )

        appendUpToUnique(
            newContents, newDisplayContents, "URL", urls, Int.MAX_VALUE, null,
            fieldFormatter, TERMINATOR
        )

        append(newContents, newDisplayContents, "NOTE", note, fieldFormatter, TERMINATOR)

        newContents.append("END:VCARD").append(TERMINATOR)

        return arrayOf(newContents.toString(), newDisplayContents.toString())
    }

    private fun buildPhoneMetadata(
        phones: Collection<String>,
        phoneTypes: List<String>
    ): List<Map<String, Set<String>?>?> {
        if (phoneTypes.isEmpty()) {
            return emptyList()
        }
        val metadataForIndex = mutableListOf<Map<String, Set<String>?>?>()
        for (i in phones.indices) {
            if (phoneTypes.size <= i) {
                metadataForIndex.add(null)
            } else {
                val metadata = mutableMapOf<String, Set<String>?>()
                metadataForIndex.add(metadata)
                val typeTokens = mutableSetOf<String>()
                metadata["TYPE"] = typeTokens
                val typeString = phoneTypes[i]
                val androidType = maybeIntValue(typeString)
                if (androidType == null) {
                    typeTokens.add(typeString)
                } else {
                    val purpose = vCardPurposeLabelForAndroidType(androidType)
                    val context = vCardContextLabelForAndroidType(androidType)
                    purpose?.let { typeTokens.add(it) }
                    context?.let { typeTokens.add(it) }
                }
            }
        }
        return metadataForIndex
    }

    private fun maybeIntValue(value: String): Int? {
        return try {
            value.toInt()
        } catch (nfe: NumberFormatException) {
            null
        }
    }

    private fun vCardPurposeLabelForAndroidType(androidType: Int): String? {
        return when (androidType) {
            ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME,
            ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK,
            ContactsContract.CommonDataKinds.Phone.TYPE_OTHER_FAX -> "fax"
            ContactsContract.CommonDataKinds.Phone.TYPE_PAGER,
            ContactsContract.CommonDataKinds.Phone.TYPE_WORK_PAGER -> "pager"
            ContactsContract.CommonDataKinds.Phone.TYPE_TTY_TDD -> "textphone"
            ContactsContract.CommonDataKinds.Phone.TYPE_MMS -> "text"
            else -> null
        }
    }

    private fun vCardContextLabelForAndroidType(androidType: Int): String? {
        return when (androidType) {
            ContactsContract.CommonDataKinds.Phone.TYPE_HOME,
            ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
            ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME,
            ContactsContract.CommonDataKinds.Phone.TYPE_PAGER -> "home"
            ContactsContract.CommonDataKinds.Phone.TYPE_COMPANY_MAIN,
            ContactsContract.CommonDataKinds.Phone.TYPE_WORK,
            ContactsContract.CommonDataKinds.Phone.TYPE_WORK_MOBILE,
            ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK,
            ContactsContract.CommonDataKinds.Phone.TYPE_WORK_PAGER -> "work"
            else -> null
        }
    }
}