#pragma once
// ─────────────────────────────────────────────────────────────────────────────
//  Renderer.h  –  OpenGL ES 3.2 pipeline
// ─────────────────────────────────────────────────────────────────────────────
#include <GLES3/gl3.h>
#include <memory>
#include <array>
#include "ShaderManager.h"
#include "ParticleSystem.h"
#include "SlotEngine.h"

namespace MagicsSlot {

class Renderer {
public:
    Renderer();
    ~Renderer();

    bool init(int w, int h);
    void resize(int w, int h);
    void cleanup();
    void drawFrame(float dt, const SlotEngine& engine);

    // Config
    float bloomStrength  = 1.2f;
    float chromaticStr   = 0.6f;
    float grainStrength  = 0.5f;
    bool  postFX         = true;

    // Called from ViewModel to trigger particle bursts
    void triggerWinFX(int winCount);
    void triggerJackpotFX();

private:
    int  m_w = 0, m_h = 0;
    float m_time = 0.f;

    ShaderManager  m_shaders;
    ParticleSystem m_particles;

    // Framebuffers
    GLuint m_sceneFBO = 0, m_sceneTex = 0;
    GLuint m_bloomFBO = 0, m_bloomTex = 0;

    // Fullscreen quad
    GLuint m_quadVAO = 0, m_quadVBO = 0;

    // Symbol textures (procedural)
    std::array<GLuint, SYM_COUNT> m_symTex{};

    bool initFBOs();
    bool initQuad();
    bool initSymTextures();
    GLuint makeSymTexture(Symbol sym, int size = 128);
    GLuint makeTexture(int w, int h, const uint8_t* data);

    void passBackground();
    void passReels(const SlotEngine& engine);
    void passParticles();
    void passBloom();
    void passPost();
    void drawQuad();

    // Uniform helpers
    void u1f(GLuint p,const char* n,float v);
    void u2f(GLuint p,const char* n,float x,float y);
    void u4f(GLuint p,const char* n,float r,float g,float b,float a);
    void u1i(GLuint p,const char* n,int v);
    void u1b(GLuint p,const char* n,bool v);
    void uMat(GLuint p,const char* n,const float* m);
};

// Neon color per symbol (RGB 0-1)
static const float kSymRGB[SYM_COUNT][3] = {
    {1.0f,0.85f,0.1f},  // WILD    – neon gold
    {0.0f,1.0f,1.0f},   // SCATTER – cyan
    {1.0f,0.15f,0.15f}, // SEVEN   – neon red
    {1.0f,0.0f,0.7f},   // BAR3    – neon magenta
    {0.6f,0.0f,1.0f},   // BAR2    – neon purple
    {0.0f,0.5f,1.0f},   // BAR1    – neon blue
    {0.2f,1.0f,0.4f},   // DIAMOND – neon green
    {1.0f,0.4f,0.0f},   // CHERRY  – neon orange-red
};

} // namespace MagicsSlot
