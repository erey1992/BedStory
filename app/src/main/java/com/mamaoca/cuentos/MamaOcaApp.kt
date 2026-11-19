@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.ui.text.ExperimentalTextApi::class)

package com.mamaoca.cuentos

import android.content.Intent
import android.speech.tts.TextToSpeech
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

private val Night = Color(0xFF241046)
private val NightDeep = Color(0xFF170A30)
private val Cream = Color(0xFFFFFAF0)
private val Paper2 = Color(0xFFF6EAD2)
private val Gold = Color(0xFFFFD166)
private val Purple = Color(0xFF7B3FA0)
private val PurpleLight = Color(0xFFA35CC9)
private val Ink = Color(0xFF3A2A22)
private val InkSoft = Color(0xFF6B5446)
private val HeartRed = Color(0xFFFF4D6D)

private fun weighted(res: Int, vararg weights: Int) = weights.map {
    Font(res, FontWeight(it), variationSettings = FontVariation.Settings(FontVariation.weight(it)))
}
private val Fredoka = FontFamily(weighted(R.font.fredoka, 400, 500, 600, 700))
private val Nunito = FontFamily(weighted(R.font.nunito, 400, 600, 700, 800))
/** Andika: letra de molde pensada para niños que aprenden a leer. */
internal val Andika = FontFamily(Font(R.font.andika_regular, FontWeight.Normal), Font(R.font.andika_bold, FontWeight.Bold),
    Font(R.font.andika_italic, FontWeight.Normal, FontStyle.Italic))

internal fun storyColor(story: Story) = Color(android.graphics.Color.parseColor(story.color))
private val speakerPalette = listOf(0xFFE63946, 0xFF2A9D8F, 0xFF7B3FA0, 0xFFE76F51, 0xFF3A86FF, 0xFFC77D00, 0xFFD6336C, 0xFF2B8A3E).map { Color(it) }
private fun speakerColor(name: String): Color {
    var h = 0L
    name.searchKey().forEach { h = (h * 31 + it.code) and 0xFFFFFFFFL }
    return speakerPalette[(h % speakerPalette.size).toInt()]
}

@Composable
fun MamaOcaApp(vm: StoryViewModel = viewModel()) {
    val context = LocalContext.current
    val narrator = remember { Narrator(context) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var foreground by remember { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) { foreground = false; vm.stopListening(); narrator.stop() }
            if (event == Lifecycle.Event.ON_RESUME) { foreground = true; narrator.refreshVoices() }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); narrator.close() }
    }
    LaunchedEffect(vm.screen, vm.page, vm.storyId, vm.listening, narrator.ready) {
        narrator.stop()
        val story = vm.story
        if (vm.listening && narrator.ready && vm.screen == "reader" && story != null) {
            val spokenPage = vm.page
            delay(900) // deja terminar el giro de página antes de leer
            val text = if (spokenPage == 0) listOf(Segment(null, story.title), Segment(null, story.description))
                else story.pages[spokenPage - 1].text
            narrator.speak(text) {
                if (vm.listening && vm.screen == "reader" && vm.storyId == story.id && vm.page == spokenPage) vm.next()
            }
        }
    }
    LaunchedEffect(narrator.message) { if (narrator.message != null) vm.stopListening() }
    val play: () -> Unit = { if (vm.sound) Sfx.play(Sound.Pop) }
    // durante las preguntas no se interrumpe con celebraciones: esperan al final
    val celebrating = vm.screen != "quiz" && (vm.celebrations.isNotEmpty() || vm.rewardCelebrations.isNotEmpty())
    BackHandler(enabled = vm.screen != "home" || celebrating) {
        when {
            celebrating && vm.celebrations.isNotEmpty() -> vm.dismissCelebration()
            celebrating -> vm.dismissReward()
            else -> vm.back()
        }
    }
    MaterialTheme(colorScheme = lightColorScheme(primary = Velvet, secondary = Gold, background = Cream, surface = Cream)) {
        CompositionLocalProvider(LocalTextStyle provides TextStyle(fontFamily = Nunito, fontWeight = FontWeight.SemiBold, color = Ink)) {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Night, NightDeep)))) {
                Box(Modifier.fillMaxSize()) {
                    when {
                        vm.error != null -> Text(vm.error!!, Modifier.align(Alignment.Center), color = Cream)
                        vm.stories.isEmpty() -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = Gold)
                        else -> AnimatedContent(targetState = vm.screen, label = "Pantalla",
                            transitionSpec = { fadeIn(tween(450)) togetherWith fadeOut(tween(300)) }) { screen ->
                            when (screen) {
                                "home" -> Cover(foreground, onStart = { if (vm.sound) Sfx.play(Sound.Pop); vm.navigate("library") },
                                    onGames = { if (vm.sound) Sfx.play(Sound.Pop); vm.navigate("games") })
                                "library" -> Library(vm, play)
                                "reader" -> vm.story?.let { Reader(vm, it, narrator, foreground, play) }
                                "finish" -> vm.story?.let { Finish(vm, it, play) }
                                "achievements" -> AchievementsScreen(vm, play)
                                "quiz" -> vm.story?.let { QuizScreen(vm, it, narrator, play) }
                                "games" -> GamesScreen(vm, play)
                                "stickers" -> StickerScreen(vm, play)
                                "coloringList" -> RewardListScreen(vm, RewardKind.Coloring, play)
                                "puzzleList" -> RewardListScreen(vm, RewardKind.Puzzle, play)
                                "coloring" -> vm.rewards.byId[vm.gameItem]?.let { ColoringScreen(vm, it, play) }
                                "puzzle" -> vm.rewards.byId[vm.gameItem]?.let { PuzzleScreen(vm, it, play) }
                                "parents" -> ParentsScreen(vm, play)
                                "profile" -> ProfileScreen(vm, play)
                            }
                        }
                    }
                    // cada logro nuevo se celebra encima de la pantalla que sea, uno tras otro
                    if (vm.screen != "quiz") {
                        val achievement = vm.celebrations.firstOrNull()
                        if (achievement != null) AchievementCelebration(achievement, vm.sound) { play(); vm.dismissCelebration() }
                        else vm.rewardCelebrations.firstOrNull()?.let { batch ->
                            RewardCelebration(batch, vm.sound, onDismiss = { play(); vm.dismissReward() },
                                onPlay = { play(); vm.dismissReward(); vm.navigate("games") })
                        }
                    }
                }
            }
        }
    }
}

