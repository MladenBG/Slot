package com.magics.slot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import com.magics.slot.ui.SlotScreen
import com.magics.slot.ui.theme.MagicsSlotTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
//  MainActivity.kt  –  package com.magics.slot
// ─────────────────────────────────────────────────────────────────────────────
class MainActivity : ComponentActivity() {

    private val vm: SlotViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MagicsSlotTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color    = Color.Transparent
                ) {
                    when (vm.currentScreen) {
                        AppScreen.LOBBY -> {
                            val uiState by vm.state.collectAsState()
                            com.magics.slot.ui.screens.LobbyScreen(
                                balance = uiState.balance,
                                onRefill = { vm.refillCredits(1000.0) },
                                onSlotSelected = { type ->
                                    vm.startSlot(type)
                                }
                            )
                        }
                        AppScreen.SLOT -> {
                            SlotScreen(viewModel = vm, onBackToLobby = {
                                vm.goToLobby()
                            })
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch(Dispatchers.Default) {
            SlotNativeBridge.nativeStartMusic()
        }
    }

    override fun onPause() {
        super.onPause()
        lifecycleScope.launch(Dispatchers.Default) {
            SlotNativeBridge.nativeStopMusic()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleScope.launch(Dispatchers.Default) {
            SlotNativeBridge.nativeCleanup()
        }
    }
}

