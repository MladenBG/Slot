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

// ─────────────────────────────────────────────────────────────────────────────
//  SlotViewModel.kt
// ─────────────────────────────────────────────────────────────────────────────

enum class SpinState { IDLE, SPINNING, WIN_ANIM, JACKPOT, FREE_SPINS }

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
)

class SlotViewModel : ViewModel() {

    private val _state = MutableStateFlow(SlotUiState())
    val state: StateFlow<SlotUiState> = _state.asStateFlow()

    private val betSteps = listOf(0.10, 0.25, 0.50, 1.0, 2.0, 5.0, 10.0)
    private var betIdx = 3
    private var pollJob: Job? = null

    // ── Spin ──────────────────────────────────────────────────────────────────
    fun spin() {
        if (_state.value.spinState == SpinState.SPINNING) return
        viewModelScope.launch(Dispatchers.IO) {
            val started = SlotNativeBridge.nativeSpin()
            if (!started) {
                _state.value = _state.value.copy(message = "Nema dovoljno kredita!")
                return@launch
            }
            SlotNativeBridge.nativePlaySound(SlotNativeBridge.Sound.SPIN_TICK)
            _state.value = _state.value.copy(
                spinState = SpinState.SPINNING,
                lastWin   = 0f,
                showWin   = false,
                message   = if (_state.value.freeSpinsLeft > 0) "🌀 FREE SPIN!" else ""
            )
            pollForResult()
        }
    }

    private fun pollForResult() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                delay(40)
                if (SlotNativeBridge.nativeIsSpinComplete()) {
                    handleResult(); break
                }
            }
        }
    }

    private fun handleResult() {
        val win      = SlotNativeBridge.nativeGetLastWin()
        val jackpot  = SlotNativeBridge.nativeIsJackpot()
        val freeAw   = SlotNativeBridge.nativeFreeSpinsAwarded()
        val balance  = SlotNativeBridge.nativeGetBalance()
        val freeLeft = SlotNativeBridge.nativeFreeSpinsLeft()

        // Particles
        if (jackpot) SlotNativeBridge.nativeTriggerJackpotFX()
        else if (win > 0f) SlotNativeBridge.nativeTriggerWinFX(if (win > 50f) 5 else 2)

        // Sound
        when {
            jackpot   -> SlotNativeBridge.nativePlaySound(SlotNativeBridge.Sound.JACKPOT)
            win > 50f -> SlotNativeBridge.nativePlaySound(SlotNativeBridge.Sound.WIN_BIG)
            win > 0f  -> SlotNativeBridge.nativePlaySound(SlotNativeBridge.Sound.WIN_SMALL)
            else      -> SlotNativeBridge.nativePlaySound(SlotNativeBridge.Sound.REEL_STOP)
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
        )

        if (next != SpinState.IDLE) {
            viewModelScope.launch {
                delay(if (jackpot) 4000L else 2200L)
                _state.value = _state.value.copy(spinState = SpinState.IDLE, showWin = false)
                if (_state.value.autoSpin) { delay(500); spin() }
            }
        } else if (_state.value.autoSpin && balance >= _state.value.totalBet) {
            viewModelScope.launch { delay(700); spin() }
        }
    }

    // ── Bet ───────────────────────────────────────────────────────────────────
    fun betUp() {
        if (betIdx < betSteps.lastIndex) { betIdx++; applyBet() }
        SlotNativeBridge.nativePlaySound(SlotNativeBridge.Sound.BTN_CLICK)
    }
    fun betDown() {
        if (betIdx > 0) { betIdx--; applyBet() }
        SlotNativeBridge.nativePlaySound(SlotNativeBridge.Sound.BTN_CLICK)
    }
    private fun applyBet() {
        val b = betSteps[betIdx]
        SlotNativeBridge.nativeSetBet(b)
        _state.value = _state.value.copy(bet = b, totalBet = b * 20.0)
    }

    // ── Auto-spin ─────────────────────────────────────────────────────────────
    fun toggleAutoSpin() {
        val v = !_state.value.autoSpin
        _state.value = _state.value.copy(autoSpin = v)
        SlotNativeBridge.nativePlaySound(SlotNativeBridge.Sound.BTN_CLICK)
        if (v && _state.value.spinState == SpinState.IDLE) spin()
    }

    // ── Mute / FX ─────────────────────────────────────────────────────────────
    fun toggleMute() {
        val v = !_state.value.isMuted
        SlotNativeBridge.nativeSetMuted(v)
        _state.value = _state.value.copy(isMuted = v)
    }
    fun togglePostFX() {
        val v = !_state.value.postFX
        SlotNativeBridge.nativeSetPostFX(v)
        _state.value = _state.value.copy(postFX = v)
    }

    override fun onCleared() { super.onCleared(); pollJob?.cancel() }
}
