#pragma once
// ─────────────────────────────────────────────────────────────────────────────
//  ParticleSystem.h  –  GPU-instanced particles for win FX
// ─────────────────────────────────────────────────────────────────────────────
#include <GLES3/gl3.h>
#include <vector>
#include <random>
#include <cstdint>

namespace MagicsSlot {

struct Particle {
    float x, y;       // position (NDC)
    float vx, vy;     // velocity
    float r, g, b, a; // color
    float life;       // 1..0
    float lifeMax;
    float size;
};

class ParticleSystem {
public:
    static constexpr int MAX_PARTICLES = 2048;

    ParticleSystem();
    ~ParticleSystem();

    bool init();
    void cleanup();

    void emitWin(float nx, float ny, int count);
    void emitJackpot();
    void emitAmbient();

    void update(float dt);
    void render(const float* mvp4x4);

    int aliveCount() const { return (int)m_particles.size(); }

private:
    std::mt19937          m_rng;
    std::vector<Particle> m_particles;
    std::vector<float>    m_gpuBuf;

    GLuint m_vao = 0;
    GLuint m_vbo = 0;

    void spawn(float x, float y, float speed, float spread,
               float r, float g, float b, float lifeMin, float lifeMax,
               float sizeMin, float sizeMax, int count);
    void upload();
};

} // namespace MagicsSlot
