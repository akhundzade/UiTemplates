package com.azersun.uitemplates.demos.inktheme

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.asComposeRenderEffect

actual fun createInkRenderer(): InkRenderer? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) AgslInkRenderer() else null

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class AgslInkRenderer : InkRenderer {
    private val shader = RuntimeShader(INK_SKSL)

    override fun effect(uniforms: InkUniforms): androidx.compose.ui.graphics.RenderEffect {
        with(uniforms) {
            shader.setFloatUniform("size", width, height)
            shader.setFloatUniform("origin", originX, originY)
            shader.setFloatUniform("progress", progress)
            shader.setFloatUniform("time", time)
            shader.setFloatUniform("tendrils", tendrils)
            shader.setFloatUniform("noiseScale", noiseScale)
            shader.setFloatUniform("softness", softness)
            shader.setFloatUniform("inkBand", inkBand)
            shader.setFloatUniform("ink", ink.red, ink.green, ink.blue, ink.alpha)
        }
        return RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
    }
}
