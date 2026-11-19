package com.mamaoca.cuentos

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val RATIO = 16f / 9f

/** Dibuja una diapositiva: se usa igual en pantalla y al guardar la imagen, así lo guardado es lo que se ve. */
private fun drawScene(canvas: android.graphics.Canvas, w: Float, h: Float, background: Bitmap?, items: List<PlacedSticker>,
                      bitmap: (String) -> Bitmap?, pose: (PlacedSticker) -> Pose = { Pose() }) {
    if (background != null) {
        val s = maxOf(w / background.width, h / background.height)
        val bw = background.width * s; val bh = background.height * s
        canvas.drawBitmap(background, null, RectF((w - bw) / 2, (h - bh) / 2, (w + bw) / 2, (h + bh) / 2), Paint(Paint.FILTER_BITMAP_FLAG))
    } else {
        val sky = Paint().apply { shader = android.graphics.LinearGradient(0f, 0f, 0f, h, 0xFF9AD0F5.toInt(), 0xFFFFF3D6.toInt(), android.graphics.Shader.TileMode.CLAMP) }
        canvas.drawRect(0f, 0f, w, h, sky)
        canvas.drawRect(0f, h * 0.78f, w, h, Paint().apply { color = 0xFF8BC34A.toInt() })
    }
    val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    for (item in items) {
        val bmp = bitmap(item.rewardId) ?: continue
        val p = pose(item)
        if (p.alpha <= 0.01f || p.scale <= 0.01f) continue
        paint.alpha = (p.alpha.coerceIn(0f, 1f) * 255).toInt()
        canvas.drawBitmap(bmp, matrixFor(item, bmp, w, h, p), paint)
    }
}

private fun matrixFor(item: PlacedSticker, bmp: Bitmap, w: Float, h: Float, pose: Pose = Pose()) = Matrix().apply {
    val s = item.scale * h / bmp.height * pose.scale
    postTranslate(-bmp.width / 2f, -bmp.height / 2f)
    postScale(if (item.flip) -s else s, s)
    postRotate(item.rotation + pose.rot)
    postTranslate((item.x + pose.dx) * w, (item.y + pose.dy) * h)
}

/**
 * Esquina inferior derecha de la calcomanía en el lienzo: ahí va el tirador para cambiarle el tamaño.
 * Si la calcomanía es tan grande que la esquina se sale, el tirador se queda en el borde, a [inset] de distancia.
 */
private fun cornerOf(item: PlacedSticker, bmp: Bitmap, w: Float, h: Float, inset: Float): Offset {
    val pt = floatArrayOf(bmp.width.toFloat(), bmp.height.toFloat())
    matrixFor(item, bmp, w, h).mapPoints(pt)
    return Offset(pt[0].coerceIn(inset, w - inset), pt[1].coerceIn(inset, h - inset))
}

/** ¿Tocó dentro de la calcomanía? Se deshace el giro y la escala del punto tocado. */
private fun hit(item: PlacedSticker, bmp: Bitmap, p: Offset, w: Float, h: Float): Boolean {
    val s = item.scale * h / bmp.height
    val a = -item.rotation * PI.toFloat() / 180f
    val dx = p.x - item.x * w; val dy = p.y - item.y * h
    val lx = (dx * cos(a) - dy * sin(a)) / s; val ly = (dx * sin(a) + dy * cos(a)) / s
    return kotlin.math.abs(lx) <= bmp.width / 2f && kotlin.math.abs(ly) <= bmp.height / 2f
}

private val badgeText = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }
private val badgeFill = Paint(Paint.ANTI_ALIAS_FLAG)

