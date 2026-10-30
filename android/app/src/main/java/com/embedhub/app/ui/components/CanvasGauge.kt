package com.embedhub.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CanvasGauge(
    value: Double,
    min: Double,
    max: Double,
    unit: String,
    title: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val clamped = value.coerceIn(min, max)
    val fraction = if (max > min) ((clamped - min) / (max - min)).toFloat() else 0f

    Box(
        modifier = modifier.size(160.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            val strokeWidth = 14.dp.toPx()
            val startAngle = 135f
            val sweepAngle = 270f
            val arcSize = Size(size.width, size.height)

            // Background track
            drawArc(
                color = Color(0xFF334155),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                size = arcSize
            )

            // Value arc
            drawArc(
                color = accentColor,
                startAngle = startAngle,
                sweepAngle = sweepAngle * fraction,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                size = arcSize
            )

            // Indicator needle point
            val currentAngleRad = Math.toRadians((startAngle + (sweepAngle * fraction)).toDouble())
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = (size.width / 2f) - (strokeWidth / 2f)
            val needleX = center.x + (radius * cos(currentAngleRad)).toFloat()
            val needleY = center.y + (radius * sin(currentAngleRad)).toFloat()

            drawCircle(
                color = Color.White,
                radius = 4.dp.toPx(),
                center = Offset(needleX, needleY)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "%.1f".format(value),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )
            if (unit.isNotEmpty()) {
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                fontSize = 11.sp
            )
        }
    }
}
