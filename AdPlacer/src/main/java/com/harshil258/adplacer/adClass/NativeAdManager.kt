package com.harshil258.adplacer.adClass

import android.app.Activity
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.harshil258.adplacer.R
import com.harshil258.adplacer.adViews.NativeBigView
import com.harshil258.adplacer.adViews.NativeMediumView
import com.harshil258.adplacer.adViews.NativeSmallView
import com.harshil258.adplacer.interfaces.AdCallback
import com.harshil258.adplacer.models.NATIVE_SIZE
import com.harshil258.adplacer.utils.Constants.adPlacerApplication
import com.harshil258.adplacer.utils.GlobalUtils
import com.harshil258.adplacer.utils.Logger
import com.harshil258.adplacer.utils.Logger.ADSLOG
import com.harshil258.adplacer.utils.commonFunctions.logCustomEvent
import com.harshil258.adplacer.utils.extentions.isAdStatusOn
import com.harshil258.adplacer.utils.extentions.isNativeEmpty
import com.harshil258.adplacer.utils.extentions.nativeAdSizeBigOrSmall
import com.harshil258.adplacer.utils.extentions.nativeAdSizeMediumOrSmall
import com.zeel_enterprise.shreekhodalkotlin.common.SecureStorageManager.Companion.sharedPrefConfig
import java.util.Random

class NativeAdManager {

    companion object {
        var isAdLoading = false
        var nativeAd: NativeAd? = null
    }

    private fun isNativeStatusNull(): Boolean {
        return !isAdStatusOn() || isNativeEmpty()
    }


