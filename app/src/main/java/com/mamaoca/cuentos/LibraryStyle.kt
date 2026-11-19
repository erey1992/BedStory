@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.mamaoca.cuentos

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.random.Random

// ---------- Paleta de biblioteca mágica: terciopelo violeta, polvo de estrellas, pan de oro y pergamino ----------
internal val Plum = Color(0xFF241046)
internal val PlumLight = Color(0xFF3E1D72)
internal val Velvet = Color(0xFF6A35A8)
internal val GoldLeaf = Color(0xFFE9C46A)
internal val GoldDeep = Color(0xFFB8862F)
internal val GoldPale = Color(0xFFFFF0B8)
internal val Parchment = Color(0xFFFBF3E0)
internal val Amethyst = Color(0xFF7B2D8E)

/** Brillo metálico del oro: claro arriba, profundo abajo. */
internal val GoldBrush = Brush.verticalGradient(listOf(GoldPale, GoldLeaf, GoldDeep))
internal val GoldStroke = Brush.linearGradient(listOf(GoldPale, GoldDeep, GoldLeaf, GoldDeep))

/** Fraunces: serifa clásica y cálida para títulos de cuentos. */
internal val Fraunces = FontFamily(listOf(500, 600, 700, 800).map {
    Font(R.font.fraunces, FontWeight(it), variationSettings = FontVariation.Settings(FontVariation.weight(it)))
})

/** Polvo de estrellas: puntitos dorados y blancos que se repiten sin costuras sobre el terciopelo. */
private val starDust: ImageBitmap by lazy {
    val w = 360; val h = 360
    val rnd = Random(8)
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = android.graphics.Canvas(bmp)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    repeat(90) {
        val x = rnd.nextFloat() * w; val y = rnd.nextFloat() * h
        val big = rnd.nextFloat() < 0.12f
        val r = if (big) 1.6f + rnd.nextFloat() else 0.5f + rnd.nextFloat() * 0.7f
        paint.color = if (rnd.nextBoolean()) 0xFFFFE9A8.toInt() else 0xFFFFFFFF.toInt()
        paint.alpha = (if (big) 200 else 60 + rnd.nextInt(110))
        c.drawCircle(x, y, r, paint)
        if (big) { paint.alpha = 40; c.drawCircle(x, y, r * 4, paint) } // halo suave
    }
    bmp.asImageBitmap()
}

/** Fondo de terciopelo violeta con polvo de estrellas y una luz mágica en el centro. */
fun Modifier.velvet(base: Color = PlumLight, shape: Shape? = null): Modifier = this
    .then(if (shape != null) Modifier.background(Brush.verticalGradient(listOf(lighten(base), base, darken(base))), shape)
          else Modifier.background(Brush.verticalGradient(listOf(lighten(base), base, darken(base)))))
    .drawBehind {
        drawRect(Brush.radialGradient(listOf(Color(0x33E9A8FF), Color.Transparent), center, size.maxDimension * 0.6f))
        drawRect(ShaderBrush(ImageShader(starDust, TileMode.Repeated, TileMode.Repeated)))
    }

private fun darken(c: Color) = Color(c.red * 0.6f, c.green * 0.55f, c.blue * 0.7f, c.alpha)
private fun lighten(c: Color) = Color(c.red + (1 - c.red) * 0.12f, c.green + (1 - c.green) * 0.08f, c.blue + (1 - c.blue) * 0.15f, c.alpha)

/** Marco de oro doble, como el de los libros antiguos. */
fun Modifier.goldFrame(radius: Dp = 12.dp, width: Dp = 2.dp): Modifier = this.drawWithContent {
    drawContent()
    val r = radius.toPx(); val sw = width.toPx(); val gap = 4.dp.toPx()
    drawRoundRect(GoldStroke, Offset(sw / 2, sw / 2), Size(size.width - sw, size.height - sw), CornerRadius(r), style = Stroke(sw))
    drawRoundRect(GoldStroke, Offset(gap + sw, gap + sw), Size(size.width - 2 * (gap + sw), size.height - 2 * (gap + sw)),
        CornerRadius((r - gap).coerceAtLeast(2f)), style = Stroke(sw * 0.5f), alpha = 0.7f)
}

/** Estante mágico bajo cada fila de cuentos: una repisa de oro que desprende luz hacia arriba. */
fun Modifier.shelf(): Modifier = this.drawBehind {
    val bar = 7.dp.toPx(); val top = size.height - 10.dp.toPx()
    drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color(0x40FFD98A)), top - 46.dp.toPx(), top), Offset(0f, top - 46.dp.toPx()),
        Size(size.width, 46.dp.toPx()))
    drawRoundRect(GoldBrush, Offset(0f, top), Size(size.width, bar), CornerRadius(bar / 2))
    drawRect(Color(0x99FFF7D6), Offset(0f, top + 1.dp.toPx()), Size(size.width, 1.dp.toPx()))
    drawRect(Brush.verticalGradient(listOf(Color(0x66000000), Color.Transparent), top + bar, top + bar + 10.dp.toPx()),
        Offset(0f, top + bar), Size(size.width, 10.dp.toPx()))
}

fun Modifier.goldBorder(shape: Shape, width: Dp = 2.dp) = this.border(width, GoldStroke, shape)
