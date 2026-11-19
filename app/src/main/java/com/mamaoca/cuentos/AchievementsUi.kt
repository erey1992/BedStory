package com.mamaoca.cuentos

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Insignias pintadas en assets/badges/<id>.webp o .png; mientras no estén, se dibuja una medalla con un icono. */
private object BadgeArt {
    private var files: Set<String>? = null
    @Synchronized fun path(context: Context, id: String): String? {
        val all = files ?: (context.assets.list("badges")?.toSet() ?: emptySet()).also { files = it }
        return listOf("$id.webp", "$id.png").firstOrNull { it in all }?.let { "badges/$it" }
    }
}

private val placeholderIcons: Map<String, ImageVector> = mapOf(
    "primer-cuento" to Icons.AutoMirrored.Filled.MenuBook, "lector-curioso" to Icons.Default.Search,
    "raton-biblioteca" to Icons.Default.LocalLibrary, "devoralibros" to Icons.Default.AutoStories,
    "gran-lector" to Icons.Default.School, "maestro-mama-oca" to Icons.Default.EmojiEvents,
    "amigo-animales" to Icons.Default.Pets, "invitado-corte" to Icons.Default.Castle,
    "aprendiz-mago" to Icons.Default.AutoFixHigh, "domador-dragones" to Icons.Default.LocalFireDepartment,
    "lobo-de-mar" to Icons.Default.Sailing, "astronauta" to Icons.Default.RocketLaunch,
    "viajero-tiempo" to Icons.Default.HourglassBottom, "corazon-valiente" to Icons.Default.Favorite,
    "amigo-marti" to Icons.Default.Spa, "pequeno-poeta" to Icons.Default.HistoryEdu,
    "racha-3" to Icons.Default.Whatshot, "racha-5" to Icons.Default.Whatshot, "racha-10" to Icons.Default.Whatshot,
    "buho-nocturno" to Icons.Default.Bedtime, "gallo-madrugador" to Icons.Default.WbSunny,
    "oidos-atentos" to Icons.Default.Headphones, "maraton" to Icons.Default.DirectionsRun,
    "cuento-del-dia" to Icons.Default.Today, "otra-vez" to Icons.Default.Replay,
)

private val grayscale = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

/** La insignia de un logro; apagada y en gris mientras no se consigue. */
@Composable
fun AchievementBadge(achievement: Achievement, unlocked: Boolean, size: Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val asset = remember(achievement.id) { BadgeArt.path(context.applicationContext, achievement.id) }
    val bitmap by produceState<ImageBitmap?>(null, asset) {
        if (asset != null) value = withContext(Dispatchers.IO) { runCatching { ArtLoader.load(context.applicationContext, asset) }.getOrNull() }
    }
    Box(modifier.size(size).graphicsLayer { alpha = if (unlocked) 1f else 0.45f }, contentAlignment = Alignment.Center) {
        val bmp = bitmap
        if (bmp != null) Image(bmp, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit,
            colorFilter = if (unlocked) null else grayscale)
        else Box(Modifier.fillMaxSize(0.92f).shadow(6.dp, CircleShape).clip(CircleShape)
            .background(if (unlocked) GoldBrush else Brush.verticalGradient(listOf(Color(0xFFB9B2C4), Color(0xFF7D7590))))
            .goldBorder(CircleShape, 3.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxSize(0.74f).clip(CircleShape).velvet(if (unlocked) Velvet else Color(0xFF5B5468)),
                contentAlignment = Alignment.Center) {
                Icon(placeholderIcons[achievement.id] ?: Icons.Default.Star, null,
                    tint = if (unlocked) GoldPale else Color(0xFFCFC9D9), modifier = Modifier.fillMaxSize(0.55f))
            }
        }
        if (!unlocked) Icon(Icons.Default.Lock, null, tint = Color.White,
            modifier = Modifier.align(Alignment.BottomEnd).size(size * 0.26f).clip(CircleShape).background(Plum).padding(size * 0.04f))
    }
}

