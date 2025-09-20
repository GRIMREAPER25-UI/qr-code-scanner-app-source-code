package com.scanner.barcodescanner.qrgenrater.qr_code_reader

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.media.ToneGenerator
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.zxing.BarcodeFormat
import com.google.zxing.Result
import com.scanner.barcodescanner.qrgenrater.R
import com.scanner.barcodescanner.qrgenrater.databinding.ActivityQrcodeReaderBinding

class ActivityQRcodeReader : AppCompatActivity(), ZXingScannerView.ResultHandler {
    private lateinit var binding: ActivityQrcodeReaderBinding
    private lateinit var adView: AdView
    private val TAG = "QRReaderActivity"
    var EXTRA_QUERY = "query"
    var TEXT_ENTRY = "text"
    var barcode_result: String? = null
    protected var camera_id = -1
    private var selected_indices: ArrayList<Int>? = null
    var viewGroup: ViewGroup? = null
    var zXingScannerView: ZXingScannerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrcodeReaderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // تهيئة SDK الإعلانات
        MobileAds.initialize(this) {}

        // إعداد إعلان البانر
        setupBannerAd()

        binding.toolbar.mToolBarThumb.setOnClickListener { finish() }
        binding.toolbar.mToolBarText.text = "QR Reader"
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

    private fun init() {
        viewGroup = binding.flCamera
        zXingScannerView = ZXingScannerView(this)
        viewGroup!!.addView(zXingScannerView)
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

    override fun handleResult(rawResult: Result?) {
        barcode_result = rawResult!!.text
        ToneGenerator(5, 100).startTone(24)
        val dialog = Dialog(this, R.style.ThemeWithRoundShape)
        dialog.requestWindowFeature(1)
        dialog.setContentView(R.layout.dialog_qr_out)
        dialog.window!!.setBackgroundDrawable(ColorDrawable(0))
        dialog.window!!.setLayout(-1, -2)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setCancelable(false)
        val tv_search = dialog.findViewById<TextView>(R.id.tv_search)
        val tv_result = dialog.findViewById<TextView>(R.id.tv_result)
        if (barcode_result!!.startsWith("tel")) {
            tv_search.text = "Call"
        }
        tv_result.text = barcode_result
        dialog.findViewById<View>(R.id.tv_share).setOnClickListener {
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "text/*"
            intent.putExtra(Intent.EXTRA_SUBJECT, "")
            intent.putExtra(Intent.EXTRA_TEXT, barcode_result)
            startActivity(Intent.createChooser(intent, "Share text using"))
            dialog.dismiss()
        }
        dialog.findViewById<View>(R.id.tv_search).setOnClickListener {
            if (barcode_result!!.startsWith("tel")) {
                val intent = Intent(Intent.ACTION_DIAL)
                intent.data = Uri.parse(barcode_result)
                startActivity(intent)
            } else {
                try {
                    val intent = Intent("android.intent.action.VIEW")
                    intent.data = Uri.parse(barcode_result)
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this@ActivityQRcodeReader, "Invalid format", Toast.LENGTH_SHORT).show()
                }
            }
            dialog.dismiss()
        }
        dialog.findViewById<View>(R.id.tv_copy).setOnClickListener {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("QR Code", barcode_result)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this@ActivityQRcodeReader, "Copied to clipboard", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        dialog.findViewById<View>(R.id.iv_close).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    override fun onPause() {
        adView.pause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        adView.resume()
        if (zXingScannerView == null) {
            zXingScannerView = ZXingScannerView(this)
            viewGroup!!.addView(zXingScannerView)
        }
        zXingScannerView!!.setResultHandler(this)
        zXingScannerView!!.startCamera(camera_id)
        setupBarcodeFormats()
    }

    override fun onDestroy() {
        adView.destroy()
        super.onDestroy()
        zXingScannerView!!.stopCamera()
    }

    fun setupBarcodeFormats() {
        val arrayList = ArrayList<BarcodeFormat>()
        if (selected_indices == null || selected_indices!!.isEmpty()) {
            selected_indices = ArrayList()
            for (i in ZXingScannerView.ALL_FORMATS.indices) {
                selected_indices!!.add(i)
            }
        }
        for (selectedIndex in selected_indices!!) {
            arrayList.add(ZXingScannerView.ALL_FORMATS[selectedIndex])
        }
        if (zXingScannerView != null) {
            zXingScannerView!!.setFormats(arrayList)
        }
    }
}