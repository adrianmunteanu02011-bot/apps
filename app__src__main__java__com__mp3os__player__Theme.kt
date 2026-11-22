package com.mp3os.player

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

// ------------------------------------------------------------------ colours from the cover

private val DEFAULT_SEED = Color(0xFFFF9F43).toArgb()

private fun hsl(h: Float, s: Float, l: Float): Color {
    val hue = ((h % 360f) + 360f) % 360f
    return Color(ColorUtils.HSLToColor(floatArrayOf(hue, s.coerceIn(0f, 1f), l.coerceIn(0f, 1f))))
}

private fun hueOf(argb: Int): Float {
    val out = FloatArray(3)
    ColorUtils.colorToHSL(argb, out)
    return out[0]
}

/** Builds a dark Material colour scheme out of the cover's palette (or the default orange). */
fun schemeFrom(sw: Swatches?): ColorScheme {
    val seed = sw?.vibrant ?: sw?.dominant ?: DEFAULT_SEED
    val seedHsl = FloatArray(3)
    ColorUtils.colorToHSL(seed, seedHsl)
    val h = seedHsl[0]
    val s = seedHsl[1].coerceIn(0.22f, 0.95f)
    val bgS = (seedHsl[1] * 0.7f).coerceIn(0.14f, 0.5f)
    val h2 = sw?.muted?.let(::hueOf) ?: (h + 28f)
    val h3 = sw?.light?.let(::hueOf) ?: (h + 60f)

    return darkColorScheme(
        primary = hsl(h, s, 0.74f),
        onPrimary = hsl(h, s, 0.12f),
        primaryContainer = hsl(h, s * 0.8f, 0.27f),
        onPrimaryContainer = hsl(h, 0.9f, 0.9f),
        secondary = hsl(h2, 0.45f, 0.76f),
        onSecondary = hsl(h2, 0.4f, 0.13f),
        secondaryContainer = hsl(h2, 0.32f, 0.25f),
        onSecondaryContainer = hsl(h2, 0.5f, 0.9f),
        tertiary = hsl(h3, 0.5f, 0.78f),
        onTertiary = hsl(h3, 0.4f, 0.13f),
        tertiaryContainer = hsl(h3, 0.35f, 0.26f),
        onTertiaryContainer = hsl(h3, 0.5f, 0.9f),
        background = hsl(h, bgS, 0.07f),
        onBackground = hsl(h, 0.15f, 0.93f),
        surface = hsl(h, bgS, 0.08f),
        onSurface = hsl(h, 0.15f, 0.93f),
        surfaceVariant = hsl(h, bgS * 0.7f, 0.2f),
        onSurfaceVariant = hsl(h, 0.15f, 0.76f),
        surfaceContainerLowest = hsl(h, bgS, 0.05f),
        surfaceContainerLow = hsl(h, bgS, 0.09f),
        surfaceContainer = hsl(h, bgS, 0.12f),
        surfaceContainerHigh = hsl(h, bgS, 0.16f),
        surfaceContainerHighest = hsl(h, bgS, 0.2f),
        outline = hsl(h, 0.1f, 0.55f),
        outlineVariant = hsl(h, 0.15f, 0.3f),
    )
}

@Composable
private fun Color.glide(): Color =
    animateColorAsState(this, tween(900, easing = FastOutSlowInEasing), label = "scheme").value

/** Cross-fades every colour when the song (and so the cover) changes. */
@Composable
fun animatedScheme(t: ColorScheme): ColorScheme = t.copy(
    primary = t.primary.glide(),
    onPrimary = t.onPrimary.glide(),
    primaryContainer = t.primaryContainer.glide(),
    onPrimaryContainer = t.onPrimaryContainer.glide(),
    secondary = t.secondary.glide(),
    onSecondary = t.onSecondary.glide(),
    secondaryContainer = t.secondaryContainer.glide(),
    onSecondaryContainer = t.onSecondaryContainer.glide(),
    tertiary = t.tertiary.glide(),
    onTertiary = t.onTertiary.glide(),
    tertiaryContainer = t.tertiaryContainer.glide(),
    onTertiaryContainer = t.onTertiaryContainer.glide(),
    background = t.background.glide(),
    onBackground = t.onBackground.glide(),
    surface = t.surface.glide(),
    onSurface = t.onSurface.glide(),
    surfaceVariant = t.surfaceVariant.glide(),
    onSurfaceVariant = t.onSurfaceVariant.glide(),
    surfaceContainerLowest = t.surfaceContainerLowest.glide(),
    surfaceContainerLow = t.surfaceContainerLow.glide(),
    surfaceContainer = t.surfaceContainer.glide(),
    surfaceContainerHigh = t.surfaceContainerHigh.glide(),
    surfaceContainerHighest = t.surfaceContainerHighest.glide(),
    outline = t.outline.glide(),
    outlineVariant = t.outlineVariant.glide(),
)

