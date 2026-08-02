package com.akaria.agent.ui

import com.akaria.agent.engine.backend.EngineViewModel
import com.akaria.agent.engine.CoreState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.akaria.agent.glass.GlassBox
import com.akaria.agent.glass.GlassContainer
import androidx.lifecycle.viewmodel.compose.viewModel

enum class Screen {
    WELCOME, HARDWARE_CHECK, SETUP_COMPLETE, HOME, INFERENCE_TEST
}

@Composable
fun AkariaApp(engineViewModel: EngineViewModel = viewModel()) {
    var currentScreen by remember { mutableStateOf(Screen.WELCOME) }
    
    // Automatically check for models when app starts
    LaunchedEffect(Unit) {
        engineViewModel.checkModels()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0A0A0A) // Deep AMOLED Black
    ) {
        GlassContainer(modifier = Modifier.fillMaxSize(), content = {}) {
            AnimatedContent(
                targetState = currentScreen,
            transitionSpec = {
                fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
            },
            label = "ScreenTransition"
        ) { targetScreen ->
            when (targetScreen) {
                Screen.WELCOME -> com.akaria.agent.ui.onboarding.OnboardScreen(
                    onNext = { currentScreen = Screen.HARDWARE_CHECK }
                )
                Screen.HARDWARE_CHECK -> HardwareCheckScreen(
                    engineViewModel = engineViewModel,
                    onNext = { currentScreen = Screen.SETUP_COMPLETE }
                )
                Screen.SETUP_COMPLETE -> SetupCompleteScreen(
                    onNext = { currentScreen = Screen.HOME }
                )
                Screen.HOME -> HomeScreen(
                    onTestModel = { currentScreen = Screen.INFERENCE_TEST }
                )
                Screen.INFERENCE_TEST -> InferenceTestScreen(
                    engineViewModel = engineViewModel,
                    onBack = { currentScreen = Screen.HOME }
                )
            }
        }
        }
    }
}

