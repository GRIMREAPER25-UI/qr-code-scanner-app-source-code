package com.scanner.barcodescanner.qrgenrater.qr_code_maker

class CardFieldFormatter(
    private val metadataForIndex: List<Map<String, Set<String>?>?>? = null
) : Formatter {

    companion object {
        private val RESERVED_VCARD_CHARS = Regex("([\\\\,;])")
        private val NEWLINE = Regex("\\n")
    }

    override fun format(value: CharSequence, index: Int): CharSequence {
        var formattedValue = value.replace(RESERVED_VCARD_CHARS, "\\\\$1")
        formattedValue = formattedValue.replace(NEWLINE, "")
        val metadata = metadataForIndex?.getOrNull(index)
        return formatMetadata(formattedValue, metadata)
    }

    private fun formatMetadata(value: CharSequence, metadata: Map<String, Set<String>?>?): CharSequence {
        val withMetadata = StringBuilder()
        metadata?.forEach { (key, values) ->
            if (values == null || values.isEmpty()) return@forEach
            withMetadata.append(';').append(key).append('=')
            if (values.size > 1) {
                withMetadata.append('"')
            }
            val valuesIterator = values.iterator()
            withMetadata.append(valuesIterator.next())
            while (valuesIterator.hasNext()) {
                withMetadata.append(',').append(valuesIterator.next())
            }
            if (values.size > 1) {
                withMetadata.append('"')
            }
        }
        withMetadata.append(':').append(value)
        return withMetadata
    }
}