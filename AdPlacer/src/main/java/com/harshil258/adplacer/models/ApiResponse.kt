package com.harshil258.adplacer.models


data class ApiResponse(
    val appDetails: AppDetails = AppDetails(),
    val adsDetails: AdsDetails = AdsDetails()
)

data class AppDetails(
    var appName: String = "Android Ad Placer",  // Application name
    var adStatus: String = "OFF",  // Status of the ads in the app
    var googleAdStatus: String = "OFF",  // Status of Google ads
    var gameZopeStatus: String = "OFF",  // Status of GameZope ads
    var premiumVideo: String = "",  // URL or identifier for premium videos, if any
    var useCachedNative: String = "OFF",  // Whether to use cached native ads

    var updateRequiredVersions: ArrayList<String> = ArrayList(),  // List of versions that require an update
    var forceUpdateVersions: ArrayList<String> = ArrayList(),  // List of versions that need to forcefully update
    var noUpdateRequiredVersion: ArrayList<String> = arrayListOf(),  // List of versions that need to forcefully update

    var networkProvider: String = "GOOGLE",  // Network provider identifier
    var showInterstitialOnNextButton: String = "OFF",  // Whether to show interstitial ad on next button
    var whichScreenToGo: String = SCREENS.HOME.name,
    var howtousestart: String = "",
    var admobBannerAd: String = "",  // AdMob banner ad ID
    var admobNativeAd: String = "",  // AdMob native ad ID

    var admobInterstitialAd: String = "",  // AdMob interstitial ad ID
    var admobRewardAd: String = "",  // AdMob reward ad ID
    var admobAppOpenAd: String = "",  // AdMob app open ad ID

    var nativeBigOrSmall: String = "",  // AdMob native ad ID
    var nativeMediumOrSmall: String = "",  // AdMob native ad ID
    var nativeAdGap: String = "15",  // Gap between native ads
    var interstitialAdFrequency: String = "3",  // Frequency of interstitial ads
    var rewardAdFrequency: String = "3",  // Frequency of reward ads
    var showNativeAdInRecyclerView: String = "OFF",  // Whether to show native ads in RecyclerView
    var showGoogleAdsInRecyclerView: String = "OFF",  // Whether to show Google ads in RecyclerView
    var bigNativeLayouts: String = "1",
    var smallNativeLayouts: String = "1",
    var privacyPolicyUrl: String = "",  // Privacy policy URL
    var contactEmail: String = "",  // Contact email
    var oneSignalAppId: String = "",  // OneSignal app ID
    var packageName: String = "",  // Package name
    var showAdvertiseDialog: String = "0",  // Whether to show advertise dialog
    var advertiseDialogInterval: String = "60",  // Time interval for advertise dialog
    var advertiseDialogTitle: String = "New Site",  // Title of advertise dialog
    var advertiseDialogDescription: String = "Google.com",  // Description of advertise dialog
    var advertiseDialogImageUrl: String = "",  // Image URL for advertise dialog
    var dialogPositiveLink: String = "",  // Positive action link in dialog
    var customBannerClickLink: String = "",  // Custom banner click link
    var isDefaultInAppRating: String = "OFF"
)