// ---------------- Portada ----------------

@Composable
private fun Cover(animated: Boolean, onStart: () -> Unit, onGames: () -> Unit) {
    val intro = remember { Animatable(0f) }
    val button = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { intro.animateTo(1f, tween(2200, easing = CubicBezierEasing(0.2f, 0.7f, 0.2f, 1f))) }
        delay(1500)
        button.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 220f))
    }
    val loop = rememberInfiniteTransition(label = "Portada")
    val breathe by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(7000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "Respirar")
    val glow by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "Brillo")
    Box(Modifier.fillMaxSize().background(Plum)
        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onStart)) {
        Image(painterResource(R.drawable.cover), "Los Cuentos de Mamá Oca", contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().graphicsLayer {
                val s = 1.25f - 0.21f * intro.value + 0.06f * breathe * intro.value
                scaleX = s; scaleY = s; alpha = intro.value
                translationX = -breathe * size.width * 0.01f; translationY = -breathe * size.height * 0.01f
            })
        CoverMagic(animated, Modifier.fillMaxSize().graphicsLayer { alpha = intro.value })
        Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp)
            .graphicsLayer { scaleX = button.value; scaleY = button.value; alpha = button.value.coerceIn(0f, 1f) },
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MagicPlaque("Empezar a leer", glow, onClick = onStart)
            MagicPlaque("Juegos", glow, color = GamesGreen, small = true, onClick = onGames)
        }
    }
}

/** Placa de terciopelo violeta con marco de oro y un destello de luz que la recorre. */
@Composable
private fun MagicPlaque(label: String, glow: Float, color: Color = Velvet, small: Boolean = false, onClick: () -> Unit) {
    val sweep by rememberInfiniteTransition(label = "Destello").animateFloat(-0.4f, 1.6f,
        infiniteRepeatable(tween(2600, delayMillis = 900, easing = LinearEasing)), label = "Luz")
    val shape = RoundedCornerShape(16.dp)
    Box(Modifier
        .shadow((10 + 10 * glow).dp, shape, ambientColor = GoldLeaf, spotColor = GoldLeaf)
        .clip(shape).velvet(color)
        .goldFrame(16.dp, 2.5.dp)
        .drawWithContent {
            drawContent()
            val x = size.width * sweep
            drawRect(Brush.linearGradient(listOf(Color.Transparent, Color(0x66FFF3C4), Color.Transparent),
                Offset(x - 70.dp.toPx(), 0f), Offset(x, size.height)))
        }
        .clickable(role = Role.Button, onClick = onClick)
        .padding(horizontal = if (small) 24.dp else 30.dp, vertical = if (small) 9.dp else 14.dp), contentAlignment = Alignment.Center) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("❦", style = TextStyle(brush = GoldBrush, fontSize = 22.sp), modifier = Modifier.graphicsLayer { scaleX = -1f })
            if (small) Icon(Icons.Default.SportsEsports, null, tint = GoldLeaf, modifier = Modifier.size(24.dp))
            Text(label, style = TextStyle(brush = GoldBrush, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = if (small) 21.sp else 26.sp,
                shadow = Shadow(Color(0xAA241046), Offset(0f, 2f), 3f)))
            Text("❦", style = TextStyle(brush = GoldBrush, fontSize = 22.sp))
        }
    }
}

// ---------------- Biblioteca ----------------

/** Botón redondo de latón. */
@Composable
private fun BrassButton(icon: ImageVector, description: String, active: Boolean = false, onClick: () -> Unit) {
    // encendido: terciopelo con el icono en oro
    Box(Modifier.size(44.dp).shadow(4.dp, CircleShape).clip(CircleShape)
        .then(if (active) Modifier.background(Amethyst) else Modifier.background(GoldBrush)).goldBorder(CircleShape, 1.5.dp)
        .clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = if (active) GoldPale else Plum)
    }
}

