package com.scanner.barcodescanner.qrgenrater.qr_code_maker


import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.Result
import com.google.zxing.WriterException
import com.google.zxing.client.result.AddressBookParsedResult
import com.google.zxing.client.result.ResultParser
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.scanner.barcodescanner.qrgenrater.R
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.EnumMap


class QRCodeEncoders(
    private val activity: Context,
    intent: Intent,
    private val dimension: Int,
    private val useVCard: Boolean
) {
    private var contents: String? = null
    private var displayContents: String? = null
    private var title: String? = null
    private var format: BarcodeFormat? = null

    companion object {
        private const val WHITE = 0xFFFFFFFF.toInt()
        private const val BLACK = 0xFF000000.toInt()
    }

    init {
        val action = intent.action
        if (Intents.Encode.ACTION == action) {
            encodeContentsFromZXingIntent(intent)
        } else if (Intent.ACTION_SEND == action) {
            encodeContentsFromShareIntent(intent)
        }
    }

    fun getContents(): String? {
        return contents
    }

    fun getDisplayContents(): String? {
        return displayContents
    }

    fun getTitle(): String? {
        return title
    }

    fun isUseVCard(): Boolean {
        return useVCard
    }

    private fun encodeContentsFromZXingIntent(intent: Intent) {
        val formatString = intent.getStringExtra(Intents.Encode.FORMAT)
        format = null
        if (formatString != null) {
            format = try {
                BarcodeFormat.valueOf(formatString)
            } catch (iae: IllegalArgumentException) {
                null
            }
        }
        if (format == null || format == BarcodeFormat.QR_CODE) {
            val type = intent.getStringExtra(Intents.Encode.TYPE)
            if (!type.isNullOrEmpty()) {
                format = BarcodeFormat.QR_CODE
                encodeQRCodeContents(intent, type)
            }
        } else {
            val data = intent.getStringExtra(Intents.Encode.DATA)
            if (!data.isNullOrEmpty()) {
                contents = data
                displayContents = data
                title = activity.getString(R.string.contents_text)
            }
        }
    }

    private fun encodeContentsFromShareIntent(intent: Intent) {
        if (intent.hasExtra(Intent.EXTRA_STREAM)) {
            encodeFromStreamExtra(intent)
        } else {
            encodeFromTextExtras(intent)
        }
    }

    private fun encodeFromTextExtras(intent: Intent) {
        var theContents = ContactEncoder.trim(intent.getStringExtra(Intent.EXTRA_TEXT))
        if (theContents.isNullOrEmpty()) {
            theContents = ContactEncoder.trim(intent.getStringExtra("android.intent.extra.HTML_TEXT"))
            if (theContents.isNullOrEmpty()) {
                theContents = ContactEncoder.trim(intent.getStringExtra(Intent.EXTRA_SUBJECT))
                if (theContents.isNullOrEmpty()) {
                    val emails = intent.getStringArrayExtra(Intent.EXTRA_EMAIL)
                    theContents = emails?.get(0) ?: "?"
                }
            }
        }

        if (theContents.isNullOrEmpty()) {
            throw WriterException("Empty EXTRA_TEXT")
        }
        contents = theContents
        format = BarcodeFormat.QR_CODE
        displayContents = if (intent.hasExtra(Intent.EXTRA_SUBJECT)) {
            intent.getStringExtra(Intent.EXTRA_SUBJECT)
        } else if (intent.hasExtra(Intent.EXTRA_TITLE)) {
            intent.getStringExtra(Intent.EXTRA_TITLE)
        } else {
            contents
        }
        title = activity.getString(R.string.contents_text)
    }

    @Throws(WriterException::class)
    private fun encodeFromStreamExtra(intent: Intent) {
        format = BarcodeFormat.QR_CODE
        val bundle = intent.extras ?: throw WriterException("No extras")
        val uri = bundle.getParcelable<Uri>(Intent.EXTRA_STREAM)
            ?: throw WriterException("No EXTRA_STREAM")
        var vcard: ByteArray
        var vcardString: String
        try {
            activity.contentResolver.openInputStream(uri).use { stream ->
                if (stream == null) {
                    throw WriterException("Can't open stream for $uri")
                }
                val baos = ByteArrayOutputStream()
                val buffer = ByteArray(2048)
                var bytesRead: Int
                while (stream.read(buffer).also { bytesRead = it } > 0) {
                    baos.write(buffer, 0, bytesRead)
                }
                vcard = baos.toByteArray()
                vcardString =
                    String(vcard, 0, vcard.size, StandardCharsets.UTF_8)
            }
        } catch (ioe: IOException) {
            throw WriterException(ioe)
        }
        val result = Result(vcardString, vcard, null, BarcodeFormat.QR_CODE)
        val parsedResult = ResultParser.parseResult(result) as? AddressBookParsedResult
            ?: throw WriterException("Result was not an address")
        encodeQRCodeContents(parsedResult)
        if (contents == null || contents!!.isEmpty()) {
            throw WriterException("No content to encode")
        }
    }


    private fun encodeQRCodeContents(intent: Intent, type: String) {
        when (type) {
            Contents.Type.TEXT -> {
                val textData = intent.getStringExtra(Intents.Encode.DATA)
                if (textData != null && !textData.isEmpty()) {
                    contents = textData
                    displayContents = textData
                    title = activity.getString(R.string.contents_text)
                }
            }

            Contents.Type.EMAIL -> {
                val emailData = ContactEncoder.trim(intent.getStringExtra(Intents.Encode.DATA))
                if (emailData != null) {
                    contents = "mailto:$emailData"
                    displayContents = emailData
                    title = activity.getString(R.string.contents_email)
                }
            }

            Contents.Type.PHONE -> {
                val phoneData = ContactEncoder.trim(intent.getStringExtra(Intents.Encode.DATA))
                if (phoneData != null) {
                    contents = "tel:$phoneData"
                    displayContents = ContactEncoder.formatPhone(phoneData)
                    title = activity.getString(R.string.contents_phone)
                }
            }

            Contents.Type.SMS -> {
                val smsData = ContactEncoder.trim(intent.getStringExtra(Intents.Encode.DATA))
                if (smsData != null) {
                    contents = "sms:$smsData"
                    displayContents = ContactEncoder.formatPhone(smsData)
                    title = activity.getString(R.string.contents_sms)
                }
            }

            Contents.URL_KEY -> {
                val urlData = intent.getStringExtra(Intents.Encode.DATA)
                if (!urlData.isNullOrEmpty()) {
                    contents = urlData
                    displayContents = urlData
                    title = "URL"
                }
            }

            Contents.Type.CONTACT -> {
                val contactBundle = intent.getBundleExtra(Intents.Encode.DATA)
                if (contactBundle != null) {
                    val name = contactBundle.getString(ContactsContract.Intents.Insert.NAME)
                    val organization =
                        contactBundle.getString(ContactsContract.Intents.Insert.COMPANY)
                    val address = contactBundle.getString(ContactsContract.Intents.Insert.POSTAL)
                    val phones: List<String> =
                        getAllBundleValues(contactBundle, Contents.PHONE_KEYS)
                    val phoneTypes: List<String> =
                        getAllBundleValues(contactBundle, Contents.PHONE_TYPE_KEYS)
                    val emails: List<String> =
                        getAllBundleValues(contactBundle, Contents.EMAIL_KEYS)
                    val url = contactBundle.getString(Contents.URL_KEY)
                    val urls = if (url == null) null else listOf(url)
                    val note = contactBundle.getString(Contents.NOTE_KEY)
                    val encoder = if (useVCard) CardContactEncoder() else MECARDContactEncoder()
                    val encoded = encoder.encode(
                        listOf(name!!),
                        organization!!, listOf(address!!),
                        phones,
                        phoneTypes,
                        emails,
                        urls!!,
                        note!!
                    )
                    // Make sure we've encoded at least one field.
                    if (encoded[1].isNotEmpty()) {
                        contents = encoded[0]
                        displayContents = encoded[1]
                        title = activity.getString(R.string.contents_contact)
                    }
                }
            }

            Contents.Type.LOCATION -> {
                val locationBundle = intent.getBundleExtra(Intents.Encode.DATA)
                if (locationBundle != null) {
                    // These must use Bundle.getFloat(), not getDouble(), it's part of the API.
                    val latitude = locationBundle.getFloat("LAT", Float.MAX_VALUE)
                    val longitude = locationBundle.getFloat("LONG", Float.MAX_VALUE)
                    if (latitude != Float.MAX_VALUE && longitude != Float.MAX_VALUE) {
                        contents = "geo:$latitude,$longitude"
                        displayContents = "$latitude,$longitude"
                        title = activity.getString(R.string.contents_location)
                    }
                }
            }
        }
    }

    private fun getAllBundleValues(bundle: Bundle, keys: Array<String>): List<String> {
        val values = ArrayList<String>(keys.size)
        for (key in keys) {
            val value = bundle.get(key)
            values.add(value.toString())
        }
        return values
    }

    private fun encodeQRCodeContents(contact: AddressBookParsedResult) {
        val encoder: ContactEncoder = if (useVCard) CardContactEncoder() else MECARDContactEncoder()
        val encoded = encoder.encode(
            toList(contact.names!!),
            contact.org,
            toList(contact.addresses),
            toList(contact.phoneNumbers),
            toList(contact.phoneTypes),
            toList(contact.emails),
            toList(contact.urLs),
            null
        )
        if (encoded[1].isNotEmpty()) {
            contents = encoded[0]
            displayContents = encoded[1]
            title = activity.getString(R.string.contents_contact)
        }
    }

    private fun toList(values: Array<String>): List<String> {
        return values.toList()
    }

    @Throws(WriterException::class)
    fun encodeAsBitmap(): Bitmap? {
        val contentsToEncode = contents
        if (contentsToEncode.isNullOrEmpty()) {
            return null
        }
        val hints: MutableMap<EncodeHintType, Any>? = guessAppropriateEncoding(contentsToEncode)?.let {
            EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                put(EncodeHintType.CHARACTER_SET, it)
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L)
            }
        }
        val result: BitMatrix
        result = try {
            QRCodeWriter().encode(contentsToEncode, format, dimension, dimension, hints)
        } catch (iae: IllegalArgumentException) {
            return null
        }
        val width = result.width
        val height = result.height
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (result[x, y]) BLACK else WHITE
            }
        }

        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, width, 0, 0, width, height)
        }
    }

    private fun guessAppropriateEncoding(contents: CharSequence): String? {
        for (i in 0 until contents.length) {
            if (contents[i].toInt() > 0xFF) {
                return "UTF-8"
            }
        }
        return null
    }
}