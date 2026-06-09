// ─────────────────────────────────────────────────────────────────────────────
//  ParticleSystem.cpp
// ─────────────────────────────────────────────────────────────────────────────
#include "ParticleSystem.h"
#include <android/log.h>
#include <cmath>
#include <algorithm>

#define LOG_TAG "MagicsSlot"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

namespace MagicsSlot {

static constexpr float PI = 3.14159265f;

ParticleSystem::ParticleSystem() : m_rng(std::random_device{}()) {
    m_particles.reserve(MAX_PARTICLES);
    m_gpuBuf.reserve(MAX_PARTICLES * 10);
}
ParticleSystem::~ParticleSystem() { cleanup(); }

bool ParticleSystem::init() {
    glGenVertexArrays(1, &m_vao);
    glGenBuffers(1, &m_vbo);
    glBindVertexArray(m_vao);
    glBindBuffer(GL_ARRAY_BUFFER, m_vbo);
    glBufferData(GL_ARRAY_BUFFER, MAX_PARTICLES * 10 * sizeof(float), nullptr, GL_DYNAMIC_DRAW);

    // pos(2), vel(2), life(1), color(4), size(1)  = 10 floats
    const int stride = 10 * sizeof(float);
    glVertexAttribPointer(0, 2, GL_FLOAT, GL_FALSE, stride, (void*)(0));
    glVertexAttribPointer(1, 2, GL_FLOAT, GL_FALSE, stride, (void*)(2*sizeof(float)));
    glVertexAttribPointer(2, 1, GL_FLOAT, GL_FALSE, stride, (void*)(4*sizeof(float)));
    glVertexAttribPointer(3, 4, GL_FLOAT, GL_FALSE, stride, (void*)(5*sizeof(float)));
    glVertexAttribPointer(4, 1, GL_FLOAT, GL_FALSE, stride, (void*)(9*sizeof(float)));
    for(int i=0;i<5;i++) glEnableVertexAttribArray(i);

    glBindVertexArray(0);
    LOGI("ParticleSystem init OK");
    return true;
}

void ParticleSystem::cleanup() {
    if(m_vao){ glDeleteVertexArrays(1,&m_vao); m_vao=0; }
    if(m_vbo){ glDeleteBuffers(1,&m_vbo);      m_vbo=0; }
}

void ParticleSystem::spawn(float x, float y, float speed, float spread,
                            float r, float g, float b,
                            float lifeMin, float lifeMax,
                            float sizeMin, float sizeMax, int count)
{
    std::uniform_real_distribution<float> dAngle(-spread*0.5f, spread*0.5f);
    std::uniform_real_distribution<float> dSpd(speed*0.5f, speed);
    std::uniform_real_distribution<float> dLife(lifeMin, lifeMax);
    std::uniform_real_distribution<float> dSize(sizeMin, sizeMax);
    std::uniform_real_distribution<float> dCol(0.7f, 1.0f);

    int canAdd = MAX_PARTICLES - (int)m_particles.size();
    count = std::min(count, canAdd);

    for(int i=0; i<count; i++){
        float angle = dAngle(m_rng) * PI / 180.f;
        float spd   = dSpd(m_rng);
        float life  = dLife(m_rng);
        Particle p;
        p.x    = x; p.y = y;
        p.vx   = cosf(angle)*spd;
        p.vy   = sinf(angle)*spd;
        p.r    = r*dCol(m_rng);
        p.g    = g*dCol(m_rng);
        p.b    = b*dCol(m_rng);
        p.a    = 1.0f;
        p.life = 1.0f; p.lifeMax = life;
        p.size = dSize(m_rng);
        m_particles.push_back(p);
    }
}

void ParticleSystem::emitWin(float nx, float ny, int count) {
    // Gold coins
    spawn(nx, ny, 0.8f, 360.f, 1.0f,0.84f,0.0f, 0.6f,2.0f, 8.f,20.f, count*20);
    // Cyan stars
    spawn(nx, ny, 1.2f, 360.f, 0.0f,1.0f,1.0f, 0.4f,1.5f, 6.f,14.f, count*10);
}

void ParticleSystem::emitJackpot() {
    // Magenta fire
    spawn(0.f, -0.4f, 1.5f, 60.f, 1.0f,0.0f,1.0f, 1.0f,3.0f, 14.f,32.f, 200);
    // Gold spray
    spawn(0.f, -0.4f, 2.0f, 90.f, 1.0f,0.84f,0.0f, 0.8f,2.5f, 10.f,24.f, 150);
}

void ParticleSystem::emitAmbient() {
    if((int)m_particles.size() < 60){
        std::uniform_real_distribution<float> dX(-1.f,1.f);
        std::uniform_real_distribution<float> dY(-1.f,1.f);
        float x=dX(m_rng), y=dY(m_rng);
        // Pick neon color
        std::uniform_int_distribution<int> dC(0,2);
        float r=0,g=0,b=0;
        switch(dC(m_rng)){
            case 0: r=0;g=1;b=1; break;   // cyan
            case 1: r=1;g=0;b=1; break;   // magenta
            default: r=1;g=0.84f;b=0; break; // gold
        }
        spawn(x,y, 0.1f, 360.f, r,g,b, 1.5f,4.0f, 3.f,7.f, 2);
    }
}

void ParticleSystem::update(float dt) {
    const float gravity = -0.4f;
    for(auto& p : m_particles){
        p.vy += gravity * dt;
        p.x  += p.vx * dt;
        p.y  += p.vy * dt;
        p.life -= dt / p.lifeMax;
    }
    m_particles.erase(
        std::remove_if(m_particles.begin(), m_particles.end(),
            [](const Particle& p){ return p.life <= 0.f; }),
        m_particles.end());
}

void ParticleSystem::upload() {
    m_gpuBuf.clear();
    for(const auto& p : m_particles){
        m_gpuBuf.push_back(p.x);   m_gpuBuf.push_back(p.y);
        m_gpuBuf.push_back(p.vx);  m_gpuBuf.push_back(p.vy);
        m_gpuBuf.push_back(p.life);
        m_gpuBuf.push_back(p.r);   m_gpuBuf.push_back(p.g);
        m_gpuBuf.push_back(p.b);   m_gpuBuf.push_back(p.a);
        m_gpuBuf.push_back(p.size);
    }
}

void ParticleSystem::render(const float* mvp) {
    if(m_particles.empty()) return;
    upload();

    glBindVertexArray(m_vao);
    glBindBuffer(GL_ARRAY_BUFFER, m_vbo);
    glBufferSubData(GL_ARRAY_BUFFER, 0,
        (GLsizeiptr)(m_gpuBuf.size()*sizeof(float)), m_gpuBuf.data());

    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE);  // additive = neon glow
    glEnable(GL_PROGRAM_POINT_SIZE);
    glDepthMask(GL_FALSE);

    glDrawArrays(GL_POINTS, 0, (GLsizei)m_particles.size());

    glDepthMask(GL_TRUE);
    glDisable(GL_PROGRAM_POINT_SIZE);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    glBindVertexArray(0);
}

} // namespace MagicsSlot
