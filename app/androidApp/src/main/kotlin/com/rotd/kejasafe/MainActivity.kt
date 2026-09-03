package com.rotd.kejasafe

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.rotd.kejasafe.storage.AndroidActivityHolder

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App()
        }
    }

    override fun onResume() {
        super.onResume()
        AndroidActivityHolder.current = this
    }

    override fun onPause() {
        if (AndroidActivityHolder.current === this) {
            AndroidActivityHolder.current = null
        }
        super.onPause()
    }
}