/** Felicitación al conseguir un logro: la insignia aparece con un salto, entre confeti. */
@Composable
fun AchievementCelebration(achievement: Achievement, sound: Boolean, onDismiss: () -> Unit) {
    val pop = remember(achievement.id) { Animatable(0f) }
    LaunchedEffect(achievement.id) {
        if (sound) Sfx.play(Sound.Fanfare)
        pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 220f))
    }
    BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xCC12062A))
        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center) {
        val fit = minOf(1f, maxHeight / 420.dp, maxWidth / 460.dp)
        Confetti(Modifier.fillMaxSize())
        Column(Modifier.wrapContentSize(unbounded = true)
            .graphicsLayer { scaleX = pop.value * fit; scaleY = pop.value * fit; alpha = pop.value.coerceIn(0f, 1f) }
            .widthIn(max = 420.dp).shadow(16.dp, RoundedCornerShape(22.dp)).clip(RoundedCornerShape(22.dp))
            .velvet(Velvet).goldFrame(22.dp, 2.5.dp).padding(horizontal = 28.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text("¡NUEVO LOGRO!", style = TextStyle(brush = GoldBrush, fontFamily = Fraunces, fontWeight = FontWeight.Bold,
                fontSize = 16.sp, letterSpacing = 3.sp))
            AchievementBadge(achievement, unlocked = true, size = 150.dp, modifier = Modifier.padding(vertical = 12.dp))
            Text(achievement.title, style = TextStyle(brush = GoldBrush, fontFamily = Fraunces, fontWeight = FontWeight.Bold,
                fontSize = 28.sp, textAlign = TextAlign.Center))
            Text(achievement.description, color = GoldPale, fontFamily = Andika, fontSize = 17.sp, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = if (achievement.id in ACHIEVEMENT_PRIZES) 4.dp else 16.dp))
            ACHIEVEMENT_PRIZES[achievement.id]?.let {
                Text("🎁 ¡Y ganaste ${it.one} de premio!", color = GoldLeaf, fontFamily = Fraunces, fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 14.dp))
            }
            Box(Modifier.height(50.dp).shadow(6.dp, RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp)).background(GoldBrush)
                .goldBorder(RoundedCornerShape(12.dp), 2.dp).clickable(role = Role.Button, onClick = onDismiss)
                .padding(horizontal = 30.dp), contentAlignment = Alignment.Center) {
                Text("¡Genial!", color = Plum, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        }
    }
}

private val dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/** «Mis logros»: las 25 insignias, conseguidas primero, con su progreso. */
@Composable
fun AchievementsScreen(vm: StoryViewModel, play: () -> Unit) {
    val ordered = ACHIEVEMENTS.sortedByDescending { it.id in vm.unlocked }
    Column(Modifier.fillMaxSize().velvet(PlumLight)) {
        Row(Modifier.fillMaxWidth().velvet(Plum)
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
            .padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(GoldBrush).goldBorder(CircleShape, 1.5.dp)
                .clickable(role = Role.Button) { play(); vm.back() }.semantics { contentDescription = "Volver" },
                contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Plum) }
            Text("Mis logros", style = TextStyle(brush = GoldBrush, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 26.sp),
                modifier = Modifier.weight(1f))
            Row(Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0x55000000)).goldBorder(RoundedCornerShape(10.dp), 1.dp)
                .padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EmojiEvents, null, tint = GoldLeaf, modifier = Modifier.size(20.dp))
                Text("  ${vm.unlocked.size} de ${ACHIEVEMENTS.size}", color = GoldPale, fontFamily = Fraunces,
                    fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }
        LazyVerticalGrid(GridCells.Adaptive(170.dp), Modifier.windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)),
            contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            if (vm.unlocked.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                Text("Lee cuentos para ganar insignias. ¡La primera llega al terminar tu primer cuento!", color = GoldPale,
                    fontFamily = Andika, fontSize = 17.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(8.dp))
            }
            items(ordered, key = { it.id }) { a -> AchievementCard(a, vm.unlocked[a.id], vm.stats) }
        }
    }
}

@Composable
private fun AchievementCard(achievement: Achievement, unlockedDay: Long?, stats: ReadingStats) {
    val unlocked = unlockedDay != null
    val shape = RoundedCornerShape(16.dp)
    Column(Modifier.fillMaxWidth().height(250.dp).shadow(if (unlocked) 8.dp else 2.dp, shape).clip(shape)
        .then(if (unlocked) Modifier.paper().goldFrame(16.dp, 1.5.dp) else Modifier.background(Color(0x33FFFFFF)))
        .semantics(mergeDescendants = true) {}
        .padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        AchievementBadge(achievement, unlocked, 104.dp)
        Text(achievement.title, color = if (unlocked) Plum else GoldPale, fontFamily = Fraunces, fontWeight = FontWeight.Bold,
            fontSize = 17.sp, lineHeight = 20.sp, textAlign = TextAlign.Center, maxLines = 2, modifier = Modifier.padding(top = 8.dp))
        Text(achievement.description, color = if (unlocked) InkSoftLib else GoldPale.copy(alpha = 0.8f), fontFamily = Andika,
            fontSize = 13.sp, lineHeight = 16.sp, textAlign = TextAlign.Center, maxLines = 3, modifier = Modifier.padding(top = 2.dp))
        ACHIEVEMENT_PRIZES[achievement.id]?.let {
            Text("🎁 Premio: ${it.one}", color = if (unlocked) GoldDeep else GoldLeaf, fontFamily = Fraunces, fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Spacer(Modifier.height(6.dp))
        if (unlocked) Text("Conseguido el ${LocalDate.ofEpochDay(unlockedDay!!).format(dateFormat)}", color = Amethyst,
            fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        else if (achievement.goal > 1) {
            val done = achievement.progress(stats).coerceAtMost(achievement.goal)
            Box(Modifier.fillMaxWidth(0.8f).height(6.dp).clip(CircleShape).background(Color(0x33000000))) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(done.toFloat() / achievement.goal).clip(CircleShape).background(GoldBrush))
            }
            Text("$done de ${achievement.goal}", color = GoldPale, fontFamily = Fraunces, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
        }
    }
}

private val InkSoftLib = Color(0xFF6B5446)
