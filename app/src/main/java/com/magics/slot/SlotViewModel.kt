package com.magics.slot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

// ─────────────────────────────────────────────────────────────────────────────
//  SlotViewModel.kt
// ─────────────────────────────────────────────────────────────────────────────

enum class AppScreen { LOBBY, SLOT }

enum class SlotType(
    val id: Int, 
    val title: String, 
    val reels: Int,
    val startingBalance: Double,
    val betSteps: List<Double>,
    val defaultBetIdx: Int,
    val vipBadge: String
) {
    WILD(
        id = 0, 
        title = "WILD 777", 
        reels = 5, 
        startingBalance = 1000.0,
        betSteps = listOf(0.10, 0.25, 0.50, 1.0, 2.0, 5.0, 10.0),
        defaultBetIdx = 3, // $1.00 / line ($20.00 total)
        vipBadge = "FEATURED CLASSIC 🔥"
    ),
    LAS_VEGAS(
        id = 1, 
        title = "LAS VEGAS 3-REEL", 
        reels = 3, 
        startingBalance = 2500.0,
        betSteps = listOf(0.50, 1.0, 2.50, 5.0, 10.0, 25.0, 50.0),
        defaultBetIdx = 2, // $2.50 / line ($50.00 total)
        vipBadge = "CLASSIC VEGAS 🎰"
    ),
    PHARAOH(
        id = 2, 
        title = "PHARAOH'S TREASURE", 
        reels = 5, 
        startingBalance = 5000.0,
        betSteps = listOf(1.0, 2.50, 5.0, 10.0, 25.0, 50.0, 100.0),
        defaultBetIdx = 3, // $10.00 / line ($200.00 total)
        vipBadge = "HIGH ROLLER 👑"
    ),
    NEON_RUSH(
        id = 3, 
        title = "NEON RUSH CYBER", 
        reels = 5, 
        startingBalance = 10000.0,
        betSteps = listOf(5.0, 10.0, 25.0, 50.0, 100.0, 250.0, 500.0),
        defaultBetIdx = 2, // $25.00 / line ($500.00 total)
        vipBadge = "ULTRA VIP HIGH STAKES ⚡"
    ),
    OCEAN(
        id = 4, 
        title = "OCEAN PEARL LUXURY", 
        reels = 5, 
        startingBalance = 7500.0,
        betSteps = listOf(2.0, 5.0, 10.0, 20.0, 50.0, 100.0, 200.0),
        defaultBetIdx = 2, // $10.00 / line ($200.00 total)
        vipBadge = "DIAMOND VIP 🌊"
    )
}

enum class SpinState { IDLE, SPINNING, WIN_ANIM, JACKPOT, FREE_SPINS, GAME_OVER }

data class SlotUiState(
    val balance       : Double    = 1000.0,
    val bet           : Double    = 1.0,
    val totalBet      : Double    = 20.0,
    val lastWin       : Float     = 0f,
    val spinState     : SpinState = SpinState.IDLE,
    val freeSpinsLeft : Int       = 0,
    val isJackpot     : Boolean   = false,
    val autoSpin      : Boolean   = false,
    val isMuted       : Boolean   = false,
    val postFX        : Boolean   = true,
    val message       : String    = "",
    val showWin       : Boolean   = false,
    val showHelp      : Boolean   = false,
    val grid          : IntArray  = IntArray(15) { 0 },
)

class SlotViewModel : ViewModel() {

    private val _state = MutableStateFlow(SlotUiState())
    val state: StateFlow<SlotUiState> = _state.asStateFlow()

    private var betIdx = 3
    private var pollJob: Job? = null
    
    // Persistent player wallet balance
    var playerWalletBalance = 1000.0
        private set

    // UI Navigation State
    var currentScreen by androidx.compose.runtime.mutableStateOf(AppScreen.LOBBY)
        private set
    var currentSlotType by androidx.compose.runtime.mutableStateOf(SlotType.WILD)
        private set

