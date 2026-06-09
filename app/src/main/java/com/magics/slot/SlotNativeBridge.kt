package com.magics.slot

// ─────────────────────────────────────────────────────────────────────────────
//  SlotNativeBridge.kt  –  JNI declarations  (package: com.magics.slot)
// ─────────────────────────────────────────────────────────────────────────────
object SlotNativeBridge {

    init { System.loadLibrary("magicsslot") }

    // Lifecycle
    external fun nativeInit()
    external fun nativeCleanup()

    // Surface
    external fun nativeSurfaceCreated(w: Int, h: Int)
    external fun nativeSurfaceChanged(w: Int, h: Int)
    external fun nativeDrawFrame(dt: Float)

    // Game controls
    external fun nativeSpin(): Boolean
    external fun nativeSetBet(bet: Double)
    external fun nativeSetBalance(balance: Double)
    external fun nativeGetBalance(): Double
    external fun nativeGetBet(): Double
    external fun nativeIsSpinning(): Boolean

    // Results
    external fun nativeIsSpinComplete(): Boolean
    external fun nativeGetLastWin(): Float
    external fun nativeIsJackpot(): Boolean
    external fun nativeFreeSpinsAwarded(): Int
    external fun nativeInFreeSpins(): Boolean
    external fun nativeFreeSpinsLeft(): Int

    // Audio
    external fun nativeSetMuted(muted: Boolean)
    external fun nativeSetVolume(vol: Float)
    external fun nativePlaySound(soundId: Int)
    external fun nativeStartMusic()
    external fun nativeStopMusic()

    // Renderer
    external fun nativeSetBloom(strength: Float)
    external fun nativeSetPostFX(enabled: Boolean)
    external fun nativeTriggerWinFX(count: Int)
    external fun nativeTriggerJackpotFX()

    object Sound {
        const val SPIN_TICK  = 0
        const val REEL_STOP  = 1
        const val WIN_SMALL  = 2
        const val WIN_BIG    = 3
        const val JACKPOT    = 4
        const val BTN_CLICK  = 5
        const val FREE_SPIN  = 6
        const val COIN_DROP  = 7
    }
}
