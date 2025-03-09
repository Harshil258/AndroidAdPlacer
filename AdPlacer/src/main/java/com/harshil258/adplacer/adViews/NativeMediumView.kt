package com.harshil258.adplacer.adViews

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.RelativeLayout
import com.harshil258.adplacer.R
import com.harshil258.adplacer.interfaces.AdCallback
import com.harshil258.adplacer.app.AdPlacerApplication

class NativeMediumView : RelativeLayout {
    private var shouldLoadDirect = false

    private var rlNativeMedium: RelativeLayout? = null

    private var frameNativeMedium: FrameLayout? = null

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
        val rootView = inflater.inflate(R.layout.ad_view_native_medium, this, true)

        rlNativeMedium = rootView.findViewById(R.id.rlNativeMedium)
        frameNativeMedium = rootView.findViewById(R.id.frameNativeMedium)
        val a = context.obtainStyledAttributes(attrs, R.styleable.NativeMediumView)
        shouldLoadDirect = a.getBoolean(R.styleable.NativeMediumView_shouldLoadDirectMedium, true)
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
        AdPlacerApplication.getInstance().nativeAdManager.showAdIfLoadedMedium(
            activity!!,
            rlNativeMedium!!,
            frameNativeMedium!!,
            adDisplayedCallback
        )
    }
}