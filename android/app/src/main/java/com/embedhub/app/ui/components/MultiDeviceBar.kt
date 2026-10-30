package com.embedhub.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.embedhub.app.core.model.ConnectionState
import com.embedhub.app.core.model.DeviceSlot

import com.embedhub.app.core.model.ScreenOrientationMode

@Composable
fun MultiDeviceBar(
    slots: List<DeviceSlot>,
    selectedSlotIndex: Int,
    onSelectSlot: (Int) -> Unit,
    orientationMode: ScreenOrientationMode = ScreenOrientationMode.AUTO_SENSOR,
    onToggleOrientation: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            slots.forEach { slot ->
                val isSelected = slot.slotIndex == selectedSlotIndex
                val slotColor = try {
                    Color(android.graphics.Color.parseColor(slot.colorHex))
                } catch (_: Exception) {
                    MaterialTheme.colorScheme.primary
                }

                val isConnected = slot.state is ConnectionState.Connected
                val isConnecting = slot.state is ConnectionState.Connecting || slot.state is ConnectionState.Reconnecting

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (isSelected) Modifier.border(1.5.dp, slotColor, RoundedCornerShape(8.dp)) else Modifier
                        )
                        .clickable { onSelectSlot(slot.slotIndex) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isConnected -> Color(0xFF10B981)
                                        isConnecting -> Color(0xFFF59E0B)
                                        else -> Color(0xFF64748B)
                                    }
                                )
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Slot ${slot.slotIndex + 1}: ${slot.name}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                fontSize = 11.sp
                            )
                            Text(
                                text = when (slot.state) {
                                    is ConnectionState.Connected -> "Connected"
                                    is ConnectionState.Connecting -> "Connecting..."
                                    is ConnectionState.Reconnecting -> "Reconnecting..."
                                    is ConnectionState.Error -> "Error"
                                    else -> "Disconnected"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = when (slot.state) {
                                    is ConnectionState.Connected -> Color(0xFF10B981)
                                    is ConnectionState.Error -> Color(0xFFF43F5E)
                                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                },
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }

            if (onToggleOrientation != null) {
                IconButton(
                    onClick = onToggleOrientation,
                    modifier = Modifier.size(36.dp)
                ) {
                    val icon = when (orientationMode) {
                        ScreenOrientationMode.AUTO_SENSOR -> Icons.Default.ScreenRotation
                        ScreenOrientationMode.LOCKED_PORTRAIT -> Icons.Default.ScreenLockPortrait
                        ScreenOrientationMode.LOCKED_LANDSCAPE, ScreenOrientationMode.REVERSE_LANDSCAPE -> Icons.Default.ScreenLockLandscape
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = "Toggle Screen Orientation (${orientationMode.name})",
                        tint = when (orientationMode) {
                            ScreenOrientationMode.AUTO_SENSOR -> Color(0xFF38BDF8)
                            ScreenOrientationMode.LOCKED_PORTRAIT -> Color(0xFF94A3B8)
                            ScreenOrientationMode.LOCKED_LANDSCAPE, ScreenOrientationMode.REVERSE_LANDSCAPE -> Color(0xFF10B981)
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
