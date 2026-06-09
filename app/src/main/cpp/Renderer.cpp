// ─────────────────────────────────────────────────────────────────────────────
//  Renderer.cpp  –  OpenGL ES 3.2 rendering pipeline
// ─────────────────────────────────────────────────────────────────────────────
#include "Renderer.h"
#include <android/log.h>
#include <cmath>
#include <cstring>
#include <algorithm>
#include <vector>

#define LOG_TAG "MagicsSlot"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace MagicsSlot {

// Fullscreen quad (NDC)
static const float kQuad[] = {
   -1.f,-1.f, 0.f,0.f,
    1.f,-1.f, 1.f,0.f,
   -1.f, 1.f, 0.f,1.f,
    1.f, 1.f, 1.f,1.f,
};

Renderer::Renderer()  = default;
Renderer::~Renderer() { cleanup(); }

bool Renderer::init(int w, int h) {
    m_w = w; m_h = h;
    LOGI("Renderer::init %dx%d", w, h);

    if(!m_shaders.init())      return false;
    if(!m_particles.init())    return false;
    if(!initQuad())            return false;
    if(!initFBOs())            return false;
    if(!initSymTextures())     return false;

    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    glDisable(GL_DEPTH_TEST);
    LOGI("Renderer ready.");
    return true;
}

void Renderer::resize(int w, int h) {
    m_w=w; m_h=h;
    glViewport(0,0,w,h);
    // Recreate FBOs
    if(m_sceneFBO){ glDeleteFramebuffers(1,&m_sceneFBO); m_sceneFBO=0; }
    if(m_sceneTex){ glDeleteTextures(1,&m_sceneTex);     m_sceneTex=0; }
    if(m_bloomFBO){ glDeleteFramebuffers(1,&m_bloomFBO); m_bloomFBO=0; }
    if(m_bloomTex){ glDeleteTextures(1,&m_bloomTex);     m_bloomTex=0; }
    initFBOs();
}

void Renderer::cleanup() {
    m_shaders.cleanup();
    m_particles.cleanup();
    if(m_quadVAO){ glDeleteVertexArrays(1,&m_quadVAO); m_quadVAO=0; }
    if(m_quadVBO){ glDeleteBuffers(1,&m_quadVBO);      m_quadVBO=0; }
    if(m_sceneFBO){ glDeleteFramebuffers(1,&m_sceneFBO); }
    if(m_sceneTex){ glDeleteTextures(1,&m_sceneTex); }
    if(m_bloomFBO){ glDeleteFramebuffers(1,&m_bloomFBO); }
    if(m_bloomTex){ glDeleteTextures(1,&m_bloomTex); }
    for(auto& t:m_symTex) if(t){ glDeleteTextures(1,&t); t=0; }
}

bool Renderer::initQuad() {
    glGenVertexArrays(1,&m_quadVAO);
    glGenBuffers(1,&m_quadVBO);
    glBindVertexArray(m_quadVAO);
    glBindBuffer(GL_ARRAY_BUFFER,m_quadVBO);
    glBufferData(GL_ARRAY_BUFFER,sizeof(kQuad),kQuad,GL_STATIC_DRAW);
    glVertexAttribPointer(0,2,GL_FLOAT,GL_FALSE,4*sizeof(float),(void*)0);
    glVertexAttribPointer(1,2,GL_FLOAT,GL_FALSE,4*sizeof(float),(void*)(2*sizeof(float)));
    glEnableVertexAttribArray(0);
    glEnableVertexAttribArray(1);
    glBindVertexArray(0);
    return true;
}

