package com.example.micromobility

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import com.example.micromobility.di.MicromobilityApplication
import com.example.micromobility.ui.navigation.AppNavGraph
import com.example.micromobility.ui.theme.Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        val container = (application as MicromobilityApplication).container

        setContent {
            Theme(darkTheme = false) {
                Surface {
                    AppNavGraph(container = container)
                }
            }
        }
    }
}