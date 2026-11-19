package com.mamaoca.cuentos

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Un dibujo para colorear: [lines] son las líneas negras (transparentes donde no hay línea), encima de todo;
 * [paint] es la capa de color, debajo, donde pintan el pincel y el balde. [wall] marca los píxeles de línea,
 * que el balde no cruza.
 */
class ColoringPage(val lines: Bitmap, val paint: Bitmap, private val wall: BooleanArray) {
    val w = lines.width
    val h = lines.height
    var version by mutableIntStateOf(0); private set
    private val undo = ArrayDeque<IntArray>()
    val canUndo get() = undo.isNotEmpty()

    fun snapshot() {
        undo.addLast(IntArray(w * h).also { paint.getPixels(it, 0, w, 0, 0, w, h) })
        while (undo.size > 12) undo.removeFirst()
    }
    fun undo() { undo.removeLastOrNull()?.let { paint.setPixels(it, 0, w, 0, 0, w, h); version++ } }
    fun clear() { snapshot(); paint.eraseColor(android.graphics.Color.WHITE); version++ }

    private val brush = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    private val canvas = android.graphics.Canvas(paint)
    private var strokeLayer: Bitmap? = null

    /**
     * Empieza un trazo de pincel: el pincel solo pinta dentro de la zona cerrada por líneas negras donde se apoyó el dedo
     * (puede tapar colores de esa zona, pero no se sale de sus bordes). Devuelve false si se tocó justo una línea.
     */
    fun beginStroke(sx: Int, sy: Int, color: Int): Boolean {
        val (mask, _) = region(sx, sy, byColor = false) ?: return false
        val px = IntArray(w * h) { if (mask[it]) color else 0 }
        strokeLayer = Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888)
        brush.shader = android.graphics.BitmapShader(strokeLayer!!, android.graphics.Shader.TileMode.CLAMP, android.graphics.Shader.TileMode.CLAMP)
        return true
    }

    fun stroke(from: Offset, to: Offset, width: Float) {
        if (strokeLayer == null) return
        brush.strokeWidth = width
        if (from == to) { brush.style = Paint.Style.FILL; canvas.drawCircle(to.x, to.y, width / 2, brush); brush.style = Paint.Style.STROKE }
        else canvas.drawLine(from.x, from.y, to.x, to.y, brush)
        version++
    }

    fun endStroke() { brush.shader = null; strokeLayer = null }

    /**
     * Balde: pinta la zona donde se tocó. La zona termina en las líneas negras y también donde empieza otro color:
     * lo ya pintado hace de borde. Si se toca encima de algo pintado, se cambia ese color.
     */
    fun fill(sx: Int, sy: Int, color: Int) {
        val (mask, px) = region(sx, sy, byColor = true) ?: return
        for (i in mask.indices) if (mask[i]) px[i] = color
        paint.setPixels(px, 0, w, 0, 0, w, h)
        version++
    }

    /**
     * La zona alrededor de (sx, sy): los píxeles conectados que no son línea negra y, con [byColor], que además tienen
     * un color parecido al del punto tocado. Se extiende un poquito por debajo de las líneas negras de su borde,
     * para que no queden huecos blancos junto a ellas. Devuelve la zona y los píxeles actuales de la capa de color.
     */
    private fun region(sx: Int, sy: Int, byColor: Boolean): Pair<BooleanArray, IntArray>? {
        val start = nearestOpen(sx, sy) ?: return null
        val px = IntArray(w * h).also { paint.getPixels(it, 0, w, 0, 0, w, h) }
        val target = px[start]
        fun open(i: Int) = !wall[i] && (!byColor || similar(px[i], target))
        val done = BooleanArray(w * h)
        val stack = ArrayDeque<Int>().apply { addLast(start) }
        var minX = w; var maxX = 0; var minY = h; var maxY = 0
        while (stack.isNotEmpty()) {
            val p = stack.removeLast()
            val y = p / w
            var x = p % w
            if (done[p]) continue
            while (x > 0 && !done[y * w + x - 1] && open(y * w + x - 1)) x--
            var up = false; var down = false
            while (x < w && !done[y * w + x] && open(y * w + x)) {
                val i = y * w + x
                done[i] = true
                if (x < minX) minX = x; if (x > maxX) maxX = x; if (y < minY) minY = y; if (y > maxY) maxY = y
                if (y > 0) { val o = i - w; val ok = !done[o] && open(o); if (ok && !up) stack.addLast(o); up = ok }
                if (y < h - 1) { val o = i + w; val ok = !done[o] && open(o); if (ok && !down) stack.addLast(o); down = ok }
                x++
            }
        }
        val r = 2
        val under = ArrayList<Int>()
        for (y in maxOf(0, minY - r)..minOf(h - 1, maxY + r)) for (x in maxOf(0, minX - r)..minOf(w - 1, maxX + r)) {
            val i = y * w + x
            if (!wall[i] || done[i]) continue
            search@ for (dy in -r..r) for (dx in -r..r) {
                val nx = x + dx; val ny = y + dy
                if (nx in 0 until w && ny in 0 until h && done[ny * w + nx]) { under += i; break@search }
            }
        }
        under.forEach { done[it] = true }
        return done to px
    }

    /** Dos colores son «el mismo» para el balde si se parecen mucho (los bordes suavizados del pincel no cortan la zona). */
    private fun similar(a: Int, b: Int): Boolean {
        val dr = ((a shr 16) and 0xFF) - ((b shr 16) and 0xFF)
        val dg = ((a shr 8) and 0xFF) - ((b shr 8) and 0xFF)
        val db = (a and 0xFF) - (b and 0xFF)
        return dr * dr + dg * dg + db * db < 48 * 48
    }

    private fun nearestOpen(x: Int, y: Int): Int? {
        for (r in 0..8) for (dy in -r..r) for (dx in -r..r) {
            val nx = x + dx; val ny = y + dy
            if (nx in 0 until w && ny in 0 until h && !wall[ny * w + nx]) return ny * w + nx
        }
        return null
    }

    /** La imagen terminada, para guardar o enviar. */
    fun composite(): Bitmap = paint.copy(Bitmap.Config.ARGB_8888, true).also { android.graphics.Canvas(it).drawBitmap(lines, 0f, 0f, null) }

    fun save(file: File) { file.parentFile?.mkdirs(); file.outputStream().use { paint.compress(Bitmap.CompressFormat.PNG, 100, it) } }

    companion object {
        fun load(context: Context, reward: Reward, saved: File): ColoringPage {
            val gray = context.assets.open(reward.image!!).use { BitmapFactory.decodeStream(it) }
            val w = gray.width; val h = gray.height
            val src = IntArray(w * h).also { gray.getPixels(it, 0, w, 0, 0, w, h) }
            val wall = BooleanArray(w * h)
            val line = IntArray(w * h) { i ->
                val g = src[i] and 0xFF
                wall[i] = g < 150
                (255 - g) shl 24 // negro con la transparencia del gris: el blanco no tapa el color
            }
            val lines = Bitmap.createBitmap(line, w, h, Bitmap.Config.ARGB_8888)
            val old = saved.takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path) }?.takeIf { it.width == w && it.height == h }
            val paint = old?.copy(Bitmap.Config.ARGB_8888, true)
                ?: Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.WHITE) }
            return ColoringPage(lines, paint, wall)
        }
    }
}

