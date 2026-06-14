package com.sportspulse.india

import android.app.KeyguardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.sportspulse.india.core.data.preferences.AppLockTrigger
import com.sportspulse.india.core.data.preferences.ThemeMode
import com.sportspulse.india.core.data.preferences.ThemePreferences
import com.sportspulse.india.core.ui.theme.SportsPulseIndiaTheme
import com.sportspulse.india.features.settings.presentation.AppLockScreen
import com.sportspulse.india.navigation.SportsPulseNavGraph
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var themePreferences: ThemePreferences

    /** Whether the app is currently locked and needs authentication */
    private var isLocked = mutableStateOf(false)

    /** Tracks whether this is the first resume (cold start) */
    private var isColdStart = true

    /** Activity result launcher for device credential prompt */
    private val authLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            isLocked.value = false
        }
        // If cancelled/failed, keep locked
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install the Android 12+ splash screen API before super.onCreate()
        installSplashScreen()
        
        super.onCreate(savedInstanceState)

        // Check if app lock is enabled and lock on cold start
        val appLockEnabled = runBlocking { themePreferences.appLockEnabled.first() }
        if (appLockEnabled) {
            isLocked.value = true
        }

        // Observe process lifecycle for foreground/background detection
        setupProcessLifecycleObserver()

        setContent {
            val themeMode by themePreferences.themeMode.collectAsState(initial = ThemeMode.DARK)

            SportsPulseIndiaTheme(themeMode = themeMode) {
                Box(modifier = Modifier.fillMaxSize()) {
                    SportsPulseNavGraph()
                    
                    if (isLocked.value) {
                        AppLockScreen(
                            onUnlockClicked = { triggerDeviceCredentialPrompt() }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Mark cold start complete after first resume
        if (isColdStart) {
            isColdStart = false
        }
    }

    /**
     * Sets up a process lifecycle observer to detect when the app moves to foreground.
     * Locks the app based on the user's chosen lock trigger mode.
     */
    private fun setupProcessLifecycleObserver() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_START) {
                    // App came to foreground
                    val lockEnabled = runBlocking { themePreferences.appLockEnabled.first() }
                    val lockTrigger = runBlocking { themePreferences.appLockTrigger.first() }

                    if (lockEnabled) {
                        when (lockTrigger) {
                            AppLockTrigger.ON_EVERY_RESUME -> {
                                // Always lock on foreground resume (skip the initial cold start
                                // since it's already handled in onCreate)
                                if (!isColdStart) {
                                    isLocked.value = true
                                }
                            }
                            AppLockTrigger.ON_LAUNCH -> {
                                // Only lock on cold start (handled in onCreate, no-op here)
                            }
                        }
                    }
                }
            }
        )
    }

    /**
     * Triggers the system device credential authentication prompt
     * (fingerprint, face, PIN, or pattern — whatever the user has configured).
     */
    private fun triggerDeviceCredentialPrompt() {
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

        if (keyguardManager.isDeviceSecure) {
            @Suppress("DEPRECATION")
            val intent = keyguardManager.createConfirmDeviceCredentialIntent(
                "SportsPulse India",
                "Authenticate to access BharatSportsAI"
            )
            if (intent != null) {
                authLauncher.launch(intent)
            } else {
                // Fallback: if intent is null, just unlock
                isLocked.value = false
            }
        } else {
            // No device lock configured — just unlock
            isLocked.value = false
        }
    }
}
