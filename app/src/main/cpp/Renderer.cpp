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

    for(int y=0;y<size;y++){
        for(int x=0;x<size;x++){
            float nx=(x/(float)size)*2.f-1.f;
            float ny=(y/(float)size)*2.f-1.f;
            float dist=sqrtf(nx*nx+ny*ny);
            float r=0.f, g=0.f, b=0.f, a=0.f;

            auto distToSeg = [](float px, float py, float ax, float ay, float bx, float by) -> float {
                float abx = bx - ax, aby = by - ay;
                float apx = px - ax, apy = py - ay;
                float ab2 = abx*abx + aby*aby;
                float t = std::clamp((apx*abx + apy*aby) / (ab2 + 1e-6f), 0.f, 1.f);
                float qx = ax + t*abx, qy = ay + t*aby;
                return sqrtf((px-qx)*(px-qx) + (py-qy)*(py-qy));
            };

            auto mixC = [&](float r1, float g1, float b1, float r2, float g2, float b2, float t) {
                r = r1*(1.f-t) + r2*t; g = g1*(1.f-t) + g2*t; b = b1*(1.f-t) + b2*t;
            };

            switch(sym){
                case SYM_CHERRY: {
                    float cx1=-0.25f, cy1=0.2f, cx2=0.25f, cy2=0.2f;
                    float d1 = sqrtf((nx-cx1)*(nx-cx1)+(ny-cy1)*(ny-cy1));
                    float d2 = sqrtf((nx-cx2)*(nx-cx2)+(ny-cy2)*(ny-cy2));
                    float s1 = distToSeg(nx,ny, cx1,cy1, 0.0f,-0.4f);
                    float s2 = distToSeg(nx,ny, cx2,cy2, 0.0f,-0.4f);
                    
                    if(s1<0.05f || s2<0.05f) { // Stems
                        r=0.2f; g=0.8f; b=0.2f; a=1.f;
                        if(std::min(s1,s2)<0.02f) { r=0.5f; g=1.0f; b=0.5f; } // Stem highlight
                    }
                    if(d1<0.35f || d2<0.35f) { // Cherries
                        float d = (d1<d2)?d1:d2;
                        float cx = (d1<d2)?cx1:cx2;
                        float cy = (d1<d2)?cy1:cy2;
                        // Glossy radial gradient
                        float t = std::clamp(d/0.35f, 0.f, 1.f);
                        mixC(1.0f,0.1f,0.2f, 0.4f,0.0f,0.1f, t*t);
                        a=1.f;
                        // Highlight reflection
                        float hlx = cx - 0.12f, hly = cy - 0.12f;
                        float hd = sqrtf((nx-hlx)*(nx-hlx)+(ny-hly)*(ny-hly));
                        if(hd<0.1f) { r+=0.5f; g+=0.5f; b+=0.5f; }
                        // Border
                        if(d>0.32f) { r=0.1f; g=0.f; b=0.f; }
                    }
                    break;
                }
                case SYM_DIAMOND: {
                    float dd = fabsf(nx*0.8f) + fabsf(ny*1.2f);
                    if(dd<0.75f) {
                        a=1.f;
                        // Facets
                        if(ny < -0.2f && fabsf(nx) < 0.4f) { mixC(0.6f,1.f,1.f, 0.0f,0.5f,1.f, dd); }
                        else if(ny > 0.0f) { mixC(0.0f,0.4f,0.8f, 0.0f,0.2f,0.5f, dd); }
                        else { mixC(0.2f,0.8f,1.f, 0.0f,0.4f,0.8f, dd); }
                        // Edges
                        if(fabsf(dd-0.7f)<0.05f || fabsf(ny)<0.05f || fabsf(fabsf(nx)-0.4f)<0.05f) {
                            r=0.8f; g=1.f; b=1.f;
                        }
                    }
                    break;
                }
                case SYM_SEVEN: {
                    float dTop = distToSeg(nx,ny, -0.45f,-0.5f, 0.45f,-0.5f);
                    float dDiag = distToSeg(nx,ny, 0.45f,-0.5f, -0.15f,0.6f);
                    float d = std::min(dTop, dDiag);
                    if(d<0.2f) {
                        a=1.f;
                        float t = std::clamp(d/0.2f, 0.f, 1.f);
                        // Red fill with Gold border
                        if(d>0.14f) { mixC(1.0f,0.9f,0.2f, 0.6f,0.4f,0.0f, (d-0.14f)/0.06f); } // Gold border
                        else { 
                            mixC(1.0f,0.1f,0.1f, 0.6f,0.0f,0.0f, d/0.14f); // Red interior
                            // Glossy top
                            if(ny < -0.3f) { r+=0.4f; g+=0.2f; b+=0.2f; }
                        }
                    }
                    break;
                }
                case SYM_WILD: {
                    float angle = atan2f(ny,nx);
                    float petal = 0.6f + 0.25f*cosf(angle*5.f);
                    if(dist<petal) {
                        a=1.f;
                        float t = dist/petal;
                        // Gold star
                        if(dist>petal-0.1f) { mixC(1.0f,0.6f,0.0f, 0.8f,0.3f,0.0f, (dist-(petal-0.1f))/0.1f); }
                        else {
                            mixC(1.0f,1.0f,0.5f, 1.0f,0.8f,0.0f, t);
                            if(dist<0.2f) { r=1.f; g=1.f; b=1.f; } // White core
                        }
                    }
                    break;
                }
                case SYM_SCATTER: {
                    if(dist<0.75f) {
                        a=1.f;
                        // Purple orb
                        mixC(0.8f,0.2f,1.0f, 0.2f,0.0f,0.4f, dist/0.75f);
                        // Inner ring
                        if(fabsf(dist-0.4f)<0.05f) { r=1.f; g=0.5f; b=1.f; }
                        // Starburst
                        if(fabsf(nx)<0.05f || fabsf(ny)<0.05f || fabsf(fabsf(nx)-fabsf(ny))<0.05f) {
                            if(dist<0.6f) { r=1.f; g=0.8f; b=1.f; }
                        }
                        // Gold border
                        if(dist>0.65f) { mixC(1.0f,0.8f,0.2f, 0.6f,0.4f,0.0f, (dist-0.65f)/0.1f); }
                    }
                    break;
                }
                default: { // BAR3, BAR2, BAR1
                    int barCount = sym - SYM_BAR3 + 3;
                    float width = 0.7f, height = 0.2f;
                    
                    auto drawBar = [&](float cy) {
                        float dx = fabsf(nx), dy = fabsf(ny-cy);
                        if(dx<width && dy<height) {
                            a=1.f;
                            // Silver/Blue metallic gradient
                            float t = dy/height;
                            if(dx>width-0.05f || dy>height-0.05f) { r=0.8f; g=0.8f; b=1.0f; } // Edge highlight
                            else { mixC(0.9f,0.9f,1.0f, 0.3f,0.3f,0.5f, t); }
                            // "BAR" pseudo-text lines
                            if(dy<0.05f && dx<width-0.2f) { r=0.1f; g=0.1f; b=0.2f; }
                        }
                    };

                    if(barCount==1) { drawBar(0.0f); }
                    else if(barCount==2) { drawBar(-0.25f); drawBar(0.25f); }
                    else { drawBar(-0.4f); drawBar(0.0f); drawBar(0.4f); }
                    break;
                }
            }

            int idx=(y*size+x)*4;
            px[idx+0]=(uint8_t)std::clamp(r*255.f, 0.f, 255.f);
            px[idx+1]=(uint8_t)std::clamp(g*255.f, 0.f, 255.f);
            px[idx+2]=(uint8_t)std::clamp(b*255.f, 0.f, 255.f);
            px[idx+3]=(uint8_t)std::clamp(a*255.f, 0.f, 255.f);
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

    // Layout: 5 reels centered, aspect-corrected to be perfectly square
    float aspect = (float)m_h/(float)m_w;
    float reelW  = 0.27f;           // NDC width per reel
    float symH   = reelW / aspect;  // NDC height (aspect-corrected to be square on screen)
    float gapX   = 0.015f;          // tight horizontal gap
    int rc = engine.getReelCount();
    float totalW = rc*reelW + (rc-1)*gapX;
    float startX = -totalW*0.5f + reelW*0.5f;
    float reelTop= symH; // Centers the 3 rows: symH, 0.0, -symH

    const auto& result = engine.getLastResult();

    // Create temporary VAO for a symbol quad
    float hw=reelW*0.44f, hh=symH*0.44f;
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

    // ── Scissor Test: Clip symbols scrolling outside the reels container boundary ──
    float scissorLeft   = -totalW * 0.5f;
    float scissorRight  =  totalW * 0.5f;
    float scissorBottom = -1.5f * symH;
    float scissorTop    =  1.5f * symH;

    int scX = static_cast<int>((scissorLeft + 1.0f) * 0.5f * m_w);
    int scY = static_cast<int>((scissorBottom + 1.0f) * 0.5f * m_h);
    int scW = static_cast<int>((scissorRight - scissorLeft) * 0.5f * m_w);
    int scH = static_cast<int>((scissorTop - scissorBottom) * 0.5f * m_h);

    glEnable(GL_SCISSOR_TEST);
    glScissor(scX, scY, scW, scH);

    for(int r=0;r<rc;r++){
        const auto& rs=engine.getReelState(r);
        float reelX = startX + r*(reelW+gapX);
        float scrollFrac = fmodf(rs.offset,1.f);

        // Loop from row -1 to ROW_COUNT - 1 (4 rows total) to cover the top gap during scrolling
        for(int row=-1;row<ROW_COUNT;row++){
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

    glDisable(GL_SCISSOR_TEST);
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
    glActiveTexture(GL_TEXTURE1); glBindTexture(GL_TEXTURE_2D,m_bloomTex); u1i(p,"uBloomTex",1);
    u1f(p,"uTime",m_time);
    u1f(p,"uChrom",chromaticStr);
    u1f(p,"uGrain",grainStrength);
    u1f(p,"uBloomStr",bloomStrength);
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
