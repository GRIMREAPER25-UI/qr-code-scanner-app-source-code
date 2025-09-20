package com.scanner.barcodescanner.qrgenrater.qr_code_maker

class CardTelDisplayFormatter(
    private val metadataForIndex: List<Map<String, Set<String>?>?>? = null
) : Formatter {

    override fun format(value: CharSequence, index: Int): CharSequence {
        var formattedValue = ContactEncoder.formatPhone(value.toString())
        val metadata = metadataForIndex?.getOrNull(index)
        return formatMetadata(formattedValue, metadata)
    }

    private fun formatMetadata(value: CharSequence, metadata: Map<String, Set<String>?>?): CharSequence {
        if (metadata.isNullOrEmpty()) {
            return value
        }
        val withMetadata = StringBuilder()
        metadata.forEach { (_, values) ->
            values?.forEach { withMetadata.append("$it,") }
        }
        if (withMetadata.isNotEmpty()) {
            withMetadata.deleteCharAt(withMetadata.length - 1)
            withMetadata.append(' ')
        }
        withMetadata.append(value)
        return withMetadata
    }
}