private val PALETTE = listOf(
    0xFFE53935, 0xFFFF7043, 0xFFFFA726, 0xFFFFEE58, 0xFFC0CA33, 0xFF66BB6A, 0xFF2E7D32, 0xFF26A69A,
    0xFF4FC3F7, 0xFF1E88E5, 0xFF3949AB, 0xFF8E24AA, 0xFFEC407A, 0xFFF8BBD0, 0xFFFAD7B5, 0xFFD7A26E,
    0xFF8D5524, 0xFF5D4037, 0xFF9E9E9E, 0xFF212121,
).map { Color(it) }

private enum class Tool { Bucket, Brush, Eraser }

@Composable
fun ColoringScreen(vm: StoryViewModel, reward: Reward, play: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val saved = remember(reward.id) { File(context.filesDir, "coloring/${reward.id}.png") }
    val page by produceState<ColoringPage?>(null, reward.id) {
        value = withContext(Dispatchers.IO) { ColoringPage.load(context, reward, saved) }
    }
    val scope = rememberCoroutineScope()
    // el progreso se guarda al salir, para seguir otro día
    DisposableEffect(page) { onDispose { page?.let { p -> scope.launch(Dispatchers.IO) { runCatching { p.save(saved) } } } } }
    var tool by remember { mutableStateOf(Tool.Bucket) }
    var color by remember { mutableStateOf(PALETTE[0]) }
    var brush by remember { mutableFloatStateOf(18f) }
    var showSizes by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().velvet(PlumLight)) {
        GameHeader(reward.title ?: "Colorear", ColoringTeal, onBack = { play(); vm.back() }, icon = Icons.Default.Palette) {
            page?.let { p -> ShareActions("dibujo-${reward.id}") { p.composite() } }
        }
        val p = page
        if (p == null) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = GoldLeaf) }
        else Row(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)).padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // herramientas
            Column(Modifier.fillMaxHeight().gamePanel().verticalScroll(rememberScrollState()).padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                ToolButton(Icons.Default.FormatColorFill, "Balde de pintura", tool == Tool.Bucket) { tool = Tool.Bucket; showSizes = false }
                // pincel y goma: al elegirlos se despliegan los tamaños al lado
                listOf(Tool.Brush to (Icons.Default.Brush to "Pincel"), Tool.Eraser to (Icons.Default.AutoFixNormal to "Goma de borrar"))
                    .forEach { (t, look) ->
                        Box {
                            ToolButton(look.first, look.second, tool == t) { showSizes = tool != t || !showSizes; tool = t }
                            if (showSizes && tool == t) SizePicker(brush, if (t == Tool.Eraser) Color.White else color,
                                onDismiss = { showSizes = false }) { brush = it; showSizes = false }
                        }
                    }
                ToolButton(Icons.AutoMirrored.Filled.Undo, "Deshacer", false) { p.undo() }
                ToolButton(Icons.Default.DeleteSweep, "Empezar de nuevo", false) { p.clear() }
            }
            // el dibujo
            BoxWithConstraints(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                val ratio = p.w.toFloat() / p.h
                val fitW = minOf(maxWidth, maxHeight * ratio)
                val density = LocalDensity.current
                val image = remember(p) { p.paint.asImageBitmap() }
                val lineImage = remember(p) { p.lines.asImageBitmap() }
                Canvas(Modifier.size(fitW, fitW / ratio).pictureFrame()
                    .pointerInput(p, tool, color, brush) {
                        val k = p.w / with(density) { fitW.toPx() }
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            val at = down.position * k
                            p.snapshot()
                            if (tool == Tool.Bucket) {
                                if (vm.sound) Sfx.play(Sound.Splash)
                                scope.launch(Dispatchers.Default) { p.fill(at.x.toInt(), at.y.toInt(), color.toArgb()) }
                            } else {
                                showSizes = false
                                val c = if (tool == Tool.Eraser) android.graphics.Color.WHITE else color.toArgb()
                                if (p.beginStroke(at.x.toInt(), at.y.toInt(), c)) try {
                                    var last = at
                                    p.stroke(last, last, brush)
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                        if (!change.pressed) break
                                        val next = change.position * k
                                        p.stroke(last, next, brush); last = next
                                        if (change.positionChange() != Offset.Zero) change.consume()
                                    }
                                } finally { p.endStroke() }
                            }
                        }
                    }) {
                    p.version // se vuelve a dibujar con cada trazo
                    val dst = IntSize(size.width.toInt(), size.height.toInt())
                    drawImage(image, IntOffset.Zero, IntSize(p.w, p.h), IntOffset.Zero, dst, filterQuality = FilterQuality.Medium)
                    drawImage(lineImage, IntOffset.Zero, IntSize(p.w, p.h), IntOffset.Zero, dst, filterQuality = FilterQuality.Medium)
                }
            }
            // colores
            Column(Modifier.fillMaxHeight().gamePanel().verticalScroll(rememberScrollState()).padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PALETTE.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        pair.forEach { c ->
                            val selected = c == color
                            Box(Modifier.size(38.dp).shadow(if (selected) 6.dp else 2.dp, CircleShape).clip(CircleShape).background(c)
                                .then(if (selected) Modifier.border(4.dp, GoldPale, CircleShape) else Modifier.goldBorder(CircleShape, 1.5.dp))
                                .clickable(role = Role.Button) { color = c; if (tool == Tool.Eraser) tool = Tool.Bucket }
                                .semantics { contentDescription = "Color" })
                        }
                    }
                }
            }
        }
    }
}

