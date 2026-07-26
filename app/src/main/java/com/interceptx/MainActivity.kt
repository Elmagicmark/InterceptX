package com.interceptx

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.interceptx.ui.nav.InterceptXNavHost
import com.interceptx.ui.theme.InterceptXTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as InterceptXApp
        setContent {
            InterceptXTheme {
                InterceptXNavHost(app)
            }
        }
    }
}
