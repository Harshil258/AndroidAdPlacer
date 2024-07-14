package com.harshil258.adplacer.adViews

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.RelativeLayout
import com.harshil258.adplacer.R
import com.harshil258.adplacer.interfaces.AdCallback
import com.harshil258.adplacer.app.AdPlacerApplication
import com.harshil258.adplacer.utils.Constants.adPlacerApplication
import com.harshil258.adplacer.utils.Logger

class NativeSmallView : RelativeLayout {
    private var shouldLoadDirect = false


    private var rlNativeSmall: RelativeLayout? = null

    private var frameNativeSmall: FrameLayout? = null

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
        val rootView = inflater.inflate(R.layout.ad_view_native_small, this, true)


        rlNativeSmall = rootView.findViewById(R.id.rlNativeSmall)

        frameNativeSmall = rootView.findViewById(R.id.frameNativeSmall)

        val a = context.obtainStyledAttributes(attrs, R.styleable.NativeSmallView)
        shouldLoadDirect = a.getBoolean(R.styleable.NativeSmallView_shouldLoadDirectSmall, true)
        a.recycle()

        // Check the shouldLoadDirect property and load the ad accordingly
        if (shouldLoadDirect) {
            loadAd(context as Activity, object : AdCallback {
                override fun adDisplayedCallback(displayed: Boolean) {

                }
            })
        }
    }

    fun loadAd(activity: Activity?, adDisplayedCallback: AdCallback) {
        //        try {
        Logger.e("NATIVEADSSS", "showAdIfLoadedSmall: loadAd NativeSmallView ")
        adPlacerApplication.nativeAdManager.showAdIfLoadedSmall(
            activity!!,
            rlNativeSmall!!,
            frameNativeSmall!!,
            adDisplayedCallback
        )

        //        } catch (Exception e) {
//            Logger.e("wrhwerhwetheth", "loadAd: " + e.getMessage());
//
//
//        }
    }
}