#pragma once
// ─────────────────────────────────────────────────────────────────────────────
//  ShaderManager.h  –  All inline GLSL ES 3.20 shaders + compilation
// ─────────────────────────────────────────────────────────────────────────────
#include <GLES3/gl3.h>
#include <string>
#include <vector>
#include <android/log.h>

#define LOG_TAG "MagicsSlot"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace MagicsSlot {

// ═════════════════════════════════════════════════════════════════════════════
// GLSL SOURCES
// ═════════════════════════════════════════════════════════════════════════════

// ── Symbol ────────────────────────────────────────────────────────────────────
static const char* kSymVert = R"GLSL(#version 300 es
precision highp float;
layout(location=0) in vec2 aPos;
layout(location=1) in vec2 aUV;
uniform mat4 uMVP;
out vec2 vUV;
void main(){
    gl_Position = uMVP * vec4(aPos, 0.0, 1.0);
    vUV = aUV;
})GLSL";

static const char* kSymFrag = R"GLSL(#version 300 es
precision highp float;
in  vec2 vUV;
out vec4 fragColor;
uniform sampler2D uTex;
uniform vec4  uTint;
uniform float uTime;
uniform bool  uWin;
float glow(vec2 uv){
    vec2 d = abs(uv - 0.5)*2.0;
    return smoothstep(0.85,1.0,max(d.x,d.y));
}
void main(){
    vec4 t = texture(uTex, vUV);
    if(t.a < 0.05) discard;
    vec3 c = mix(t.rgb, uTint.rgb, 0.2);
    if(uWin){ float p=0.5+0.5*sin(uTime*8.0); c=mix(c,uTint.rgb,p*0.5); }
    c += uTint.rgb * glow(vUV) * 1.8;
    fragColor = vec4(c, t.a);
})GLSL";

// ── Background (procedural cyberpunk) ─────────────────────────────────────────
static const char* kBgVert = R"GLSL(#version 300 es
precision highp float;
layout(location=0) in vec2 aPos;
layout(location=1) in vec2 aUV;
out vec2 vUV;
void main(){ gl_Position=vec4(aPos,0.0,1.0); vUV=aUV; })GLSL";

static const char* kBgFrag = R"GLSL(#version 300 es
precision highp float;
in  vec2 vUV;
out vec4 fragColor;
uniform float uTime;
uniform vec2  uRes;

float hash(vec2 p){ p=fract(p*vec2(234.34,435.345)); p+=dot(p,p+34.23); return fract(p.x*p.y); }
float noise(vec2 p){
    vec2 i=floor(p), f=fract(p); f=f*f*(3.0-2.0*f);
    return mix(mix(hash(i),hash(i+vec2(1,0)),f.x),mix(hash(i+vec2(0,1)),hash(i+vec2(1,1)),f.x),f.y);
}

void main(){
    vec2 uv = vUV;
    vec3 bg = mix(vec3(0.02,0.01,0.08), vec3(0.06,0.02,0.18), uv.y);

    // Stars
    vec2 su=uv*vec2(80.0,60.0); vec2 si=floor(su),sf=fract(su);
    float h=hash(si);
    if(h>0.92){
        float tw=0.6+0.4*sin(uTime*3.0*h+h*6.28);
        float s=smoothstep(0.08,0.0,length(sf-0.5))*tw;
        bg+=vec3(s*0.8,s*0.9,s);
    }

    // Nebula
    float n1=noise(uv*4.0+vec2(uTime*0.03,0.0));
    float n2=noise(uv*8.0-vec2(0.0,uTime*0.02));
    bg+=vec3(0.4,0.0,0.8)*n1*0.12+vec3(0.0,0.8,1.0)*n2*0.08;

    // City silhouette
    if(uv.y<0.28){
        float city=0.0;
        for(float i=0.0;i<8.0;i++){
            float bx=fract(i*0.137+0.05), bw=0.06+hash(vec2(i,0.0))*0.06;
            float bh=0.06+hash(vec2(i,1.0))*0.22;
            if(uv.x>bx&&uv.x<bx+bw&&uv.y<bh) city=1.0;
        }
        bg=mix(bg,mix(vec3(0.05,0.03,0.12),vec3(0.01,0.0,0.05),city),city);
        // Neon windows
        vec2 wi=floor(uv*vec2(120.0,80.0));
        float wh=hash(wi);
        if(wh>0.85&&city>0.5){
            float fl=step(0.7,fract(wh*100.0+uTime*wh*2.0));
            bg+=mix(vec3(0.0,1.0,1.0),vec3(1.0,0.0,1.0),wh)*0.3*fl;
        }
    }

    // Scanline
    bg*=0.97+0.03*sin(uv.y*uRes.y*1.5);

    // Neon grid lines (reel area highlight)
    float gridX=abs(fract(uv.x*6.0)-0.5);
    float gridLine=smoothstep(0.48,0.5,gridX)*0.06;
    bg+=vec3(0.0,0.8,1.0)*gridLine*(1.0-abs(uv.y*2.0-1.0));

    fragColor=vec4(bg,1.0);
})GLSL";

