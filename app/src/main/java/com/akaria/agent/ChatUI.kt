package com.akaria.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.akaria.agent.glass.GlassBox
import com.akaria.agent.glass.GlassContainer
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AkariaFloatingUI() {
    var expanded by remember { mutableStateOf(false) }
    val latestFrame by ScreenCaptureService.screenFrames.collectAsState()

    GlassContainer(
        modifier = Modifier.fillMaxSize(),
        content = {
            // Live background for true liquid glass refraction
            latestFrame?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Background",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    ) {
        if (expanded) {
            // Expanded Chat Interface
            GlassBox(
                modifier = Modifier
                    .width(320.dp)
                    .height(400.dp)
                    .padding(8.dp),
                shape = RoundedCornerShape(24.dp),
                blur = 0.4f,
                tint = Color(0x88121212),
                darkness = 0.2f,
                scale = 0.1f,
                elevation = 10.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Akaria",
                            color = Color(0xFFBB86FC),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "Close",
                            color = Color.LightGray,
                            modifier = Modifier.clickable { expanded = false }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Chat Area
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "I am ready. Tell me what to do.",
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Input Area
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = "",
                            onValueChange = {},
                            placeholder = { Text("Type a command...", color = Color.Gray) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFBB86FC),
                                unfocusedBorderColor = Color.DarkGray,
                                focusedTextColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { /* TODO: Send to Planner */ },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBB86FC))
                        ) {
                            Text("Go", color = Color.Black)
                        }
                    }
                }
            }
        } else {
            // Collapsed Floating Bubble
            GlassBox(
                modifier = Modifier
                    .size(60.dp)
                    .clickable { expanded = true },
                shape = CircleShape,
                blur = 0.3f,
                tint = Color(0xBBBB86FC),
                darkness = 0.1f,
                scale = 0.2f
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "A",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                }
            }
        }
    }
}