@Composable
private fun Library(vm: StoryViewModel, play: () -> Unit) {
    LaunchedEffect(vm.stories) { if (vm.stories.isNotEmpty()) vm.pickDaily() } // por si cambió el día con la app abierta
    BoxWithConstraints(Modifier.fillMaxSize().velvet(PlumLight)) {
    val wide = maxWidth >= 780.dp
    val width = maxWidth
    Column(Modifier.fillMaxSize()) {
        // cornisa de terciopelo oscuro con remate dorado
        Row(Modifier.fillMaxWidth().velvet(Plum)
            .drawBehind { drawRect(GoldStroke, Offset(0f, size.height - 2.dp.toPx()), Size(size.width, 2.dp.toPx())) }
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
            .padding(horizontal = if (wide) 18.dp else 12.dp, vertical = if (wide) 10.dp else 7.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (wide) 14.dp else 8.dp)) {
            Image(painterResource(R.drawable.app_icon), "Volver a la portada", Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                .goldBorder(RoundedCornerShape(12.dp)).clickable { vm.navigate("home") })
            if (wide) Column {
                Text("Los Cuentos", style = TextStyle(brush = GoldBrush, fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 16.sp))
                Text("de Mamá Oca", style = TextStyle(brush = GoldBrush, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 23.sp))
            }
            Row(Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(10.dp)).background(Parchment).goldBorder(RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Search, null, tint = Velvet)
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f)) {
                    if (vm.query.isEmpty()) Text("Buscar…", color = Color(0xFFA48A6C), fontFamily = Fraunces, fontWeight = FontWeight.Medium,
                        fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    BasicTextField(vm.query, { vm.query = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Ink))
                }
                if (vm.query.isNotEmpty()) Icon(Icons.Default.Close, "Borrar búsqueda", tint = InkSoft,
                    modifier = Modifier.clip(CircleShape).clickable { vm.query = "" })
            }
            Row(Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0x55000000)).goldBorder(RoundedCornerShape(10.dp), 1.dp).padding(3.dp)) {
                Tab("Todos", !vm.favoritesOnly) { vm.favoritesOnly = false }
                Tab(if (wide) "Favoritos" else "", vm.favoritesOnly, Icons.Default.Favorite) { vm.favoritesOnly = true }
            }
            if (vm.stories.any { it.id in vm.read }) BrassButton(Icons.Default.TaskAlt,
                if (vm.showRead) "Ocultar los cuentos ya leídos" else "Ver los cuentos ya leídos", active = vm.showRead) {
                play(); vm.showRead = !vm.showRead
            }
            BrassButton(Icons.Default.Casino, "Ordenar al azar", active = vm.shuffleOrder != null) { play(); vm.shuffle() }
            BrassButton(Icons.Default.EmojiEvents, "Mis logros") { play(); vm.openAchievements() }
            // mi personaje (el sonido se enciende y apaga desde ahí)
            BrassButton(Icons.Default.AccountCircle, "Mi personaje") { play(); vm.navigate("profile") }
        }
        val items = vm.visibleStories
        val gridState = rememberLazyGridState()
        LaunchedEffect(vm.shuffleOrder) { if (vm.shuffleOrder != null) gridState.scrollToItem(0) } // tras tirar el dado, desde arriba
        if (items.isEmpty()) Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(if (vm.favoritesOnly && vm.query.isBlank()) "Aún no tienes favoritos.\nToca el corazón de un cuento para guardarlo aquí."
                else "No encontramos ese cuento.\nPrueba con otra palabra.", textAlign = TextAlign.Center,
                style = TextStyle(brush = GoldBrush, fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 30.sp))
        } else LazyVerticalGrid(columns = GridCells.Adaptive(330.dp), state = gridState,
            modifier = Modifier.windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)),
            contentPadding = PaddingValues(start = 6.dp, end = 6.dp, top = 14.dp, bottom = 8.dp)) {
            // sin separación entre filas: cada elemento deja su hueco abajo, así las filas de leídos se juntan en un bloque
            val daily = vm.daily
            if (daily != null && vm.query.isBlank() && !vm.favoritesOnly) item(key = "daily", span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.padding(bottom = 16.dp)) { DailyPick(vm, daily, wide, play) }
            }
            // cada cuento descansa sobre un estante de oro; los estantes de una fila se unen
            val (done, pending) = items.partition { it.id in vm.read }
            // los ya leídos, solo si se piden con el botón (al buscar, siempre): agrupados arriba sobre un fondo propio
            if (done.isNotEmpty() && (vm.showRead || vm.query.isNotBlank())) {
                val cols = maxOf(1, ((width - 12.dp) / 330.dp).toInt())
                val rows = done.chunked(cols)
                item(key = "read-label", span = { GridItemSpan(maxLineSpan) }) {
                    Row(Modifier.readBand(top = true, bottom = false).padding(start = 16.dp, top = 10.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TaskAlt, null, tint = GoldLeaf, modifier = Modifier.size(18.dp))
                        Text("  Ya leídos · ${done.size}", color = GoldPale, fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                }
                rows.forEachIndexed { r, row ->
                    item(key = "read-" + row.first().id, span = { GridItemSpan(maxLineSpan) }) {
                        Row(Modifier.readBand(top = false, bottom = r == rows.lastIndex)) {
                            row.forEachIndexed { c, story ->
                                Box(Modifier.weight(1f).shelf().padding(start = 10.dp, end = 10.dp, bottom = 18.dp)) {
                                    StoryCard(vm, story, r * cols + c, play)
                                }
                            }
                            repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
            itemsIndexed(pending, key = { _, s -> s.id }) { i, story ->
                Box(Modifier.padding(bottom = 16.dp).shelf().padding(start = 10.dp, end = 10.dp, bottom = 18.dp)) { StoryCard(vm, story, i, play) }
            }
        }
    }
    // rincón de papás: una llavecita discreta en la esquina, para que los niños no la busquen
    Icon(Icons.Default.Key, null, tint = GoldLeaf.copy(alpha = 0.4f), modifier = Modifier.align(Alignment.BottomStart)
        .padding(start = 2.dp, bottom = 2.dp).size(40.dp).clip(CircleShape)
        .clickable(role = Role.Button) { play(); vm.navigate("parents") }
        .semantics { contentDescription = "Rincón de papás" }.padding(10.dp))
    }
}

/** Cuento del día: un atril de terciopelo con la lámina grande, una cinta dorada y un botón para leerlo. */
@Composable
private fun DailyPick(vm: StoryViewModel, story: Story, wide: Boolean, play: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val reading = story.id == vm.inProgress
    Row(Modifier.padding(horizontal = 10.dp).fillMaxWidth().height(if (wide) 176.dp else 150.dp)
        .shadow(10.dp, shape).clip(shape).velvet(Velvet).goldFrame(16.dp, 2.dp)
        .clickable(role = Role.Button, onClickLabel = "Leer el cuento del día") { play(); vm.open(story.id) }
        .clearAndSetSemantics { contentDescription = "Cuento del día: ${story.title}" }
        .padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        StoryArt(story, 0, "Portada de ${story.title}", Modifier.fillMaxHeight().aspectRatio(1.6f)
            .shadow(4.dp, RoundedCornerShape(8.dp)).clip(RoundedCornerShape(8.dp)).goldBorder(RoundedCornerShape(8.dp)), thumbnail = true)
        Column(Modifier.weight(1f).padding(start = 16.dp, end = 6.dp), verticalArrangement = Arrangement.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, null, tint = GoldLeaf, modifier = Modifier.size(18.dp))
                Text("  CUENTO DEL DÍA", style = TextStyle(brush = GoldBrush, fontFamily = Fraunces, fontWeight = FontWeight.Bold,
                    fontSize = 13.sp, letterSpacing = 2.sp))
            }
            Text(story.title, color = GoldPale, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = if (wide) 24.sp else 19.sp,
                lineHeight = if (wide) 28.sp else 22.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(vertical = 4.dp))
            if (wide) Text(story.description, color = GoldPale.copy(alpha = 0.8f), fontFamily = Andika, fontSize = 14.sp, lineHeight = 18.sp,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Row(Modifier.clip(RoundedCornerShape(10.dp)).background(GoldBrush).goldBorder(RoundedCornerShape(10.dp), 1.dp)
            .padding(horizontal = if (wide) 16.dp else 10.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.PlayArrow, null, tint = Plum, modifier = Modifier.size(20.dp))
            if (wide) Text(if (reading) " Continuar" else " Leer", color = Plum, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

/** Fondo que agrupa los cuentos ya leídos: un paño de terciopelo más oscuro, redondeado arriba y abajo. */
private fun Modifier.readBand(top: Boolean, bottom: Boolean): Modifier {
    val r = 16.dp
    return this.padding(bottom = if (bottom) 16.dp else 0.dp).fillMaxWidth()
        .clip(RoundedCornerShape(topStart = if (top) r else 0.dp, topEnd = if (top) r else 0.dp,
            bottomStart = if (bottom) r else 0.dp, bottomEnd = if (bottom) r else 0.dp))
        .background(Color(0x66120628))
        .padding(top = if (top) 4.dp else 0.dp, bottom = if (bottom) 8.dp else 0.dp)
}

@Composable
private fun Tab(label: String, selected: Boolean, icon: ImageVector? = null, onClick: () -> Unit) {
    Row(Modifier.height(36.dp).clip(RoundedCornerShape(8.dp)).then(if (selected) Modifier.background(GoldBrush) else Modifier)
        .clickable(role = Role.Tab, onClick = onClick).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) Icon(icon, "Favoritos", tint = if (selected) Amethyst else HeartRed, modifier = Modifier.size(if (label.isEmpty()) 20.dp else 16.dp))
        if (icon != null && label.isNotEmpty()) Spacer(Modifier.width(6.dp))
        if (label.isNotEmpty()) Text(label, color = if (selected) Plum else GoldPale, fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

/** Cuento en el estante: pergamino con lomo de cuero, marco dorado y su lámina enmarcada. */
@Composable
private fun StoryCard(vm: StoryViewModel, story: Story, index: Int, play: () -> Unit) {
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { delay(index * 45L); appear.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 300f)) }
    val accent = storyColor(story)
    val shape = RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 14.dp, bottomEnd = 14.dp)
    Row(Modifier.graphicsLayer { alpha = appear.value.coerceIn(0f, 1f); translationY = (1 - appear.value) * 24.dp.toPx() }
        .fillMaxWidth().height(144.dp)
        .shadow(8.dp, shape).clip(shape).paper().goldFrame(14.dp, 1.5.dp)
        .clickable(role = Role.Button, onClickLabel = (if (story.id == vm.inProgress) "Continuar " else "Leer ") + story.title) { play(); vm.open(story.id) },
        verticalAlignment = Alignment.CenterVertically) {
        // lomo de cuero del color del cuento, con nervios dorados
        Box(Modifier.width(14.dp).fillMaxHeight()
            .background(Brush.horizontalGradient(listOf(lerp(accent, Color.Black, 0.45f), lerp(accent, Color.Black, 0.15f), lerp(accent, Color.Black, 0.5f))))
            .drawBehind {
                listOf(0.2f, 0.27f, 0.73f, 0.8f).forEach { f -> drawRect(GoldStroke, Offset(0f, size.height * f), Size(size.width, 2.dp.toPx())) }
            })
        StoryArt(story, 0, "Portada de ${story.title}", Modifier.padding(start = 12.dp, end = 12.dp).width(150.dp).height(96.dp)
            .shadow(3.dp, RoundedCornerShape(6.dp)).clip(RoundedCornerShape(6.dp)).goldBorder(RoundedCornerShape(6.dp)), thumbnail = true)
        Column(Modifier.weight(1f).padding(vertical = 12.dp), verticalArrangement = Arrangement.Center) {
            Text(story.title, fontFamily = Fraunces, fontWeight = FontWeight.Bold, color = Plum, fontSize = 18.sp, lineHeight = 21.sp,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(story.description, color = InkSoft, fontFamily = Andika, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 17.sp,
                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(vertical = 4.dp))
            when {
                story.id == vm.inProgress -> {
                    // a medias: cinta de cuero para continuar y barra de progreso dorada
                    Row(Modifier.padding(top = 2.dp).clip(RoundedCornerShape(6.dp)).background(Amethyst).goldBorder(RoundedCornerShape(6.dp), 1.dp)
                        .padding(horizontal = 9.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PlayArrow, null, tint = GoldLeaf, modifier = Modifier.size(16.dp))
                        Text("Continuar · página ${vm.progressPage} de ${story.pages.size}", color = GoldPale, fontFamily = Fraunces,
                            fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1)
                    }
                    Box(Modifier.padding(top = 6.dp, end = 4.dp).fillMaxWidth().height(4.dp).clip(CircleShape).background(Color(0x223A2A22))) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(vm.progressPage.toFloat() / story.pages.size).clip(CircleShape).background(GoldBrush))
                    }
                }
                story.id in vm.read -> Row(verticalAlignment = Alignment.CenterVertically) {
                    // sello de lacre: ya leído
                    Box(Modifier.size(20.dp).clip(CircleShape).background(Amethyst).goldBorder(CircleShape, 1.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Check, null, tint = GoldPale, modifier = Modifier.size(13.dp))
                    }
                    Text("  Leído  ·  ${story.pages.size} páginas  ", color = Amethyst, fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    vm.quizStars[story.id]?.let { n -> repeat(3) { i ->
                        Icon(if (i < n) Icons.Default.Star else Icons.Default.StarBorder, null, tint = GoldDeep, modifier = Modifier.size(15.dp))
                    } }
                }
                else -> Text("${story.pages.size} páginas", color = Color(0xFF8A7A6A), fontFamily = Fraunces, fontWeight = FontWeight.Medium, fontSize = 12.sp)
            }
        }
        Favorite(story.id in vm.favorites) { vm.toggleFavorite(story.id); play() }
        Spacer(Modifier.width(6.dp))
    }
}

@Composable
private fun Favorite(selected: Boolean, onClick: () -> Unit) {
    val pop = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    IconToggleButton(checked = selected, onCheckedChange = {
        onClick()
        scope.launch { pop.snapTo(1.45f); pop.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 400f)) }
    }) {
        Icon(if (selected) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            if (selected) "Quitar de favoritos" else "Añadir a favoritos", tint = HeartRed,
            modifier = Modifier.size(28.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value })
    }
}

// ---------------- Lector ----------------

private val PageEdges = Brush.horizontalGradient(listOf(Color(0xFFCDBB98), Color(0xFFFFF8EA), Color(0xFFD9C8A8), Color(0xFFFFF8EA), Color(0xFFCDBB98)))
private val Leather = Brush.verticalGradient(listOf(Color(0xFF5B2A93), Color(0xFF3E1D72), Color(0xFF2A1252)))

/**
 * Esquina doblada de la página: se toca para pasar la hoja. [right] = esquina inferior derecha (siguiente),
 * si no, esquina inferior izquierda (anterior). Se levanta un poco, como invitando a pasar la página.
 */
@Composable
private fun PageCorner(right: Boolean, description: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val lift by rememberInfiniteTransition(label = "Esquina").animateFloat(0f, 1f,
        infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "Levantar")
    Box(modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null,
        role = Role.Button, onClick = onClick).semantics { contentDescription = description }) {
        Canvas(Modifier.fillMaxSize()) {
            val s = size.width * (0.62f + 0.08f * lift)
            val w = size.width; val h = size.height
            // coordenadas de la esquina inferior derecha; se reflejan para la izquierda
            fun x(v: Float) = if (right) v else w - v
            val corner = Offset(x(w), h); val a = Offset(x(w), h - s); val b = Offset(x(w - s), h)
            val tip = Offset(x(w - s), h - s)
            // página de debajo que queda a la vista
            drawPath(Path().apply { moveTo(a.x, a.y); lineTo(corner.x, corner.y); lineTo(b.x, b.y); close() },
                Brush.linearGradient(listOf(Color(0xFFE2D2B0), Color(0xFFCDB993)), a, b))
            // sombra de la solapa
            drawPath(Path().apply { moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(tip.x - (if (right) 4f else -4f) * density, tip.y - 4f * density); close() },
                Color(0x33000000))
            // solapa doblada (reverso del papel)
            drawPath(Path().apply { moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(tip.x, tip.y); close() },
                Brush.linearGradient(listOf(Color(0xFFFFFCF4), Color(0xFFEADCC0)), tip, Offset((a.x + b.x) / 2, (a.y + b.y) / 2)))
            drawLine(Color(0x22000000), a, b, 1.5f)
            // flechita de tinta sobre la solapa
            val c = Offset(tip.x + (if (right) 1 else -1) * s * 0.34f, tip.y + s * 0.34f)
            val k = s * 0.09f; val d = if (right) 1 else -1
            drawPath(Path().apply { moveTo(c.x - d * k * 0.6f, c.y - k); lineTo(c.x + d * k * 0.5f, c.y); lineTo(c.x - d * k * 0.6f, c.y + k) },
                InkSoft, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.2f * density, cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round))
        }
    }
}

/** Cinta marcapáginas: cuelga del borde superior; roja y larga cuando el cuento es favorito. */
@Composable
private fun RibbonBookmark(selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val drop by animateFloatAsState(if (selected) 1f else 0f, spring(dampingRatio = 0.45f, stiffness = 260f), label = "Cinta")
    val color = lerp(Color(0xFFD9C7A3), Color(0xFFC62E3A), drop)
    Box(modifier.width(34.dp).height(96.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null,
        role = Role.Button) { onClick() }.semantics { contentDescription = if (selected) "Quitar de favoritos" else "Añadir a favoritos" },
        contentAlignment = Alignment.TopCenter) {
        val length = 50.dp + 40.dp * drop
        Canvas(Modifier.width(28.dp).height(length)) {
            val w = size.width; val h = size.height; val notch = w * 0.45f
            drawPath(Path().apply { moveTo(0f, 0f); lineTo(w, 0f); lineTo(w, h); lineTo(w / 2, h - notch); lineTo(0f, h); close() }, color)
            drawRect(Color(0x26000000), Offset.Zero, Size(w, 6f * density))
            drawLine(Color(0x1FFFFFFF), Offset(w * 0.22f, 0f), Offset(w * 0.22f, h - notch * 0.6f), 1.5f * density)
        }
        Icon(if (selected) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null,
            tint = if (selected) Color.White else InkSoft, modifier = Modifier.padding(top = length - 34.dp).size(16.dp))
    }
}

/** Texto de tinta sobre el papel que funciona como botón. */
@Composable
private fun InkAction(icon: ImageVector, label: String, modifier: Modifier = Modifier, active: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    Row(modifier.minimumInteractiveComponentSize().clip(RoundedCornerShape(8.dp)).background(if (active) Gold.copy(alpha = 0.4f) else Color.Transparent)
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically) {
        val tint = if (enabled) InkSoft else InkSoft.copy(alpha = 0.4f)
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        if (label.isNotEmpty()) {
            Spacer(Modifier.width(5.dp))
            Text(label, fontFamily = Andika, fontWeight = FontWeight.Normal, fontSize = 15.sp, color = tint, maxLines = 1)
        }
    }
}

@Composable
private fun Reader(vm: StoryViewModel, story: Story, narrator: Narrator, foreground: Boolean, play: () -> Unit) {
    val context = LocalContext.current
    // La tapa del libro ocupa toda la pantalla; las páginas, todo su interior.
    BoxWithConstraints(Modifier.fillMaxSize().background(Leather).drawBehind {
        // filete de oro repujado en el borde de la tapa
        val i = 2.5.dp.toPx()
        drawRoundRect(GoldStroke, Offset(i, i), Size(size.width - 2 * i, size.height - 2 * i), CornerRadius(12.dp.toPx()), style = Stroke(1.2.dp.toPx()))
    }) {
        val narrow = narrowPage((maxWidth - 12.dp) / 2)
        val corner = if (maxHeight < 420.dp) 64.dp else 84.dp
        val inside = Modifier.fillMaxSize().padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 10.dp)
        Book(vm, story, narrator, foreground, inside)
        Row(inside) {
            Box(Modifier.weight(1f).fillMaxHeight()) {
                InkAction(Icons.AutoMirrored.Filled.ArrowBack, if (narrow) "" else "Cuentos",
                    Modifier.semantics { contentDescription = "Volver a los cuentos" }.align(Alignment.TopStart).windowInsetsPadding(WindowInsets.displayCutout).padding(start = 14.dp, top = 10.dp)) { vm.navigate("library") }
                if (vm.page > 0) PageCorner(false, "Página anterior", Modifier.align(Alignment.BottomStart).size(corner)) { vm.previous() }
            }
            Box(Modifier.weight(1f).fillMaxHeight()) {
                InkAction(if (vm.listening) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                    if (narrow) "" else if (vm.listening) "Detener" else "Escuchar", Modifier.semantics { contentDescription = if (vm.listening) "Detener la lectura" else "Escuchar el cuento" }
                        .align(Alignment.TopEnd).padding(end = 62.dp, top = 10.dp),
                    active = vm.listening, enabled = narrator.ready) { vm.toggleListening() }
                RibbonBookmark(story.id in vm.favorites, Modifier.align(Alignment.TopEnd).padding(end = 18.dp)) { vm.toggleFavorite(story.id); play() }
                PageCorner(true, if (vm.page == story.pages.size) "Terminar cuento" else "Página siguiente", Modifier.align(Alignment.BottomEnd).size(corner)) { vm.next() }
            }
        }
        // progreso en el canto inferior del libro
        val progress by animateFloatAsState(vm.page.toFloat() / story.pages.size, tween(600), label = "Progreso")
        Box(Modifier.align(Alignment.BottomStart).padding(horizontal = 6.dp, vertical = 3.dp)
            .fillMaxWidth(progress).height(4.dp).clip(CircleShape).background(Brush.horizontalGradient(listOf(Gold, Color(0xFFFF9F1C)))))
        if (narrator.message != null) Row(Modifier.align(Alignment.TopCenter).padding(top = 70.dp).shadow(8.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp)).background(Color.White).padding(start = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(narrator.message!!, Modifier.widthIn(max = 420.dp), fontSize = 13.sp, color = Ink)
            TextButton(onClick = { runCatching { context.startActivity(Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)) } }) { Text("Configurar voz") }
        }
    }
}