@Composable
fun StickerScreen(vm: StoryViewModel, play: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val story = remember { StickerStory(context) }
    val player = remember { StoryPlayer(context) }
    val recorder = remember { VoiceRecorder(context) }
    // se guarda poco después de cada cambio, por si la app se cierra de golpe
    LaunchedEffect(story) { snapshotFlow { story.encode() }.collectLatest { kotlinx.coroutines.delay(500); withContext(Dispatchers.IO) { story.save() } } }
    DisposableEffect(Unit) { onDispose { recorder.stop(); story.save() } }
    var slideIndex by rememberSaveable { mutableIntStateOf(0) }
    slideIndex = slideIndex.coerceIn(0, story.slides.lastIndex)
    val editing = story.slides[slideIndex]
    val slide = if (player.playing) story.slides[player.slideIndex] else editing
    val owned = vm.rewards.all.filter { it.id in vm.ownedRewards }
    // el personaje del niño va primero
    val stickers = listOfNotNull(vm.avatarReward) + owned.filter { it.kind == RewardKind.Sticker }
    val backgrounds = owned.filter { it.kind == RewardKind.Background }
    val bitmaps = remember { mutableStateMapOf<String, Bitmap>() }
    val scope = rememberCoroutineScope()
    var playJob by remember { mutableStateOf<Job?>(null) }
    // carga los dibujos de las calcomanías y fondos de todas las diapositivas
    val needed = story.slides.flatMap { s -> s.items.map { it.rewardId } + listOfNotNull(s.background) }.toSet()
    LaunchedEffect(needed) {
        // las calcomanías que ya no existen (las dibujadas de antes) se quitan
        story.slides.forEach { s -> s.items.filter { vm.reward(it.rewardId) == null }.forEach(s::remove) }
        withContext(Dispatchers.Default) {
            for (id in needed) if (id !in bitmaps) vm.reward(id)?.let { r ->
                runCatching {
                    bitmaps[id] = if (r.kind == RewardKind.Sticker) StickerArt.bitmap(context, r)
                        else context.assets.open(r.image!!).use { android.graphics.BitmapFactory.decodeStream(it) }
                }
            }
        }
    }
    var selected by remember { mutableStateOf<PlacedSticker?>(null) }
    if (selected != null && selected !in editing.items) selected = null
    var tab by remember { mutableIntStateOf(0) }
    fun render(): Bitmap = Bitmap.createBitmap(1600, 900, Bitmap.Config.ARGB_8888).also {
        drawScene(android.graphics.Canvas(it), 1600f, 900f, editing.background?.let(bitmaps::get), editing.items, bitmaps::get)
    }
    fun stop() { playJob?.cancel(); playJob = null }
    fun preview(step: Step) {
        if (player.playing) return
        playJob?.cancel()
        playJob = scope.launch { player.preview(editing, step, vm.sound) }
    }
    Column(Modifier.fillMaxSize().velvet(PlumLight)) {
        GameHeader("Calcomanías", StickerPink, onBack = { stop(); play(); vm.back() }, icon = Icons.Default.EmojiEmotions) {
            if (player.playing) PillAction(Icons.Default.Stop, "Detener", gold = true) { stop() }
            else {
                PillAction(Icons.Default.PlayArrow, "Reproducir", gold = true) {
                    selected = null
                    playJob?.cancel()
                    playJob = scope.launch { player.playAll(story, vm.sound) }
                }
                GoldCircleButton(Icons.Default.DeleteSweep, "Vaciar esta diapositiva") {
                    play(); editing.items.clear(); editing.steps.clear(); selected = null
                }
                ShareActions("calcomanias") { selected = null; render() }
            }
        }
        Row(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)).padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val fitW = minOf(maxWidth, maxHeight * RATIO)
                    Box(Modifier.size(fitW, fitW / RATIO)) {
                        Canvas(Modifier.fillMaxSize().pictureFrame()
                            .pointerInput(editing, player.playing) {
                                if (player.playing) return@pointerInput
                                awaitEachGesture {
                                    val down = awaitFirstDown()
                                    val w = size.width.toFloat(); val h = size.height.toFloat()
                                    // arrastrar el tirador de la esquina agranda o achica la calcomanía elegida
                                    val current = selected
                                    val currentBmp = current?.let { bitmaps[it.rewardId] }
                                    if (current != null && currentBmp != null &&
                                        (down.position - cornerOf(current, currentBmp, w, h, 20.dp.toPx())).getDistance() < 34.dp.toPx()) {
                                        val center = Offset(current.x * w, current.y * h)
                                        val d0 = (down.position - center).getDistance().coerceAtLeast(1f)
                                        val s0 = current.scale
                                        do {
                                            val event = awaitPointerEvent()
                                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                            current.scale = (s0 * (change.position - center).getDistance() / d0).coerceIn(0.08f, 1.6f)
                                            event.changes.forEach { it.consume() }
                                        } while (event.changes.any { it.pressed })
                                        return@awaitEachGesture
                                    }
                                    val target = editing.items.lastOrNull { item -> bitmaps[item.rewardId]?.let { hit(item, it, down.position, w, h) } == true }
                                    selected = target
                                    if (target != null) {
                                        // al tocarla, pasa adelante
                                        editing.items.remove(target); editing.items.add(target)
                                        do {
                                            val event = awaitPointerEvent()
                                            val pan = event.calculatePan(); val zoom = event.calculateZoom(); val rot = event.calculateRotation()
                                            target.x = (target.x + pan.x / w).coerceIn(0f, 1f)
                                            target.y = (target.y + pan.y / h).coerceIn(0f, 1f)
                                            target.scale = (target.scale * zoom).coerceIn(0.08f, 1.6f)
                                            target.rotation += rot
                                            event.changes.forEach { it.consume() }
                                        } while (event.changes.any { it.pressed })
                                    }
                                }
                            }) {
                            drawIntoCanvas { c ->
                                drawScene(c.nativeCanvas, size.width, size.height, slide.background?.let(bitmaps::get), slide.items, { bitmaps[it] }) {
                                    player.poses[it.uid] ?: Pose()
                                }
                            }
                            if (player.playing) return@Canvas
                            // números de los pasos de cada calcomanía, en su esquina de arriba
                            val r = 12.dp.toPx()
                            badgeText.textSize = 13.dp.toPx()
                            editing.items.forEach { item ->
                                val bmp = bitmaps[item.rewardId] ?: return@forEach
                                val numbers = editing.steps.withIndex().filter { it.value.itemUid == item.uid }
                                if (numbers.isEmpty()) return@forEach
                                val pt = floatArrayOf(0f, 0f, bmp.width.toFloat(), 0f)
                                matrixFor(item, bmp, size.width, size.height).mapPoints(pt)
                                // encima de la calcomanía, sin tapar su marco
                                var x = minOf(pt[0], pt[2]) + r; val y = (minOf(pt[1], pt[3]) - r - 6.dp.toPx()).coerceIn(r, size.height - r)
                                x = x.coerceIn(r, size.width - r)
                                numbers.forEach { (i, step) ->
                                    val now = player.currentStep?.uid == step.uid
                                    drawIntoCanvas { c ->
                                        badgeFill.color = if (now) StickerPink.toArgb() else Plum.toArgb()
                                        c.nativeCanvas.drawCircle(x, y, r, badgeFill)
                                        badgeFill.color = GoldLeaf.toArgb()
                                        badgeFill.style = Paint.Style.STROKE; badgeFill.strokeWidth = 2.dp.toPx()
                                        c.nativeCanvas.drawCircle(x, y, r, badgeFill)
                                        badgeFill.style = Paint.Style.FILL
                                        badgeText.color = GoldPale.toArgb()
                                        c.nativeCanvas.drawText("${i + 1}", x, y + badgeText.textSize * 0.36f, badgeText)
                                    }
                                    x += r * 2.2f
                                }
                            }
                            // marco punteado de la calcomanía elegida
                            val sel = selected
                            val bmp = sel?.let { bitmaps[it.rewardId] }
                            if (sel != null && bmp != null && player.poses.isEmpty()) {
                                val m = matrixFor(sel, bmp, size.width, size.height)
                                val pts = floatArrayOf(0f, 0f, bmp.width.toFloat(), 0f, bmp.width.toFloat(), bmp.height.toFloat(), 0f, bmp.height.toFloat())
                                m.mapPoints(pts)
                                val path = Path().apply { moveTo(pts[0], pts[1]); lineTo(pts[2], pts[3]); lineTo(pts[4], pts[5]); lineTo(pts[6], pts[7]); close() }
                                drawPath(path, GoldPale, style = Stroke(3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f))))
                                // tirador redondo con una flecha doble en diagonal
                                val c = cornerOf(sel, bmp, size.width, size.height, 20.dp.toPx())
                                val hr = 17.dp.toPx()
                                drawCircle(Color(0x55000000), hr + 2.dp.toPx(), c + Offset(0f, 2.dp.toPx()))
                                drawCircle(GoldLeaf, hr, c)
                                drawCircle(Plum, hr, c, style = Stroke(2.dp.toPx()))
                                val a = hr * 0.45f
                                val arrow = Stroke(2.5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                                drawLine(Plum, c - Offset(a, a), c + Offset(a, a), arrow.width, arrow.cap)
                                listOf(-1f, 1f).forEach { d ->
                                    val tip = c + Offset(a, a) * d
                                    drawLine(Plum, tip, tip - Offset(a * 0.8f, 0f) * d, arrow.width, arrow.cap)
                                    drawLine(Plum, tip, tip - Offset(0f, a * 0.8f) * d, arrow.width, arrow.cap)
                                }
                            }
                        }
                        if (player.playing) Text("Diapositiva ${player.slideIndex + 1} de ${story.slides.size}", color = GoldPale,
                            fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                            modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp).clip(RoundedCornerShape(8.dp))
                                .background(Color(0x99241046)).padding(horizontal = 10.dp, vertical = 4.dp))
                        if (!player.playing) selected?.let { sel ->
                            Row(Modifier.align(Alignment.TopCenter).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                GoldCircleButton(Icons.Default.AutoAwesome, "Animar y poner sonidos", active = tab == 2, size = 40.dp) { play(); tab = 2 }
                                GoldCircleButton(Icons.Default.ZoomIn, "Más grande", size = 40.dp) { sel.scale = (sel.scale * 1.2f).coerceAtMost(1.6f) }
                                GoldCircleButton(Icons.Default.ZoomOut, "Más pequeña", size = 40.dp) { sel.scale = (sel.scale / 1.2f).coerceAtLeast(0.08f) }
                                GoldCircleButton(Icons.Default.RotateRight, "Girar", size = 40.dp) { sel.rotation += 15f }
                                GoldCircleButton(Icons.Default.Flip, "Dar la vuelta", size = 40.dp) { sel.flip = !sel.flip }
                                GoldCircleButton(Icons.Default.Delete, "Quitar", size = 40.dp) { editing.remove(sel); selected = null }
                            }
                        }
                    }
                }
                if (!player.playing) SlideStrip(story, slideIndex, play, onSelect = { slideIndex = it; selected = null })
            }
            if (!player.playing) Column(Modifier.width(320.dp).fillMaxHeight().gamePanel().padding(10.dp)) {
                GoldTabs(listOf("Calcomanías", "Fondos", "✨ Animar"), tab, Modifier.fillMaxWidth(), fill = true) { play(); tab = it }
                Spacer(Modifier.height(10.dp))
                when (tab) {
                    0 -> LazyVerticalGrid(GridCells.Fixed(3), verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (stickers.isEmpty()) item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                            Text("Termina un cuento para ganar tu primera calcomanía.", color = GoldPale, fontFamily = Andika, fontSize = 15.sp)
                        }
                        items(stickers, key = { it.id }) { r ->
                            RewardThumb(r, Modifier.aspectRatio(1f).clickable(role = Role.Button) {
                                play()
                                scope.launch {
                                    withContext(Dispatchers.Default) { bitmaps.getOrPut(r.id) { StickerArt.bitmap(context, r) } }
                                    val item = PlacedSticker(r.id, 0.4f + 0.2f * kotlin.random.Random.nextFloat(), 0.45f + 0.2f * kotlin.random.Random.nextFloat(), 0.32f)
                                    editing.items.add(item); selected = item
                                }
                            })
                        }
                    }
                    1 -> LazyVerticalGrid(GridCells.Fixed(2), verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            Box(Modifier.aspectRatio(16f / 9f).shadow(3.dp, RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp))
                                .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFF9AD0F5), Color(0xFFFFF3D6))))
                                .goldBorder(RoundedCornerShape(12.dp), if (editing.background == null) 3.dp else 1.dp)
                                .clickable(role = Role.Button) { play(); editing.background = null }, contentAlignment = Alignment.Center) {
                                Text("Cielo", color = Plum, fontFamily = Fraunces, fontWeight = FontWeight.Bold)
                            }
                        }
                        items(backgrounds, key = { it.id }) { r ->
                            RewardThumb(r, Modifier.aspectRatio(16f / 9f).clickable(role = Role.Button) { play(); editing.background = r.id })
                        }
                    }
                    else -> AnimatePanel(editing, selected, recorder, vm, play, ::preview)
                }
            }
        }
    }
}

