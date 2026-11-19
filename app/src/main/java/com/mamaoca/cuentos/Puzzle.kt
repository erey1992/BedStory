package com.mamaoca.cuentos

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import android.os.SystemClock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.random.Random

/** Tamaños de rompecabezas: columnas × filas. */
private val SIZES = listOf(4 to 3, 6 to 4, 8 to 5)

private class Piece(val row: Int, val col: Int, start: Offset) {
    var pos by mutableStateOf(start)
    var placed by mutableStateOf(false)
    var z by mutableFloatStateOf(0f)
}

/**
 * Silueta de una pieza: cada borde es recto (en el marco) o tiene una lengüeta que sale ([sign] = 1) o entra (-1).
 * Los bordes se recorren en el sentido de las agujas del reloj; la lengüeta sale hacia fuera de la pieza.
 */
private fun piecePath(w: Float, h: Float, top: Int, right: Int, bottom: Int, left: Int): Path {
    val path = Path().apply { moveTo(0f, 0f) }
    fun edge(ax: Float, ay: Float, bx: Float, by: Float, sign: Int) {
        val len = hypot(bx - ax, by - ay)
        val dx = (bx - ax) / len; val dy = (by - ay) / len
        val nx = dy; val ny = -dx // normal hacia fuera
        val s = sign * minOf(w, h)
        fun p(u: Float, v: Float) = Offset(ax + dx * u * len + nx * v * s, ay + dy * u * len + ny * v * s)
        if (sign == 0) { path.lineTo(bx, by); return }
        fun cubic(a: Offset, b: Offset, c: Offset) = path.cubicTo(a.x, a.y, b.x, b.y, c.x, c.y)
        p(0.36f, 0f).let { path.lineTo(it.x, it.y) }
        cubic(p(0.40f, 0.02f), p(0.32f, 0.18f), p(0.43f, 0.25f))
        cubic(p(0.48f, 0.29f), p(0.52f, 0.29f), p(0.57f, 0.25f))
        cubic(p(0.68f, 0.18f), p(0.60f, 0.02f), p(0.64f, 0f))
        path.lineTo(bx, by)
    }
    edge(0f, 0f, w, 0f, top)
    edge(w, 0f, w, h, right)
    edge(w, h, 0f, h, bottom)
    edge(0f, h, 0f, 0f, left)
    path.close()
    return path
}

@Composable
fun PuzzleScreen(vm: StoryViewModel, reward: Reward, play: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val image by produceState<ImageBitmap?>(null, reward.id) {
        value = withContext(Dispatchers.IO) { runCatching { ArtLoader.load(context, reward.image!!) }.getOrNull() }
    }
    var size by rememberSaveable { mutableIntStateOf(0) }
    var round by remember { mutableIntStateOf(0) } // cambia para volver a mezclar
    Column(Modifier.fillMaxSize().velvet(PlumLight)) {
        GameHeader(reward.title ?: "Rompecabezas", PuzzleOrange, onBack = { play(); vm.back() }, icon = Icons.Default.Extension) {
            vm.puzzleRecord(reward.id, size)?.let { best ->
                Row(Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0x55000000)).goldBorder(RoundedCornerShape(10.dp), 1.dp)
                    .padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.EmojiEvents, null, tint = GoldLeaf, modifier = Modifier.size(20.dp))
                    Text("Tu récord: ${formatTime(best)}", color = GoldPale, fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }
            GoldTabs(SIZES.map { (c, r) -> "${c * r} piezas" }, size) { play(); size = it; round++ }
            GoldCircleButton(Icons.Default.Refresh, "Mezclar otra vez") { play(); round++ }
        }
        val img = image
        if (img == null) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = GoldLeaf) }
        else key(size, round) { Board(vm, reward.id, size, img, SIZES[size].first, SIZES[size].second, onAgain = { round++ }, onMore = { play(); vm.back() }) }
    }
}