/** Libro abierto: ilustración a la izquierda, texto a la derecha y una hoja que gira en 3D al pasar de página. */
@Composable
private fun Book(vm: StoryViewModel, story: Story, narrator: Narrator, animated: Boolean, modifier: Modifier) {
    var shown by remember(story.id) { mutableIntStateOf(vm.page) }
    var target by remember(story.id) { mutableIntStateOf(vm.page) }
    val turn = remember { Animatable(0f) }
    LaunchedEffect(vm.page, story.id) {
        if (vm.page != shown) {
            target = vm.page
            if (vm.sound) Sfx.play(Sound.Page)
            turn.snapTo(0f)
            turn.animateTo(1f, tween(900, easing = CubicBezierEasing(0.45f, 0.05f, 0.3f, 1f)))
            shown = target
            turn.snapTo(0f)
        }
    }
    val flipping = target != shown
    val forward = target > shown
    val baseLeft = if (flipping && !forward) target else shown
    val baseRight = if (flipping && forward) target else shown
    val image: @Composable (Int) -> Unit = { i ->
        PrintedArt(story, i, "${story.title}, ilustración ${if (i == 0) "de portada" else "de la página $i"}", Modifier.fillMaxSize())
    }
    val text: @Composable (Int) -> Unit = { i -> TextPage(story, i, if (!flipping && i == vm.page && vm.listening) narrator.activeSegment else -1) }

    Box(modifier
        .clip(RoundedCornerShape(10.dp))
        .pointerInput(story.id) {
            var distance = 0f
            detectHorizontalDragGestures(onDragStart = { distance = 0f }, onDragEnd = {
                if (distance < -60.dp.toPx()) vm.next() else if (distance > 60.dp.toPx()) vm.previous()
            }) { change, delta -> change.consume(); distance += delta }
        }
        .pointerInput(story.id) { detectTapGestures { if (it.x > size.width / 2) vm.next() } }) {
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxHeight()) { image(baseLeft) }
            Box(Modifier.weight(1f).fillMaxHeight()) { text(baseRight) }
        }
        // sombra del lomo
        Row(Modifier.fillMaxSize()) {
            Spacer(Modifier.weight(0.88f))
            Box(Modifier.weight(0.12f).fillMaxHeight().background(Brush.horizontalGradient(listOf(Color.Transparent, Color(0x405A3214)))))
            Box(Modifier.weight(0.12f).fillMaxHeight().background(Brush.horizontalGradient(listOf(Color(0x385A3214), Color.Transparent))))
            Spacer(Modifier.weight(0.88f))
        }
        Box(Modifier.align(Alignment.Center).width(2.dp).fillMaxHeight().background(Color(0x595A3C1E)))
        Box(Modifier.align(Alignment.CenterStart).width(5.dp).fillMaxHeight().background(PageEdges))
        Box(Modifier.align(Alignment.CenterEnd).width(5.dp).fillMaxHeight().background(PageEdges))
        if (flipping) {
            val p = turn.value
            Box(Modifier.fillMaxWidth(0.5f).fillMaxHeight().align(if (forward) Alignment.CenterEnd else Alignment.CenterStart)
                .graphicsLayer {
                    transformOrigin = TransformOrigin(if (forward) 0f else 1f, 0.5f)
                    rotationY = if (forward) -180f * p else 180f * p
                    cameraDistance = 14f * density
                }) {
                val front = p < 0.5f
                Box(Modifier.fillMaxSize().graphicsLayer { if (!front) scaleX = -1f }.paper()) {
                    when {
                        front && forward -> text(shown)
                        front -> image(shown)
                        forward -> image(target)
                        else -> text(target)
                    }
                    val shade = sin(p * PI).toFloat() * 0.35f
                    val darkAtSpine = front == forward
                    Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(
                        if (darkAtSpine) listOf(Color.Black.copy(alpha = shade), Color.Transparent)
                        else listOf(Color.Transparent, Color.Black.copy(alpha = shade)))))
                }
            }
        }
    }
}