/** Tira de diapositivas: elegir, añadir una nueva, copiar la actual (con sus calcomanías) o borrarla. */
@Composable
private fun SlideStrip(story: StickerStory, current: Int, play: () -> Unit, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().height(48.dp).gamePanel(12.dp).padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Diapositivas", color = GoldPale, fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            story.slides.indices.forEach { i ->
                Box(Modifier.size(width = 44.dp, height = 34.dp).clip(RoundedCornerShape(8.dp))
                    .then(if (i == current) Modifier.background(GoldBrush) else Modifier.background(Color(0x33FFFFFF)))
                    .goldBorder(RoundedCornerShape(8.dp), 1.dp).clickable(role = Role.Tab) { play(); onSelect(i) }
                    .semantics { contentDescription = "Diapositiva ${i + 1}" }, contentAlignment = Alignment.Center) {
                    Text("${i + 1}", color = if (i == current) Plum else GoldPale, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            }
        }
        StripButton(Icons.Default.Add, "Nueva diapositiva") {
            play(); story.slides.add(current + 1, Slide(story.slides[current].background)); onSelect(current + 1)
        }
        StripButton(Icons.Default.ContentCopy, "Copiar esta diapositiva") {
            play()
            val src = story.slides[current]
            story.slides.add(current + 1, Slide(src.background).apply {
                src.items.forEach { items.add(PlacedSticker(it.rewardId, it.x, it.y, it.scale, it.rotation, it.flip)) }
            })
            onSelect(current + 1)
        }
        if (story.slides.size > 1) StripButton(Icons.Default.Delete, "Borrar esta diapositiva") {
            play(); story.slides.removeAt(current); onSelect((current - 1).coerceAtLeast(0))
        }
    }
}

