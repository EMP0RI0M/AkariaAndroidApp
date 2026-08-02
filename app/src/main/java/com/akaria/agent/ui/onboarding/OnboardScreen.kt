package com.akaria.agent.ui.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class OnboardingPage(val title: String, val body: String)

val PAGES = listOf(
    OnboardingPage("Private AI", "Running completely on your device.\nNo cloud required."),
    OnboardingPage("Vision", "Understand your screen.\nUnderstand images.\nUnderstand documents."),
    OnboardingPage("Voice", "Natural conversations.\nOffline speech.\nFast response."),
    OnboardingPage("Automation", "Control your phone.\nApps. Files. Settings.\nMessages. Calendar."),
    OnboardingPage("Ready", "Everything is configured.\nLet's begin.")
)

@Composable
fun OnboardScreen(onNext: () -> Unit) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val screenWidthPx = with(LocalDensity.current) { screenWidth.toPx() }
    
    val maxPage = PAGES.size - 1
    val dragX = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    
    // Page state (0 to 4)
    val p = (-dragX.value / screenWidthPx).coerceIn(0f, maxPage.toFloat())
    val currentPage = p.toInt()

    // Springs mapped from React: { type: 'spring', duration: 0.75, bounce: 0.16 }
    val springSpec = spring<Float>(
        dampingRatio = 0.84f, // 1 - 0.16
        stiffness = Spring.StiffnessLow
    )

    // Theming: Dynamic dark theme / Material 3
    val bgGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0A0A0A),
            Color(0xFF141414),
            Color(0xFF1E1E1E)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta ->
                    coroutineScope.launch {
                        dragX.snapTo((dragX.value + delta).coerceIn(-maxPage * screenWidthPx, 0f))
                    }
                },
                onDragStopped = { velocity ->
                    val projected = -(dragX.value + velocity * 0.24f) / screenWidthPx
                    val targetPage = Math.round(projected).coerceIn(0, maxPage)
                    coroutineScope.launch {
                        dragX.animateTo(-targetPage * screenWidthPx, springSpec)
                    }
                }
            )
    ) {
        // AI Core (replaces orb)
        AICore(p)

        // Neural Landscape / Grid (replaces mountains)
        NeuralLandscape(p)

        // Particles (replaces clouds/birds)
        FloatingParticles(p)

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = PAGES[currentPage].title,
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = PAGES[currentPage].body,
                color = Color(0xFFA0A0A0),
                fontSize = 16.sp,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Progress Arc / Scrubber
            ArcPager(p, maxPage)

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (currentPage == maxPage) {
                        onNext()
                    } else {
                        coroutineScope.launch {
                            dragX.animateTo(-(currentPage + 1) * screenWidthPx, springSpec)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFBB86FC),
                    contentColor = Color(0xFF141414)
                )
            ) {
                Text(if (currentPage == maxPage) "Get started" else "Next", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun AICore(p: Float) {
    val infiniteTransition = rememberInfiniteTransition()
    val breathe by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Core rotates and shifts hue slightly based on progress (p)
    val colorPrimary = Color(0xFFBB86FC)
    val colorSecondary = Color(0xFF78D890)
    
    // Mix colors based on phase (p)
    val mixColor = androidx.compose.ui.graphics.lerp(colorPrimary, colorSecondary, p / 4f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 100.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        // Halo
        Box(
            modifier = Modifier
                .size(180.dp)
                .scale(breathe)
                .blur(24.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(mixColor.copy(alpha = 0.4f), Color.Transparent)
                    ),
                    shape = CircleShape
                )
        )

        // Glass Sphere Body
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(breathe)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.1f),
                            mixColor.copy(alpha = 0.3f),
                            Color(0xFF141414).copy(alpha = 0.8f)
                        )
                    )
                )
        ) {
            // Specular highlight
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.4f),
                                Color.Transparent
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(120f, 120f)
                        )
                    )
            )
        }
    }
}

@Composable
fun NeuralLandscape(p: Float) {
    // Abstract grid / neural nodes at the bottom background
    Canvas(modifier = Modifier.fillMaxSize()) {
        // A minimal, tech-like representation replacing the nature mountains
        // ... (can be expanded later)
    }
}

@Composable
fun FloatingParticles(p: Float) {
    // Data packets / floating nodes replacing birds/clouds
    // ...
}

@Composable
fun ArcPager(p: Float, maxPage: Int) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
    ) {
        val w = size.width
        val h = size.height

        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(24f, h - 10f)
            quadraticBezierTo(w / 2, -h + 20f, w - 24f, h - 10f)
        }
        
        drawPath(
            path = path,
            color = Color(0xFF505050),
            style = Stroke(width = 4f)
        )

        // Compute dot position along the arc (approximate bezier calculation for scrubber)
        val t = p / maxPage
        val dotX = 24f * (1 - t) * (1 - t) + 2 * (w / 2) * (1 - t) * t + (w - 24f) * t * t
        val dotY = (h - 10f) * (1 - t) * (1 - t) + 2 * (-h + 20f) * (1 - t) * t + (h - 10f) * t * t

        drawCircle(
            color = Color(0xFFBB86FC),
            radius = 16f,
            center = Offset(dotX, dotY)
        )
        drawCircle(
            color = Color.White,
            radius = 8f,
            center = Offset(dotX, dotY)
        )
    }
}
