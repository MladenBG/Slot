// ─────────────────────────────────────────────────────────────────────────────
//  jni_bridge.cpp  –  JNI bridge  com.magics.slot <-> C++ engine
// ─────────────────────────────────────────────────────────────────────────────
#include <jni.h>
#include <android/log.h>
#include <memory>
#include <mutex>

#include "SlotEngine.h"
#include "Renderer.h"
#include "AudioEngine.h"

#define LOG_TAG "MagicsSlot"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// ── Global singletons ─────────────────────────────────────────────────────────
static std::unique_ptr<MagicsSlot::SlotEngine>  gEngine;
static std::unique_ptr<MagicsSlot::Renderer>    gRenderer;
static std::unique_ptr<MagicsSlot::AudioEngine> gAudio;
static std::mutex gMtx;

// ── Cached spin result ────────────────────────────────────────────────────────
static float gLastWin    = 0.f;
static bool  gJackpot    = false;
static int   gFreeSpins  = 0;
static bool  gSpinDone   = false;

// ─────────────────────────────────────────────────────────────────────────────
#define PKG Java_com_magics_slot_SlotNativeBridge

extern "C" {

// ── Lifecycle ─────────────────────────────────────────────────────────────────
JNIEXPORT void JNICALL PKG_nativeInit(JNIEnv*, jobject) {
    std::lock_guard<std::mutex> lk(gMtx);
    gEngine   = std::make_unique<MagicsSlot::SlotEngine>();
    gRenderer = std::make_unique<MagicsSlot::Renderer>();
    gAudio    = std::make_unique<MagicsSlot::AudioEngine>();
    gAudio->init();

    gEngine->onSpinComplete = [](const MagicsSlot::SpinResult& r){
        std::lock_guard<std::mutex> lk2(gMtx);
        gLastWin   = r.totalWin;
        gJackpot   = r.jackpot;
        gFreeSpins = r.freeSpins;
        gSpinDone  = true;
    };
    LOGI("nativeInit OK");
}

JNIEXPORT void JNICALL PKG_nativeCleanup(JNIEnv*, jobject) {
    std::lock_guard<std::mutex> lk(gMtx);
    gRenderer.reset(); gEngine.reset(); gAudio.reset();
}

// ── Surface ───────────────────────────────────────────────────────────────────
JNIEXPORT void JNICALL PKG_nativeSurfaceCreated(JNIEnv*, jobject, jint w, jint h) {
    std::lock_guard<std::mutex> lk(gMtx);
    if(gRenderer) gRenderer->init(w, h);
}
JNIEXPORT void JNICALL PKG_nativeSurfaceChanged(JNIEnv*, jobject, jint w, jint h) {
    std::lock_guard<std::mutex> lk(gMtx);
    if(gRenderer) gRenderer->resize(w, h);
}
JNIEXPORT void JNICALL PKG_nativeDrawFrame(JNIEnv*, jobject, jfloat dt) {
    std::lock_guard<std::mutex> lk(gMtx);
    if(gEngine && gRenderer){ gEngine->update(dt); gRenderer->drawFrame(dt, *gEngine); }
}

// ── Game controls ─────────────────────────────────────────────────────────────
JNIEXPORT jboolean JNICALL PKG_nativeSpin(JNIEnv*, jobject) {
    std::lock_guard<std::mutex> lk(gMtx);
    if(!gEngine||!gEngine->canSpin()) return JNI_FALSE;
    gSpinDone=false;
    gEngine->startSpin();
    if(gAudio) gAudio->play(MagicsSlot::SoundId::SPIN_TICK);
    return JNI_TRUE;
}
JNIEXPORT void JNICALL PKG_nativeSetBet(JNIEnv*, jobject, jdouble bet) {
    std::lock_guard<std::mutex> lk(gMtx);
    if(gEngine) gEngine->setBet(bet);
}
JNIEXPORT void JNICALL PKG_nativeSetBalance(JNIEnv*, jobject, jdouble bal) {
    std::lock_guard<std::mutex> lk(gMtx);
    if(gEngine) gEngine->setBalance(bal);
}
JNIEXPORT jdouble JNICALL PKG_nativeGetBalance(JNIEnv*, jobject) {
    std::lock_guard<std::mutex> lk(gMtx);
    return gEngine ? gEngine->getBalance() : 0.0;
}
JNIEXPORT jdouble JNICALL PKG_nativeGetBet(JNIEnv*, jobject) {
    std::lock_guard<std::mutex> lk(gMtx);
    return gEngine ? gEngine->getBet() : 1.0;
}
JNIEXPORT jboolean JNICALL PKG_nativeIsSpinning(JNIEnv*, jobject) {
    std::lock_guard<std::mutex> lk(gMtx);
    return (gEngine && gEngine->isSpinning()) ? JNI_TRUE : JNI_FALSE;
}

// ── Results ───────────────────────────────────────────────────────────────────
JNIEXPORT jboolean JNICALL PKG_nativeIsSpinComplete(JNIEnv*, jobject) {
    std::lock_guard<std::mutex> lk(gMtx);
    if(gSpinDone){ gSpinDone=false; return JNI_TRUE; }
    return JNI_FALSE;
}
JNIEXPORT jfloat JNICALL PKG_nativeGetLastWin(JNIEnv*, jobject) {
    std::lock_guard<std::mutex> lk(gMtx); return gLastWin;
}
JNIEXPORT jboolean JNICALL PKG_nativeIsJackpot(JNIEnv*, jobject) {
    std::lock_guard<std::mutex> lk(gMtx); return gJackpot ? JNI_TRUE : JNI_FALSE;
}
JNIEXPORT jint JNICALL PKG_nativeFreeSpinsAwarded(JNIEnv*, jobject) {
    std::lock_guard<std::mutex> lk(gMtx); return gFreeSpins;
}
JNIEXPORT jboolean JNICALL PKG_nativeInFreeSpins(JNIEnv*, jobject) {
    std::lock_guard<std::mutex> lk(gMtx);
    return (gEngine&&gEngine->inFreeSpins()) ? JNI_TRUE : JNI_FALSE;
}
JNIEXPORT jint JNICALL PKG_nativeFreeSpinsLeft(JNIEnv*, jobject) {
    std::lock_guard<std::mutex> lk(gMtx);
    return gEngine ? gEngine->freeSpinsLeft() : 0;
}

// ── Audio ─────────────────────────────────────────────────────────────────────
JNIEXPORT void JNICALL PKG_nativeSetMuted(JNIEnv*, jobject, jboolean m) {
    if(gAudio) gAudio->setMuted(m);
}
JNIEXPORT void JNICALL PKG_nativeSetVolume(JNIEnv*, jobject, jfloat v) {
    if(gAudio) gAudio->setVolume(v);
}
JNIEXPORT void JNICALL PKG_nativePlaySound(JNIEnv*, jobject, jint id) {
    if(gAudio) gAudio->play(static_cast<MagicsSlot::SoundId>(id));
}
JNIEXPORT void JNICALL PKG_nativeStartMusic(JNIEnv*, jobject) {
    if(gAudio) gAudio->startMusic();
}
JNIEXPORT void JNICALL PKG_nativeStopMusic(JNIEnv*, jobject) {
    if(gAudio) gAudio->stopMusic();
}

// ── Renderer config ───────────────────────────────────────────────────────────
JNIEXPORT void JNICALL PKG_nativeSetBloom(JNIEnv*, jobject, jfloat v) {
    if(gRenderer) gRenderer->bloomStrength=v;
}
JNIEXPORT void JNICALL PKG_nativeSetPostFX(JNIEnv*, jobject, jboolean en) {
    if(gRenderer) gRenderer->postFX=en;
}
JNIEXPORT void JNICALL PKG_nativeTriggerWinFX(JNIEnv*, jobject, jint count) {
    std::lock_guard<std::mutex> lk(gMtx);
    if(gRenderer) gRenderer->triggerWinFX(count);
}
JNIEXPORT void JNICALL PKG_nativeTriggerJackpotFX(JNIEnv*, jobject) {
    std::lock_guard<std::mutex> lk(gMtx);
    if(gRenderer) gRenderer->triggerJackpotFX();
}

} // extern "C"