// ------------------------------------------------------------------ dancing

/** Smoothed beat numbers for the current frame. Read them inside draw / graphicsLayer lambdas. */
@Stable
class Dance {
    var bass by mutableFloatStateOf(0f)
    var level by mutableFloatStateOf(0f)
    var t by mutableFloatStateOf(0f)
}

val LocalDance = staticCompositionLocalOf { Dance() }

@Composable
fun rememberDance(playing: Boolean): Dance {
    val dance = remember { Dance() }
    val playingNow by rememberUpdatedState(playing)

    LaunchedEffect(dance) {
        var last = 0L
        while (true) {
            val active = playingNow || dance.bass > 0.002f || dance.level > 0.002f
            if (!active) {
                last = 0L
                delay(150) // nothing moving: don't spend battery on frames
                continue
            }
            androidx.compose.runtime.withFrameNanos { now ->
                val dt = if (last == 0L) 0.016f else ((now - last) / 1_000_000_000f).coerceIn(0f, 0.05f)
                last = now

                var targetBass = 0f
                var targetLevel = 0f
                if (playingNow) {
                    val live = System.nanoTime() - AudioLevels.stamp < 400_000_000L
                    if (live) {
                        targetBass = AudioLevels.bass
                        targetLevel = AudioLevels.level
                    } else {
                        // meter not running (unusual audio format): fake a gentle groove
                        targetBass = (0.5f + 0.5f * sin(dance.t * 5.5f)).pow(3) * 0.6f
                        targetLevel = 0.35f
                    }
                }
                // quick attack so beats snap, slow release so it glides back
                val kb = 1f - exp(-dt * (if (targetBass > dance.bass) 30f else 6f))
                val kl = 1f - exp(-dt * 4f)
                dance.bass += (targetBass - dance.bass) * kb
                dance.level += (targetLevel - dance.level) * kl
                if (playingNow) dance.t += dt * (0.6f + 2.2f * dance.level)
            }
        }
    }
    return dance
}

// ------------------------------------------------------------------ motion blur

/**
 * Directional blur driven by motion (tab slides, track changes, fast scrolling).
 * Radii are in pixels and read at draw time, so animating them never recomposes.
 * Needs Android 12+; on older phones it simply does nothing.
 */
fun Modifier.motionBlur(radiusX: () -> Float, radiusY: () -> Float): Modifier =
    if (Build.VERSION.SDK_INT < 31) this
    else graphicsLayer {
        val rx = radiusX()
        val ry = radiusY()
        renderEffect = if (max(rx, ry) > 0.5f) BlurEffect(rx, ry, TileMode.Clamp) else null
    }

// ------------------------------------------------------------------ backdrop

/** Soft colour blobs in the cover's palette that drift, and swell with the bass. */
@Composable
fun Backdrop(modifier: Modifier = Modifier) {
    val dance = LocalDance.current
    val cs = MaterialTheme.colorScheme
    val c1 = cs.primaryContainer
    val c2 = cs.secondaryContainer
    val c3 = cs.tertiaryContainer

    Canvas(modifier.fillMaxSize()) {
        val t = dance.t
        val b = dance.bass
        val w = size.width
        val h = size.height
        val m = size.minDimension

        fun blob(color: Color, cx: Float, cy: Float, r: Float, alpha: Float) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = alpha), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = r,
                ),
                radius = r,
                center = Offset(cx, cy),
            )
        }
        blob(c1, w * (0.15f + 0.12f * sin(t * 0.50f)), h * (0.12f + 0.06f * cos(t * 0.40f)), m * (0.85f + 0.30f * b), 0.95f)
        blob(c2, w * (0.95f + 0.08f * cos(t * 0.35f)), h * (0.45f + 0.10f * sin(t * 0.30f)), m * (0.80f + 0.25f * b), 0.8f)
        blob(c3, w * (0.30f + 0.10f * cos(t * 0.45f)), h * (0.92f + 0.05f * sin(t * 0.55f)), m * (0.75f + 0.35f * b), 0.7f)
    }
}
