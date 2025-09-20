package com.scanner.barcodescanner.qrgenrater.qr_code_maker

import android.provider.ContactsContract

object Contents {
    object Type {
        const val TEXT = "TEXT_TYPE"
        const val EMAIL = "EMAIL_TYPE"
        const val PHONE = "PHONE_TYPE"
        const val SMS = "SMS_TYPE"
        const val CONTACT = "CONTACT_TYPE"
        const val LOCATION = "LOCATION_TYPE"
    }

    const val URL_KEY = "URL_KEY"
    const val NOTE_KEY = "NOTE_KEY"

    val PHONE_KEYS = arrayOf(
        ContactsContract.Intents.Insert.PHONE,
        ContactsContract.Intents.Insert.SECONDARY_PHONE,
        ContactsContract.Intents.Insert.TERTIARY_PHONE
    )

    val PHONE_TYPE_KEYS = arrayOf(
        ContactsContract.Intents.Insert.PHONE_TYPE,
        ContactsContract.Intents.Insert.SECONDARY_PHONE_TYPE,
        ContactsContract.Intents.Insert.TERTIARY_PHONE_TYPE
    )

    val EMAIL_KEYS = arrayOf(
        ContactsContract.Intents.Insert.EMAIL,
        ContactsContract.Intents.Insert.SECONDARY_EMAIL,
        ContactsContract.Intents.Insert.TERTIARY_EMAIL
    )

    val EMAIL_TYPE_KEYS = arrayOf(
        ContactsContract.Intents.Insert.EMAIL_TYPE,
        ContactsContract.Intents.Insert.SECONDARY_EMAIL_TYPE,
        ContactsContract.Intents.Insert.TERTIARY_EMAIL_TYPE
    )
}