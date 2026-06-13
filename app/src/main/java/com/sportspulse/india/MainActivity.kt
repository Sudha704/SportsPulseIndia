package com.sportspulse.india

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.sportspulse.india.core.ui.theme.SportsPulseIndiaTheme
import com.sportspulse.india.navigation.SportsPulseNavGraph
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install the Android 12+ splash screen API before super.onCreate()
        installSplashScreen()
        
        super.onCreate(savedInstanceState)
        
        setContent {
            SportsPulseIndiaTheme {
                SportsPulseNavGraph()
            }
        }
    }
}
