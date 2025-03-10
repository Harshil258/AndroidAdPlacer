package com.harshil258.androidadplacer

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.harshil258.adplacer.adClass.InterstitialManager.InterstitialAdCallback
import com.harshil258.adplacer.app.AdPlacerApplication
import com.harshil258.adplacer.utils.Constants

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
//        sharedPrefConfig.appDetails.adStatus = "OFF"
//        sharedPrefConfig.appDetails = sharedPrefConfig.appDetails.copy(adStatus = "ON")
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        Handler(Looper.getMainLooper()).postDelayed({
//            adPlacerApplication.openAdInspector()
        }, 5000)


        findViewById<Button>(R.id.next).setOnClickListener {
            AdPlacerApplication.getInstance().interstitialAdManager.loadAndDisplayInterstitialAd(
                Constants.currentActivity!!,
                object :
                    InterstitialAdCallback {
                    override fun onContinueFlow() {
                        Intent(
                            this@MainActivity,
                            MainActivity2::class.java
                        ).apply { startActivity(this) }
                    }
                })
        }


    }
}