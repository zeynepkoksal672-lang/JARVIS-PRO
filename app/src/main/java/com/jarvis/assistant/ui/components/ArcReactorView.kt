package com.jarvis.assistant.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jarvis.assistant.ui.theme.JarvisAmber
import com.jarvis.assistant.ui.theme.JarvisCyan
import com.jarvis.assistant.ui.theme.JarvisElectricGreen
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class ArcReactorState {
    IDLE,
    LISTENING,
    ANALYZING,
    SPEAKING
}

@Composable
fun ArcReactorView(
    state: ArcReactorState = ArcReactorState.IDLE,
    modifier: Modifier = Modifier,
    size: Dp = 160.dp,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "arc_anim")

    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outer_rot"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (state == ArcReactorState.LISTENING) 500 else 1500,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val primaryColor by animateColorAsState(
        targetValue = when (state) {
            ArcReactorState.IDLE -> JarvisCyan
            ArcReactorState.LISTENING -> JarvisElectricGreen
            ArcReactorState.ANALYZING -> JarvisAmber
            ArcReactorState.SPEAKING -> JarvisCyan
        },
        animationSpec = tween(300),
        label = "color"
    )

    Box(
        modifier = modifier
            .size(size)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val maxRadius = this.size.minDimension / 2f - 8.dp.toPx()

            // Dış Segmentli Çember
            drawCircle(
                color = primaryColor.copy(alpha = 0.3f),
                radius = maxRadius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            val segments = 12
            for (i in 0 until segments) {
                val angle = (i * (360f / segments) + outerRotation) * (PI / 180f)
                val start = Offset(
                    (center.x + (maxRadius - 6.dp.toPx()) * cos(angle)).toFloat(),
                    (center.y + (maxRadius - 6.dp.toPx()) * sin(angle)).toFloat()
                )
                val end = Offset(
                    (center.x + (maxRadius + 4.dp.toPx()) * cos(angle)).toFloat(),
                    (center.y + (maxRadius + 4.dp.toPx()) * sin(angle)).toFloat()
                )
                drawLine(
                    color = primaryColor,
                    start = start,
                    end = end,
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // İç Parlayan Çekirdek
            val coreRadius = (maxRadius * 0.45f) * pulseScale

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.7f), Color.Transparent),
                    center = center,
                    radius = coreRadius * 1.5f
                ),
                radius = coreRadius * 1.5f,
                center = center
            )

            drawCircle(
                color = primaryColor,
                radius = coreRadius,
                center = center
            )

            drawCircle(
                color = Color.White,
                radius = coreRadius * 0.4f,
                center = center
            )
        }
    }
}
