package com.magics.slot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.magics.slot.ui.SlotScreen
import com.magics.slot.ui.theme.MagicsSlotTheme

// ─────────────────────────────────────────────────────────────────────────────
//  MainActivity.kt  –  package com.magics.slot
// ─────────────────────────────────────────────────────────────────────────────
class MainActivity : ComponentActivity() {

    private val vm: SlotViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        SlotNativeBridge.nativeInit()
        setContent {
            MagicsSlotTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color    = Color.Transparent
                ) {
                    SlotScreen(viewModel = vm)
                }
            }
        }
    }

    override fun onResume()  { super.onResume();  SlotNativeBridge.nativeStartMusic() }
    override fun onPause()   { super.onPause();   SlotNativeBridge.nativeStopMusic()  }
    override fun onDestroy() { super.onDestroy(); SlotNativeBridge.nativeCleanup()    }
}