private val BRUSH_SIZES = listOf(6f, 14f, 26f, 44f)

/** Los tamaños del pincel, desplegados al lado de su botón; cada uno muestra un punto de ese grosor y color. */
@Composable
private fun SizePicker(current: Float, color: Color, onDismiss: () -> Unit, onPick: (Float) -> Unit) {
    val density = LocalDensity.current
    androidx.compose.ui.window.Popup(alignment = Alignment.CenterStart, offset = with(density) { IntOffset(58.dp.roundToPx(), 0) },
        onDismissRequest = onDismiss) {
        Row(Modifier.shadow(8.dp, RoundedCornerShape(14.dp)).clip(RoundedCornerShape(14.dp)).velvet(Plum)
            .goldBorder(RoundedCornerShape(14.dp), 1.5.dp).padding(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            BRUSH_SIZES.forEachIndexed { i, s ->
                val active = s == current
                Box(Modifier.size(50.dp).clip(RoundedCornerShape(10.dp))
                    .then(if (active) Modifier.background(GoldBrush) else Modifier.background(Color(0x33FFFFFF)))
                    .clickable(role = Role.Button) { onPick(s) }
                    .semantics { contentDescription = listOf("Pincel muy fino", "Pincel fino", "Pincel grueso", "Pincel muy grueso")[i] },
                    contentAlignment = Alignment.Center) {
                    Box(Modifier.size((6 + i * 9).dp).clip(CircleShape).background(color).border(1.dp, Color(0x55000000), CircleShape))
                }
            }
        }
    }
}

@Composable
private fun ToolButton(icon: ImageVector?, description: String, active: Boolean, dot: androidx.compose.ui.unit.Dp = 0.dp, onClick: () -> Unit) {
    Box(Modifier.size(50.dp).shadow(4.dp, RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp))
        .then(if (active) Modifier.background(Amethyst) else Modifier.background(GoldBrush)).goldBorder(RoundedCornerShape(12.dp), 1.5.dp)
        .clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
        if (icon != null) Icon(icon, null, tint = if (active) GoldPale else Plum)
        else Box(Modifier.size(dot).clip(CircleShape).background(if (active) GoldPale else Plum))
    }
}