    fun startSlot(type: SlotType) {
        currentSlotType = type
        betIdx = type.defaultBetIdx
        val currentBet = type.betSteps[betIdx]
        val totalBet = currentBet * 20.0

        // If player balance is lower than this table's starting balance, upgrade to table's starting balance!
        if (playerWalletBalance < type.startingBalance) {
            playerWalletBalance = type.startingBalance
        }

        // Synchronously reset UI state so GAME OVER never lingers from previous screens
        _state.value = _state.value.copy(
            balance = playerWalletBalance,
            bet = currentBet,
            totalBet = totalBet,
            spinState = SpinState.IDLE,
            message = "",
            showWin = false
        )
        currentScreen = AppScreen.SLOT

        viewModelScope.launch(Dispatchers.Default) {
            SlotNativeBridge.nativeInit(type.id)
            SlotNativeBridge.nativeSetBalance(playerWalletBalance)
            SlotNativeBridge.nativeSetBet(currentBet)
            val initialGrid = SlotNativeBridge.nativeGetGrid()
            _state.value = _state.value.copy(
                grid = initialGrid
            )
        }
    }

    fun goToLobby() {
        playerWalletBalance = _state.value.balance
        _state.value = _state.value.copy(
            spinState = SpinState.IDLE,
            message = "",
            showWin = false
        )
        viewModelScope.launch(Dispatchers.Default) {
            SlotNativeBridge.nativeCleanup()
        }
        currentScreen = AppScreen.LOBBY
    }

    // Refill player credits in the Lobby
    fun refillCredits(amount: Double = 1000.0) {
        playerWalletBalance += amount
        _state.value = _state.value.copy(
            balance = playerWalletBalance,
            spinState = SpinState.IDLE,
            message = ""
        )
    }

    // ── Spin ──────────────────────────────────────────────────────────────────
    fun spin() {
        if (_state.value.spinState == SpinState.SPINNING) return

        // Standard casino rule: If balance is insufficient and not in free spins, game over
        if (_state.value.balance < _state.value.totalBet && _state.value.freeSpinsLeft == 0) {
            _state.value = _state.value.copy(spinState = SpinState.GAME_OVER)
            return
        }

        viewModelScope.launch(Dispatchers.Default) {
            val started = SlotNativeBridge.nativeSpin()
            if (!started) {
                _state.value = _state.value.copy(spinState = SpinState.GAME_OVER)
                return@launch
            }
            SlotNativeBridge.nativePlaySound(SlotNativeBridge.Sound.SPIN_TICK)
            val newGrid = SlotNativeBridge.nativeGetGrid()
            _state.value = _state.value.copy(
                spinState = SpinState.SPINNING,
                grid      = newGrid,
                lastWin   = 0f,
                showWin   = false,
                message   = if (_state.value.freeSpinsLeft > 0) "🌀 FREE SPIN!" else ""
            )
            
            // Wait for all staggered reels to complete their long energetic spin and mechanical bounce
            val settleDelay = if (currentSlotType.reels == 3) 3100L else 4150L
            delay(settleDelay)
            
            handleResult()
        }
    }

