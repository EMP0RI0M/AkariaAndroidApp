package com.akaria.agent.ui

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.max

// AGSL Shader Port of the FeralUI Voice Cloud Scene
const val AGSL_CLOUD_SHADER = """
uniform float2 u_res;
uniform float u_time;
uniform float u_state;
uniform float u_level;
uniform float u_wind;
uniform float u_punch;
uniform shader u_noise;
uniform float3 u_main;
uniform float3 u_low;
uniform float3 u_mid;
uniform float3 u_high;

const float E = 2.71828182846;

float scaled(float e0, float e1, float x) { return clamp((x - e0) / (e1 - e0), 0.0, 1.0); }
float fixedSpring(float t, float d) {
  float s = mix(1.0 - exp(-E * 2.0 * t) * cos((1.0 - d) * 115.0 * t), 1.0, clamp(t, 0.0, 1.0));
  return s * (1.0 - t) + t;
}
vec3 linearBurn(vec3 base, vec3 blend, float opacity) {
  return (max(base + blend - vec3(1.0), vec3(0.0))) * opacity + base * (1.0 - opacity);
}

vec4 permute(vec4 x) { return mod((x * 34.0 + 1.0) * x, 289.0); }
vec4 taylorInvSqrt(vec4 r) { return 1.79284291400159 - 0.85373472095314 * r; }
vec3 fade(vec3 t) { return t * t * t * (t * (t * 6.0 - 15.0) + 10.0); }
float rand(vec2 n) { return fract(sin(dot(n, vec2(12.9898, 4.1414))) * 43758.5453); }

float noise(vec2 p) {
  vec2 ip = floor(p);
  vec2 u = fract(p);
  u = u * u * (3.0 - 2.0 * u);
  float res = mix(
    mix(rand(ip), rand(ip + vec2(1.0, 0.0)), u.x),
    mix(rand(ip + vec2(0.0, 1.0)), rand(ip + vec2(1.0, 1.0)), u.x),
    u.y
  );
  return res * res;
}

float fbm(vec2 x) {
  float v = 0.0;
  float a = 0.5;
  vec2 shift = vec2(100.0);
  mat2 rot = mat2(cos(0.5), sin(0.5), -sin(0.5), cos(0.5));
  for (int i = 0; i < 4; i++) {
    v += a * noise(x);
    x = rot * x * 2.0 + shift;
    a *= 0.5;
  }
  return v;
}

float cnoise(vec3 P) {
  vec3 Pi0 = floor(P); vec3 Pi1 = Pi0 + vec3(1.0);
  Pi0 = mod(Pi0, 289.0); Pi1 = mod(Pi1, 289.0);
  vec3 Pf0 = fract(P); vec3 Pf1 = Pf0 - vec3(1.0);
  vec4 ix = vec4(Pi0.x, Pi1.x, Pi0.x, Pi1.x);
  vec4 iy = vec4(Pi0.y, Pi1.y, Pi0.y, Pi1.y);
  vec4 iz0 = vec4(Pi0.z); vec4 iz1 = vec4(Pi1.z);
  vec4 ixy = permute(permute(ix) + iy);
  vec4 ixy0 = permute(ixy + iz0); vec4 ixy1 = permute(ixy + iz1);
  vec4 gx0 = ixy0 / 7.0; vec4 gy0 = fract(floor(gx0) / 7.0) - 0.5;
  gx0 = fract(gx0);
  vec4 gz0 = vec4(0.5) - abs(gx0) - abs(gy0);
  vec4 sz0 = step(gz0, vec4(0.0));
  gx0 -= sz0 * (step(vec4(0.0), gx0) - 0.5);
  gy0 -= sz0 * (step(vec4(0.0), gy0) - 0.5);
  vec4 gx1 = ixy1 / 7.0; vec4 gy1 = fract(floor(gx1) / 7.0) - 0.5;
  gx1 = fract(gx1);
  vec4 gz1 = vec4(0.5) - abs(gx1) - abs(gy1);
  vec4 sz1 = step(gz1, vec4(0.0));
  gx1 -= sz1 * (step(vec4(0.0), gx1) - 0.5);
  gy1 -= sz1 * (step(vec4(0.0), gy1) - 0.5);
  vec3 g000 = vec3(gx0.x, gy0.x, gz0.x); vec3 g100 = vec3(gx0.y, gy0.y, gz0.y);
  vec3 g010 = vec3(gx0.z, gy0.z, gz0.z); vec3 g110 = vec3(gx0.w, gy0.w, gz0.w);
  vec3 g001 = vec3(gx1.x, gy1.x, gz1.x); vec3 g101 = vec3(gx1.y, gy1.y, gz1.y);
  vec3 g011 = vec3(gx1.z, gy1.z, gz1.z); vec3 g111 = vec3(gx1.w, gy1.w, gz1.w);
  vec4 norm0 = taylorInvSqrt(vec4(dot(g000, g000), dot(g010, g010), dot(g100, g100), dot(g110, g110)));
  g000 *= norm0.x; g010 *= norm0.y; g100 *= norm0.z; g110 *= norm0.w;
  vec4 norm1 = taylorInvSqrt(vec4(dot(g001, g001), dot(g011, g011), dot(g101, g101), dot(g111, g111)));
  g001 *= norm1.x; g011 *= norm1.y; g101 *= norm1.z; g111 *= norm1.w;
  float n000 = dot(g000, Pf0); float n100 = dot(g100, vec3(Pf1.x, Pf0.y, Pf0.z));
  float n010 = dot(g010, vec3(Pf0.x, Pf1.y, Pf0.z)); float n110 = dot(g110, vec3(Pf1.x, Pf1.y, Pf0.z));
  float n001 = dot(g001, vec3(Pf0.x, Pf0.y, Pf1.z)); float n101 = dot(g101, vec3(Pf1.x, Pf0.y, Pf1.z));
  float n011 = dot(g011, vec3(Pf0.x, Pf1.y, Pf1.z)); float n111 = dot(g111, Pf1);
  vec3 fade_xyz = fade(Pf0);
  vec4 n_z = mix(vec4(n000, n100, n010, n110), vec4(n001, n101, n011, n111), fade_xyz.z);
  vec2 n_yz = mix(vec2(n_z.x, n_z.y), vec2(n_z.z, n_z.w), fade_xyz.y);
  float n_xyz = mix(n_yz.x, n_yz.y, fade_xyz.x);
  return 2.2 * n_xyz;
}

half4 main(float2 fragCoord) {
  vec2 v_uv = fragCoord / u_res;
  vec2 st = v_uv - 0.5;
  st.y *= u_res.y / u_res.x;

  float entry = fixedSpring(scaled(0.0, 2.0, u_state), 0.92);
  float entryScale = mix(0.9, 1.0, entry);
  float radius = 0.46 * entryScale;

  float scaleFactor = 1.0 / (2.0 * 0.37 * entryScale);
  vec2 uv = st * scaleFactor + 0.5;
  uv.y = 1.0 - uv.y;

  float condense = clamp(u_level * 1.0 + u_wind * 0.6, 0.0, 1.0);
  uv = (uv - 0.5) * mix(1.05, 0.95, condense) + 0.5;

  float time = u_state * 0.15 + u_time * 0.7;

  float noiseScale = 1.25;
  float windSpeed = 0.12 + u_level * 0.05;
  float warpPower = 0.35;
  float waterColorNoiseScale = 18.0;
  float waterColorNoiseStrength = 0.02;
  float textureNoiseStrength = 0.15;
  
  float waveSpread = 1.0 + u_level * 0.2 + u_wind * 0.14;
  float layer1Amplitude = 1.5;
  float layer2Amplitude = 1.4;
  float layer3Amplitude = 1.3;
  float fbmStrength = 1.2 + u_level * 0.1;
  float fbmPowerDamping = 0.55;
  float blurRadius = 1.0;

  float lift = u_level * 0.6 + u_wind * 0.55;
  float verticalOffset = 0.075 + 0.115 * (lift / (lift + 0.65));

  float noiseX = cnoise(vec3(uv + vec2(0.0, 74.8572), time * 0.3));
  float noiseY = cnoise(vec3(uv + vec2(203.91282, 10.0), time * 0.3));
  uv += vec2(noiseX * 2.0, noiseY) * warpPower;

  float noiseA = cnoise(vec3(uv * waterColorNoiseScale + vec2(344.91282, 0.0), time * 0.3)) +
                 cnoise(vec3(uv * waterColorNoiseScale * 2.2 + vec2(723.937, 0.0), time * 0.4)) * 0.5;
  uv += noiseA * waterColorNoiseStrength;
  uv.y -= verticalOffset;

  float dispMix = (sin(time) + 1.0) * 0.5;
  vec2 textureUv = uv;
  
  // Bitmap is 256x256, AGSL eval uses pixel coordinates
  float tR0 = u_noise.eval(textureUv * 256.0).r;
  float tG0 = u_noise.eval(vec2(textureUv.x, 1.0 - textureUv.y) * 256.0).g;
  float disp0 = mix(tR0 - 0.5, tG0 - 0.5, dispMix) * textureNoiseStrength;

  textureUv += vec2(63.861, 368.937);
  float tR1 = u_noise.eval(textureUv * 256.0).r;
  float tG1 = u_noise.eval(vec2(textureUv.x, 1.0 - textureUv.y) * 256.0).g;
  float disp1 = mix(tR1 - 0.5, tG1 - 0.5, dispMix) * textureNoiseStrength;

  textureUv += vec2(453.163, 1649.808);
  float tR3 = u_noise.eval(textureUv * 256.0).r;
  float tG3 = u_noise.eval(vec2(textureUv.x, 1.0 - textureUv.y) * 256.0).g;
  float disp3 = mix(tR3 - 0.5, tG3 - 0.5, dispMix) * textureNoiseStrength;
  uv += disp0;

  vec2 st_fbm = uv * noiseScale;
  vec2 q = vec2(fbm(st_fbm * 0.5 + windSpeed * time));
  vec2 r = vec2(
    fbm(st_fbm + q + vec2(0.3, 9.2) + 0.15 * time),
    fbm(st_fbm + q + vec2(8.3, 0.8) + 0.126 * time)
  );
  float f = fbm(st_fbm + r - q);
  float fullFbm = (f + 0.6 * f * f + 0.7 * f + 0.5) * 0.5;
  fullFbm = pow(fullFbm, fbmPowerDamping);
  fullFbm *= fbmStrength;

  blurRadius = blurRadius * 1.5;

  vec2 snUv = uv + vec2((fullFbm - 0.5) * 1.2) + vec2(0.0, 0.025) + disp0;
  float sn = noise(snUv * 2.0 + vec2(0.0, time * 0.5)) * 2.0 * layer1Amplitude;
  float sn2 = smoothstep(sn - 1.2 * blurRadius, sn + 1.2 * blurRadius, (snUv.y - 0.5 * waveSpread) * 5.0 + 0.5);

  vec2 snUvBis = uv + vec2((fullFbm - 0.5) * 0.85) + vec2(0.0, 0.025) + disp1;
  float snBis = noise(snUvBis * 4.0 + vec2(293.0, time * 1.0)) * 2.0 * layer2Amplitude;
  float sn2Bis = smoothstep(snBis - 0.9 * blurRadius, snBis + 0.9 * blurRadius, (snUvBis.y - 0.6 * waveSpread) * 5.0 + 0.5);

  vec2 snUvThird = uv + vec2((fullFbm - 0.5) * 1.1) + disp3;
  float snThird = noise(snUvThird * 6.0 + vec2(153.0, time * 1.2)) * 2.0 * layer3Amplitude;
  float sn2Third = smoothstep(snThird - 0.7 * blurRadius, snThird + 0.7 * blurRadius, (snUvThird.y - 0.9 * waveSpread) * 6.0 + 0.5);

  sn2 = pow(sn2, 0.8);
  sn2Bis = pow(sn2Bis, 0.9);

  vec3 col = linearBurn(u_main, u_low, 1.0 - sn2);
  col = linearBurn(col, mix(u_main, u_mid, 1.0 - sn2Bis), sn2);
  col = mix(col, mix(u_main, u_high, 1.0 - sn2Third), sn2 * sn2Bis);
  
  col = mix(col, u_high, u_level * 0.04 + u_punch * 0.05);

  float rr = length(st) / radius;
  float topLight = smoothstep(0.95, 0.1, rr) * smoothstep(0.05, 0.8, -st.y / radius);
  col = mix(col, u_high, topLight * (0.04 + u_level * 0.06));
  col = mix(col, u_high, smoothstep(1.0, 0.2, rr) * u_level * 0.05);
  float rim = smoothstep(0.78, 0.975, rr);
  col = mix(col, u_high, rim * (0.05 + u_level * 0.08));

  float dist = length(st) - radius;
  float shape = smoothstep(0.0075, 0.0, dist);
  return half4(col * shape, shape);
}
"""