@Composable
private fun Board(vm: StoryViewModel, id: String, level: Int, image: ImageBitmap, cols: Int, rows: Int, onAgain: () -> Unit, onMore: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))) {
        val density = LocalDensity.current
        val areaW = with(density) { maxWidth.toPx() }; val areaH = with(density) { maxHeight.toPx() }
        val ratio = image.width.toFloat() / image.height
        val margin = with(density) { 18.dp.toPx() }
        val boardW = minOf(areaW * 0.52f, (areaH - 2 * margin) * ratio)
        val boardH = boardW / ratio
        val origin = Offset(margin, (areaH - boardH) / 2)
        val cw = boardW / cols; val ch = boardH / rows
        val tab = 0.3f * minOf(cw, ch) + 2f
        val pieces = remember {
            val rnd = Random(System.nanoTime())
            // las piezas esperan a la derecha, repartidas en una cuadrícula con un poco de desorden
            val trayX = origin.x + boardW + margin
            val trayW = (areaW - trayX - cw - margin).coerceAtLeast(0f)
            val trayH = (areaH - ch - 2 * margin).coerceAtLeast(0f)
            val n = rows * cols
            val tc = maxOf(1, kotlin.math.ceil(kotlin.math.sqrt(n * (trayW + cw) / (trayH + ch).coerceAtLeast(1f))).toInt())
            val tr = (n + tc - 1) / tc
            val slots = (0 until n).shuffled(rnd)
            List(n) { i ->
                val slot = slots[i]
                val fx = if (tc == 1) 0.5f else (slot % tc) / (tc - 1f)
                val fy = if (tr == 1) 0.5f else (slot / tc) / (tr - 1f)
                val jitter = Offset((rnd.nextFloat() - 0.5f) * cw * 0.2f, (rnd.nextFloat() - 0.5f) * ch * 0.2f)
                val start = Offset(trayX + fx * trayW, margin + fy * trayH) + jitter
                Piece(i / cols, i % cols, start).also { it.z = rnd.nextFloat() }
            }
        }
        // lengüetas: 1 si sale de la pieza de arriba/izquierda, -1 si entra
        val edges = remember { val rnd = Random(System.nanoTime()); IntArray(rows * cols * 2) { if (rnd.nextBoolean()) 1 else -1 } }
        fun down(r: Int, c: Int) = if (r == rows - 1) 0 else edges[(r * cols + c) * 2]
        fun rightOf(r: Int, c: Int) = if (c == cols - 1) 0 else edges[(r * cols + c) * 2 + 1]
        var top by remember { mutableFloatStateOf(1f) }
        val done = pieces.all { it.placed }
        var startedAt by remember { mutableLongStateOf(0L) }
        // al terminar: cuánto tardó y el récord que tenía antes (se guarda una sola vez)
        val result = remember(done) {
            if (!done) null else {
                val time = SystemClock.elapsedRealtime() - (if (startedAt == 0L) SystemClock.elapsedRealtime() else startedAt)
                time to vm.finishPuzzle(id, level, time)
            }
        }

        // el tablero vacío: sin la imagen de fondo, hay que armarlo mirando las piezas
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Color(0x66120628), origin, Size(boardW, boardH))
            // marco doble de oro, como el de los libros
            val sw = 3.dp.toPx(); val gap = 5.dp.toPx()
            drawRect(GoldStroke, origin - Offset(sw, sw), Size(boardW + 2 * sw, boardH + 2 * sw), style = Stroke(sw))
            drawRect(GoldStroke, origin - Offset(sw + gap, sw + gap), Size(boardW + 2 * (sw + gap), boardH + 2 * (sw + gap)),
                style = Stroke(sw * 0.5f), alpha = 0.7f)
        }
        for (piece in pieces) {
            val path = remember(piece) {
                piecePath(cw, ch, if (piece.row > 0) -down(piece.row - 1, piece.col) else 0, rightOf(piece.row, piece.col),
                    down(piece.row, piece.col), if (piece.col > 0) -rightOf(piece.row, piece.col - 1) else 0)
            }
            val target = Offset(origin.x + piece.col * cw, origin.y + piece.row * ch)
            Canvas(Modifier.offset { IntOffset((piece.pos.x - tab).roundToInt(), (piece.pos.y - tab).roundToInt()) }
                .size(with(density) { (cw + 2 * tab).toDp() }, with(density) { (ch + 2 * tab).toDp() })
                .zIndex(if (piece.placed) 0f else 1f + piece.z)
                .then(if (piece.placed) Modifier else Modifier.pointerInput(piece) {
                    detectDragGestures(
                        onDragStart = { top += 1f; piece.z = top; if (startedAt == 0L) startedAt = SystemClock.elapsedRealtime() },
                        onDragEnd = {
                            if (hypot(piece.pos.x - target.x, piece.pos.y - target.y) < minOf(cw, ch) * 0.25f) {
                                piece.pos = target; piece.placed = true
                                if (vm.sound) Sfx.play(if (pieces.all { it.placed }) Sound.Fanfare else Sound.Snap)
                            }
                        },
                    ) { change, drag -> change.consume(); piece.pos += drag }
                })) {
                translate(tab, tab) {
                    clipPath(path) {
                        drawImage(image, IntOffset.Zero, IntSize(image.width, image.height),
                            IntOffset((-piece.col * cw).roundToInt(), (-piece.row * ch).roundToInt()),
                            IntSize(boardW.roundToInt(), boardH.roundToInt()))
                    }
                    if (!done) drawPath(path, Color(0xCCFFF0B8), style = Stroke(if (piece.placed) 1f else 2.5f))
                }
            }
        }
        if (done) {
            Confetti(Modifier.fillMaxSize())
            // a lo ancho, para que quepa en un teléfono acostado: la medalla a la izquierda, lo que se logró a la derecha
            Column(Modifier.align(Alignment.CenterEnd).padding(end = 20.dp).widthIn(max = 420.dp).clip(RoundedCornerShape(18.dp))
                .velvet(Velvet).goldFrame(18.dp, 2.dp).padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val medal = Medal.entries[level]
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    MedalBadge(medal, Modifier.size(84.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("¡Lo armaste!", style = TextStyle(brush = GoldBrush, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 26.sp))
                        Text("¡Medalla de ${medal.label}!", color = GoldPale, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        result?.let { (time, before) ->
                            Text("Lo hiciste en ${formatTime(time)}", color = GoldPale, fontFamily = Andika, fontSize = 16.sp)
                            when {
                                before == null -> {}
                                time < before -> Text("¡Nuevo récord! Antes: ${formatTime(before)}", style = TextStyle(brush = GoldBrush,
                                    fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 15.sp))
                                else -> Text("Tu récord: ${formatTime(before)}", color = GoldPale.copy(alpha = 0.8f), fontFamily = Andika, fontSize = 15.sp)
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillAction(Icons.Default.Refresh, "Otra vez", gold = true, onClick = onAgain)
                    PillAction(Icons.Default.Extension, "Más rompecabezas", Plum, onClick = onMore)
                }
            }
        }
    }
}


/** «45 segundos», «2:05»... */
internal fun formatTime(millis: Long): String {
    val total = ((millis + 500) / 1000).coerceAtLeast(1)
    return if (total == 1L) "1 segundo" else if (total < 60) "$total segundos" else "%d:%02d minutos".format(total / 60, total % 60)
}

/** Una medalla redonda con su cinta: bronce, plata u oro. */
@Composable
internal fun MedalBadge(medal: Medal, modifier: Modifier = Modifier) {
    val (light, mid, dark) = when (medal) {
        Medal.Bronze -> Triple(Color(0xFFF2B98A), Color(0xFFCD7F32), Color(0xFF7A4219))
        Medal.Silver -> Triple(Color(0xFFFFFFFF), Color(0xFFC7CDD6), Color(0xFF6E7684))
        Medal.Gold -> Triple(Color(0xFFFFF4B0), Color(0xFFF2C230), Color(0xFF9A6A08))
    }
    Canvas(modifier.semantics { contentDescription = "Medalla de ${medal.label}" }) {
        val w = size.width; val h = size.height
        val r = minOf(w, h) * 0.34f
        val c = Offset(w / 2, h - r - h * 0.04f)
        // cinta
        fun ribbon(x0: Float, x1: Float, color: Color) {
            val path = Path().apply {
                moveTo(x0, 0f); lineTo(x0 + w * 0.16f, 0f); lineTo(x1 + w * 0.08f, c.y); lineTo(x1 - w * 0.08f, c.y); close()
            }
            drawPath(path, color)
        }
        ribbon(w * 0.18f, w * 0.42f, Color(0xFF7B2D8E))
        ribbon(w * 0.66f, w * 0.58f, Color(0xFFB8336A))
        drawCircle(Color(0x55000000), r * 1.04f, c + Offset(0f, r * 0.08f))
        drawCircle(Brush.linearGradient(listOf(light, mid, dark), c - Offset(r, r), c + Offset(r, r)), r, c)
        drawCircle(dark, r * 0.74f, c, style = Stroke(r * 0.06f))
        star(c, r * 0.48f, light)
    }
}

private fun DrawScope.star(c: Offset, r: Float, color: Color) {
    val path = Path()
    for (i in 0 until 10) {
        val a = -Math.PI / 2 + i * Math.PI / 5
        val rr = if (i % 2 == 0) r else r * 0.45f
        val x = c.x + (kotlin.math.cos(a) * rr).toFloat(); val y = c.y + (kotlin.math.sin(a) * rr).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color)
}
