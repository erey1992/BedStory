package com.mamaoca.cuentos

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.RectF
import android.os.Build
import android.util.LruCache
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.caverock.androidsvg.SVG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Cada juego tiene un color joya, que solo se usa en su medallón: el resto es el terciopelo y el oro de la biblioteca
internal val StickerPink = Color(0xFF9B2F63) // rubí
internal val ColoringTeal = Color(0xFF1F6B5B) // esmeralda
internal val PuzzleOrange = Color(0xFF2F4E92) // zafiro
internal val GamesGreen = Color(0xFF1F6B5B)

/** Panel de los juegos: terciopelo oscuro con marco de oro, como los estantes de la biblioteca. */
internal fun Modifier.gamePanel(radius: Dp = 16.dp): Modifier =
    this.shadow(8.dp, RoundedCornerShape(radius)).clip(RoundedCornerShape(radius)).velvet(Plum).goldFrame(radius, 1.5.dp)

/** Marco de cuadro dorado para el lienzo de un juego. */
internal fun Modifier.pictureFrame(): Modifier =
    this.shadow(14.dp, RoundedCornerShape(10.dp)).clip(RoundedCornerShape(10.dp)).goldFrame(10.dp, 3.dp)

/** Medallón redondo con el icono del juego en su color joya. */
@Composable
internal fun GameMedal(icon: ImageVector, color: Color, size: Dp) {
    Box(Modifier.size(size).shadow(6.dp, CircleShape).clip(CircleShape).velvet(color).goldBorder(CircleShape, 2.dp),
        contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = GoldPale, modifier = Modifier.fillMaxSize(0.56f))
    }
}

/** Grupo de pestañas como el de «Todos / Favoritos». */
@Composable
internal fun GoldTabs(labels: List<String>, selected: Int, modifier: Modifier = Modifier, fill: Boolean = false, onSelect: (Int) -> Unit) {
    Row(modifier.clip(RoundedCornerShape(10.dp)).background(Color(0x55000000)).goldBorder(RoundedCornerShape(10.dp), 1.dp).padding(3.dp)) {
        labels.forEachIndexed { i, label ->
            Box(Modifier.weight(1f, fill = fill).height(36.dp).clip(RoundedCornerShape(8.dp))
                .then(if (i == selected) Modifier.background(GoldBrush) else Modifier)
                .clickable(role = Role.Tab) { onSelect(i) }.padding(horizontal = if (fill) 4.dp else 10.dp), contentAlignment = Alignment.Center) {
                Text(label, color = if (i == selected) Plum else GoldPale, fontFamily = Fraunces, fontWeight = FontWeight.SemiBold,
                    fontSize = if (fill) 13.sp else 15.sp, maxLines = 1)
            }
        }
    }
}

