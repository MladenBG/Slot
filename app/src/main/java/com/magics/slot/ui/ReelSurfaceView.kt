package com.magics.slot.ui

import android.content.Context
import android.opengl.GLSurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.magics.slot.SlotNativeBridge
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

// ─────────────────────────────────────────────────────────────────────────────
//  ReelSurfaceView.kt  –  GLSurfaceView ES 3 + Compose wrapper
// ─────────────────────────────────────────────────────────────────────────────
class MagicsGLSurface(context: Context) : GLSurfaceView(context) {
    private var lastTime = 0L

    init {
        setEGLContextClientVersion(3)
        setEGLConfigChooser(8, 8, 8, 8, 0, 0)
        setRenderer(object : Renderer {
            override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                // init called after size is known
            }
            override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
                SlotNativeBridge.nativeSurfaceCreated(width, height)
                lastTime = System.nanoTime()
            }
            override fun onDrawFrame(gl: GL10?) {
                val now = System.nanoTime()
                val dt  = ((now - lastTime) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                lastTime = now
                SlotNativeBridge.nativeDrawFrame(dt)
            }
        })
        renderMode = RENDERMODE_CONTINUOUSLY
    }
}

@Composable
fun ReelSurface(modifier: Modifier = Modifier) {
    AndroidView(
        factory  = { ctx -> MagicsGLSurface(ctx) },
        modifier = modifier
    )
}
