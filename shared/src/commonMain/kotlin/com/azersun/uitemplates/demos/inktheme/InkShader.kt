package com.azersun.uitemplates.demos.inktheme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RenderEffect

class InkUniforms(
    val width: Float,
    val height: Float,
    val originX: Float,
    val originY: Float,
    /** 0 = nothing revealed, 1 = new theme fully revealed. */
    val progress: Float,
    /** Seconds; drives the drift of the marbled ink. */
    val time: Float,
    val tendrils: Float,
    val noiseScale: Float,
    val softness: Float,
    /** How far behind the front the ink wash stays before the new page shows through. */
    val inkBand: Float,
    val ink: Color,
)

/** Renders the incoming theme through an ink-in-water mask. Null where runtime shaders are unavailable. */
fun interface InkRenderer {
    fun effect(uniforms: InkUniforms): RenderEffect
}

expect fun createInkRenderer(): InkRenderer?

/**
 * SkSL, which is also valid AGSL. The mask is a radial front from [origin] whose distance field is
 * displaced by domain-warped fbm (the tendrils), and the band just behind the front is washed with
 * marbled ink before the new content shows through.
 */
internal const val INK_SKSL = """
uniform shader content;
uniform float2 size;
uniform float2 origin;
uniform float progress;
uniform float time;
uniform float tendrils;
uniform float noiseScale;
uniform float softness;
uniform float inkBand;
uniform float4 ink;

float hash(float2 p) {
    p = fract(p * float2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float noise(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    float a = hash(i);
    float b = hash(i + float2(1.0, 0.0));
    float c = hash(i + float2(0.0, 1.0));
    float d = hash(i + float2(1.0, 1.0));
    float2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float fbm(float2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 4; i++) {
        v += a * noise(p);
        p = p * 2.03 + float2(17.0, 9.0);
        a *= 0.5;
    }
    return v;
}

float fbm2(float2 p) {
    return 0.5 * noise(p) + 0.25 * noise(p * 2.03 + float2(17.0, 9.0));
}

half4 main(float2 coord) {
    float aspect = size.x / size.y;
    float2 uv = coord / size.y;
    float2 o = origin / size.y;
    float maxR = max(
        max(length(o), length(o - float2(aspect, 0.0))),
        max(length(o - float2(0.0, 1.0)), length(o - float2(aspect, 1.0)))
    );
    float2 d = uv - o;
    float dist = length(d) / maxR;
    float front = mix(-0.8 * tendrils, 1.0 + 0.8 * tendrils + inkBand + softness, progress);

    // Pixels far outside or far inside the front skip the noise entirely.
    if (dist - 0.8 * tendrils > front) return half4(0.0);
    if (dist + 0.8 * tendrils < front - inkBand - softness) return content.eval(coord);

    float2 q = uv * noiseScale;
    float2 w = float2(fbm2(q + time * 0.15), fbm2(q + float2(5.2, 1.3) - time * 0.12));
    float body = fbm(q + 2.5 * w);
    float ang = atan(d.y, d.x);
    float streak = fbm2(float2(ang * 5.0, dist * 2.5) * noiseScale * 0.6 + 1.5 * w) * 1.33;
    float field = dist + tendrils * (body - 0.5) * 0.9 + tendrils * (streak - 0.5) * 0.7;

    float reveal = 1.0 - smoothstep(front - softness, front, field);
    if (reveal <= 0.0) return half4(0.0);
    float behind = front - field;
    float band = 1.0 - smoothstep(0.0, inkBand, behind);
    float4 c = float4(content.eval(coord));
    if (band <= 0.0) return half4(c * reveal);
    float marble = smoothstep(0.2, 0.55, fbm2(q * 2.2 + 3.0 * w + time * 0.25));
    float inkAmount = band * mix(0.45, 1.0, marble) * reveal;
    float4 washed = mix(c, float4(ink.rgb, 1.0), inkAmount);
    return half4(washed * reveal);
}
"""
