package com.example.welive

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.welive.training.TrainingCaptureState
import com.example.welive.ui.WeLiveApp
import com.example.welive.ui.theme.WeLiveTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TrainingCaptureState.initialize(applicationContext)
        enableEdgeToEdge()
        setContent {
            WeLiveTheme {
                WeLiveApp()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        com.example.welive.accessibility.WeLiveAccessibilityService.dismissAllInterventions()
    }

    override fun onStop() {
        com.example.welive.accessibility.WeLiveAccessibilityService.dismissAllInterventions()
        super.onStop()
    }
}