// ── Particle ──────────────────────────────────────────────────────────────────
static const char* kPartVert = R"GLSL(#version 300 es
precision highp float;
layout(location=0) in vec2  aPos;
layout(location=1) in vec2  aVel;
layout(location=2) in float aLife;
layout(location=3) in vec4  aColor;
layout(location=4) in float aSize;
uniform mat4  uMVP;
out vec4  vColor;
out float vLife;
void main(){
    gl_Position  = uMVP * vec4(aPos, 0.0, 1.0);
    gl_PointSize = aSize * aLife;
    vColor = aColor; vLife = aLife;
})GLSL";

static const char* kPartFrag = R"GLSL(#version 300 es
precision highp float;
in  vec4  vColor;
in  float vLife;
out vec4  fragColor;
void main(){
    vec2 c=gl_PointCoord-0.5; float d=length(c);
    if(d>0.5) discard;
    float a=smoothstep(0.5,0.0,d)*vLife;
    float core=smoothstep(0.15,0.0,d);
    fragColor=vec4(mix(vColor.rgb,vec3(1.0),core*0.7), a*vColor.a);
})GLSL";

// ── Bloom ─────────────────────────────────────────────────────────────────────
static const char* kBloomVert = R"GLSL(#version 300 es
precision highp float;
layout(location=0) in vec2 aPos; layout(location=1) in vec2 aUV;
out vec2 vUV; void main(){ gl_Position=vec4(aPos,0.0,1.0); vUV=aUV; })GLSL";

static const char* kBloomFrag = R"GLSL(#version 300 es
precision highp float;
in  vec2 vUV; out vec4 fragColor;
uniform sampler2D uTex; uniform vec2 uTexel; uniform float uThresh; uniform bool uDown;
vec3 kawase(sampler2D t,vec2 uv,vec2 tx,float o){
    return (texture(t,uv+vec2(o+.5, o+.5)*tx).rgb+texture(t,uv+vec2(-o-.5, o+.5)*tx).rgb
           +texture(t,uv+vec2(o+.5,-o-.5)*tx).rgb+texture(t,uv+vec2(-o-.5,-o-.5)*tx).rgb)*0.25;
}
void main(){
    if(uDown){
        vec3 c=texture(uTex,vUV).rgb;
        float br=dot(c,vec3(0.2126,0.7152,0.0722));
        fragColor=vec4(br<uThresh?vec3(0.0):c,1.0);
    } else { fragColor=vec4(kawase(uTex,vUV,uTexel,0.0),1.0); }
})GLSL";

// ── Post-process (chromatic aberration + grain + ACES) ────────────────────────
static const char* kPostVert = R"GLSL(#version 300 es
precision highp float;
layout(location=0) in vec2 aPos; layout(location=1) in vec2 aUV;
out vec2 vUV; void main(){ gl_Position=vec4(aPos,0.0,1.0); vUV=aUV; })GLSL";