    private fun handleResult() {
        val win      = SlotNativeBridge.nativeGetLastWin()
        val jackpot  = SlotNativeBridge.nativeIsJackpot()
        val freeAw   = SlotNativeBridge.nativeFreeSpinsAwarded()
        val balance  = SlotNativeBridge.nativeGetBalance()
        val freeLeft = SlotNativeBridge.nativeFreeSpinsLeft()
        val finalGrid = SlotNativeBridge.nativeGetGrid()

        playerWalletBalance = balance

        // Visual FX triggers
        if (jackpot) SlotNativeBridge.nativeTriggerJackpotFX()
        else if (win > 0f) SlotNativeBridge.nativeTriggerWinFX(if (win > 50f) 5 else 2)

        // Sound triggers
        when {
            jackpot   -> SlotNativeBridge.nativePlaySound(SlotNativeBridge.Sound.JACKPOT)
            win > 50f -> SlotNativeBridge.nativePlaySound(SlotNativeBridge.Sound.WIN_BIG)
            win > 0f  -> SlotNativeBridge.nativePlaySound(SlotNativeBridge.Sound.WIN_SMALL)
        }

        val next = when {
            jackpot      -> SpinState.JACKPOT
            freeAw > 0   -> SpinState.FREE_SPINS
            win > 0f     -> SpinState.WIN_ANIM
            else         -> SpinState.IDLE
        }
        val msg = when {
            jackpot    -> "🎰 MEGA JACKPOT!  +${win.toInt()} kredita!"
            freeAw > 0 -> "🌀 FREE SPINS × $freeAw"
            win > 0f   -> "WIN!  +${String.format("%.2f", win)}"
            else       -> ""
        }

        _state.value = _state.value.copy(
            balance       = balance,
            lastWin       = win,
            spinState     = next,
            freeSpinsLeft = freeLeft,
            isJackpot     = jackpot,
            message       = msg,
            showWin       = win > 0f,
            grid          = finalGrid
        )

        if (next != SpinState.IDLE) {
            viewModelScope.launch(Dispatchers.Default) {
                delay(if (jackpot) 4000L else 2200L)
                _state.value = _state.value.copy(spinState = SpinState.IDLE, showWin = false)
                if (_state.value.autoSpin) { delay(500); spin() }
            }
        } else if (_state.value.autoSpin && balance >= _state.value.totalBet) {
            viewModelScope.launch(Dispatchers.Default) { delay(700); spin() }
        }
    }

    // ── Bet ───────────────────────────────────────────────────────────────────
    fun betUp() {
        val steps = currentSlotType.betSteps
        if (betIdx < steps.lastIndex) { 
            betIdx++
            applyBet() 
        }
        viewModelScope.launch(Dispatchers.Default) {
            SlotNativeBridge.nativePlaySound(SlotNativeBridge.Sound.BTN_CLICK)
        }
    }
    fun betDown() {
        val steps = currentSlotType.betSteps
        if (betIdx > 0) { 
            betIdx--
            applyBet() 
        }
        viewModelScope.launch(Dispatchers.Default) {
            SlotNativeBridge.nativePlaySound(SlotNativeBridge.Sound.BTN_CLICK)
        }
    }
    private fun applyBet() {
        val steps = currentSlotType.betSteps
        val b = steps[betIdx]
        viewModelScope.launch(Dispatchers.Default) {
            SlotNativeBridge.nativeSetBet(b)
        }
        _state.value = _state.value.copy(bet = b, totalBet = b * 20.0, message = "")
    }

    fun toggleAutoSpin() {
        val v = !_state.value.autoSpin
        _state.value = _state.value.copy(autoSpin = v)
        viewModelScope.launch(Dispatchers.Default) {
            SlotNativeBridge.nativePlaySound(SlotNativeBridge.Sound.BTN_CLICK)
        }
        if (v && _state.value.spinState == SpinState.IDLE) spin()
    }

    fun maxBet() {
        val steps = currentSlotType.betSteps
        if (betIdx < steps.lastIndex) {
            betIdx = steps.lastIndex
            applyBet()
        }
        viewModelScope.launch(Dispatchers.Default) {
            SlotNativeBridge.nativePlaySound(SlotNativeBridge.Sound.BTN_CLICK)
        }
    }

    fun toggleHelp() {
        val v = !_state.value.showHelp
        _state.value = _state.value.copy(showHelp = v)
        viewModelScope.launch(Dispatchers.Default) {
            SlotNativeBridge.nativePlaySound(SlotNativeBridge.Sound.BTN_CLICK)
        }
    }

    // ── Mute / FX ─────────────────────────────────────────────────────────────
    fun toggleMute() {
        val v = !_state.value.isMuted
        viewModelScope.launch(Dispatchers.Default) {
            SlotNativeBridge.nativeSetMuted(v)
        }
        _state.value = _state.value.copy(isMuted = v)
    }
    fun togglePostFX() {
        val v = !_state.value.postFX
        viewModelScope.launch(Dispatchers.Default) {
            SlotNativeBridge.nativeSetPostFX(v)
        }
        _state.value = _state.value.copy(postFX = v)
    }

    override fun onCleared() { super.onCleared(); pollJob?.cancel() }
}