    fun loadNativeAd(activity: Activity) {
        if (isNativeStatusNull() || nativeAd != null || isAdLoading || !GlobalUtils().isNetworkAvailable(
                activity.applicationContext
            )
        ) {
            return
        }

        isAdLoading = true
        Logger.e("ADIDSSSS", "NATIVE   ${sharedPrefConfig.appDetails.admobNativeAd}")
        val adLoader = AdLoader.Builder(
            activity, sharedPrefConfig.appDetails.admobNativeAd
        ).forNativeAd { nativeAd ->
            isAdLoading = false
            Companion.nativeAd = nativeAd
            Log.i(ADSLOG, "onAdLoaded: Native")

        }.withAdListener(object : AdListener() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                isAdLoading = false
                Logger.e(
                    "NATIVELOADIMPRESSION",
                    "ERROR ${adError.message}   ${sharedPrefConfig.appDetails.admobNativeAd}"
                )

            }

            override fun onAdImpression() {
                super.onAdImpression()
                Log.i(ADSLOG, "onAdImpression: Native")
                val eventParams = mapOf("ADIMPRESSION" to "NATIVE")
                activity?.let { logCustomEvent(it, "ADS_EVENT", eventParams) }
            }
        }).withNativeAdOptions(NativeAdOptions.Builder().build()).build()
        adLoader.loadAd(AdRequest.Builder().build())
    }

    private fun loadNativeAdAndShow(
        activity: Activity,
        rlNative: RelativeLayout,
        frameLayout: FrameLayout,
        NATIVESIZE: NATIVE_SIZE,
        adDisplayedCallback: AdCallback
    ) {
        Logger.e("NATIVEADSSS", "loadNativeAdAndShow: 1")

        if (isNativeStatusNull()) {
            rlNative.visibility = View.GONE
            return
        }

        Logger.e("NATIVEADSSS", "loadNativeAdAndShow: 2")

        nativeAd?.apply {
            rlNative.visibility = View.VISIBLE
            populateNativeAdView(
                activity, frameLayout, this, NATIVESIZE, false, adDisplayedCallback
            )
            return
        }
        Logger.e("NATIVEADSSS", "loadNativeAdAndShow: 3")
        Logger.e("ADIDSSSS", "NATIVE   ${sharedPrefConfig.appDetails.admobNativeAd}")

        if (!GlobalUtils().isNetworkAvailable(activity.applicationContext)) return

        val adLoader = AdLoader.Builder(
            activity, sharedPrefConfig.appDetails.admobNativeAd
        ).forNativeAd { nativeAd ->
            Log.i(ADSLOG, "onAdLoaded: Native")

            rlNative.visibility = View.VISIBLE
            populateNativeAdView(
                activity, frameLayout, nativeAd, NATIVESIZE, true, adDisplayedCallback
            )
        }.withAdListener(object : AdListener() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Logger.e("NATIVEADSSS", "loadNativeAdAndShow: 4 failed  ${adError.message}")
                rlNative.visibility = View.GONE
                Logger.e(
                    "NATIVELOADIMPRESSION",
                    "FAILED    ${adError.message}  ${sharedPrefConfig.appDetails.admobNativeAd}"
                )

                adDisplayedCallback.adDisplayedCallback(false)
            }

            override fun onAdImpression() {
                super.onAdImpression()
                Log.i(ADSLOG, "onAdImpression: Native 2")
                val eventParams = mapOf("ADIMPRESSION" to "NATIVE")
                activity?.let { logCustomEvent(it, "ADS_EVENT", eventParams) }
            }
        }).withNativeAdOptions(NativeAdOptions.Builder().build()).build()
        Logger.e("NATIVEADSSS", "loadNativeAdAndShow: 5")
        adLoader.loadAd(AdRequest.Builder().build())
    }

    fun showAdIfLoadedSmall(
        activity: Activity,
        rlNativeSmall: RelativeLayout,
        frameLayoutSmall: FrameLayout,
        adDisplayedCallback: AdCallback
    ) {
        Logger.e("NATIVEADSSS", "showAdIfLoadedSmall: 1")
        if (isNativeStatusNull()) {
            rlNativeSmall.visibility = View.GONE
            return
        }

        Logger.e("NATIVEADSSS", "showAdIfLoadedSmall:  2")
        val NATIVESIZE = NATIVE_SIZE.SMALL
        rlNativeSmall.visibility = View.INVISIBLE

        if (isAdLoading && nativeAd == null) {
            loadNativeAdAndShow(
                activity, rlNativeSmall, frameLayoutSmall, NATIVESIZE, adDisplayedCallback
            )
            return
        }
        Logger.e("NATIVEADSSS", "showAdIfLoadedSmall:  3")
        nativeAd?.apply {
            rlNativeSmall.visibility = View.VISIBLE
            populateNativeAdView(
                activity, frameLayoutSmall, this, NATIVESIZE, false, adDisplayedCallback
            )
            return
        }
        Logger.e("NATIVEADSSS", "showAdIfLoadedSmall:  4")

        if (!GlobalUtils().isNetworkAvailable(activity.applicationContext)) {
            rlNativeSmall.visibility = View.GONE
            return
        }
        Logger.e("NATIVEADSSS", "showAdIfLoadedSmall:  5")

        loadNativeAdAndShow(
            activity, rlNativeSmall, frameLayoutSmall, NATIVESIZE, adDisplayedCallback
        )
        Logger.e("NATIVEADSSS", "showAdIfLoadedSmall:  6")
        rlNativeSmall.visibility = View.VISIBLE
        Logger.e("NATIVEADSSS", "showAdIfLoadedSmall:  7")
        nativeAd?.let {
            populateNativeAdView(
                activity, frameLayoutSmall, it, NATIVESIZE, false, adDisplayedCallback
            )
        }
        Logger.e("NATIVEADSSS", "showAdIfLoadedSmall:  8")
    }

    fun showAdIfLoadedBig(
        activity: Activity,
        rlNativeBig: RelativeLayout,
        frameLayoutBig: FrameLayout,
        adDisplayedCallback: AdCallback
    ) {

        Logger.e("NATIVEADSSS", "showAdIfLoadedBig: 1")

        if (isNativeStatusNull()) {
            rlNativeBig.visibility = View.GONE
            return
        }
        Logger.e("NATIVEADSSS", "showAdIfLoadedBig: 2")

        val NATIVESIZE = NATIVE_SIZE.LARGE
        rlNativeBig.visibility = View.INVISIBLE

        if (isAdLoading && nativeAd == null) {
            loadNativeAdAndShow(
                activity, rlNativeBig, frameLayoutBig, NATIVESIZE, adDisplayedCallback
            )
            return
        }
        Logger.e("NATIVEADSSS", "showAdIfLoadedBig: 3")

        nativeAd?.apply {
            rlNativeBig.visibility = View.VISIBLE
            populateNativeAdView(
                activity, frameLayoutBig, this, NATIVESIZE, false, adDisplayedCallback
            )
            return
        }

        if (!GlobalUtils().isNetworkAvailable(activity.applicationContext)) {
            rlNativeBig.visibility = View.GONE
            return
        }
        Logger.e("NATIVEADSSS", "showAdIfLoadedBig: 4")

        loadNativeAdAndShow(activity, rlNativeBig, frameLayoutBig, NATIVESIZE, adDisplayedCallback)
        rlNativeBig.visibility = View.VISIBLE
        Logger.e("NATIVEADSSS", "showAdIfLoadedBig: 5")

        nativeAd?.apply {
            populateNativeAdView(
                activity, frameLayoutBig, this, NATIVESIZE, false, adDisplayedCallback
            )
        }
    }

    fun showAdIfLoadedMedium(
        activity: Activity,
        rlNativeBig: RelativeLayout,
        frameLayoutBig: FrameLayout,
        adDisplayedCallback: AdCallback
    ) {
        if (isNativeStatusNull()) {
            rlNativeBig.visibility = View.GONE
            return
        }

        val NATIVESIZE = NATIVE_SIZE.MEDIUM
        rlNativeBig.visibility = View.INVISIBLE

        if (isAdLoading && nativeAd == null) {
            loadNativeAdAndShow(
                activity, rlNativeBig, frameLayoutBig, NATIVESIZE, adDisplayedCallback
            )
            return
        }
        nativeAd?.apply {
            rlNativeBig.visibility = View.VISIBLE
            populateNativeAdView(
                activity, frameLayoutBig, this, NATIVESIZE, false, adDisplayedCallback
            )
            return
        }

        if (!GlobalUtils().isNetworkAvailable(activity.applicationContext)) {
            rlNativeBig.visibility = View.GONE
            return
        }

        loadNativeAdAndShow(activity, rlNativeBig, frameLayoutBig, NATIVESIZE, adDisplayedCallback)
        rlNativeBig.visibility = View.VISIBLE
        nativeAd?.apply {
            populateNativeAdView(
                activity, frameLayoutBig, this, NATIVESIZE, false, adDisplayedCallback
            )
        }
    }

    var bigLayouts = listOf(
        R.layout.layout_ad_native_big,
        R.layout.layout_ad_native_big_2,
        R.layout.layout_ad_native_big_3,
        R.layout.layout_ad_native_big_4,
        R.layout.layout_ad_native_big,
        R.layout.layout_ad_native_big_2,
        R.layout.layout_ad_native_big_3,
        R.layout.layout_ad_native_big_4,
        R.layout.layout_ad_native_big,
        R.layout.layout_ad_native_big_2,
        R.layout.layout_ad_native_big_3,
        R.layout.layout_ad_native_big_4,
    )

    private fun populateNativeAdView(
        activity: Activity,
        frameLayout: FrameLayout,
        nativeAd: NativeAd,
        NATIVESIZE: NATIVE_SIZE,
        isOnDemand: Boolean,
        adDisplayedCallback: AdCallback
    ) {
        Logger.w("NATIVEADSSS", "populateNativeAdView: 1")
        val nativeAdView = when (NATIVESIZE) {
            NATIVE_SIZE.MEDIUM -> activity.layoutInflater.inflate(
                R.layout.ad_layout_native_medium, null
            ) as NativeAdView

            NATIVE_SIZE.LARGE -> activity.layoutInflater.inflate(
                bigLayouts[Random().nextInt(bigLayouts.size)], null
            ) as NativeAdView

            else -> activity.layoutInflater.inflate(
                R.layout.ad_layout_native_small, null
            ) as NativeAdView
        }

        Logger.w("NATIVEADSSS", "populateNativeAdView: 2")
        nativeAdView.apply {
            adDisplayedCallback.adDisplayedCallback(true)
            findViewById<TextView>(R.id.txtHead)?.apply {
                nativeAd.headline?.let {
                    text = it
                    nativeAdView.headlineView = this
                }
            }
            findViewById<TextView>(R.id.dialogDescription)?.apply {
                nativeAd.body?.let {
                    text = it
                    nativeAdView.bodyView = this
                }
            }
            findViewById<ImageView>(R.id.icon)?.apply {
                nativeAd.icon?.drawable?.let {
                    setImageDrawable(it)
                    nativeAdView.iconView = this
                }
            }
            findViewById<RelativeLayout>(R.id.btnClick)?.apply {
                nativeAd.callToAction?.let {
                    findViewById<TextView>(R.id.callToActionText).text = it
                    nativeAdView.callToActionView = this
                }
            }
//            if (NATIVESIZE == NATIVE_SIZE.LARGE || NATIVESIZE == NATIVE_SIZE.MEDIUM) {
            Logger.e("NATIVESIZE", "populateNativeAdView: NATIVESIZE  ${NATIVESIZE}")
            findViewById<MediaView>(R.id.mediaView)?.apply {
                nativeAd.mediaContent?.let {
                    mediaContent = it
                    nativeAdView.mediaView = this
                }
            }

            if (NATIVESIZE == NATIVE_SIZE.LARGE){
                findViewById<MediaView>(R.id.mediaView)?.setOnHierarchyChangeListener(object :
                    ViewGroup.OnHierarchyChangeListener {

                    override fun onChildViewAdded(parent: View, child: View) {
                        val scale = context.resources.displayMetrics.density

                        // Define the max height in dp (use a suitable value based on your UI requirements)
                        val maxHeightDp = 300
                        val maxHeightPixels = (maxHeightDp * scale + 0.5f).toInt() // Convert dp to pixels

                        if (child is ImageView) { // If the child is an ImageView
                            child.adjustViewBounds = true // Allows the view to adjust its bounds while maintaining the aspect ratio
                            child.scaleType = ImageView.ScaleType.CENTER_CROP // Adjust the image scaling
                            child.layoutParams.height = maxHeightPixels // Set the height in pixels
                        } else { // If the child is a video view or other types
                            val params = child.layoutParams
                            params.height = maxHeightPixels
                            child.layoutParams = params
                        }
                    }

                    override fun onChildViewRemoved(parent: View, child: View) {
                        // Handle any logic if needed when a child is removed
                    }
                })
            }


//            }

            findViewById<TextView>(R.id.ratingTextview)?.let { starTextview ->
                nativeAd.starRating?.let { rating ->
                    starTextview.text = "$rating ★"
                    starRatingView = starTextview
                }
            }

            findViewById<TextView>(R.id.advertiserTextview)?.let { otherText ->
                nativeAd.advertiser?.let { advertiser ->
                    otherText.text = advertiser
                    advertiserView = otherText
                }
            }

            Logger.w("NATIVEADSSS", "populateNativeAdView: 3")
            setNativeAd(nativeAd)
        }

        Logger.w("NATIVEADSSS", "populateNativeAdView: 4")
        frameLayout.removeAllViews()
        frameLayout.addView(nativeAdView)

        Logger.w("NATIVEADSSS", "populateNativeAdView: 5")
        if (!isOnDemand) {
            Companion.nativeAd = null
            if (adPlacerApplication.messagingCallback?.isExitActivity != true) {
                loadNativeAd(activity)
            }
        }
        Logger.w("NATIVEADSSS", "populateNativeAdView: 6")
    }


    fun callBigOrSmall(
        activity: Activity,
        nativeBigView: NativeBigView,
        nativeSmallView: NativeSmallView,
        adDisplayedCallback: AdCallback
    ) {
        if (isNativeStatusNull()) {
            nativeBigView.visibility = View.GONE
            nativeSmallView.visibility = View.GONE
            return
        }

        when (nativeAdSizeBigOrSmall()) {
            NATIVE_SIZE.LARGE -> {
                nativeBigView.visibility = View.VISIBLE
                nativeSmallView.visibility = View.GONE
                nativeBigView.loadAd(activity, adDisplayedCallback)
            }

            NATIVE_SIZE.SMALL -> {
                nativeBigView.visibility = View.GONE
                nativeSmallView.visibility = View.VISIBLE
                nativeSmallView.loadAd(activity, adDisplayedCallback)
            }

            else -> {
                nativeBigView.visibility = View.GONE
                nativeSmallView.visibility = View.GONE
            }
        }
    }

    fun callMediumOrSmall(
        activity: Activity,
        nativeMediumView: NativeMediumView,
        nativeSmallView: NativeSmallView,
        adDisplayedCallback: AdCallback
    ) {
        if (isNativeStatusNull()) {
            nativeMediumView.visibility = View.GONE
            nativeSmallView.visibility = View.GONE
            return
        }

        when (nativeAdSizeMediumOrSmall()) {
            NATIVE_SIZE.MEDIUM -> {
                nativeMediumView.visibility = View.VISIBLE
                nativeSmallView.visibility = View.GONE
                nativeMediumView.loadAd(activity, adDisplayedCallback)
            }

            NATIVE_SIZE.SMALL -> {
                nativeMediumView.visibility = View.GONE
                nativeSmallView.visibility = View.VISIBLE
                nativeSmallView.loadAd(activity, adDisplayedCallback)
            }

            else -> {
                nativeMediumView.visibility = View.GONE
                nativeSmallView.visibility = View.GONE
            }
        }
    }

    fun callBigOnly(
        activity: Activity, nativeBigView: NativeBigView, adDisplayedCallback: AdCallback
    ) {
        if (isNativeStatusNull()) {
            nativeBigView.visibility = View.GONE
            return
        }

        nativeBigView.visibility = View.VISIBLE
        nativeBigView.loadAd(activity, adDisplayedCallback)
    }


}