/** Calcomanías: el dibujo vectorial se pinta una vez, se recorta el espacio vacío y se guarda en memoria. */
object StickerArt {
    private val cache = object : LruCache<String, Bitmap>(40 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    @Synchronized fun bitmap(context: Context, reward: Reward, maxSide: Int = 420): Bitmap = cache.get(reward.id)
        ?: (reward.svg?.let { render(it, maxSide) }
            ?: reward.image!!.takeIf { it.startsWith("file:") }?.let { android.graphics.BitmapFactory.decodeFile(it.removePrefix("file:")) }
            ?: context.assets.open(reward.image!!).use { android.graphics.BitmapFactory.decodeStream(it) })
            .also { cache.put(reward.id, it) }

    /** Olvida un dibujo guardado (el personaje, cuando el niño lo cambia). */
    @Synchronized fun forget(id: String) { cache.remove(id) }

    private fun render(xml: String, maxSide: Int): Bitmap {
        val svg = SVG.getFromString(xml)
        val box = svg.documentViewBox ?: RectF(0f, 0f, 100f, 100f)
        val scale = 2f * maxSide / maxOf(box.width(), box.height())
        val full = Bitmap.createBitmap((box.width() * scale).toInt(), (box.height() * scale).toInt(), Bitmap.Config.ARGB_8888)
        svg.renderToCanvas(android.graphics.Canvas(full), RectF(0f, 0f, full.width.toFloat(), full.height.toFloat()))
        // recorta lo transparente alrededor del dibujo
        val w = full.width; val h = full.height
        val px = IntArray(w * h).also { full.getPixels(it, 0, w, 0, 0, w, h) }
        var left = w; var top = h; var right = -1; var bottom = -1
        for (y in 0 until h) for (x in 0 until w) if ((px[y * w + x] ushr 24) > 10) {
            if (x < left) left = x; if (x > right) right = x; if (y < top) top = y; if (y > bottom) bottom = y
        }
        if (right < 0) return full
        val pad = 6
        left = maxOf(0, left - pad); top = maxOf(0, top - pad); right = minOf(w - 1, right + pad); bottom = minOf(h - 1, bottom + pad)
        val cut = Bitmap.createBitmap(full, left, top, right - left + 1, bottom - top + 1)
        val f = maxSide.toFloat() / maxOf(cut.width, cut.height)
        return if (f < 1f) Bitmap.createScaledBitmap(cut, (cut.width * f).toInt().coerceAtLeast(1), (cut.height * f).toInt().coerceAtLeast(1), true) else cut
    }
}

/** La imagen de un premio, cargada en segundo plano. */
@Composable
internal fun rememberRewardImage(reward: Reward?): ImageBitmap? {
    val context = LocalContext.current.applicationContext
    val bitmap by produceState<ImageBitmap?>(null, reward?.id) {
        value = reward?.let { r ->
            withContext(Dispatchers.Default) {
                runCatching { if (r.kind == RewardKind.Sticker) StickerArt.bitmap(context, r).asImageBitmap() else ArtLoader.load(context, r.image!!) }.getOrNull()
            }
        }
    }
    return bitmap
}

@Composable
internal fun RewardThumb(reward: Reward, modifier: Modifier = Modifier) {
    val image = rememberRewardImage(reward)
    val shape = RoundedCornerShape(12.dp)
    Box(modifier.shadow(3.dp, shape).clip(shape).background(Parchment).goldBorder(shape, 1.5.dp), contentAlignment = Alignment.Center) {
        image?.let {
            Image(it, reward.title, Modifier.fillMaxSize().padding(if (reward.kind == RewardKind.Sticker) 6.dp else 0.dp),
                contentScale = if (reward.kind == RewardKind.Sticker) ContentScale.Fit else ContentScale.Crop)
        }
    }
}

/** Botón redondo dorado de las barras de los juegos. */
@Composable
internal fun GoldCircleButton(icon: ImageVector, description: String, active: Boolean = false, size: Dp = 44.dp, onClick: () -> Unit) {
    Box(Modifier.size(size).shadow(4.dp, CircleShape).clip(CircleShape)
        .then(if (active) Modifier.background(Amethyst) else Modifier.background(GoldBrush)).goldBorder(CircleShape, 1.5.dp)
        .clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = if (active) GoldPale else Plum)
    }
}

/** Botón con icono y texto: «Guardar», «WhatsApp»... El principal va en oro; los demás, en terciopelo. */
@Composable
internal fun PillAction(icon: ImageVector, label: String, color: Color = Velvet, gold: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(Modifier.height(44.dp).shadow(4.dp, shape).clip(shape).then(if (gold) Modifier.background(GoldBrush) else Modifier.velvet(color))
        .goldBorder(shape, 1.5.dp).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, null, tint = if (gold) Plum else GoldLeaf, modifier = Modifier.size(22.dp))
        Text(label, color = if (gold) Plum else GoldPale, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

/** Barra de arriba de cada juego: la misma cornisa de terciopelo de la biblioteca, con el medallón del juego. */
@Composable
internal fun GameHeader(title: String, color: Color, onBack: () -> Unit, icon: ImageVector? = null, actions: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().velvet(Plum)
        .drawBehind { drawRect(GoldStroke, Offset(0f, size.height - 2.dp.toPx()), Size(size.width, 2.dp.toPx())) }
        .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
        .padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        GoldCircleButton(Icons.AutoMirrored.Filled.ArrowBack, "Volver", onClick = onBack)
        if (icon != null) GameMedal(icon, color, 40.dp)
        Text(title, style = TextStyle(brush = GoldBrush, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 24.sp),
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        actions()
    }
}

/** Botones de guardar en la galería y enviar por WhatsApp; [render] crea la imagen al tocarlos. */
@Composable
internal fun ShareActions(name: String, render: () -> Bitmap) {
    val context = LocalContext.current
    var pending by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok && pending) ImageShare.save(context, render(), name)
        pending = false
    }
    PillAction(Icons.Default.Download, "Guardar", gold = true) {
        if (needsStoragePermission(context)) { pending = true; permission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE) }
        else ImageShare.save(context, render(), name)
    }
    PillAction(Icons.Default.Share, "WhatsApp") { ImageShare.share(context, render(), name) }
}