/** Páginas estrechas (teléfonos): botones solo con icono y cabecera más corta. */
private fun narrowPage(pageWidth: Dp) = pageWidth < 400.dp

@Composable
private fun TextPage(story: Story, index: Int, activeSegment: Int) {
    val accent = storyColor(story)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxSize().paper()) {
        val compact = maxHeight < 420.dp
        val narrow = narrowPage(maxWidth)
        val maxBase = (maxWidth.value * 0.043f).coerceIn(14f, 25f)
        if (index == 0) {
            val base = kotlin.math.min(maxBase, maxHeight.value / 20f).coerceAtLeast(12f)
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(start = maxWidth * 0.1f, end = maxWidth * 0.1f, top = if (compact) 44.dp else 60.dp, bottom = if (compact) 44.dp else 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("❦", color = accent, fontSize = (base * 1.6f).sp)
                Text(story.title, fontFamily = Andika, fontWeight = FontWeight.Bold, fontSize = (base * 2.1f).sp, lineHeight = (base * 2.4f).sp,
                    textAlign = TextAlign.Center, color = Ink, modifier = Modifier.padding(vertical = (base * 0.6f).dp))
                Box(Modifier.width(64.dp).height(2.dp).background(accent.copy(alpha = 0.5f)))
                Text(story.description, textAlign = TextAlign.Center, color = InkSoft, fontFamily = Andika, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Normal,
                    fontSize = base.sp, lineHeight = (base * 1.5f).sp, modifier = Modifier.padding(top = (base * 0.7f).dp))
                Text("Pasa la página para empezar", color = accent, fontFamily = Andika, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Normal,
                    fontSize = (base * 0.8f).sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = (base * 1.2f).dp))
            }
            return@BoxWithConstraints
        }
        val scroll = rememberScrollState()
        Column(Modifier.fillMaxSize().padding(start = maxWidth * 0.08f, end = maxWidth * 0.07f, top = if (compact) 8.dp else 14.dp)) {
            // cabecera de página, como en los libros impresos (deja sitio a los botones de la derecha)
            Box(Modifier.height(if (compact) 36.dp else 44.dp).padding(end = if (narrow) 100.dp else 170.dp), contentAlignment = Alignment.CenterStart) {
                Text(story.title.uppercase(), fontFamily = Andika, fontWeight = FontWeight.Normal, fontSize = (maxBase * 0.6f).coerceAtLeast(10f).sp,
                    letterSpacing = if (narrow) 1.sp else 2.sp, color = InkSoft.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val segments = story.pages[index - 1].text
                // el tamaño de letra más grande con el que todo el texto de la página cabe sin desplazarse
                val base = remember(story.id, index, constraints.maxWidth, constraints.maxHeight) {
                    with(density) {
                        val width = (constraints.maxWidth - 8.dp.roundToPx()).coerceAtLeast(1)
                        fun fits(b: Float): Boolean {
                            val gap = (b * 0.6f).dp.toPx()
                            val total = segments.withIndex().sumOf { (k, seg) ->
                                measurer.measure(segmentText(seg, b, accent, index == 1 && k == 0 && !story.poem), segmentStyle(b),
                                    constraints = Constraints(maxWidth = width)).size.height
                            } + gap * (segments.size - 1) + 12.dp.toPx()
                            return total <= constraints.maxHeight
                        }
                        var b = maxBase
                        while (b > 11f && !fits(b)) b -= 0.5f
                        b
                    }
                }
                val area = maxHeight
                Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
                    Column(Modifier.heightIn(min = area).padding(vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy((base * 0.6f).dp, Alignment.CenterVertically)) {
                        segments.forEachIndexed { k, segment ->
                            SegmentView(segment, base, accent, dropCap = index == 1 && k == 0 && !story.poem, active = k == activeSegment)
                        }
                    }
                }
                if (scroll.canScrollForward) Text("▾", color = InkSoft, fontSize = 18.sp, modifier = Modifier.align(Alignment.BottomCenter))
            }
            // pie con el número de página, a la altura de las esquinas dobladas
            Box(Modifier.fillMaxWidth().height(if (compact) 46.dp else 60.dp), contentAlignment = Alignment.Center) {
                Text("$index / ${story.pages.size}", fontFamily = Andika, fontWeight = FontWeight.Normal, color = InkSoft.copy(alpha = 0.8f),
                    fontSize = (maxBase * 0.65f).coerceAtLeast(10f).sp)
            }
        }
    }
}

