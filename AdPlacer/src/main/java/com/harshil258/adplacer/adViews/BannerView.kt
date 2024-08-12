package com.harshil258.adplacer.adViews

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.RelativeLayout
import com.harshil258.adplacer.adClass.BannerAdManager
import com.harshil258.adplacer.R
import com.google.android.gms.ads.AdSize

class BannerView : RelativeLayout {
    var isShouldLoadDirect: Boolean = false

    private var rlBanner: RelativeLayout? = null
    private var rlLoader: RelativeLayout? = null

    private var frameBanner: FrameLayout? = null

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        val inflater = LayoutInflater.from(context)
        val rootView = inflater.inflate(R.layout.ad_view_banner_view, this, true)

        rlBanner = rootView.findViewById(R.id.rlBanner)
        rlLoader = rootView.findViewById(R.id.rlLoader)
        frameBanner = rootView.findViewById(R.id.frameBanner)

        val a = context.obtainStyledAttributes(attrs, R.styleable.BannerView)
        isShouldLoadDirect = a.getBoolean(R.styleable.BannerView_shouldLoadDirectBanner, true)

        if (isShouldLoadDirect) {
            loadAd(context as Activity)
        }
    }

    fun loadAd(activity: Activity?) {
        BannerAdManager().showBannerAd(
            activity!!,
            rlBanner!!,
            frameBanner!!,
            rlLoader!!,
            AdSize.BANNER
        )
    }
}