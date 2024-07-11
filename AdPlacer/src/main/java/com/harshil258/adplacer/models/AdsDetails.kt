package com.harshil258.adplacer.models

data class AdsDetails(
    val largeNativeAds: List<NativeAd> = ArrayList(),
    val mediumNativeAds: List<NativeAd> = ArrayList(),
    val bannerAds: List<String> = ArrayList(),
    val isManualAdEnabled: String = ""
)

data class NativeAd(
    val imageUrl: String,
    val description: String,
    val title: String,
    val openLink: String
)