// Create a watercolor noise texture as a Bitmap
private fun createNoiseBitmap(): Bitmap {
    val size = 256
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint()

    canvas.drawColor(AndroidColor.rgb(128, 128, 128))
    
    val cells = listOf(4, 8, 16)
    val alphas = listOf((255 * 0.6).toInt(), (255 * 0.32).toInt(), (255 * 0.16).toInt())
    
    for (i in cells.indices) {
        val cell = cells[i]
        val layer = Bitmap.createBitmap(cell, cell, Bitmap.Config.ARGB_8888)
        for (y in 0 until cell) {
            for (x in 0 until cell) {
                layer.setPixel(x, y, AndroidColor.argb(255, (Math.random()*255).toInt(), (Math.random()*255).toInt(), (Math.random()*255).toInt()))
            }
        }
        paint.alpha = alphas[i]
        // Scale it up
        val scaledLayer = Bitmap.createScaledBitmap(layer, size, size, true)
        canvas.drawBitmap(scaledLayer, 0f, 0f, paint)
    }
    
    // Smooth out
    var finalBitmap = bitmap
    for (i in 0..1) {
        val shrunk = Bitmap.createScaledBitmap(finalBitmap, size / 4, size / 4, true)
        finalBitmap = Bitmap.createScaledBitmap(shrunk, size, size, true)
    }
    return finalBitmap
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun VoiceScreen(onNext: () -> Unit) {
    val runtimeShader = remember { RuntimeShader(AGSL_CLOUD_SHADER) }
    val noiseBitmap = remember { createNoiseBitmap() }
    val noiseShader = remember { BitmapShader(noiseBitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT) }
    
    var time by remember { mutableStateOf(0f) }
    var state by remember { mutableStateOf(0f) }
    var level by remember { mutableStateOf(0f) }
    var isListening by remember { mutableStateOf(false) }

    LaunchedEffect(isListening) {
        val startTime = System.currentTimeMillis()
        while (isActive) {
            val now = System.currentTimeMillis()
            time = (now - startTime) / 1000f
            state += 0.05f
            
            // Fake audio level for demo
            if (isListening) {
                level = (0.2f + Math.random().toFloat() * 0.5f)
            } else {
                level = max(0f, level - 0.05f)
            }
            delay(16L) // ~60 FPS
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101014)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Say it out loud.",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "A minute of talking beats an hour of circling.",
                color = Color(0xFFA0A0A0),
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(64.dp))
            
            // The Orb
            Canvas(
                modifier = Modifier
                    .size(240.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { isListening = !isListening }
                    .graphicsLayer {
                        runtimeShader.setFloatUniform("u_res", size.width, size.height)
                        runtimeShader.setFloatUniform("u_time", time)
                        runtimeShader.setFloatUniform("u_state", state)
                        runtimeShader.setFloatUniform("u_level", level)
                        runtimeShader.setFloatUniform("u_wind", level * 1.5f)
                        runtimeShader.setFloatUniform("u_punch", level * 0.5f)
                        runtimeShader.setInputShader("u_noise", noiseShader)
                        
                        // "Sky" Tone
                        runtimeShader.setFloatUniform("u_main", 0.7f, 0.85f, 1.0f)
                        runtimeShader.setFloatUniform("u_low", 0.4f, 0.6f, 0.9f)
                        runtimeShader.setFloatUniform("u_mid", 0.5f, 0.7f, 1.0f)
                        runtimeShader.setFloatUniform("u_high", 0.9f, 0.95f, 1.0f)
                        
                        renderEffect = android.graphics.RenderEffect
                            .createRuntimeShaderEffect(runtimeShader, "content")
                            .asComposeRenderEffect()
                    }
            ) {
                drawRect(color = Color.Black)
            }
            
            Spacer(modifier = Modifier.height(64.dp))
            
            Box(
                modifier = Modifier
                    .background(Color(0x1AFFFFFF), RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    if (isListening) "Listening..." else "Tap the orb to talk",
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Nothing is recorded. Your voice only moves the light.",
                color = Color(0xFF606060),
                fontSize = 12.sp
            )
        }
    }
}