bool Renderer::initFBOs() {
    auto makeFBO = [&](GLuint& fbo, GLuint& tex, int w, int h) -> bool {
        glGenTextures(1,&tex);
        glBindTexture(GL_TEXTURE_2D,tex);
        glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA16F,w,h,0,GL_RGBA,GL_HALF_FLOAT,nullptr);
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE);
        glGenFramebuffers(1,&fbo);
        glBindFramebuffer(GL_FRAMEBUFFER,fbo);
        glFramebufferTexture2D(GL_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D,tex,0);
        bool ok = glCheckFramebufferStatus(GL_FRAMEBUFFER)==GL_FRAMEBUFFER_COMPLETE;
        glBindFramebuffer(GL_FRAMEBUFFER,0);
        return ok;
    };
    return makeFBO(m_sceneFBO,m_sceneTex,m_w,m_h) &&
           makeFBO(m_bloomFBO,m_bloomTex,m_w/2,m_h/2);
}

bool Renderer::initSymTextures() {
    for(int i=0;i<SYM_COUNT;i++)
        m_symTex[i] = makeSymTexture(static_cast<Symbol>(i),128);
    return true;
}

GLuint Renderer::makeTexture(int w, int h, const uint8_t* data) {
    GLuint tex=0;
    glGenTextures(1,&tex);
    glBindTexture(GL_TEXTURE_2D,tex);
    glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA,w,h,0,GL_RGBA,GL_UNSIGNED_BYTE,data);
    glGenerateMipmap(GL_TEXTURE_2D);
    glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR_MIPMAP_LINEAR);
    glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE);
    return tex;
}

GLuint Renderer::makeSymTexture(Symbol sym, int size) {
    std::vector<uint8_t> px(size*size*4, 0);
    float cr=kSymRGB[sym][0], cg=kSymRGB[sym][1], cb=kSymRGB[sym][2];

    for(int y=0;y<size;y++){
        for(int x=0;x<size;x++){
            float nx=(x/(float)size)*2.f-1.f;
            float ny=(y/(float)size)*2.f-1.f;
            float dist=sqrtf(nx*nx+ny*ny);
            uint8_t alpha=0; float bright=1.f;

            switch(sym){
                case SYM_CHERRY: {
                    float d1=sqrtf((nx+0.3f)*(nx+0.3f)+(ny+0.2f)*(ny+0.2f));
                    float d2=sqrtf((nx-0.3f)*(nx-0.3f)+(ny+0.2f)*(ny+0.2f));
                    alpha=(d1<0.5f||d2<0.5f)?255:0;
                    bright=std::max(0.f,1.f-dist*0.5f);
                    break;
                }
                case SYM_DIAMOND: {
                    float dd=fabsf(nx)+fabsf(ny);
                    alpha=(dd<0.9f)?255:0;
                    bright=1.f-dd*0.25f;
                    break;
                }
                case SYM_SEVEN: {
                    // '7' shape
                    bool topBar=(ny>0.5f&&fabsf(nx)<0.7f);
                    bool diag  =(ny>-0.2f&&ny<0.5f&&(nx-ny*0.5f)>0.1f&&(nx-ny*0.5f)<0.7f);
                    bool bot   =(ny<-0.5f&&fabsf(nx)<0.6f);
                    alpha=(topBar||diag||bot)?255:0;
                    bright=1.2f-dist*0.3f;
                    break;
                }
                case SYM_WILD: {
                    float angle=atan2f(ny,nx);
                    float petal=0.7f+0.3f*cosf(angle*5.f);
                    alpha=(dist<petal*0.85f)?255:0;
                    bright=1.f-dist*0.4f;
                    break;
                }
                case SYM_SCATTER: {
                    alpha=(dist<0.85f)?255:0;
                    bright=(dist>0.5f&&dist<0.85f)?1.5f:0.9f;
                    break;
                }
                default: { // BAR symbols
                    float bh=0.35f-(sym-SYM_BAR1)*0.04f;
                    alpha=(fabsf(nx)<0.75f&&fabsf(ny)<bh)?255:0;
                    bright=1.f;
                    break;
                }
            }

            int idx=(y*size+x)*4;
            px[idx+0]=(uint8_t)std::min(255.f,cr*bright*255.f);
            px[idx+1]=(uint8_t)std::min(255.f,cg*bright*255.f);
            px[idx+2]=(uint8_t)std::min(255.f,cb*bright*255.f);
            px[idx+3]=alpha;
        }
    }
    return makeTexture(size,size,px.data());
}