private val pluralSpeakers = setOf("Todos", "Cabritillos", "Patitos", "Cisnes", "Enanitos")

/** Diálogo al estilo de los libros en español: «—¿Quién eres? —preguntó Enanito.» */
private fun dialogue(speaker: String, text: String): Pair<String, String> {
    val t = text.trim()
    val plural = speaker in pluralSpeakers
    val verb = when {
        t.endsWith("?") -> if (plural) "preguntaron" else "preguntó"
        t.endsWith("!") -> if (plural) "exclamaron" else "exclamó"
        else -> if (plural) "dijeron" else "dijo"
    }
    val who = when { speaker == "Todos" -> "todos"; plural -> "los " + speaker.lowercase(); else -> speaker }
    // espacio duro y unión de palabra: la raya nunca queda sola al final de una línea
    return "—" + t.removeSuffix(".") to " —⁠$verb $who."
}

private fun segmentStyle(base: Float) =
    TextStyle(fontFamily = Andika, fontWeight = FontWeight.Normal, fontSize = base.sp, lineHeight = (base * 1.45f).sp, color = Ink)

private fun segmentText(segment: Segment, base: Float, accent: Color, dropCap: Boolean) = buildAnnotatedString {
    val speaker = segment.speaker
    if (speaker != null) {
        val (line, attribution) = dialogue(speaker, segment.text)
        append(line)
        withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = InkSoft)) { append(attribution) }
    } else if (dropCap && segment.text.isNotEmpty()) {
        withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = (base * 2.3f).sp, color = accent)) { append(segment.text.first()) }
        append(segment.text.drop(1))
    } else append(segment.text)
}