@Composable
private fun StripButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    Box(Modifier.size(34.dp).clip(CircleShape).background(GoldBrush).goldBorder(CircleShape, 1.dp)
        .clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = Plum, modifier = Modifier.size(20.dp))
    }
}

/** Panel «Animar»: los pasos de la calcomanía elegida (con su número) y los botones para añadir animaciones y sonidos. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AnimatePanel(slide: Slide, item: PlacedSticker?, recorder: VoiceRecorder, vm: StoryViewModel, play: () -> Unit,
                         preview: (Step) -> Unit) {
    val context = LocalContext.current
    if (item == null) {
        Text("Toca una calcomanía del dibujo para darle animaciones y sonidos.\n\nCada animación lleva un número: al tocar «Reproducir», pasan en ese orden.",
            color = GoldPale, fontFamily = Andika, fontSize = 16.sp)
        return
    }
    fun add(step: Step) { play(); slide.steps.add(step); preview(step) }
    fun finishRecording() = recorder.stop()?.let { add(Step(item.uid, sound = "rec:$it")) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> if (ok) recorder.start() }
    DisposableEffect(recorder) {
        recorder.onAutoStop = { name -> name?.let { slide.steps.add(Step(item.uid, sound = "rec:$it")) } }
        onDispose { recorder.onAutoStop = null }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Lo que hace esta calcomanía:", color = GoldLeaf, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        val mine = slide.steps.withIndex().filter { it.value.itemUid == item.uid }
        if (mine.isEmpty()) Text("Todavía nada. ¡Elige abajo!", color = GoldPale, fontFamily = Andika, fontSize = 14.sp)
        mine.forEach { (i, step) ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0x33FFFFFF)).padding(4.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.size(26.dp).clip(CircleShape).background(GoldBrush), contentAlignment = Alignment.Center) {
                    Text("${i + 1}", color = Plum, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Text(step.label, color = GoldPale, fontFamily = Andika, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).clickable { preview(step) })
                SmallIcon(Icons.Default.KeyboardArrowUp, "Antes") { if (i > 0) { slide.steps.removeAt(i); slide.steps.add(i - 1, step) } }
                SmallIcon(Icons.Default.KeyboardArrowDown, "Después") { if (i < slide.steps.lastIndex) { slide.steps.removeAt(i); slide.steps.add(i + 1, step) } }
                SmallIcon(Icons.Default.Close, "Quitar") { slide.steps.remove(step) }
            }
        }
        EffectGroup.entries.forEach { group ->
            Text(group.label, color = GoldLeaf, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(top = 6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Effect.entries.filter { it.group == group }.forEach { fx -> Chip(fx.label) { add(Step(item.uid, effect = fx)) } }
            }
        }
        Text("Sonido", color = GoldLeaf, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(top = 6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            STORY_SOUNDS.forEach { (key, value) -> Chip(value.first) { add(Step(item.uid, sound = "sfx:$key")) } }
        }
        val shape = RoundedCornerShape(12.dp)
        Row(Modifier.fillMaxWidth().height(46.dp).clip(shape)
            .then(if (recorder.recording) Modifier.background(Color(0xFFC62828)) else Modifier.background(GoldBrush)).goldBorder(shape, 1.5.dp)
            .clickable(role = Role.Button) {
                when {
                    recorder.recording -> finishRecording()
                    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED -> recorder.start()
                    else -> permission.launch(Manifest.permission.RECORD_AUDIO)
                }
            }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(if (recorder.recording) Icons.Default.Stop else Icons.Default.Mic, null, tint = if (recorder.recording) Color.White else Plum)
            Text(if (recorder.recording) "  Terminar de grabar" else "  Grabar mi voz", color = if (recorder.recording) Color.White else Plum,
                fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        if (recorder.recording) Text("Grabando... (hasta 10 segundos)", color = Color(0xFFFFB4A8), fontFamily = Andika, fontSize = 13.sp)
    }
}

@Composable
private fun Chip(label: String, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(10.dp)).background(Parchment).goldBorder(RoundedCornerShape(10.dp), 1.dp)
        .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 8.dp, vertical = 6.dp)) {
        Text(label, color = Plum, fontFamily = Andika, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun SmallIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    Box(Modifier.size(28.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = description },
        contentAlignment = Alignment.Center) { Icon(icon, null, tint = GoldPale, modifier = Modifier.size(20.dp)) }
}
