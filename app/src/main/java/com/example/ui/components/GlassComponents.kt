package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.model.FeatureAvailabilityItem
import com.example.ui.theme.LiveEmerald
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun SoniqGlassCard(
    modifier: Modifier = Modifier,
    highlightCrimson: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val surfaceColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
    val elevatedColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f)
    val borderBrush = if (highlightCrimson) {
        Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f),
                MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
            )
        )
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(listOf(elevatedColor, surfaceColor))
            )
            .border(width = 1.dp, brush = borderBrush, shape = RoundedCornerShape(24.dp)),
        color = Color.Transparent,
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            content = content
        )
    }
}

@Composable
fun LiveTelemetryBadge(
    label: String = "LIVE TELEMETRY",
    isActive: Boolean = true,
    modifier: Modifier = Modifier
) {
    val tint = if (isActive) LiveEmerald else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = modifier.testTag("badge_live_telemetry"),
        color = tint.copy(alpha = 0.14f),
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Verified,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = tint,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun UnavailableBadge(
    text: String = "UNAVAILABLE",
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = text,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
fun FeatureMatrixList(
    items: List<FeatureAvailabilityItem>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    1.dp,
                    if (item.isSupported) LiveEmerald.copy(alpha = 0.35f)
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (item.isSupported) Icons.Rounded.CheckCircle else Icons.Rounded.Lock,
                        contentDescription = if (item.isSupported) "Supported" else "Unavailable",
                        tint = if (item.isSupported) LiveEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.featureName,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = item.requirementNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!item.isSupported) {
                        Spacer(modifier = Modifier.width(8.dp))
                        UnavailableBadge()
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedBatteryRing(
    label: String,
    percent: Int?,
    isCharging: Boolean,
    modifier: Modifier = Modifier,
    ringSize: Dp = 78.dp
) {
    val targetFraction = (percent?.coerceIn(0, 100) ?: 0) / 100f
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 240f),
        label = "battery_sweep"
    )
    val arcColor by animateColorAsState(
        targetValue = when {
            percent == null -> MaterialTheme.colorScheme.outline
            percent <= 20 -> MaterialTheme.colorScheme.primary
            isCharging -> LiveEmerald
            else -> MaterialTheme.colorScheme.onSurface
        },
        label = "battery_color"
    )
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val crimsonAccent = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(ringSize),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 6.dp.toPx()
                val inset = strokeWidth / 2f
                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                drawArc(
                    color = trackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                if (percent != null && animatedFraction > 0f) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(crimsonAccent, arcColor, crimsonAccent)
                        ),
                        startAngle = -90f,
                        sweepAngle = 360f * animatedFraction,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isCharging && percent != null) {
                    Icon(
                        imageVector = Icons.Rounded.Bolt,
                        contentDescription = "Charging",
                        tint = LiveEmerald,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = percent?.let { "$it%" } ?: "N/A",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (percent != null) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun AcousticVisualizerCanvas(
    isActive: Boolean,
    eqBandsMb: List<Int>,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "acoustic_wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    val primary = MaterialTheme.colorScheme.primary
    val secondary = if (isActive) Color(0xFFFF2E55) else MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(modifier = modifier) {
        val barCount = 28
        val spacing = size.width / (barCount * 1.45f)
        val barWidth = (size.width - spacing * (barCount - 1)) / barCount
        val centerY = size.height / 2f

        for (i in 0 until barCount) {
            val norm = i.toFloat() / (barCount - 1).coerceAtLeast(1)
            val eqIdx = ((norm * (eqBandsMb.size - 1)).toInt()).coerceIn(0, (eqBandsMb.size - 1).coerceAtLeast(0))
            val eqBoost = ((eqBandsMb.getOrElse(eqIdx) { 0 } + 1500f) / 3000f).coerceIn(0.25f, 1f)
            val wave = if (isActive) {
                (0.45f + 0.55f * ((sin(phase + i * 0.45f) + 1f) / 2f)) * eqBoost
            } else {
                0.22f * eqBoost
            }
            val barH = (size.height * 0.86f * wave).coerceAtLeast(6.dp.toPx())
            val x = i * (barWidth + spacing)
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(primary, secondary)),
                topLeft = Offset(x, centerY - barH / 2f),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
                alpha = if (isActive) 0.92f else 0.45f
            )
        }
    }
}

@Composable
fun BluetoothRadarPulse(
    isScanning: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "radar")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_pulse"
    )
    val crimson = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier.size(64.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isScanning) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val maxR = size.minDimension / 2f
                drawCircle(
                    color = crimson.copy(alpha = (1f - pulse) * 0.5f),
                    radius = maxR * (0.35f + 0.65f * pulse),
                    style = Stroke(width = 2.dp.toPx())
                )
                val second = (pulse + 0.5f) % 1f
                drawCircle(
                    color = crimson.copy(alpha = (1f - second) * 0.35f),
                    radius = maxR * (0.35f + 0.65f * second),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(crimson.copy(alpha = 0.16f))
                .border(1.dp, crimson.copy(alpha = 0.55f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Bluetooth,
                contentDescription = "Bluetooth scanner",
                tint = crimson,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun PermissionRationaleBanner(
    title: String,
    rationale: String,
    buttonText: String,
    onGrantClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "grant_permission_button"
) {
    SoniqGlassCard(
        modifier = modifier.fillMaxWidth(),
        highlightCrimson = true
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Bluetooth,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = rationale,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onGrantClick,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(text = buttonText)
        }
    }
}