@Composable
fun GlassBoxScope.WelcomeScreen(onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Section: The Breathing Orb
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            GlassBox(
                modifier = Modifier.size(120.dp),
                shape = CircleShape,
                blur = 0.5f,
                tint = Color(0x66BB86FC),
                darkness = 0.1f,
                scale = 0.2f,
                elevation = 12.dp
            ) {}
        }

        // Bottom Section: Typography
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Akaria",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Private AI. Running entirely on your device.",
                color = Color(0xFFA0A0A0),
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            val features = listOf(
                "Local AI",
                "Private",
                "Works Offline",
                "Phone Automation",
                "Vision & Voice"
            )

            features.forEach { feature ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("✔", color = Color(0xFF78D890), fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(feature, color = Color.White, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFBB86FC),
                    contentColor = Color(0xFF141414)
                )
            ) {
                Text("Get Started", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun GlassBoxScope.HardwareCheckScreen(engineViewModel: EngineViewModel, onNext: () -> Unit) {
    val coreState by engineViewModel.coreState.collectAsState()
    val downloadProgress by engineViewModel.downloadProgress.collectAsState()
    val telemetry by engineViewModel.telemetry.collectAsState()

    LaunchedEffect(Unit) {
        delay(2000) // Simulate diagnostics
        isChecking = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
    ) {
        if (isChecking) {
            Text(
                text = "Scanning Hardware...",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator(color = Color(0xFFBB86FC))
        } else {
            Text(
                text = "Device Profile",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            GlassBox(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                blur = 0.3f,
                tint = Color(0x33FFFFFF),
                darkness = 0.2f,
                elevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("CPU: Snapdragon / Tensor Equivalent", color = Color(0xFFA0A0A0), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("RAM: 12GB (Available)", color = Color(0xFFA0A0A0), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("NPU: Detected", color = Color(0xFFA0A0A0), fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Akaria Engine",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            GlassBox(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                blur = 0.3f,
                tint = Color(0x33FFFFFF),
                darkness = 0.2f,
                elevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Model: Gemma 4B Q4 (Recommended)", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Optimized for your hardware.", color = Color(0xFF78D890), fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            when {
                downloadProgress != null -> {
                    val progress = downloadProgress!!
                    Text("Downloading Model: ${(progress * 100).toInt()}%", color = Color.White)
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = Color(0xFFBB86FC)
                    )
                }
                coreState is CoreState.Ready || coreState is CoreState.WarmingUp -> {
                    Button(
                        onClick = onNext,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF78D890),
                            contentColor = Color(0xFF141414)
                        )
                    ) {
                        Text("System Ready - Continue", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
                else -> {
                    Button(
                        onClick = { engineViewModel.downloadTinyModel() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFBB86FC),
                            contentColor = Color(0xFF141414)
                        )
                    ) {
                        Text("Download Model", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun GlassBoxScope.SetupCompleteScreen(onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Everything Ready.",
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(32.dp))
        
        val checks = listOf("Backend", "Model", "Memory", "Automation", "Vision")
        checks.forEach { check ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("✓", color = Color(0xFF78D890), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(16.dp))
                Text(check, color = Color.White, fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFBB86FC),
                contentColor = Color(0xFF141414)
            )
        ) {
            Text("Enter Akaria", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun GlassBoxScope.HomeScreen(onTestModel: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .padding(top = 24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Akaria",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )
            // Settings Icon Placeholder
            Text("⚙", color = Color(0xFFA0A0A0), fontSize = 24.sp)
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        Text("System Status", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        GlassBox(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            blur = 0.3f,
            tint = Color(0x33FFFFFF),
            darkness = 0.2f,
            elevation = 12.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                StatusRow("Engine", coreState.javaClass.simpleName, if (coreState is CoreState.Ready) Color(0xFF78D890) else Color.White)
                ItemRow("Android Storage", "${telemetry.freeStorageMb} MB Free", telemetry.freeStorageMb > 1000)
                ItemRow("Memory (RAM)", "${telemetry.usedRamMb}/${telemetry.maxRamMb} MB Used", telemetry.usedRamMb < telemetry.maxRamMb * 0.9)
                ItemRow("Battery Level", "${telemetry.batteryLevel}%", telemetry.batteryLevel > 15)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        Text("Modules", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ModuleCard(title = "\uD83D\uDCF1 Automation", subtitle = "ON", modifier = Modifier.weight(1f))
            ModuleCard(title = "\uD83D\uDC41\uFE0F Vision", subtitle = "ON", modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ModuleCard(title = "\uD83E\uDDE0 Memory", subtitle = "204 Nodes", modifier = Modifier.weight(1f))
            ModuleCard(title = "\uD83D\uDCC8 Telemetry", subtitle = "14 tok/s", modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onTestModel,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(32.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFBB86FC),
                contentColor = Color(0xFF141414)
            )
        ) {
            Text("Test Model Inference", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun StatusRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color(0xFFA0A0A0), fontSize = 16.sp)
        Text(value, color = valueColor, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun GlassBoxScope.ModuleCard(title: String, subtitle: String, modifier: Modifier = Modifier) {
    GlassBox(
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(20.dp),
        blur = 0.3f,
        tint = Color(0x33FFFFFF),
        darkness = 0.2f,
        elevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, color = Color(0xFF78D890), fontSize = 14.sp)
        }
    }
}

@Composable
fun GlassBoxScope.InferenceTestScreen(engineViewModel: EngineViewModel, onBack: () -> Unit) {
    val coreState by engineViewModel.coreState.collectAsState()
    val inferenceResult by engineViewModel.inferenceResult.collectAsState()
    var prompt by remember { mutableStateOf("Hello!") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .padding(top = 24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text("Engine Diagnostics", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Enter prompt", color = Color(0xFFA0A0A0)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFFBB86FC),
                unfocusedBorderColor = Color(0xFF505050)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { engineViewModel.runInference(prompt) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = coreState is CoreState.Ready,
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFBB86FC),
                contentColor = Color(0xFF141414),
                disabledContainerColor = Color(0xFF505050)
            )
        ) {
            if (coreState is CoreState.Inferencing) {
                CircularProgressIndicator(color = Color(0xFF141414), modifier = Modifier.size(24.dp))
            } else {
                Text("Generate Response", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text("Output:", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        GlassBox(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(16.dp),
            blur = 0.3f,
            tint = Color(0x33FFFFFF),
            darkness = 0.2f,
            elevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                if (inferenceResult != null) {
                    if (inferenceResult!!.startsWith("Error")) {
                        Text(
                            text = inferenceResult!!,
                            color = Color(0xFFF28B82),
                            fontSize = 16.sp
                        )
                    } else {
                        Text(
                            text = inferenceResult!!,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                } else if (coreState is CoreState.Inferencing) {
                    Text("Generating...", color = Color(0xFF78D890), fontSize = 16.sp)
                } else {
                    Text("Waiting for input...", color = Color(0xFFA0A0A0), fontSize = 16.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to Hub", color = Color(0xFFA0A0A0))
        }
    }
}
