#pragma once
// ─────────────────────────────────────────────────────────────────────────────
//  AudioEngine.h  –  AAudio low-latency synthesized sound
// ─────────────────────────────────────────────────────────────────────────────
#include <aaudio/AAudio.h>
#include <atomic>
#include <array>
#include <cstdint>

namespace MagicsSlot {

enum class SoundId : int {
    SPIN_TICK   = 0,
    REEL_STOP   = 1,
    WIN_SMALL   = 2,
    WIN_BIG     = 3,
    JACKPOT     = 4,
    BTN_CLICK   = 5,
    FREE_SPIN   = 6,
    COIN_DROP   = 7,
    COUNT       = 8
};

class AudioEngine {
public:
    AudioEngine();
    ~AudioEngine();

    bool init();
    void cleanup();

    void play(SoundId id);
    void setVolume(float v);
    void setMuted(bool m);
    bool isMuted() const { return m_muted.load(); }
    void startMusic();
    void stopMusic();

private:
    struct Tone {
        float freq      = 440.f;
        float amp       = 0.f;
        float duration  = 0.f;
        float elapsed   = 0.f;
        float phase     = 0.f;
        bool  active    = false;
    };

    static constexpr float TONES[8][3] = {
    //  freq    dur    amp
        {200.f, 0.05f, 0.15f}, // SPIN_TICK
        {350.f, 0.12f, 0.40f}, // REEL_STOP
        {523.f, 0.30f, 0.50f}, // WIN_SMALL
        {659.f, 0.60f, 0.70f}, // WIN_BIG
        {880.f, 1.20f, 0.90f}, // JACKPOT
        {1000.f,0.05f, 0.25f}, // BTN_CLICK
        {440.f, 0.80f, 0.60f}, // FREE_SPIN
        {300.f, 0.20f, 0.45f}, // COIN_DROP
    };

    AAudioStream*          m_stream    = nullptr;
    int32_t                m_sampleRate= 48000;
    std::atomic<bool>      m_muted{false};
    std::atomic<float>     m_volume{0.8f};
    std::array<Tone,4>     m_tones{};
    Tone                   m_music{};

    static aaudio_data_callback_result_t audioCallback(
        AAudioStream*, void* userData, void* data, int32_t frames);
    void fillBuffer(float* out, int32_t frames);
    void activateTone(Tone& t, int id);
};

} // namespace MagicsSlot
