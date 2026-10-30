package com.embedhub.app.ui.screens.control

import android.content.Context
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.embedhub.app.core.manager.ConnectionManager
import com.embedhub.app.core.model.ControlProfile
import com.embedhub.app.core.model.CustomControlButton
import com.embedhub.app.core.model.ScreenOrientationMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.*

@Composable
fun ControlScreen(
    connectionManager: ConnectionManager,
    selectedSlot: Int,
    profile: ControlProfile = ControlProfile(),
    orientationMode: ScreenOrientationMode = ScreenOrientationMode.AUTO_SENSOR,
    onToggleOrientation: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var speed by remember { mutableStateOf(5f) }
    var tiltSteeringEnabled by remember { mutableStateOf(false) }
    var activeDirection by remember { mutableStateOf<String?>(null) }
    var customButtons by remember { mutableStateOf(profile.customButtons) }
    var leftControlMode by remember { mutableStateOf("DPAD") } // "DPAD" or "JOYSTICK"

    // Sensor roll & pitch for artificial horizon
    var currentRoll by remember { mutableStateOf(0f) }
    var currentPitch by remember { mutableStateOf(0f) }

    // Analog Joystick state (-100..100)
    var joystickX by remember { mutableStateOf(0) }
    var joystickY by remember { mutableStateOf(0) }
    var isDraggingJoystick by remember { mutableStateOf(false) }

    // 20Hz Joystick Loop
    LaunchedEffect(isDraggingJoystick, joystickX, joystickY, selectedSlot) {
        if (isDraggingJoystick) {
            val intervalMs = (1000L / profile.joystickRateHz).coerceAtLeast(10L)
            while (isActive && isDraggingJoystick) {
                val formatted = profile.joystickFormat
                    .replace("{x}", joystickX.toString())
                    .replace("{y}", joystickY.toString())
                connectionManager.sendCommand(selectedSlot, formatted)
                delay(intervalMs)
            }
        }
    }

    // Accelerometer Sensor for Tilt Steering & Artificial Horizon
    DisposableEffect(tiltSteeringEnabled, isLandscape) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                val roll = event.values[0]
                val pitch = event.values[1]
                currentRoll = roll
                currentPitch = pitch

                if (tiltSteeringEnabled) {
                    val targetX = if (isLandscape) (-pitch * 15).coerceIn(-100f, 100f).toInt() else (-roll * 15).coerceIn(-100f, 100f).toInt()
                    val targetY = if (isLandscape) (-roll * 15).coerceIn(-100f, 100f).toInt() else (pitch * 15).coerceIn(-100f, 100f).toInt()
                    joystickX = targetX
                    joystickY = targetY
                    isDraggingJoystick = true
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager?.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        onDispose {
            sensorManager?.unregisterListener(listener)
            if (tiltSteeringEnabled) {
                isDraggingJoystick = false
                coroutineScope.launch {
                    connectionManager.sendImmediateStop(selectedSlot, profile.stopCmd)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        if (isLandscape) {
            // WIDESCREEN LANDSCAPE DRIVING COCKPIT
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LEFT THUMB: D-Pad or Joystick with quick toggle
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        modifier = Modifier.padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChip(
                            selected = leftControlMode == "DPAD",
                            onClick = { leftControlMode = "DPAD" },
                            label = { Text("D-Pad", fontSize = 10.sp) }
                        )
                        FilterChip(
                            selected = leftControlMode == "JOYSTICK",
                            onClick = { leftControlMode = "JOYSTICK" },
                            label = { Text("20Hz Joy", fontSize = 10.sp) }
                        )
                    }

                    if (leftControlMode == "DPAD") {
                        CompactDPad(
                            profile = profile,
                            activeDirection = activeDirection,
                            onDirectionChange = { activeDirection = it },
                            onSend = { cmd -> coroutineScope.launch { connectionManager.sendCommand(selectedSlot, cmd) } }
                        )
                    } else {
                        AnalogJoystickView(
                            sizeDp = 150,
                            onValuesChanged = { x, y, isDragging ->
                                joystickX = x
                                joystickY = y
                                isDraggingJoystick = isDragging
                                if (!isDragging) {
                                    coroutineScope.launch { connectionManager.sendImmediateStop(selectedSlot, profile.stopCmd) }
                                }
                            }
                        )
                    }
                }

                // CENTER: Head-Up Display (HUD) with Horizon, Speed & Emergency Stop
                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight()
                        .padding(horizontal = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Orientation & Mode Badge Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1E293B)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Speed, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
                                Text("PWM ${speed.toInt()}", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                            }
                        }

                        // Screen rotation toggle button
                        if (onToggleOrientation != null) {
                            OutlinedButton(
                                onClick = onToggleOrientation,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.ScreenRotation, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF38BDF8))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    when (orientationMode) {
                                        ScreenOrientationMode.AUTO_SENSOR -> "Auto"
                                        ScreenOrientationMode.LOCKED_LANDSCAPE -> "Lock Land"
                                        ScreenOrientationMode.LOCKED_PORTRAIT -> "Lock Port"
                                        else -> "Orient"
                                    },
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    // Artificial Horizon Canvas
                    ArtificialHorizonView(
                        roll = currentRoll,
                        pitch = currentPitch,
                        modifier = Modifier.size(width = 160.dp, height = 75.dp)
                    )

                    // Prominent Emergency STOP
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                connectionManager.sendEmergencyStopAll(profile.stopCmd)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.StopCircle, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("EMERGENCY STOP", fontWeight = FontWeight.Black, fontSize = 14.sp)
                    }

                    // Tilt steering switch
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Gyro Tilt", fontSize = 10.sp, color = Color.LightGray)
                        Switch(
                            checked = tiltSteeringEnabled,
                            onCheckedChange = { tiltSteeringEnabled = it },
                            modifier = Modifier.scale(0.6f)
                        )
                    }
                }

                // RIGHT THUMB: Speed Slider & Actuator Buttons
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Speed slider
                    Slider(
                        value = speed,
                        onValueChange = {
                            speed = it
                            if (profile.continuousSpeed) {
                                coroutineScope.launch { connectionManager.sendCommand(selectedSlot, "V${speed.toInt()}") }
                            }
                        },
                        onValueChangeFinished = {
                            if (!profile.continuousSpeed) {
                                coroutineScope.launch { connectionManager.sendCommand(selectedSlot, "V${speed.toInt()}") }
                            }
                        },
                        valueRange = 0f..9f,
                        steps = 8,
                        modifier = Modifier.fillMaxWidth(0.9f)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // 4 Actuator Buttons Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(0.9f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { coroutineScope.launch { connectionManager.sendCommand(selectedSlot, "H") } },
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Horn", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        FilledIconToggleButton(
                            checked = customButtons.getOrNull(1)?.isToggledOn ?: false,
                            onCheckedChange = { checked ->
                                val updated = customButtons.toMutableList()
                                if (updated.size > 1) {
                                    updated[1] = updated[1].copy(isToggledOn = checked)
                                    customButtons = updated
                                }
                                coroutineScope.launch {
                                    connectionManager.sendCommand(selectedSlot, if (checked) "W" else "w")
                                }
                            },
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Lights", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(0.9f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilledIconToggleButton(
                            checked = customButtons.getOrNull(2)?.isToggledOn ?: false,
                            onCheckedChange = { checked ->
                                val updated = customButtons.toMutableList()
                                if (updated.size > 2) {
                                    updated[2] = updated[2].copy(isToggledOn = checked)
                                    customButtons = updated
                                }
                                coroutineScope.launch {
                                    connectionManager.sendCommand(selectedSlot, if (checked) "A1" else "A0")
                                }
                            },
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Relay", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { coroutineScope.launch { connectionManager.sendCommand(selectedSlot, "SERVO:90") } },
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Center", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // PORTRAIT LAYOUT
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Toolbar: Speed slider, Orientation Toggle & Emergency Stop
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Speed: ${speed.toInt()}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Tilt", fontSize = 11.sp, color = Color.LightGray)
                                Switch(
                                    checked = tiltSteeringEnabled,
                                    onCheckedChange = { tiltSteeringEnabled = it },
                                    modifier = Modifier.scale(0.7f)
                                )
                                if (onToggleOrientation != null) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(onClick = onToggleOrientation, modifier = Modifier.size(32.dp)) {
                                        Icon(
                                            Icons.Default.ScreenRotation,
                                            contentDescription = "Rotate Screen",
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                        Slider(
                            value = speed,
                            onValueChange = {
                                speed = it
                                if (profile.continuousSpeed) {
                                    coroutineScope.launch { connectionManager.sendCommand(selectedSlot, "V${speed.toInt()}") }
                                }
                            },
                            onValueChangeFinished = {
                                if (!profile.continuousSpeed) {
                                    coroutineScope.launch { connectionManager.sendCommand(selectedSlot, "V${speed.toInt()}") }
                                }
                            },
                            valueRange = 0f..9f,
                            steps = 8
                        )
                    }

                    // Always Visible Emergency STOP
                    Button(
                        onClick = {
                            coroutineScope.launch { connectionManager.sendEmergencyStopAll(profile.stopCmd) }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Icon(Icons.Default.StopCircle, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("STOP", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Center Area: Hold-to-move D-Pad & Analog Joystick
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // D-Pad
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("D-Pad (Hold)", color = Color.Gray, fontSize = 11.sp)
                        CompactDPad(
                            profile = profile,
                            activeDirection = activeDirection,
                            onDirectionChange = { activeDirection = it },
                            onSend = { cmd -> coroutineScope.launch { connectionManager.sendCommand(selectedSlot, cmd) } }
                        )
                    }

                    // Analog Joystick
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Analog Joystick (20Hz)", color = Color.Gray, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        AnalogJoystickView(
                            sizeDp = 180,
                            onValuesChanged = { x, y, isDragging ->
                                joystickX = x
                                joystickY = y
                                isDraggingJoystick = isDragging
                                if (!isDragging) {
                                    coroutineScope.launch { connectionManager.sendImmediateStop(selectedSlot, profile.stopCmd) }
                                }
                            }
                        )
                        Text(
                            text = "X: $joystickX | Y: $joystickY",
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Custom Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    customButtons.forEachIndexed { index, btn ->
                        if (btn.isToggle) {
                            FilledIconToggleButton(
                                checked = btn.isToggledOn,
                                onCheckedChange = { checked ->
                                    val updated = customButtons.toMutableList()
                                    updated[index] = btn.copy(isToggledOn = checked)
                                    customButtons = updated
                                    coroutineScope.launch {
                                        val cmd = if (checked) btn.pressCmd else btn.releaseCmd
                                        connectionManager.sendCommand(selectedSlot, cmd)
                                    }
                                },
                                modifier = Modifier.weight(1f).height(44.dp)
                            ) {
                                Text(btn.label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = {
                                    coroutineScope.launch { connectionManager.sendCommand(selectedSlot, btn.pressCmd) }
                                },
                                modifier = Modifier.weight(1f).height(44.dp)
                            ) {
                                Text(btn.label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArtificialHorizonView(
    roll: Float,
    pitch: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E293B)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val pitchOffset = (pitch * 2.5f).coerceIn(-size.height / 3f, size.height / 3f)
            val angleRad = Math.toRadians((roll * 4.0).coerceIn(-45.0, 45.0))

            val halfW = size.width * 0.35f
            val dx = (halfW * cos(angleRad)).toFloat()
            val dy = (halfW * sin(angleRad)).toFloat()

            // Artificial horizon line
            drawLine(
                color = Color(0xFF38BDF8),
                start = Offset(center.x - dx, center.y + pitchOffset - dy),
                end = Offset(center.x + dx, center.y + pitchOffset + dy),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Center aircraft reticle
            drawCircle(
                color = Color(0xFFF59E0B),
                radius = 4.dp.toPx(),
                center = center
            )
        }

        Text(
            text = "ROLL: %.0f°".format(roll * 5f),
            color = Color(0xFF94A3B8),
            fontSize = 9.sp,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp)
        )
    }
}

@Composable
fun CompactDPad(
    profile: ControlProfile,
    activeDirection: String?,
    onDirectionChange: (String?) -> Unit,
    onSend: (String) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        DPadButton("▲", profile.forwardCmd, profile.stopCmd, onDirectionChange, onSend, size = 48)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            DPadButton("◀", profile.leftCmd, profile.stopCmd, onDirectionChange, onSend, size = 48)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF334155)),
                contentAlignment = Alignment.Center
            ) {
                Text(activeDirection ?: "●", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            DPadButton("▶", profile.rightCmd, profile.stopCmd, onDirectionChange, onSend, size = 48)
        }
        DPadButton("▼", profile.backwardCmd, profile.stopCmd, onDirectionChange, onSend, size = 48)
    }
}

@Composable
fun DPadButton(
    label: String,
    command: String,
    stopCommand: String,
    onDirectionChange: (String?) -> Unit,
    onSend: (String) -> Unit,
    size: Int = 60
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E293B))
            .pointerInput(command) {
                detectTapGestures(
                    onPress = {
                        onDirectionChange(command)
                        onSend(command)
                        val released = tryAwaitRelease()
                        if (released) {
                            onDirectionChange(null)
                            onSend(stopCommand)
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AnalogJoystickView(
    sizeDp: Int = 180,
    onValuesChanged: (x: Int, y: Int, isDragging: Boolean) -> Unit
) {
    var thumbOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(Color(0xFF1E293B))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val delta = offset - center
                        val maxDist = size.width / 2f
                        val dist = delta.getDistance().coerceAtMost(maxDist)
                        val angle = atan2(delta.y, delta.x)
                        thumbOffset = Offset(cos(angle) * dist, sin(angle) * dist)

                        val xVal = ((thumbOffset.x / maxDist) * 100).toInt()
                        val yVal = ((-thumbOffset.y / maxDist) * 100).toInt()
                        onValuesChanged(xVal, yVal, true)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val maxDist = size.width / 2f
                        val newOffset = thumbOffset + dragAmount
                        val dist = newOffset.getDistance().coerceAtMost(maxDist)
                        val angle = atan2(newOffset.y, newOffset.x)
                        thumbOffset = Offset(cos(angle) * dist, sin(angle) * dist)

                        val xVal = ((thumbOffset.x / maxDist) * 100).toInt()
                        val yVal = ((-thumbOffset.y / maxDist) * 100).toInt()
                        onValuesChanged(xVal, yVal, true)
                    },
                    onDragEnd = {
                        thumbOffset = Offset.Zero
                        onValuesChanged(0, 0, false)
                    },
                    onDragCancel = {
                        thumbOffset = Offset.Zero
                        onValuesChanged(0, 0, false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(Color(0xFF334155), radius = size.width / 2f)
            drawCircle(Color(0xFF0284C7), radius = (sizeDp * 0.16f).dp.toPx(), center = center + thumbOffset)
        }
    }
}
