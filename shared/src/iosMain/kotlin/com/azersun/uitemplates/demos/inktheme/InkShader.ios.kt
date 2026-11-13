package com.azersun.uitemplates.demos.inktheme

import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder

actual fun createInkRenderer(): InkRenderer? = SkslInkRenderer()

private class SkslInkRenderer : InkRenderer {
    private val effect = RuntimeEffect.makeForShader(INK_SKSL)

    override fun effect(uniforms: InkUniforms): RenderEffect {
        val builder = RuntimeShaderBuilder(effect)
        with(uniforms) {
            builder.uniform("size", width, height)
            builder.uniform("origin", originX, originY)
            builder.uniform("progress", progress)
            builder.uniform("time", time)
            builder.uniform("tendrils", tendrils)
            builder.uniform("noiseScale", noiseScale)
            builder.uniform("softness", softness)
            builder.uniform("inkBand", inkBand)
            builder.uniform("ink", ink.red, ink.green, ink.blue, ink.alpha)
        }
        return ImageFilter.makeRuntimeShader(builder, "content", null).asComposeRenderEffect()
    }
}