@Composable
private fun SegmentView(segment: Segment, base: Float, accent: Color, dropCap: Boolean, active: Boolean) {
    val requester = remember { BringIntoViewRequester() }
    LaunchedEffect(active) { if (active) requester.bringIntoView() }
    // al narrar, la frase se marca como con un rotulador suave
    Text(segmentText(segment, base, accent, dropCap), style = segmentStyle(base),
        modifier = Modifier.fillMaxWidth().bringIntoViewRequester(requester).clip(RoundedCornerShape(6.dp))
            .background(if (active) Gold.copy(alpha = 0.32f) else Color.Transparent).padding(horizontal = 4.dp))
}

// ---------------- Fin ----------------

@Composable
private fun Finish(vm: StoryViewModel, story: Story, play: () -> Unit) {
    LaunchedEffect(Unit) { if (vm.sound) Sfx.play(Sound.Fanfare) }
    val loop = rememberInfiniteTransition(label = "Fin")
    val wiggle by loop.animateFloat(-6f, 6f, infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "Icono")
    val float by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "Letras")
    val card = remember { Animatable(0.4f) }
    LaunchedEffect(Unit) { card.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 260f)) }
    BoxWithConstraints(Modifier.fillMaxSize().velvet(Plum)
        .background(Brush.radialGradient(listOf(Color(0x99E9B04A), Color(0x55A35CC9), Color(0x00000000)))), contentAlignment = Alignment.Center) {
        val fit = minOf(1f, maxHeight / 430.dp, maxWidth / 600.dp)
        Confetti(Modifier.fillMaxSize(), onTap = play)
        Column(Modifier.wrapContentSize(unbounded = true).graphicsLayer {
            scaleX = card.value * fit; scaleY = card.value * fit; alpha = card.value.coerceIn(0f, 1f) }.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painterResource(R.drawable.app_icon), null, Modifier.size(78.dp).graphicsLayer { rotationZ = wiggle }
                .shadow(10.dp, RoundedCornerShape(22.dp)).clip(RoundedCornerShape(22.dp)).goldBorder(RoundedCornerShape(22.dp), 2.5.dp))
            Row(Modifier.clearAndSetSemantics { text = AnnotatedString("¡Fin!") }) {
                "¡Fin!".forEachIndexed { i, c -> FallingLetter(c, i, float) }
            }
            Text("¡Felicidades! Terminaste", color = Parchment, fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 22.sp)
            Text("«${story.title}»", style = TextStyle(brush = GoldBrush, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 24.sp, textAlign = TextAlign.Center))
            Box(Modifier.padding(vertical = 8.dp)) { StarRow(vm.lastStars ?: 3) }
            starMessage(vm.lastStars)?.let {
                Text(it, color = GoldPale, fontFamily = Andika, fontSize = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 10.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                PillButton("Leer otra vez", ghost = true) { play(); vm.restart(story.id) }
                Box(Modifier.clip(CircleShape).background(Parchment).goldBorder(CircleShape)) {
                    Favorite(story.id in vm.favorites) { vm.toggleFavorite(story.id); play() }
                }
                PillButton("Más cuentos", ghost = false) { play(); vm.navigate("library") }
            }
        }
    }
}

