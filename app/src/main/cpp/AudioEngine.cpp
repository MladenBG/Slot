// ─────────────────────────────────────────────────────────────────────────────
//  AudioEngine.cpp  –  AAudio synthesis
// ─────────────────────────────────────────────────────────────────────────────
#include "AudioEngine.h"
#include <android/log.h>
#include <cmath>
#include <algorithm>

#define LOG_TAG "MagicsSlot"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace MagicsSlot {

constexpr float AudioEngine::TONES[8][3];
static constexpr float TWO_PI = 6.28318530f;

AudioEngine::AudioEngine()  = default;
AudioEngine::~AudioEngine() { cleanup(); }

bool AudioEngine::init() {
    AAudioStreamBuilder* b=nullptr;
    if(AAudio_createStreamBuilder(&b)!=AAUDIO_OK){ LOGE("AAudio builder failed"); return false; }

    AAudioStreamBuilder_setDirection(b,       AAUDIO_DIRECTION_OUTPUT);
    AAudioStreamBuilder_setSharingMode(b,     AAUDIO_SHARING_MODE_EXCLUSIVE);
    AAudioStreamBuilder_setFormat(b,          AAUDIO_FORMAT_PCM_FLOAT);
    AAudioStreamBuilder_setChannelCount(b,    1);
    AAudioStreamBuilder_setPerformanceMode(b, AAUDIO_PERFORMANCE_MODE_LOW_LATENCY);
    AAudioStreamBuilder_setDataCallback(b,    audioCallback, this);

    aaudio_result_t res = AAudioStreamBuilder_openStream(b, &m_stream);
    AAudioStreamBuilder_delete(b);

    if(res!=AAUDIO_OK||!m_stream){ LOGE("AAudio open failed: %s",AAudio_convertResultToText(res)); m_stream=nullptr; return false; }

    m_sampleRate = AAudioStream_getSampleRate(m_stream);
    if(AAudioStream_requestStart(m_stream)!=AAUDIO_OK){ LOGE("AAudio start failed"); return false; }

    LOGI("AudioEngine ready. SR=%d", m_sampleRate);
    return true;
}

void AudioEngine::cleanup() {
    if(m_stream){ AAudioStream_requestStop(m_stream); AAudioStream_close(m_stream); m_stream=nullptr; }
}

void AudioEngine::play(SoundId id) {
    if(m_muted.load()) return;
    int i=static_cast<int>(id);
    if(i<0||i>=(int)SoundId::COUNT) return;
    for(auto& t:m_tones){ if(!t.active){ activateTone(t,i); return; } }
    activateTone(m_tones[0],i); // steal
}

void AudioEngine::activateTone(Tone& t, int id) {
    t.freq     = TONES[id][0];
    t.duration = TONES[id][1];
    t.amp      = TONES[id][2] * m_volume.load();
    t.elapsed  = 0.f;
    t.active   = true;
}

void AudioEngine::setVolume(float v) { m_volume.store(std::clamp(v,0.f,1.f)); }
void AudioEngine::setMuted(bool m)   { m_muted.store(m); }

void AudioEngine::startMusic() {
    m_music.freq=130.8f; m_music.amp=0.07f*m_volume.load();
    m_music.duration=1e9f; m_music.elapsed=0.f; m_music.active=true;
}
void AudioEngine::stopMusic() { m_music.active=false; }

aaudio_data_callback_result_t AudioEngine::audioCallback(
    AAudioStream*, void* ud, void* data, int32_t frames)
{
    static_cast<AudioEngine*>(ud)->fillBuffer(static_cast<float*>(data),frames);
    return AAUDIO_CALLBACK_RESULT_CONTINUE;
}

void AudioEngine::fillBuffer(float* out, int32_t frames) {
    float dt = 1.f/static_cast<float>(m_sampleRate);
    if(m_muted.load()){ for(int i=0;i<frames;i++) out[i]=0.f; return; }

    for(int i=0;i<frames;i++){
        float s=0.f;
        for(auto& t:m_tones){
            if(!t.active) continue;
            float env=1.f;
            float rem=t.duration-t.elapsed;
            if(t.elapsed<0.005f) env=t.elapsed/0.005f;
            if(rem<0.03f)        env=rem/0.03f;
            s+=sinf(t.phase)*t.amp*env;
            t.phase+=TWO_PI*t.freq*dt;
            if(t.phase>TWO_PI) t.phase-=TWO_PI;
            t.elapsed+=dt;
            if(t.elapsed>=t.duration) t.active=false;
        }
        if(m_music.active){
            s+=sinf(m_music.phase)*m_music.amp;
            s+=sinf(m_music.phase*1.5f)*m_music.amp*0.5f;
            s+=sinf(m_music.phase*1.2f)*m_music.amp*0.4f;
            m_music.phase+=TWO_PI*m_music.freq*dt;
            if(m_music.phase>TWO_PI) m_music.phase-=TWO_PI;
        }
        out[i]=std::clamp(s,-1.f,1.f);
    }
}

} // namespace MagicsSlot
