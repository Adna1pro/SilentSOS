package com.example.silentsos.ui

import android.os.CountDownTimer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 Hackathon UI Layer
 - Home screen (status + arm/disarm)
 - Distress screen (auto SOS countdown + cancel)
*/

@Composable
fun SilentSOSApp(distressTriggered: Boolean, onCancel: () -> Unit, onArmToggle: (Boolean) -> Unit) {
    MaterialTheme {
        if (distressTriggered) {
            DistressScreen(onCancel)
        } else {
            HomeScreen(onArmToggle)
        }
    }
}

@Composable
fun HomeScreen(onArmToggle: (Boolean) -> Unit) {
    var armed by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F14)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (armed) "Protection Active" else "Protection Off",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = {
                armed = !armed
                onArmToggle(armed)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (armed) Color(0xFF2ECC71) else Color(0xFFE74C3C)
            ),
            modifier = Modifier
                .size(180.dp),
            shape = CircleShape
        ) {
            Text(if (armed) "ARMED" else "ARM", fontSize = 20.sp)
        }

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = "Sensors running in background",
            color = Color.Gray,
            fontSize = 14.sp
        )
    }
}

@Composable
fun DistressScreen(onCancel: () -> Unit) {
    var seconds by remember { mutableStateOf(5) }
    var sent by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "scale"
    )

    LaunchedEffect(Unit) {
        object : CountDownTimer(5000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                seconds = (millisUntilFinished / 1000).toInt() + 1
            }

            override fun onFinish() {
                sent = true
            }
        }.start()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1B0000)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(220.dp)
                .scale(scale)
                .background(Color.Red, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("SOS", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(30.dp))

        AnimatedVisibility(!sent) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Sending alert in $seconds", color = Color.White, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = onCancel, colors = ButtonDefaults.buttonColors(containerColor = Color.White)) {
                    Text("I AM SAFE", color = Color.Black)
                }
            }
        }

        AnimatedVisibility(sent) {
            Text("Alert Sent", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}
