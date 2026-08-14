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
// Use the ## operator to correctly concatenate the package name and method name
#define JNI_METHOD(name) Java_com_magics_slot_SlotNativeBridge_##name

extern "C" {

// ── Lifecycle ─────────────────────────────────────────────────────────────────
JNIEXPORT void JNICALL JNI_METHOD(nativeInit)(JNIEnv*, jobject, jint slotType) {
    std::lock_guard<std::mutex> lk(gMtx);
    gEngine   = std::make_unique<MagicsSlot::SlotEngine>(slotType);
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

JNIEXPORT void JNICALL JNI_METHOD(nativeCleanup)(JNIEnv*, jobject) {
    std::lock_guard<std::mutex> lk(gMtx);
    gRenderer.reset(); gEngine.reset(); gAudio.reset();
}

// ── Surface ───────────────────────────────────────────────────────────────────
JNIEXPORT void JNICALL JNI_METHOD(nativeSurfaceCreated)(JNIEnv*, jobject, jint w, jint h) {
    std::lock_guard<std::mutex> lk(gMtx);
    if(gRenderer) gRenderer->init(w, h);
}
JNIEXPORT void JNICALL JNI_METHOD(nativeSurfaceChanged)(JNIEnv*, jobject, jint w, jint h) {
    std::lock_guard<std::mutex> lk(gMtx);
    if(gRenderer) gRenderer->resize(w, h);
}
JNIEXPORT void JNICALL JNI_METHOD(nativeDrawFrame)(JNIEnv*, jobject, jfloat dt) {
    std::lock_guard<std::mutex> lk(gMtx);
    if(gEngine && gRenderer){ gEngine->update(dt); gRenderer->drawFrame(dt, *gEngine); }
}
JNIEXPORT void JNICALL JNI_METHOD(nativeUpdate)(JNIEnv*, jobject, jfloat dt) {
    std::lock_guard<std::mutex> lk(gMtx);
    if(gEngine) gEngine->update(dt);
}

// ── Game controls ─────────────────────────────────────────────────────────────
JNIEXPORT jboolean JNICALL JNI_METHOD(nativeSpin)(JNIEnv*, jobject) {
    std::lock_guard<std::mutex> lk(gMtx);
    if(!gEngine||!gEngine->canSpin()) return JNI_FALSE;
    gSpinDone=false;
    gEngine->startSpin();
    if(gAudio) gAudio->play(MagicsSlot::SoundId::SPIN_TICK);
    return JNI_TRUE;
}
JNIEXPORT void JNICALL JNI_METHOD(nativeSetBet)(JNIEnv*, jobject, jdouble bet) {
    std::lock_guard<std::mutex> lk(gMtx);
    if(gEngine) gEngine->setBet(bet);
}
JNIEXPORT void JNICALL JNI_METHOD(nativeSetBalance)(JNIEnv*, jobject, jdouble bal) {
    std::lock_guard<std::mutex> lk(gMtx);
    if(gEngine) gEngine->setBalance(bal);
}
JNIEXPORT jdouble JNICALL JNI_METHOD(nativeGetBalance)(JNIEnv*, jobject) {
    std::lock_guard<std::mutex> lk(gMtx);
    return gEngine ? gEngine->getBalance() : 0.0;
}
JNIEXPORT jdouble JNICALL JNI_METHOD(nativeGetBet)(JNIEnv*, jobject) {
    std::lock_guard<std::mutex> lk(gMtx);
    return gEngine ? gEngine->getBet() : 1.0;
}
JNIEXPORT jboolean JNICALL JNI_METHOD(nativeIsSpinning)(JNIEnv*, jobject) {
    std::lock_guard<std::mutex> lk(gMtx);
    return (gEngine && gEngine->isSpinning()) ? JNI_TRUE : JNI_FALSE;
}

// ── Results ───────────────────────────────────────────────────────────────────
JNIEXPORT jboolean JNICALL JNI_METHOD(nativeIsSpinComplete)(JNIEnv*, jobject) {
    std::lock_guard<std::mutex> lk(gMtx);
    if(gSpinDone){ gSpinDone=false; return JNI_TRUE; }
    return JNI_FALSE;
}
JNIEXPORT jfloat JNICALL JNI_METHOD(nativeGetLastWin)(JNIEnv*, jobject) {
    std::lock_guard<std::mutex> lk(gMtx); return gLastWin;
}
JNIEXPORT jboolean JNICALL JNI_METHOD(nativeIsJackpot)(JNIEnv*, jobject) {
    std::lock_guard<std::mutex> lk(gMtx); return gJackpot ? JNI_TRUE : JNI_FALSE;
}
JNIEXPORT jint JNICALL JNI_METHOD(nativeFreeSpinsAwarded)(JNIEnv*, jobject) {
    std::lock_guard<std::mutex> lk(gMtx); return gFreeSpins;
}
JNIEXPORT jboolean JNICALL JNI_METHOD(nativeInFreeSpins)(JNIEnv*, jobject) {
    std::lock_guard<std::mutex> lk(gMtx);
    return (gEngine&&gEngine->inFreeSpins()) ? JNI_TRUE : JNI_FALSE;
}
JNIEXPORT jint JNICALL JNI_METHOD(nativeFreeSpinsLeft)(JNIEnv*, jobject) {
    std::lock_guard<std::mutex> lk(gMtx);
    return gEngine ? gEngine->freeSpinsLeft() : 0;
}

// ── Audio ─────────────────────────────────────────────────────────────────────
JNIEXPORT void JNICALL JNI_METHOD(nativeSetMuted)(JNIEnv*, jobject, jboolean m) {
    if(gAudio) gAudio->setMuted(m);
}
JNIEXPORT void JNICALL JNI_METHOD(nativeSetVolume)(JNIEnv*, jobject, jfloat v) {
    if(gAudio) gAudio->setVolume(v);
}
JNIEXPORT void JNICALL JNI_METHOD(nativePlaySound)(JNIEnv*, jobject, jint id) {
    if(gAudio) gAudio->play(static_cast<MagicsSlot::SoundId>(id));
}
JNIEXPORT void JNICALL JNI_METHOD(nativeStartMusic)(JNIEnv*, jobject) {
    if(gAudio) gAudio->startMusic();
}
JNIEXPORT void JNICALL JNI_METHOD(nativeStopMusic)(JNIEnv*, jobject) {
    if(gAudio) gAudio->stopMusic();
}

// ── Renderer config ───────────────────────────────────────────────────────────
JNIEXPORT void JNICALL JNI_METHOD(nativeSetBloom)(JNIEnv*, jobject, jfloat v) {
    if(gRenderer) gRenderer->bloomStrength=v;
}
JNIEXPORT void JNICALL JNI_METHOD(nativeSetPostFX)(JNIEnv*, jobject, jboolean en) {
    if(gRenderer) gRenderer->postFX=en;
}
JNIEXPORT jintArray JNICALL Java_com_magics_slot_SlotNativeBridge_nativeGetGrid(JNIEnv* env, jobject) {
    std::lock_guard<std::mutex> lk(gMtx);
    if(!gEngine) {
        jintArray result = env->NewIntArray(15);
        jint fill[15] = {0};
        env->SetIntArrayRegion(result, 0, 15, fill);
        return result;
    }
    jintArray result = env->NewIntArray(15);
    jint fill[15];
    for(int r=0; r<5; r++) {
        for(int row=0; row<3; row++) {
            fill[r*3 + row] = static_cast<jint>(gEngine->getGridSymbol(r, row));
        }
    }
    env->SetIntArrayRegion(result, 0, 15, fill);
    return result;
}
JNIEXPORT void JNICALL JNI_METHOD(nativeTriggerWinFX)(JNIEnv*, jobject, jint count) {
    std::lock_guard<std::mutex> lk(gMtx);
    if(gRenderer) gRenderer->triggerWinFX(count);
}
JNIEXPORT void JNICALL JNI_METHOD(nativeTriggerJackpotFX)(JNIEnv*, jobject) {
    std::lock_guard<std::mutex> lk(gMtx);
    if(gRenderer) gRenderer->triggerJackpotFX();
}

} // extern "C"