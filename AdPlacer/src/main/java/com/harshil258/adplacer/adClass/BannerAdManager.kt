package com.harshil258.adplacer.adClass

import android.app.Activity
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.RelativeLayout
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.harshil258.adplacer.utils.Extensions.isBannerAdEmpty
import com.harshil258.adplacer.utils.GlobalUtils
import com.harshil258.adplacer.utils.Logger
import com.harshil258.adplacer.utils.Logger.ADSLOG
import com.harshil258.adplacer.utils.commonFunctions.logCustomEvent
import com.harshil258.adplacer.utils.sharedpreference.SecureStorageManager.Companion.sharedPrefConfig

class BannerAdManager {
    private val TAG = "BannerAdManager"

    fun showBannerAd(
        activity: Activity,
        rlBanner: RelativeLayout,
        frameLayout: FrameLayout,
        rlLoader: RelativeLayout,
        bannerType: AdSize?
    ) {
        Logger.d(TAG, "Attempting to show banner ad")
        rlBanner.visibility = View.INVISIBLE

        // Check if banner ad unit is empty.
        if (isBannerAdEmpty()) {
            Logger.d(TAG, "Banner ad unit is empty. Hiding loader and banner.")
            rlLoader.visibility = View.GONE
            rlBanner.visibility = View.GONE
            return
        }

        // Check network availability.
        if (!GlobalUtils().isNetworkAvailable(activity.applicationContext)) {
            Logger.e(TAG, "Network unavailable. Cannot load banner ad.")
            rlLoader.visibility = View.GONE
            rlBanner.visibility = View.GONE
            return
        }

        // Create and configure the AdView.
        val adView = AdView(activity).apply {
            setAdSize(bannerType ?: AdSize.BANNER)
            adUnitId = sharedPrefConfig.appDetails.admobBannerAd
        }
        Logger.i(TAG, "Banner ad unit ID: ${adView.adUnitId}")

        // Prepare the container for the ad.
        frameLayout.removeAllViews()
        frameLayout.addView(adView)

        // Make the banner container visible.
        rlBanner.visibility = View.VISIBLE

        // Build and load the ad.
        val adRequest = AdRequest.Builder().build()
        Logger.d(TAG, "Loading banner ad with ad request.")
        adView.loadAd(adRequest)

        adView.adListener = object : AdListener() {
            override fun onAdClicked() {
                Logger.d(TAG, "Banner ad clicked.")
            }

            override fun onAdClosed() {
                Logger.d(TAG, "Banner ad closed.")
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                Logger.e(TAG, "Banner ad failed to load: ${loadAdError.message}")
                rlLoader.visibility = View.GONE
                rlBanner.visibility = View.GONE
            }

            override fun onAdImpression() {
                Logger.i(TAG, "Banner ad impression recorded.")
                val eventParams = mapOf("ADIMPRESSION" to "BANNER")
                activity.let { logCustomEvent(it, "ADS_EVENT", eventParams) }
            }

            override fun onAdLoaded() {
                Logger.i(TAG, "Banner ad loaded successfully.")
                rlLoader.visibility = View.GONE
                rlBanner.visibility = View.VISIBLE
            }

            override fun onAdOpened() {
                Logger.d(TAG, "Banner ad opened.")
            }
        }
    }

}
