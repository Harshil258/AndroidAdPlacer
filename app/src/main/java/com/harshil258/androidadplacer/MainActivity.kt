package com.harshil258.androidadplacer

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.harshil258.adplacer.interfaces.InterAdCallBack
import com.harshil258.adplacer.utils.Constants.runningActivity
import com.harshil258.adplacer.utils.Constants.adPlacerApplication
import com.harshil258.adplacer.utils.SharedPrefConfig.Companion.sharedPrefConfig

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        sharedPrefConfig.appDetails.adStatus = "OFF"
        sharedPrefConfig.appDetails = sharedPrefConfig.appDetails.copy(adStatus = "OFF")
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        findViewById<Button>(R.id.next).setOnClickListener {
            adPlacerApplication.interstitialManager.loadAndShowInter(runningActivity!!, object :
                InterAdCallBack {
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