cmake_minimum_required(VERSION 3.22.1)
project("magicsslot" VERSION 1.0.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

find_library(log-lib    log)
find_library(android-lib android)
find_library(gles3-lib  GLESv3)
find_library(egl-lib    EGL)
find_library(aaudio-lib aaudio)

add_library(
    magicsslot
    SHARED
    jni_bridge.cpp
    SlotEngine.cpp
    Renderer.cpp
    ParticleSystem.cpp
    ShaderManager.cpp
    AudioEngine.cpp
)

target_include_directories(magicsslot PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})

target_compile_options(magicsslot PRIVATE
    -O2
    -std=c++17
    -DANDROID
    -DUSE_OPENGL_ES3
    -frtti
    -fexceptions
    -Wall
    -Wno-unused-variable
)

target_link_libraries(
    magicsslot
    ${log-lib}
    ${android-lib}
    ${gles3-lib}
    ${egl-lib}
    ${aaudio-lib}
    atomic
)