// ─────────────────────────────────────────────────────────────────────────────
void Renderer::drawFrame(float dt, const SlotEngine& engine) {
    m_time += dt;
    m_particles.update(dt);
    m_particles.emitAmbient();

    // Pass 1 – scene to FBO
    glBindFramebuffer(GL_FRAMEBUFFER, m_sceneFBO);
    glViewport(0,0,m_w,m_h);
    glClearColor(0,0,0,1); glClear(GL_COLOR_BUFFER_BIT);

    passBackground();
    passReels(engine);
    passParticles();

    if(postFX){
        passBloom();
        passPost();
    } else {
        glBindFramebuffer(GL_READ_FRAMEBUFFER,m_sceneFBO);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER,0);
        glBlitFramebuffer(0,0,m_w,m_h,0,0,m_w,m_h,GL_COLOR_BUFFER_BIT,GL_LINEAR);
        glBindFramebuffer(GL_FRAMEBUFFER,0);
    }
}

void Renderer::passBackground() {
    GLuint p=m_shaders.bgProg; glUseProgram(p);
    u1f(p,"uTime",m_time);
    u2f(p,"uRes",(float)m_w,(float)m_h);
    drawQuad();
}

void Renderer::passReels(const SlotEngine& engine) {
    GLuint p=m_shaders.symProg; glUseProgram(p);
    u1f(p,"uTime",m_time);

    // Identity MVP (draw in NDC directly)
    float identity[16]={1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1};

    // Layout: 5 reels centered, aspect-corrected
    float aspect = (float)m_h/(float)m_w;
    float reelW  = 0.30f;           // NDC width
    float symH   = 0.22f * aspect;  // NDC height (keep square-ish)
    float gapX   = 0.04f;
    float totalW = REEL_COUNT*reelW + (REEL_COUNT-1)*gapX;
    float startX = -totalW*0.5f + reelW*0.5f;
    float reelTop= 0.45f;

    const auto& result = engine.getLastResult();

    // Create temporary VAO for a symbol quad
    float hw=reelW*0.46f, hh=symH*0.46f;
    float qverts[16]={
       -hw,-hh,0,0,
        hw,-hh,1,0,
       -hw, hh,0,1,
        hw, hh,1,1,
    };
    GLuint vao,vbo;
    glGenVertexArrays(1,&vao); glGenBuffers(1,&vbo);
    glBindVertexArray(vao);
    glBindBuffer(GL_ARRAY_BUFFER,vbo);
    glBufferData(GL_ARRAY_BUFFER,sizeof(qverts),qverts,GL_STREAM_DRAW);
    glVertexAttribPointer(0,2,GL_FLOAT,GL_FALSE,4*sizeof(float),(void*)0);
    glVertexAttribPointer(1,2,GL_FLOAT,GL_FALSE,4*sizeof(float),(void*)(2*sizeof(float)));
    glEnableVertexAttribArray(0); glEnableVertexAttribArray(1);

    GLint mvpLoc=glGetUniformLocation(p,"uMVP");

    for(int r=0;r<REEL_COUNT;r++){
        const auto& rs=engine.getReelState(r);
        float reelX = startX + r*(reelW+gapX);
        float scrollFrac = fmodf(rs.offset,1.f);

        for(int row=0;row<ROW_COUNT;row++){
            float symY = reelTop - (row + scrollFrac)*symH;
            // Simple translate matrix
            float mvp[16]={1,0,0,0,0,1,0,0,0,0,1,0,reelX,symY,0,1};
            glUniformMatrix4fv(mvpLoc,1,GL_FALSE,mvp);

            bool winHL=!engine.isSpinning()&&!result.wins.empty();
            u1b(p,"uWin",winHL);
            u1f(p,"uTime",m_time);

            Symbol sym=engine.getGridSymbol(r,row);
            u4f(p,"uTint",kSymRGB[sym][0],kSymRGB[sym][1],kSymRGB[sym][2],1.f);

            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D,m_symTex[sym]);
            u1i(p,"uTex",0);

            glDrawArrays(GL_TRIANGLE_STRIP,0,4);
        }
    }
    glBindVertexArray(0);
    glDeleteVertexArrays(1,&vao); glDeleteBuffers(1,&vbo);
}

