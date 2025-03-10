package com.harshil258.adplacer.adViews

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import com.harshil258.adplacer.interfaces.AdCallback
import com.harshil258.adplacer.R
import com.harshil258.adplacer.app.AdPlacerApplication

class NativeMediumOrSmallView : RelativeLayout {
    private var shouldLoadDirect = false

    private var myAdViewMedium: NativeMediumView? = null
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
        val rootView = inflater.inflate(R.layout.ad_view_native_mediumorsmall, this, true)

        myAdViewMedium = rootView.findViewById(R.id.myAdViewMedium)
        myAdViewSmall = rootView.findViewById(R.id.myAdViewSmall)

        val a = context.obtainStyledAttributes(attrs, R.styleable.NativeMediumOrSmallView)
        shouldLoadDirect =
            a.getBoolean(R.styleable.NativeMediumOrSmallView_shouldLoadDirectMS, false)
        a.recycle()

        // Check the shouldLoadDirect property and load the ad accordingly
        if (shouldLoadDirect) {
            loadAd(context as Activity, object : AdCallback {
                override fun onAdDisplayed(isDisplayed: Boolean) {

                }
            })
        }
    }

    fun loadAd(
        activity: Activity?, adDisplayedCallback: AdCallback
    ) {
        AdPlacerApplication.getInstance().nativeAdManager.callMediumOrSmall(
            activity!!,
            myAdViewMedium!!,
            myAdViewSmall!!, adDisplayedCallback
        )
    }
}