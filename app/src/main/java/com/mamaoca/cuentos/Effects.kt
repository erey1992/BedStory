package com.mamaoca.cuentos

import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotateRad
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

@Composable
private fun rememberSeconds(running: Boolean): State<Float> {
    val time = remember { mutableFloatStateOf(0f) }
    if (running) LaunchedEffect(Unit) {
        val start = withInfiniteAnimationFrameNanos { it } - (time.floatValue * 1e9f).toLong()
        while (isActive) withInfiniteAnimationFrameNanos { time.floatValue = (it - start) / 1e9f }
    }
    return time
}

/** Estrella de cuatro puntas de radio 1, centrada en el origen. */
private val sparklePath = Path().apply {
    moveTo(0f, -1f); quadraticTo(0f, 0f, 1f, 0f); quadraticTo(0f, 0f, 0f, 1f)
    quadraticTo(0f, 0f, -1f, 0f); quadraticTo(0f, 0f, 0f, -1f); close()
}
private val starPath = Path().apply {
    for (i in 0 until 10) {
        val a = -PI / 2 + i * PI / 5; val r = if (i % 2 == 0) 1f else 0.45f
        val x = (cos(a) * r).toFloat(); val y = (sin(a) * r).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}
private val heartPath = Path().apply {
    moveTo(0f, 0.8f); cubicTo(-1.4f, -0.2f, -0.6f, -1.4f, 0f, -0.5f); cubicTo(0.6f, -1.4f, 1.4f, -0.2f, 0f, 0.8f); close()
}

private fun DrawScope.shape(path: Path, center: Offset, radius: Float, color: Color, rotation: Float = 0f) {
    translate(center.x, center.y) { rotateRad(rotation, Offset.Zero) { scale(radius, radius, Offset.Zero) { drawPath(path, color) } } }
}

/** Brillos de la portada: estrellas que titilan, resplandor de luna y farol, y polvo mágico que sube. */
@Composable
fun CoverMagic(animated: Boolean, modifier: Modifier = Modifier) {
    val time by rememberSeconds(animated)
    val stars = remember {
        val fixed = listOf(0.47f to 0.07f, 0.52f to 0.03f, 0.96f to 0.33f, 0.68f to 0.47f, 0.41f to 0.55f, 0.72f to 0.66f, 0.14f to 0.06f, 0.88f to 0.52f, 0.30f to 0.30f, 0.60f to 0.25f)
        val rnd = Random(4)
        (fixed + List(14) { rnd.nextFloat() to rnd.nextFloat() * 0.6f }).map { (x, y) ->
            floatArrayOf(x, y, 5f + rnd.nextFloat() * 9f, 2f + rnd.nextFloat() * 3f, rnd.nextFloat() * 4f)
        }
    }
    Canvas(modifier) {
        val pulse = 0.5f + 0.5f * sin(time * 2 * PI.toFloat() / 4f)
        val moonC = Offset(size.width * 0.93f, size.height * 0.12f); val moonR = size.minDimension * (0.2f + 0.03f * pulse)
        drawCircle(Brush.radialGradient(listOf(Color(0x99FFF3B0), Color.Transparent), moonC, moonR), moonR, moonC)
        val flick = 0.5f + 0.5f * sin(time * 2 * PI.toFloat() / 2.4f)
        val lampC = Offset(size.width * 0.08f, size.height * 0.24f); val lampR = size.minDimension * (0.14f + 0.03f * flick)
        drawCircle(Brush.radialGradient(listOf(Color(0xAAFFC85A), Color.Transparent), lampC, lampR), lampR, lampC)
        stars.forEach { (x, y, s, period, delay) ->
            val p = ((time + delay) % period) / period
            val a = sin(p * PI).toFloat()
            if (a > 0.02f) shape(sparklePath, Offset(x * size.width, y * size.height), s * density * (0.3f + 0.7f * a),
                Color(0xFFFFF4C2).copy(alpha = a), p * PI.toFloat() / 2)
        }
        for (i in 0 until 60) {
            val seed = (i * 0.618f) % 1f
            val speed = 0.04f + 0.08f * ((i * 0.37f) % 1f)
            val p = (seed + time * speed) % 1f
            val y = size.height * (1.05f - p * 1.1f)
            val x = size.width * ((i * 0.137f + 0.05f * sin(time * 0.8f + i)) % 1f)
            val a = sin(p * PI).toFloat() * (0.5f + 0.5f * sin(time * 3 + i))
            drawCircle((if (i % 3 == 0) Color.White else Color(0xFFFFE9A8)).copy(alpha = a.coerceIn(0f, 1f)),
                (1f + (i % 4) * 0.6f) * density, Offset(x, y))
        }
    }
}

private class Bit(var x: Float, var y: Float, var vx: Float, var vy: Float, val size: Float,
                  var rot: Float, val vr: Float, val color: Color, val kind: Int, val wobble: Float)

private val confettiColors = listOf(Color(0xFFFFD166), Color(0xFFFF4D6D), Color(0xFF4CC9F0), Color(0xFF80ED99),
    Color(0xFFC77DFF), Color(0xFFFF9F1C), Color.White)

/** Confeti de felicitaciones: estallidos al aparecer, lluvia suave y más estallidos al tocar la pantalla. */
@Composable
fun Confetti(modifier: Modifier = Modifier, onTap: () -> Unit = {}) {
    val d = LocalDensity.current.density
    val bits = remember { ArrayList<Bit>() }
    var frame by remember { mutableIntStateOf(0) }
    var area by remember { mutableStateOf(Size.Zero) }
    val rnd = remember { Random(System.nanoTime()) }
    fun burst(x: Float, y: Float, n: Int, spread: Float) {
        repeat(n) { i ->
            val a = -PI / 2 + (rnd.nextFloat() - 0.5f) * spread
            val v = (6f + rnd.nextFloat() * 10f) * d
            bits += Bit(x, y, (cos(a) * v).toFloat(), (sin(a) * v).toFloat(), (5f + rnd.nextFloat() * 7f) * d,
                rnd.nextFloat() * 6f, (rnd.nextFloat() - 0.5f) * 0.3f, confettiColors[i % confettiColors.size], i % 4, rnd.nextFloat() * 6f)
        }
    }
    LaunchedEffect(Unit) {
        while (area == Size.Zero) withInfiniteAnimationFrameNanos { }
        burst(area.width * 0.1f, area.height, 90, 1.2f); burst(area.width * 0.9f, area.height, 90, 1.2f)
        launch { delay(450); burst(area.width * 0.5f, area.height * 0.95f, 110, 1.6f) }
        var last = withInfiniteAnimationFrameNanos { it }
        while (isActive) withInfiniteAnimationFrameNanos { now ->
            val dt = min(3f, (now - last) / 16_666_667f); last = now; frame++
            if (frame % 18 == 0 && frame < 900) repeat(4) {
                bits += Bit(rnd.nextFloat() * area.width, -20f, (rnd.nextFloat() - 0.5f) * 2 * d, (2f + rnd.nextFloat() * 2) * d,
                    (5f + rnd.nextFloat() * 6f) * d, 0f, (rnd.nextFloat() - 0.5f) * 0.2f,
                    confettiColors[rnd.nextInt(confettiColors.size)], rnd.nextInt(4), rnd.nextFloat() * 6f)
            }
            bits.forEach { b ->
                b.vy = min(b.vy + 0.13f * d * dt, 5f * d); b.vx *= 0.985f
                b.x += (b.vx + sin((frame + b.wobble * 20) / 20f) * 0.6f * d) * dt; b.y += b.vy * dt; b.rot += b.vr * dt
            }
            bits.removeAll { it.y > area.height + 40 * d }
        }
    }
    Canvas(modifier.pointerInput(Unit) {
        detectTapGestures { burst(it.x, it.y, 40, 2 * PI.toFloat()); onTap() }
    }) {
        area = size
        frame.let { }
        bits.forEach { b ->
            val c = Offset(b.x, b.y)
            when (b.kind) {
                0 -> translate(b.x, b.y) {
                    rotateRad(b.rot, Offset.Zero) {
                        val h = b.size / 2 * abs(cos(frame / 10f + b.wobble)) + 1f
                        drawRect(b.color, Offset(-b.size / 2, -h / 2), Size(b.size, h))
                    }
                }
                1 -> drawCircle(b.color, b.size / 2.4f, c)
                2 -> shape(starPath, c, b.size / 1.6f, b.color, b.rot)
                else -> shape(heartPath, c, b.size / 1.5f, b.color, b.rot * 0.3f)
            }
        }
    }
}