void Renderer::passParticles() {
    GLuint p=m_shaders.partProg; glUseProgram(p);
    float identity[16]={1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1};
    uMat(p,"uMVP",identity);
    u1f(p,"uTime",m_time);
    m_particles.render(identity);
}

void Renderer::passBloom() {
    glBindFramebuffer(GL_FRAMEBUFFER,m_bloomFBO);
    glViewport(0,0,m_w/2,m_h/2);
    glClear(GL_COLOR_BUFFER_BIT);

    GLuint p=m_shaders.bloomProg; glUseProgram(p);
    glActiveTexture(GL_TEXTURE0); glBindTexture(GL_TEXTURE_2D,m_sceneTex);
    u1i(p,"uTex",0);
    u2f(p,"uTexel",2.f/m_w,2.f/m_h);
    u1f(p,"uThresh",0.55f);
    u1b(p,"uDown",true);
    drawQuad();

    glBindFramebuffer(GL_FRAMEBUFFER,0);
    glViewport(0,0,m_w,m_h);
}

void Renderer::passPost() {
    glBindFramebuffer(GL_FRAMEBUFFER,0);
    glViewport(0,0,m_w,m_h);
    glClear(GL_COLOR_BUFFER_BIT);

    GLuint p=m_shaders.postProg; glUseProgram(p);
    glActiveTexture(GL_TEXTURE0); glBindTexture(GL_TEXTURE_2D,m_sceneTex); u1i(p,"uScene",0);
    glActiveTexture(GL_TEXTURE1); glBindTexture(GL_TEXTURE_2D,m_bloomTex); u1i(p,"uBloom",1);
    u1f(p,"uTime",m_time);
    u1f(p,"uChrom",chromaticStr);
    u1f(p,"uGrain",grainStrength);
    u1f(p,"uBloom",bloomStrength);
    drawQuad();
}

void Renderer::triggerWinFX(int winCount) {
    // Emit at reel center
    m_particles.emitWin(0.f, 0.f, winCount);
}

void Renderer::triggerJackpotFX() {
    m_particles.emitJackpot();
}

void Renderer::drawQuad() {
    glBindVertexArray(m_quadVAO);
    glDrawArrays(GL_TRIANGLE_STRIP,0,4);
    glBindVertexArray(0);
}

// Uniform helpers
void Renderer::u1f(GLuint p,const char* n,float v)           { glUniform1f(glGetUniformLocation(p,n),v); }
void Renderer::u2f(GLuint p,const char* n,float x,float y)   { glUniform2f(glGetUniformLocation(p,n),x,y); }
void Renderer::u4f(GLuint p,const char* n,float r,float g,float b,float a){ glUniform4f(glGetUniformLocation(p,n),r,g,b,a); }
void Renderer::u1i(GLuint p,const char* n,int v)              { glUniform1i(glGetUniformLocation(p,n),v); }
void Renderer::u1b(GLuint p,const char* n,bool v)             { glUniform1i(glGetUniformLocation(p,n),v?1:0); }
void Renderer::uMat(GLuint p,const char* n,const float* m)    { glUniformMatrix4fv(glGetUniformLocation(p,n),1,GL_FALSE,m); }

} // namespace MagicsSlot