static const char* kPostFrag = R"GLSL(#version 300 es
precision highp float;
in  vec2 vUV; out vec4 fragColor;
uniform sampler2D uScene; uniform sampler2D uBloom;
uniform float uTime; uniform float uChrom; uniform float uGrain; uniform float uBloom;

float hash13(vec3 p){ p=fract(p*vec3(443.8975,397.2973,491.1871)); p+=dot(p.zxy,p.yxz+19.19); return fract(p.x*p.y*p.z); }

void main(){
    vec2 uv=vUV;
    vec2 center=uv-0.5; float len=length(center);
    vec2 dir=normalize(center+0.001);
    float str=uChrom*0.012*len;
    float r=texture(uScene,uv+dir*str*1.0).r;
    float g=texture(uScene,uv             ).g;
    float b=texture(uScene,uv-dir*str*0.8).b;
    vec3 col=vec3(r,g,b);
    col+=texture(uBloom,uv).rgb*uBloom;
    float grain=(hash13(vec3(uv*1000.0,uTime*0.1))*2.0-1.0)*uGrain*0.04;
    col+=grain;
    col*=mix(0.6,1.0,1.0-smoothstep(0.4,1.0,len));
    // ACES
    col=col*(2.51*col+0.03)/(col*(2.43*col+0.59)+0.14);
    col=clamp(col,0.0,1.0);
    col=pow(col,vec3(1.0/2.2));
    fragColor=vec4(col,1.0);
})GLSL";

// ─────────────────────────────────────────────────────────────────────────────
class ShaderManager {
public:
    GLuint symProg  = 0;
    GLuint bgProg   = 0;
    GLuint partProg = 0;
    GLuint bloomProg= 0;
    GLuint postProg = 0;

    bool init() {
        symProg   = build(kSymVert,    kSymFrag);
        bgProg    = build(kBgVert,     kBgFrag);
        partProg  = build(kPartVert,   kPartFrag);
        bloomProg = build(kBloomVert,  kBloomFrag);
        postProg  = build(kPostVert,   kPostFrag);
        bool ok = symProg && bgProg && partProg && bloomProg && postProg;
        if (ok) LOGI("All shaders compiled."); else LOGE("Shader compile failed!");
        return ok;
    }

    void cleanup() {
        auto d=[](GLuint& p){if(p){glDeleteProgram(p);p=0;}};
        d(symProg); d(bgProg); d(partProg); d(bloomProg); d(postProg);
    }

private:
    GLuint compile(GLenum type, const char* src) {
        GLuint s = glCreateShader(type);
        glShaderSource(s, 1, &src, nullptr);
        glCompileShader(s);
        GLint ok=0; glGetShaderiv(s,GL_COMPILE_STATUS,&ok);
        if(!ok){
            GLint len=0; glGetShaderiv(s,GL_INFO_LOG_LENGTH,&len);
            std::vector<char> log(len+1); glGetShaderInfoLog(s,len,nullptr,log.data());
            LOGE("Shader error: %s",log.data()); glDeleteShader(s); return 0;
        }
        return s;
    }
    GLuint build(const char* vs, const char* fs) {
        GLuint v=compile(GL_VERTEX_SHADER,vs), f=compile(GL_FRAGMENT_SHADER,fs);
        if(!v||!f) return 0;
        GLuint p=glCreateProgram();
        glAttachShader(p,v); glAttachShader(p,f); glLinkProgram(p);
        GLint ok=0; glGetProgramiv(p,GL_LINK_STATUS,&ok);
        if(!ok){
            GLint len=0; glGetProgramiv(p,GL_INFO_LOG_LENGTH,&len);
            std::vector<char> log(len+1); glGetProgramInfoLog(p,len,nullptr,log.data());
            LOGE("Link error: %s",log.data()); glDeleteProgram(p); p=0;
        }
        glDeleteShader(v); glDeleteShader(f);
        return p;
    }
};

} // namespace MagicsSlot
