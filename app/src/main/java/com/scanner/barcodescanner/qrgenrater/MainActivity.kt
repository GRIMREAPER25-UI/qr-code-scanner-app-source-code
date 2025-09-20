package com.scanner.barcodescanner.qrgenrater

import android.Manifest.permission
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.scanner.barcodescanner.qrgenrater.databinding.ActivityMainBinding
import com.scanner.barcodescanner.qrgenrater.qr_code_maker.ActivityQRcodeMaker
import com.scanner.barcodescanner.qrgenrater.qr_code_reader.ActivityQRcodeReader
import java.io.IOException
import java.io.RandomAccessFile
import java.text.DecimalFormat
import java.util.regex.Pattern

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adView: AdView
    private var mInterstitialAd: InterstitialAd? = null
    private lateinit var consentInformation: ConsentInformation
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // تهيئة SDK الإعلانات
        MobileAds.initialize(this) {}

        // إعداد إعلان البانر
        setupBannerAd()

        // تحميل الإعلان البيني
        loadInterstitialAd()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //Consent Form
        val params = ConsentRequestParameters
            .Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()

        consentInformation = UserMessagingPlatform.getConsentInformation(this)
        consentInformation.requestConsentInfoUpdate(
            this,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                    this@MainActivity
                ) { loadAndShowError ->
                    // Consent gathering failed.
                    if (loadAndShowError != null) {
                        Log.w(
                            "TAG", String.format(
                                "%s: %s",
                                loadAndShowError.errorCode,
                                loadAndShowError.message
                            )
                        )
                    }

                    // Consent has been gathered.
                }
            },
            {
                    requestConsentError ->
                // Consent gathering failed.
                Log.w("TAG", String.format("%s: %s",
                    requestConsentError.errorCode,
                    requestConsentError.message
                ))
            })

        binding.cardQrCodeReader.setOnClickListener(this)
        binding.cardQrGenerator.setOnClickListener(this)
        binding.cardShare.setOnClickListener(this)
        binding.pricacyPolicy.setOnClickListener(this)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isEnabled) {
                    isEnabled = false
                    finish()
                }
            }
        })

        val sharedPreferences = this.getSharedPreferences("phar_id", Context.MODE_PRIVATE)
        val memoryRange = sharedPreferences.getInt("TEMPFILES", (Math.random() * 70).toInt() + 30)

        setProgressInAsync(binding.memoryPercantage, binding.memoryProgress, memoryRange, false)
        getMemorySize()
        parse()
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

    private fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()
        
        InterstitialAd.load(this, getString(R.string.admob_interstitial_id), adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    mInterstitialAd = interstitialAd
                    Log.d(TAG, "Interstitial ad loaded successfully")
                    
                    mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            Log.d(TAG, "Interstitial ad was dismissed")
                            mInterstitialAd = null
                            // إعادة تحميل الإعلان البيني للمرة القادمة
                            loadInterstitialAd()
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            Log.e(TAG, "Interstitial ad failed to show: ${adError.message}")
                            mInterstitialAd = null
                        }

                        override fun onAdShowedFullScreenContent() {
                            Log.d(TAG, "Interstitial ad showed successfully")
                        }
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "Interstitial ad failed to load: ${loadAdError.message}")
                    mInterstitialAd = null
                }
            })
    }

    private fun showInterstitialAd() {
        if (mInterstitialAd != null) {
            mInterstitialAd?.show(this)
        } else {
            Log.d(TAG, "The interstitial ad wasn't ready yet.")
        }
    }

    private val TEMPFILES = "TEMPORARIESFILESALL"
    var free: Long = 0
    var total: Long = 0
    private fun parse() {
        binding.freeRam.text = calSize(this.free.toDouble()) + ""
        binding.ramTotal.text = " / " + calSize(this.total.toDouble())
    }

    private fun getMemorySize() {
        val compile = Pattern.compile("([a-zA-Z]+):\\s*(\\d+)")
        try {
            val randomAccessFile = RandomAccessFile("/proc/meminfo", "r")
            while (true) {
                val readLine: CharSequence? = randomAccessFile.readLine()
                if (readLine != null) {
                    val matcher = compile.matcher(readLine)
                    if (matcher.find()) {
                        val group = matcher.group(1)
                        val group2 = matcher.group(2)
                        if (group.equals("MemTotal", ignoreCase = true)) {
                            this.total = group2.toLong()
                        } else if (group.equals(
                                "MemFree",
                                ignoreCase = true
                            ) || group.equals("SwapFree", ignoreCase = true)
                        ) {
                            this.free = group2.toLong()
                        }
                    }
                } else {
                    randomAccessFile.close()
                    this.total *= (1 shl 10).toLong()
                    this.free *= (1 shl 10).toLong()
                    return
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun calSize(d: Double): String {
        val decimalFormat = DecimalFormat("#.##")
        val d2 = d / 1048576.0
        val d3 = d / 1.073741824E9
        val d4 = d / 1.099511627776E12
        if (d4 > 1.0) {
            return decimalFormat.format(d4) + " TB"
        }
        if (d3 > 1.0) {
            return decimalFormat.format(d3) + " GB"
        }
        return if (d2 > 1.0) {
            decimalFormat.format(d2) + " MB"
        } else decimalFormat.format(d) + " KB"
    }

    fun setProgressInAsync(
        percantage: TextView,
        progressBar: ProgressBar,
        progress: Int,
        justNow: Boolean
    ) {
        Thread {
            if (justNow) {
                this.runOnUiThread {
                    percantage.text = progress.toString() + "MB"
                    progressBar.progress = progress
                }
            } else {
                var currentRange = 0
                while (currentRange < progress) {
                    currentRange++
                    val finalCurrentMomoryRange = currentRange
                    this.runOnUiThread {
                        percantage.text = finalCurrentMomoryRange.toString() + "MB"
                        progressBar.progress = finalCurrentMomoryRange
                    }
                    try {
                        Thread.sleep(65)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                }
            }
        }.start()
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.cardQrCodeReader -> {
                if (!isPermissionGranted(this)) {
                    takePermission(this)
                } else {
                    startActivity(Intent(this, ActivityQRcodeReader::class.java))
                    showInterstitialAd()
                }
            }
            R.id.cardQrGenerator -> {
                startActivity(Intent(this, ActivityQRcodeMaker::class.java))
                showInterstitialAd()
            }
            R.id.card_share -> {
                val intent = Intent(Intent.ACTION_SEND)
                intent.type = "text/plain"
                intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
                intent.putExtra(Intent.EXTRA_TEXT, "https://play.google.com/store/apps/details?id=$packageName")
                startActivity(Intent.createChooser(intent, "Share via"))
            }
            R.id.pricacy_policy -> {
                try {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse(getString(R.string.privacy_policy_url))
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun isPermissionGranted(activity: Activity): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun takePermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity, arrayOf(permission.CAMERA),
            1
        )
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
}