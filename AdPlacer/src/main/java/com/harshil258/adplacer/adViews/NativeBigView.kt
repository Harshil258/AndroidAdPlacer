package com.harshil258.adplacer.adViews

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.RelativeLayout
import com.harshil258.adplacer.interfaces.AdCallback
import com.harshil258.adplacer.utils.Logger
import com.harshil258.adplacer.R
import com.harshil258.adplacer.app.AdPlacerApplication

class NativeBigView : RelativeLayout {
    private var shouldLoadDirect = false

    private var rlNativeBig: RelativeLayout? = null

    private var frameNativebig: FrameLayout? = null

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
        val rootView = inflater.inflate(R.layout.ad_view_native_big, this, true)

        rlNativeBig = rootView.findViewById(R.id.rlNativeBig)
        frameNativebig = rootView.findViewById(R.id.frameNativeBig)
        val a = context.obtainStyledAttributes(attrs, R.styleable.NativeBigView)
        shouldLoadDirect = a.getBoolean(R.styleable.NativeBigView_shouldLoadDirectBig, true)
        a.recycle()

        // Check the shouldLoadDirect property and load the ad accordingly
        if (shouldLoadDirect) {
            loadAd(context as Activity, object : AdCallback {
                override fun onAdDisplayed(isDisplayed: Boolean) {

                }
            })
        }
    }

    fun loadAd(activity: Activity?, adDisplayedCallback: AdCallback) {
        try {
            AdPlacerApplication.getInstance().nativeAdManager.showAdIfLoadedBig(
                activity!!,
                rlNativeBig!!,
                frameNativebig!!,
                adDisplayedCallback
            )
        } catch (e: Exception) {
            Logger.i("APPFUTIGAI", "${e.message}")
        }
    }
}