private fun needsStoragePermission(context: Context) = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
    ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED

// ---------------- Menú de juegos ----------------

@Composable
fun GamesScreen(vm: StoryViewModel, play: () -> Unit) {
    val owned = vm.rewards.all.filter { it.id in vm.ownedRewards }
    fun count(kind: RewardKind) = owned.count { it.kind == kind }
    Column(Modifier.fillMaxSize().velvet(PlumLight)) {
        GameHeader("Juegos", GamesGreen, onBack = { play(); vm.back() }, icon = Icons.Default.SportsEsports)
        BoxWithConstraints(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            val cardW = minOf(300.dp, (maxWidth - 40.dp) / 3)
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    GameCard("Calcomanías", "${count(RewardKind.Sticker)} calcomanías · ${count(RewardKind.Background)} fondos",
                        Icons.Default.EmojiEmotions, StickerPink, cardW) { play(); vm.openGame("stickers") }
                    GameCard("Colorear", "${count(RewardKind.Coloring)} dibujos", Icons.Default.Palette, ColoringTeal, cardW) {
                        play(); vm.openGame("coloringList")
                    }
                    GameCard("Rompecabezas", "${count(RewardKind.Puzzle)} rompecabezas", Icons.Default.Extension, PuzzleOrange, cardW) {
                        play(); vm.openGame("puzzleList")
                    }
                }
                Text("✨ Lee cuentos para descubrir sorpresas nuevas ✨", color = GoldPale, fontFamily = Andika, fontSize = 17.sp,
                    textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun GameCard(title: String, subtitle: String, icon: ImageVector, color: Color, width: Dp, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Column(Modifier.width(width).height(width * 1.05f).shadow(12.dp, shape).clip(shape).paper().goldFrame(22.dp, 2.5.dp)
        .clickable(role = Role.Button, onClickLabel = title, onClick = onClick).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        GameMedal(icon, color, width * 0.42f)
        Text(title, color = Plum, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 26.sp, modifier = Modifier.padding(top = 14.dp))
        Text(subtitle, color = Color(0xFF6B5446), fontFamily = Andika, fontSize = 14.sp, textAlign = TextAlign.Center)
    }
}

/** Lista de dibujos o rompecabezas desbloqueados (solo se ven los que ya se tienen). */
@Composable
fun RewardListScreen(vm: StoryViewModel, kind: RewardKind, play: () -> Unit) {
    val (title, color, next) = when (kind) {
        RewardKind.Coloring -> Triple("Colorear", ColoringTeal, "coloring")
        else -> Triple("Rompecabezas", PuzzleOrange, "puzzle")
    }
    val icon = if (kind == RewardKind.Coloring) Icons.Default.Palette else Icons.Default.Extension
    val items = vm.rewards.of(kind).filter { it.id in vm.ownedRewards }
    Column(Modifier.fillMaxSize().velvet(PlumLight)) {
        GameHeader(title, color, onBack = { play(); vm.back() }, icon = icon)
        if (items.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Todavía no tienes ninguno.\nSigue leyendo: ¡pronto llegará una sorpresa!", color = GoldPale, fontFamily = Andika,
                fontSize = 20.sp, textAlign = TextAlign.Center)
        } else LazyVerticalGrid(GridCells.Adaptive(220.dp), Modifier.windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)),
            contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(items, key = { it.id }) { r ->
                Column(Modifier.clip(RoundedCornerShape(14.dp)).clickable(role = Role.Button, onClickLabel = r.title) { play(); vm.openGame(next, r.id) },
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Box {
                        RewardThumb(r, Modifier.fillMaxWidth().aspectRatio(16f / 9f).shadow(6.dp, RoundedCornerShape(12.dp)))
                        if (kind == RewardKind.Puzzle) vm.puzzleMedal(r.id)?.let {
                            MedalBadge(it, Modifier.align(Alignment.TopEnd).padding(6.dp).size(44.dp))
                        }
                    }
                    Text(r.title.orEmpty(), color = GoldPale, fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
    }
}

// ---------------- Celebración de premios ----------------

private fun summary(rewards: List<Reward>): String {
    val parts = rewards.groupingBy { it.kind }.eachCount().map { (kind, n) ->
        when (kind) {
            RewardKind.Sticker -> if (n == 1) "una calcomanía" else "$n calcomanías"
            RewardKind.Background -> if (n == 1) "un fondo" else "$n fondos"
            RewardKind.Coloring -> if (n == 1) "un dibujo para colorear" else "$n dibujos para colorear"
            RewardKind.Puzzle -> if (n == 1) "un rompecabezas" else "$n rompecabezas"
        }
    }
    return if (parts.size == 1) parts[0] else parts.dropLast(1).joinToString(", ") + " y " + parts.last()
}

/** Un regalo que se abre: muestra los premios nuevos entre confeti. */
@Composable
fun RewardCelebration(batch: RewardBatch, sound: Boolean, onDismiss: () -> Unit, onPlay: () -> Unit) {
    val pop = remember(batch) { Animatable(0f) }
    LaunchedEffect(batch) {
        if (sound) Sfx.play(Sound.Fanfare)
        pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 220f))
    }
    BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xCC12062A))
        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center) {
        val fit = minOf(1f, maxHeight / 430.dp, maxWidth / 620.dp)
        Confetti(Modifier.fillMaxSize())
        Column(Modifier.wrapContentSize(unbounded = true)
            .graphicsLayer { scaleX = pop.value * fit; scaleY = pop.value * fit; alpha = pop.value.coerceIn(0f, 1f) }
            .widthIn(max = 600.dp).shadow(16.dp, RoundedCornerShape(22.dp)).clip(RoundedCornerShape(22.dp))
            .velvet(Velvet).goldFrame(22.dp, 2.5.dp).padding(horizontal = 28.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            val single = batch.rewards.singleOrNull()
            if (single != null) {
                // un solo premio: se ve grande, con lo que es y de qué cuento
                Text(batch.title ?: "¡GANASTE UN PREMIO!", style = TextStyle(brush = GoldBrush, fontFamily = Fraunces, fontWeight = FontWeight.Bold,
                    fontSize = 16.sp, letterSpacing = 2.sp, textAlign = TextAlign.Center))
                RewardThumb(single, Modifier.padding(vertical = 12.dp)
                    .size(if (single.kind == RewardKind.Sticker) 170.dp else 260.dp, if (single.kind == RewardKind.Sticker) 170.dp else 146.dp))
                Text(when (single.kind) {
                    RewardKind.Sticker -> "¡Una calcomanía nueva!"
                    RewardKind.Background -> "¡Un fondo nuevo para tus calcomanías!"
                    RewardKind.Coloring -> "¡Un dibujo para colorear!"
                    RewardKind.Puzzle -> "¡Un rompecabezas nuevo!"
                }, style = TextStyle(brush = GoldBrush, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 26.sp, textAlign = TextAlign.Center))
                if (single.story != null && single.title != null) Text("de «${single.title}»", color = GoldPale, fontFamily = Andika,
                    fontSize = 18.sp, textAlign = TextAlign.Center)
            } else {
            Icon(Icons.Default.CardGiftcard, null, tint = GoldLeaf, modifier = Modifier.size(54.dp))
            Text(batch.title ?: "¡SORPRESA!", style = TextStyle(brush = GoldBrush, fontFamily = Fraunces, fontWeight = FontWeight.Bold,
                fontSize = 24.sp, textAlign = TextAlign.Center))
            Text("Desbloqueaste ${summary(batch.rewards)}", color = GoldPale, fontFamily = Andika, fontSize = 18.sp,
                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                batch.rewards.take(4).forEach { RewardThumb(it, Modifier.size(width = 96.dp, height = 72.dp)) }
                if (batch.rewards.size > 4) Box(Modifier.size(width = 72.dp, height = 72.dp).clip(RoundedCornerShape(12.dp))
                    .background(Color(0x33FFFFFF)), contentAlignment = Alignment.Center) {
                    Text("+${batch.rewards.size - 4}", color = GoldPale, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                }
            }
            }
            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PillAction(Icons.Default.Check, "¡Genial!", gold = true, onClick = onDismiss)
                PillAction(Icons.Default.SportsEsports, "Ir a jugar", Plum, onClick = onPlay)
            }
        }
    }
}
