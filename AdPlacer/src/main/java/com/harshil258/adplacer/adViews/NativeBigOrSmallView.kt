package com.harshil258.adplacer.adViews

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import com.harshil258.adplacer.interfaces.AdCallback
import com.harshil258.adplacer.R
import com.harshil258.adplacer.app.AdPlacerApplication

class NativeBigOrSmallView : RelativeLayout {
    private var shouldLoadDirect = false

    private var myAdViewBig: NativeBigView? = null
    private var myAdViewSmall: NativeSmallView? = null


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
        val rootView = inflater.inflate(R.layout.ad_view_native_bigorsmall, this, true)
//
        myAdViewBig = rootView.findViewById(R.id.myAdViewBig)
        myAdViewSmall = rootView.findViewById(R.id.myAdViewSmall)

        val a = context.obtainStyledAttributes(attrs, R.styleable.NativeBigOrSmallView)
        shouldLoadDirect = a.getBoolean(R.styleable.NativeBigOrSmallView_shouldLoadDirect, false)
        a.recycle()

        // Check the shouldLoadDirect property and load the ad accordingly
        if (shouldLoadDirect) {
            loadAd(context as Activity, object : AdCallback {
                override fun adDisplayedCallback(displayed: Boolean) {

                }
            })
        }
    }


    fun loadAd(
        activity: Activity?, adDisplayedCallback: AdCallback
    ) {

        try {
            AdPlacerApplication.adPlacerApplication.nativeAdManager.callBigOrSmall(
                activity!!,
                myAdViewBig!!,
                myAdViewSmall!!,
                adDisplayedCallback
            )
        } catch (e: Exception) {
            com.harshil258.adplacer.utils.Logger.e("TAG123", "loadAd:e    ${e}")
        }



    }
}