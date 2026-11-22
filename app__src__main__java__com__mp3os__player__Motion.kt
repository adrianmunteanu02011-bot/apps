package com.mp3os.player

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star

// Material 3 Expressive style springs (same damping / stiffness as the expressive motion scheme).
// "Spatial" = things that move or change size and may overshoot a little; "effects" = colour / fade, no bounce.
fun <T> springSpatial(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.8f, stiffness = 380f)
fun <T> springFast(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.6f, stiffness = 800f)
fun <T> springSlow(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.8f, stiffness = 200f)
fun <T> springEffects(): FiniteAnimationSpec<T> = spring(dampingRatio = 1f, stiffness = 1600f)

/** Scalloped "cookie" outline, used for the no-cover placeholder. */
object CookieShape : Shape {
    private val polygon = RoundedPolygon.star(
        numVerticesPerRadius = 9,
        innerRadius = 0.84f,
        rounding = CornerRounding(0.45f),
        innerRounding = CornerRounding(0.45f),
    )

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        var first = true
        for (c in polygon.cubics) {
            // polygon is centred on (0,0) with radius 1: map -1..1 to the box
            fun px(v: Float) = (v + 1f) * 0.5f * size.width
            fun py(v: Float) = (v + 1f) * 0.5f * size.height
            if (first) {
                path.moveTo(px(c.anchor0X), py(c.anchor0Y))
                first = false
            }
            path.cubicTo(
                px(c.control0X), py(c.control0Y),
                px(c.control1X), py(c.control1Y),
                px(c.anchor1X), py(c.anchor1Y),
            )
        }
        path.close()
        return Outline.Generic(path)
    }
}
