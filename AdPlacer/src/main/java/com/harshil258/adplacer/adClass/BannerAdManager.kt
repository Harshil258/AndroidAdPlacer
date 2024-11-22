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
import com.harshil258.adplacer.utils.GlobalUtils
import com.harshil258.adplacer.utils.Logger
import com.harshil258.adplacer.utils.Logger.ADSLOG
import com.harshil258.adplacer.utils.commonFunctions.logCustomEvent
import com.harshil258.adplacer.utils.extentions.isBannerEmpty
import com.zeel_enterprise.shreekhodalkotlin.common.SecureStorageManager.Companion.sharedPrefConfig

class BannerAdManager {
    var TAG: String = "BannerAd"

    fun showBannerAd(
        activity: Activity,
        rlBanner: RelativeLayout,
        frameLayout: FrameLayout,
        rlLoader: RelativeLayout,
        bannerType: AdSize?
    ) {
        rlBanner.visibility = View.INVISIBLE
        if (isBannerEmpty()) {
            rlLoader.visibility = View.GONE
            rlBanner.visibility = View.GONE
            return
        }

        if (!GlobalUtils().isNetworkAvailable(activity.applicationContext)) {
            rlLoader.visibility = View.GONE

            rlBanner.visibility = View.GONE
            return
        }

        val adView = AdView(activity)

        adView.setAdSize(bannerType!!)
        adView.adUnitId = sharedPrefConfig.appDetails.admobBannerAd
        Logger.e("ADIDSSSS", "BANNER   ${adView.adUnitId}")

        frameLayout.removeAllViews()
        frameLayout.addView(adView)

        rlBanner.visibility = View.VISIBLE
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)

        adView.adListener = object : AdListener() {
            override fun onAdClicked() {
                super.onAdClicked()
            }

            override fun onAdClosed() {
                super.onAdClosed()
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                super.onAdFailedToLoad(loadAdError)
                rlLoader.visibility = View.GONE
                rlBanner.visibility = View.GONE
            }

            override fun onAdImpression() {
                super.onAdImpression()
                val eventParams = mapOf("ADIMPRESSION" to "BANNER")
                activity?.let { logCustomEvent(it, "ADS_EVENT", eventParams) }
                Log.i(ADSLOG, "onAdImpression: Banner")

            }

            override fun onAdLoaded() {
                super.onAdLoaded()
                Log.i(ADSLOG, "onAdLoaded: Banner")
                rlLoader.visibility = View.GONE
                rlBanner.visibility = View.VISIBLE
            }

            override fun onAdOpened() {
                super.onAdOpened()
            }
        }
    }
}
