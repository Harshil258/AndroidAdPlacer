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
import com.harshil258.adplacer.utils.Constants.adPlacerInstance
import com.harshil258.adplacer.utils.Extensions.getNativeAdSizeBigOrSmall
import com.harshil258.adplacer.utils.Extensions.getNativeAdSizeMediumOrSmall
import com.harshil258.adplacer.utils.Extensions.isAdStatusEnabled
import com.harshil258.adplacer.utils.Extensions.isNativeAdEmpty
import com.harshil258.adplacer.utils.GlobalUtils
import com.harshil258.adplacer.utils.Logger
import com.harshil258.adplacer.utils.Logger.ADSLOG
import com.harshil258.adplacer.utils.commonFunctions.logCustomEvent
import com.harshil258.adplacer.utils.sharedpreference.SecureStorageManager.Companion.sharedPrefConfig
import java.util.Random

class NativeAdManager {

    companion object {
        const val TAG = "NativeAdManager"
        var isAdLoading = false
        var nativeAd: NativeAd? = null
    }

    private fun isNativeStatusNull(): Boolean {
        val status = !isAdStatusEnabled() || isNativeAdEmpty()
        Logger.d(TAG, "isNativeStatusNull: $status")
        return status
    }

    fun loadNativeAd(activity: Activity) {
        Logger.i(TAG, "loadNativeAd: Entered")
        if (isNativeStatusNull() || nativeAd != null || isAdLoading || !GlobalUtils().isNetworkAvailable(activity.applicationContext)) {
            Logger.i(TAG, "loadNativeAd: Aborting load (status: ${isNativeStatusNull()}, nativeAd: $nativeAd, isAdLoading: $isAdLoading, networkAvailable: ${GlobalUtils().isNetworkAvailable(activity.applicationContext)})")
            return
        }

        isAdLoading = true
        Logger.d(TAG, "loadNativeAd: Loading native ad with ad unit ${sharedPrefConfig.appDetails.admobNativeAd}")

        val adLoader = AdLoader.Builder(activity, sharedPrefConfig.appDetails.admobNativeAd)
            .forNativeAd { nativeAd ->
                isAdLoading = false
                Companion.nativeAd = nativeAd
                Logger.i(TAG, "loadNativeAd: onAdLoaded - Native ad loaded successfully")
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    isAdLoading = false
                    Logger.e(TAG, "loadNativeAd: onAdFailedToLoad - Error: ${adError.message} for ad unit ${sharedPrefConfig.appDetails.admobNativeAd}")
                }

                override fun onAdImpression() {
                    super.onAdImpression()
                    Logger.i(TAG, "loadNativeAd: onAdImpression - Native ad impression recorded")
                    val eventParams = mapOf("ADIMPRESSION" to "NATIVE")
                    activity.let { logCustomEvent(it, "ADS_EVENT", eventParams) }
                }
            })
            .withNativeAdOptions(NativeAdOptions.Builder().build())
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
        Logger.i(TAG, "loadNativeAd: Ad loading initiated")
    }

    private fun loadNativeAdAndShow(
        activity: Activity,
        rlNative: RelativeLayout,
        frameLayout: FrameLayout,
        NATIVESIZE: NATIVE_SIZE,
        adDisplayedCallback: AdCallback
    ) {
        Logger.i(TAG, "loadNativeAdAndShow: Entered")
        if (isNativeStatusNull()) {
            rlNative.visibility = View.GONE
            Logger.i(TAG, "loadNativeAdAndShow: Native status is null. Hiding layout.")
            return
        }

        nativeAd?.let {
            rlNative.visibility = View.VISIBLE
            populateNativeAdView(activity, frameLayout, it, NATIVESIZE, isOnDemand = false, adDisplayedCallback)
            Logger.i(TAG, "loadNativeAdAndShow: Showing existing native ad")
            return
        }

        if (!GlobalUtils().isNetworkAvailable(activity.applicationContext)) {
            rlNative.visibility = View.GONE
            Logger.e(TAG, "loadNativeAdAndShow: Network not available. Hiding layout.")
            return
        }

        Logger.d(TAG, "loadNativeAdAndShow: Loading new native ad for ad unit ${sharedPrefConfig.appDetails.admobNativeAd}")
        val adLoader = AdLoader.Builder(activity, sharedPrefConfig.appDetails.admobNativeAd)
            .forNativeAd { nativeAd ->
                Logger.i(TAG, "loadNativeAdAndShow: onAdLoaded - Native ad loaded")
                rlNative.visibility = View.VISIBLE
                populateNativeAdView(activity, frameLayout, nativeAd, NATIVESIZE, isOnDemand = true, adDisplayedCallback)
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    rlNative.visibility = View.GONE
                    Logger.e(TAG, "loadNativeAdAndShow: onAdFailedToLoad - Error: ${adError.message}")
                    adDisplayedCallback.onAdDisplayed(false)
                }

                override fun onAdImpression() {
                    super.onAdImpression()
                    Logger.i(TAG, "loadNativeAdAndShow: onAdImpression - Native ad impression recorded")
                    val eventParams = mapOf("ADIMPRESSION" to "NATIVE")
                    activity.let { logCustomEvent(it, "ADS_EVENT", eventParams) }
                }
            })
            .withNativeAdOptions(NativeAdOptions.Builder().build())
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
        Logger.i(TAG, "loadNativeAdAndShow: Ad loading initiated")
    }

    fun showAdIfLoadedSmall(
        activity: Activity,
        rlNativeSmall: RelativeLayout,
        frameLayoutSmall: FrameLayout,
        adDisplayedCallback: AdCallback
    ) {
        Logger.i(TAG, "showAdIfLoadedSmall: Entered")
        if (isNativeStatusNull()) {
            rlNativeSmall.visibility = View.GONE
            Logger.i(TAG, "showAdIfLoadedSmall: Native status is null. Hiding layout.")
            return
        }

        val NATIVESIZE = NATIVE_SIZE.SMALL
        rlNativeSmall.visibility = View.INVISIBLE

        if (isAdLoading && nativeAd == null) {
            Logger.d(TAG, "showAdIfLoadedSmall: Ad is loading and no native ad available. Loading ad on-demand.")
            loadNativeAdAndShow(activity, rlNativeSmall, frameLayoutSmall, NATIVESIZE, adDisplayedCallback)
            return
        }

        nativeAd?.let {
            rlNativeSmall.visibility = View.VISIBLE
            populateNativeAdView(activity, frameLayoutSmall, it, NATIVESIZE, isOnDemand = false, adDisplayedCallback)
            Logger.i(TAG, "showAdIfLoadedSmall: Displaying existing native ad")
            return
        }

        if (!GlobalUtils().isNetworkAvailable(activity.applicationContext)) {
            rlNativeSmall.visibility = View.GONE
            Logger.e(TAG, "showAdIfLoadedSmall: Network not available. Hiding layout.")
            return
        }

        Logger.d(TAG, "showAdIfLoadedSmall: Loading ad on-demand")
        loadNativeAdAndShow(activity, rlNativeSmall, frameLayoutSmall, NATIVESIZE, adDisplayedCallback)
        rlNativeSmall.visibility = View.VISIBLE
        Logger.i(TAG, "showAdIfLoadedSmall: Exiting")
    }

    fun showAdIfLoadedBig(
        activity: Activity,
        rlNativeBig: RelativeLayout,
        frameLayoutBig: FrameLayout,
        adDisplayedCallback: AdCallback
    ) {
        Logger.i(TAG, "showAdIfLoadedBig: Entered")
        if (isNativeStatusNull()) {
            rlNativeBig.visibility = View.GONE
            Logger.i(TAG, "showAdIfLoadedBig: Native status is null. Hiding layout.")
            return
        }

        val NATIVESIZE = NATIVE_SIZE.LARGE
        rlNativeBig.visibility = View.INVISIBLE

        if (isAdLoading && nativeAd == null) {
            Logger.d(TAG, "showAdIfLoadedBig: Ad is loading and no native ad available. Loading ad on-demand.")
            loadNativeAdAndShow(activity, rlNativeBig, frameLayoutBig, NATIVESIZE, adDisplayedCallback)
            return
        }

        nativeAd?.let {
            rlNativeBig.visibility = View.VISIBLE
            populateNativeAdView(activity, frameLayoutBig, it, NATIVESIZE, isOnDemand = false, adDisplayedCallback)
            Logger.i(TAG, "showAdIfLoadedBig: Displaying existing native ad")
            return
        }

        if (!GlobalUtils().isNetworkAvailable(activity.applicationContext)) {
            rlNativeBig.visibility = View.GONE
            Logger.e(TAG, "showAdIfLoadedBig: Network not available. Hiding layout.")
            return
        }

        Logger.d(TAG, "showAdIfLoadedBig: Loading new native ad")
        loadNativeAdAndShow(activity, rlNativeBig, frameLayoutBig, NATIVESIZE, adDisplayedCallback)
        rlNativeBig.visibility = View.VISIBLE
        Logger.i(TAG, "showAdIfLoadedBig: Exiting")
    }

    fun showAdIfLoadedMedium(
        activity: Activity,
        rlNativeMedium: RelativeLayout,
        frameLayoutMedium: FrameLayout,
        adDisplayedCallback: AdCallback
    ) {
        Logger.i(TAG, "showAdIfLoadedMedium: Entered")
        if (isNativeStatusNull()) {
            rlNativeMedium.visibility = View.GONE
            Logger.i(TAG, "showAdIfLoadedMedium: Native status is null. Hiding layout.")
            return
        }

        val NATIVESIZE = NATIVE_SIZE.MEDIUM
        rlNativeMedium.visibility = View.INVISIBLE

        if (isAdLoading && nativeAd == null) {
            Logger.d(TAG, "showAdIfLoadedMedium: Ad is loading and no native ad available. Loading ad on-demand.")
            loadNativeAdAndShow(activity, rlNativeMedium, frameLayoutMedium, NATIVESIZE, adDisplayedCallback)
            return
        }

        nativeAd?.let {
            rlNativeMedium.visibility = View.VISIBLE
            populateNativeAdView(activity, frameLayoutMedium, it, NATIVESIZE, isOnDemand = false, adDisplayedCallback)
            Logger.i(TAG, "showAdIfLoadedMedium: Displaying existing native ad")
            return
        }

        if (!GlobalUtils().isNetworkAvailable(activity.applicationContext)) {
            rlNativeMedium.visibility = View.GONE
            Logger.e(TAG, "showAdIfLoadedMedium: Network not available. Hiding layout.")
            return
        }

        Logger.d(TAG, "showAdIfLoadedMedium: Loading new native ad")
        loadNativeAdAndShow(activity, rlNativeMedium, frameLayoutMedium, NATIVESIZE, adDisplayedCallback)
        rlNativeMedium.visibility = View.VISIBLE
        Logger.i(TAG, "showAdIfLoadedMedium: Exiting")
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
        R.layout.layout_ad_native_big_4
    )

    private fun populateNativeAdView(
        activity: Activity,
        frameLayout: FrameLayout,
        nativeAd: NativeAd,
        NATIVESIZE: NATIVE_SIZE,
        isOnDemand: Boolean,
        adDisplayedCallback: AdCallback
    ) {
        Logger.i(TAG, "populateNativeAdView: Entered for size $NATIVESIZE")
        val nativeAdView: NativeAdView = when (NATIVESIZE) {
            NATIVE_SIZE.MEDIUM -> activity.layoutInflater.inflate(R.layout.ad_layout_native_medium, null) as NativeAdView
            NATIVE_SIZE.LARGE -> activity.layoutInflater.inflate(bigLayouts.random(), null) as NativeAdView
            else -> activity.layoutInflater.inflate(R.layout.ad_layout_native_small, null) as NativeAdView
        }

        Logger.d(TAG, "populateNativeAdView: Inflated layout for $NATIVESIZE")
        nativeAdView.apply {
            adDisplayedCallback.onAdDisplayed(true)
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
            Logger.d(TAG, "populateNativeAdView: Setting media content")
            findViewById<MediaView>(R.id.mediaView)?.apply {
                nativeAd.mediaContent?.let {
                    mediaContent = it
                    nativeAdView.mediaView = this
                }
            }

            if (NATIVESIZE == NATIVE_SIZE.LARGE) {
                findViewById<MediaView>(R.id.mediaView)?.setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {
                    override fun onChildViewAdded(parent: View, child: View) {
                        val scale = context.resources.displayMetrics.density
                        val maxHeightPixels = (300 * scale + 0.5f).toInt()
                        if (child is ImageView) {
                            child.adjustViewBounds = true
                            child.scaleType = ImageView.ScaleType.CENTER_CROP
                            child.layoutParams.height = maxHeightPixels
                        } else {
                            child.layoutParams = child.layoutParams.apply { height = maxHeightPixels }
                        }
                    }
                    override fun onChildViewRemoved(parent: View, child: View) {}
                })
            }

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
            Logger.d(TAG, "populateNativeAdView: Finalizing native ad view")
            setNativeAd(nativeAd)
        }

        frameLayout.removeAllViews()
        frameLayout.addView(nativeAdView)
        Logger.i(TAG, "populateNativeAdView: Native ad view populated in layout")

        if (!isOnDemand) {
            Companion.nativeAd = null
            if (adPlacerInstance.messagingListener?.isExitActivity != true) {
                loadNativeAd(activity)
            }
        }
        Logger.i(TAG, "populateNativeAdView: Exiting")
    }

    fun callBigOrSmall(
        activity: Activity,
        nativeBigView: NativeBigView,
        nativeSmallView: NativeSmallView,
        adDisplayedCallback: AdCallback
    ) {
        Logger.i(TAG, "callBigOrSmall: Entered")
        if (isNativeStatusNull()) {
            nativeBigView.visibility = View.GONE
            nativeSmallView.visibility = View.GONE
            Logger.i(TAG, "callBigOrSmall: Native status is null. Hiding both views.")
            return
        }

        when (getNativeAdSizeBigOrSmall()) {
            NATIVE_SIZE.LARGE -> {
                nativeBigView.visibility = View.VISIBLE
                nativeSmallView.visibility = View.GONE
                nativeBigView.loadAd(activity, adDisplayedCallback)
                Logger.i(TAG, "callBigOrSmall: Loaded big native ad")
            }
            NATIVE_SIZE.SMALL -> {
                nativeBigView.visibility = View.GONE
                nativeSmallView.visibility = View.VISIBLE
                nativeSmallView.loadAd(activity, adDisplayedCallback)
                Logger.i(TAG, "callBigOrSmall: Loaded small native ad")
            }
            else -> {
                nativeBigView.visibility = View.GONE
                nativeSmallView.visibility = View.GONE
                Logger.w(TAG, "callBigOrSmall: Unknown ad size")
            }
        }
    }

    fun callMediumOrSmall(
        activity: Activity,
        nativeMediumView: NativeMediumView,
        nativeSmallView: NativeSmallView,
        adDisplayedCallback: AdCallback
    ) {
        Logger.i(TAG, "callMediumOrSmall: Entered")
        if (isNativeStatusNull()) {
            nativeMediumView.visibility = View.GONE
            nativeSmallView.visibility = View.GONE
            Logger.i(TAG, "callMediumOrSmall: Native status is null. Hiding both views.")
            return
        }

        when (getNativeAdSizeMediumOrSmall()) {
            NATIVE_SIZE.MEDIUM -> {
                nativeMediumView.visibility = View.VISIBLE
                nativeSmallView.visibility = View.GONE
                nativeMediumView.loadAd(activity, adDisplayedCallback)
                Logger.i(TAG, "callMediumOrSmall: Loaded medium native ad")
            }
            NATIVE_SIZE.SMALL -> {
                nativeMediumView.visibility = View.GONE
                nativeSmallView.visibility = View.VISIBLE
                nativeSmallView.loadAd(activity, adDisplayedCallback)
                Logger.i(TAG, "callMediumOrSmall: Loaded small native ad")
            }
            else -> {
                nativeMediumView.visibility = View.GONE
                nativeSmallView.visibility = View.GONE
                Logger.w(TAG, "callMediumOrSmall: Unknown ad size")
            }
        }
    }

    fun callBigOnly(
        activity: Activity,
        nativeBigView: NativeBigView,
        adDisplayedCallback: AdCallback
    ) {
        Logger.i(TAG, "callBigOnly: Entered")
        if (isNativeStatusNull()) {
            nativeBigView.visibility = View.GONE
            Logger.i(TAG, "callBigOnly: Native status is null. Hiding view.")
            return
        }

        nativeBigView.visibility = View.VISIBLE
        nativeBigView.loadAd(activity, adDisplayedCallback)
        Logger.i(TAG, "callBigOnly: Loaded big native ad")
    }
}
