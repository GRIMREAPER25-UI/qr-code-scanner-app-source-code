package com.scanner.barcodescanner.qrgenrater.qr_code_maker

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.InputType
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.SpinnerAdapter
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.scanner.barcodescanner.qrgenrater.R
import com.scanner.barcodescanner.qrgenrater.databinding.ActivityQrcodeMakerBinding
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ActivityQRcodeMaker : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivityQrcodeMakerBinding
    private lateinit var adView: AdView
    private val TAG = "QRGenerator"
    private var qrImg: Bitmap? = null
    private var qrPath: File? = null
    var qrType: String = Contents.Type.TEXT
    protected var qr_value: String = ""
    private val spinner_item = arrayOf("Text", "E-mail", "Phone", "Sms", "Url_Key")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrcodeMakerBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // تهيئة SDK الإعلانات
        MobileAds.initialize(this) {}

        // إعداد إعلان البانر
        setupBannerAd()

        binding.toolbar.mToolBarThumb.setOnClickListener { onBackPressed() }
        binding.toolbar.mToolBarText.text = "QR Generator"

        init()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isEnabled) {
                    isEnabled = false
                    finish()
                }
            }
        })
    }

    private fun setupBannerAd() {
        adView = binding.adView
        val adRequest = AdRequest.Builder().build()
        
        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                super.onAdLoaded()
                Log.d(TAG, "Banner ad loaded successfully")
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                super.onAdFailedToLoad(error)
                Log.e(TAG, "Banner ad failed to load: ${error.message}")
            }
        }
        
        adView.loadAd(adRequest)
    }

    override fun onPause() {
        adView.pause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        adView.resume()
    }

    override fun onDestroy() {
        adView.destroy()
        super.onDestroy()
    }

    private fun init() {
        qrType = Contents.Type.TEXT
        initSpinner()
    }

    private fun initSpinner() {
        binding.qrtypeSpinner.adapter = CustomSpinnerAdapter(spinner_item)
        binding.qrtypeSpinner.setPopupBackgroundDrawable(ColorDrawable(getResources().getColor(R.color.white)))


        binding.qrtypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(adapterView: AdapterView<*>?) {}

            override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, i: Int, l: Long) {
                qrType = when (i) {
                    0 -> Contents.Type.TEXT
                    1 -> Contents.Type.EMAIL
                    2 -> Contents.Type.PHONE
                    3 -> Contents.Type.SMS
                    4 -> Contents.URL_KEY
                    else -> Contents.Type.TEXT
                }
                binding.etValue.hint = when (qrType) {
                    Contents.Type.TEXT -> "Enter your text"
                    Contents.Type.EMAIL -> "Enter your e-mail"
                    Contents.Type.PHONE -> "Enter your phone"
                    Contents.Type.SMS -> "Enter your sms"
                    Contents.URL_KEY -> "Enter your url_key"
                    else -> "Enter your text"
                }
                binding.etValue.inputType = when (qrType) {
                    Contents.Type.TEXT -> InputType.TYPE_CLASS_TEXT
                    Contents.Type.EMAIL -> InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                    Contents.Type.PHONE -> InputType.TYPE_CLASS_PHONE
                    else -> InputType.TYPE_CLASS_TEXT
                }
            }
        }
    }

    inner class CustomSpinnerAdapter(private val array: Array<String>) : BaseAdapter(), SpinnerAdapter {
        override fun getItemId(i: Int): Long = i.toLong()

        override fun getCount(): Int = array.size

        override fun getItem(i: Int): Any = array[i]

        override fun getView(i: Int, view: View?, viewGroup: ViewGroup?): View {
            val textView = TextView(this@ActivityQRcodeMaker)
            textView.text = array[i]
            textView.textSize = 14f
            textView.typeface = Typeface.createFromAsset(assets, "inter_medium.ttf")
            textView.setTextColor(resources.getColor(R.color.black))
            textView.gravity = Gravity.CENTER
            textView.setPadding(43, 15, 43, 15)
            return textView
        }

        override fun getDropDownView(i: Int, view: View?, viewGroup: ViewGroup?): View {
            val textView = TextView(this@ActivityQRcodeMaker)
            textView.text = array[i]
            textView.textSize = 16f
            textView.typeface = Typeface.createFromAsset(assets, "inter_medium.ttf")
            textView.setTextColor(resources.getColor(R.color.black))
            textView.gravity = Gravity.START
            textView.setPadding(60, 35, 0x00A0, 35)
            return textView
        }
    }

    override fun onClick(view: View?) {
        var uri: Uri
        when (view?.id) {
            R.id.iv_generate -> {
                qr_value = binding.etValue.text.toString()
                hideSoftKeyboard(view)
                if (qr_value.isNotEmpty()) {
                    val defaultDisplay = (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
                    val point = Point()
                    defaultDisplay.getSize(point)
                    var x = point.x
                    var y = point.y
                    if (x >= y) {
                        x = y
                    }
                    try {
                        val intent = Intent("com.google.zxing.client.android.ENCODE")
                        intent.putExtra(Intents.Encode.FORMAT, BarcodeFormat.QR_CODE.toString())
                        intent.putExtra(Intents.Encode.TYPE, qrType)
                        intent.putExtra(Intents.Encode.DATA, qr_value)
                        val qRCodeEncoder = QRCodeEncoders(this, intent, (x * 3) / 4, false)
                        Log.e(TAG, "onClick: $qrType")
                        qrImg = qRCodeEncoder.encodeAsBitmap()
                        binding.ivQrcode.visibility = View.VISIBLE
                        binding.ivQrcode.setImageBitmap(qrImg)
                        binding.ivGenerate.visibility = View.GONE
                        binding.ivRefresh.visibility = View.VISIBLE
                        qrImg?.let { saveBitmap() }
                        return
                    } catch (e: WriterException) {
                        e.printStackTrace()
                        return
                    }
                } else {
                    binding.etValue.error = "Required"
                    return
                }
            }
            R.id.iv_qrcode -> {
                try {
                    val file = File(qrPath?.path ?: "")
                    val intent = Intent("android.intent.action.SEND")
                    intent.type = "image/*"
                    if (Build.VERSION.SDK_INT >= 23) {
                        uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
                    } else {
                        uri = Uri.fromFile(file)
                    }
                    intent.putExtra("android.intent.extra.STREAM", uri)
                    intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    startActivity(Intent.createChooser(intent, "Share image using"))
                    return
                } catch (e: Exception) {
                    e.printStackTrace()
                    return
                }
            }
            R.id.iv_refresh -> {
                binding.etValue.setText("")
                binding.ivQrcode.visibility = View.GONE
                binding.ivRefresh.visibility = View.GONE
                binding.ivGenerate.visibility = View.VISIBLE
                return
            }
        }
    }

    private fun makeDir(): File? {
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + File.separator + getString(R.string.app_name) + "/QRCode/")
        if (!file.exists() && !file.mkdirs()) {
            return null
        }
        val file2 = File(file.path + File.separator + "temp.jpg")
        qrPath = file2
        return file2
    }

    fun saveBitmap() {
        val makeDir = makeDir() ?: return
        try {
            FileOutputStream(makeDir).use { fileOutputStream ->
                qrImg?.compress(Bitmap.CompressFormat.PNG, 80, fileOutputStream)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun hideSoftKeyboard(view: View) {
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(view.windowToken, 0)
    }
}