@Composable
private fun FallingLetter(c: Char, index: Int, float: Float) {
    val drop = remember { Animatable(0f) }
    LaunchedEffect(Unit) { delay(300L + index * 120L); drop.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 180f)) }
    Text(c.toString(), style = TextStyle(brush = GoldBrush, fontFamily = Fraunces, fontWeight = FontWeight.ExtraBold, fontSize = 84.sp, lineHeight = 92.sp,
        shadow = Shadow(Color(0x99241046), Offset(0f, 4f), 6f)),
        modifier = Modifier.graphicsLayer {
            val wave = sin(float * PI + index).toFloat()
            translationY = (1 - drop.value) * -140.dp.toPx() - 6.dp.toPx() * wave * drop.value
            rotationZ = (1 - drop.value) * -30f
            alpha = drop.value.coerceIn(0f, 1f)
            scaleX = 0.2f + 0.8f * drop.value; scaleY = scaleX
        })
}


@Composable
private fun PillButton(label: String, ghost: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(Modifier.height(50.dp).shadow(6.dp, shape).clip(shape)
        .then(if (ghost) Modifier.velvet(Velvet) else Modifier.background(GoldBrush))
        .goldBorder(shape, 2.dp)
        .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 22.dp), contentAlignment = Alignment.Center) {
        Text(label, color = if (ghost) GoldPale else Plum, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

