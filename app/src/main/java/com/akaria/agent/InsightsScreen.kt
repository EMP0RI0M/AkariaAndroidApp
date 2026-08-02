package com.akaria.agent

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val WeekMoodSets = listOf(
    listOf(3, 4, 2, 3, 5, 6, 5),
    listOf(2, 3, 3, 4, 4, 5, 3)
)

val WeekTakeaways = listOf(
    "Your week lifted toward the weekend" to "Saturday was your brightest day",
    "A steady, gentle week overall" to "Friday was your brightest day"
)

val WeekDayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
val MoodLabels = listOf("Very Unpleasant", "Unpleasant", "Slightly Unpleasant", "Neutral", "Slightly Pleasant", "Pleasant", "Very Pleasant")

@Composable
fun InsightsScreen(playKey: Int = 0, onNext: () -> Unit = {}) {
    var sel by remember { mutableIntStateOf(5) }
    var week by remember { mutableIntStateOf(0) }
    
    val moods = WeekMoodSets[week]
    val avg = moods.sum() / 7
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAF1EC))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x4DFF9AB2), Color.Transparent),
                    center = Offset(size.width * 0.16f, size.height * 0.14f),
                    radius = size.width * 0.36f
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x6BFFC7BE), Color.Transparent),
                    center = Offset(size.width * 0.86f, size.height * 0.34f),
                    radius = size.width * 0.38f
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x29E5637F), Color.Transparent),
                    center = Offset(size.width * 0.42f, size.height * 0.92f),
                    radius = size.width * 0.44f
                )
            )
        }
        
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(96.dp))
            
            Text(
                text = if (week == 0) "a brighter week." else "a steadier week.",
                color = Color(0xFF79404E),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(7.dp))
            
            Text(
                text = "${MoodLabels[avg]} on average",
                color = Color(0xAD79404E),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(14.dp))
            
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0x1779404E))
                    .padding(3.dp)
            ) {
                Row {
                    listOf("This week", "Last week").forEachIndexed { index, label ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (week == index) Color(0xEBFFFFFF) else Color.Transparent)
                                .clickable { week = index }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (week == index) Color(0xFF79404E) else Color(0x9979404E)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .blur(16.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0x8CFFFFFF), Color(0x4DFFFFFF))
                        )
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "${WeekDayNames[sel]} · ${MoodLabels[moods[sel]]}",
                        color = Color(0xD179404E),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    
                    Spacer(modifier = Modifier.height(80.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        "MTWTFSS".forEachIndexed { index, char ->
                            Text(
                                text = char.toString(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (sel == index) Color(0xFFC2405C) else Color(0x8079404E),
                                modifier = Modifier
                                    .clickable { sel = index }
                                    .padding(4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
