package com.mamaoca.cuentos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.random.Random

private val ParentInk = Color(0xFF3A2A22)
private val ParentSoft = Color(0xFF6B5446)
private val dayFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/** Nombres de las categorías de los logros, para las estadísticas. */
private val TAG_LABELS = listOf(
    "animales" to "Animales", "realeza" to "Reyes y princesas", "magia" to "Magia y hadas", "dragones" to "Dragones",
    "mar" to "El mar", "espacio" to "Estrellas y planetas", "historia" to "Otras épocas", "valores" to "Emociones y valores",
    "marti" to "José Martí",
)

/** El rincón de papás: primero una cuenta que un niño pequeño no sabe hacer, después las estadísticas y los temas. */
@Composable
fun ParentsScreen(vm: StoryViewModel, play: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().velvet(PlumLight)) {
        GameHeader("Rincón de papás", Plum, onBack = { play(); vm.back() })
        if (!open) ParentGate { open = true } else ParentDashboard(vm)
    }
}

@Composable
private fun ParentGate(onOpen: () -> Unit) {
    var seed by remember { mutableIntStateOf(Random.nextInt()) }
    val (a, b) = remember(seed) { Random(seed).let { (6 + it.nextInt(4)) to (6 + it.nextInt(4)) } }
    var typed by remember(seed) { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(40.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.widthIn(max = 360.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Key, null, tint = GoldLeaf, modifier = Modifier.size(64.dp))
            Text("Solo para adultos", style = TextStyle(brush = GoldBrush, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 26.sp))
            Text("Para entrar, resuelve esta cuenta:", color = GoldPale, fontFamily = Andika, fontSize = 17.sp, textAlign = TextAlign.Center)
            Text("$a × $b = ${typed.ifEmpty { "?" }}", color = GoldPale, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 40.sp,
                modifier = Modifier.padding(vertical = 10.dp))
            if (wrong) Text("Esa no es. Prueba con esta otra.", color = Color(0xFFFFB4A8), fontFamily = Andika, fontSize = 15.sp)
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("⌫", "0", "OK")).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { key ->
                        Box(Modifier.size(64.dp).shadow(4.dp, RoundedCornerShape(14.dp)).clip(RoundedCornerShape(14.dp))
                            .then(if (key == "OK") Modifier.background(GoldBrush) else Modifier.velvet(Velvet))
                            .goldBorder(RoundedCornerShape(14.dp), 1.5.dp)
                            .clickable(role = Role.Button) {
                                when (key) {
                                    "⌫" -> typed = typed.dropLast(1)
                                    "OK" -> if (typed.toIntOrNull() == a * b) onOpen() else { wrong = true; seed = Random.nextInt() }
                                    else -> if (typed.length < 3) typed += key
                                }
                            }.semantics { contentDescription = if (key == "⌫") "Borrar" else key }, contentAlignment = Alignment.Center) {
                            if (key == "⌫") Icon(Icons.AutoMirrored.Filled.Backspace, null, tint = GoldPale)
                            else Text(key, color = if (key == "OK") Plum else GoldPale, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParentDashboard(vm: StoryViewModel) {
    val byId = vm.stories.associateBy { it.id }
    val finished = vm.stats.finished
    val stars = vm.quizStars.values
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // cifras principales
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(Icons.AutoMirrored.Filled.MenuBook, "${finished.size} de ${vm.stories.size}", "cuentos distintos leídos", Modifier.weight(1f))
            StatTile(Icons.Default.Replay, "${vm.totalFinishes}", "lecturas terminadas", Modifier.weight(1f))
            StatTile(Icons.Default.CalendarMonth, "${vm.readingDayCount}", "días con lectura", Modifier.weight(1f))
            StatTile(Icons.Default.Whatshot, "${vm.currentStreak}", "días seguidos ahora (mejor: ${vm.stats.longestStreak})", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(Icons.Default.EmojiEvents, "${vm.unlocked.size} de ${ACHIEVEMENTS.size}", "logros conseguidos", Modifier.weight(1f))
            StatTile(Icons.Default.Star, if (stars.isEmpty()) "—" else "%.1f de 3".format(stars.average()), "estrellas de media en las preguntas", Modifier.weight(1f))
            StatTile(Icons.Default.Verified, "${stars.count { it == 3 }}", "cuentos con las 3 preguntas bien", Modifier.weight(1f))
            StatTile(Icons.Default.CardGiftcard, "${vm.ownedRewards.count { it in vm.rewards.byId }}", "premios desbloqueados", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // cuentos por categoría
            Section("Cuentos leídos por tema", Modifier.weight(1f)) {
                TAG_LABELS.forEach { (tag, label) ->
                    val total = vm.stories.count { tag in it.tags }
                    if (total == 0) return@forEach
                    val read = vm.stats.tagCounts[tag] ?: 0
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                        Text(label, color = ParentInk, fontFamily = Andika, fontSize = 15.sp, modifier = Modifier.width(170.dp))
                        Box(Modifier.weight(1f).height(12.dp).clip(CircleShape).background(Color(0x223A2A22))) {
                            Box(Modifier.fillMaxHeight().fillMaxWidth(read.toFloat() / total).clip(CircleShape).background(Amethyst))
                        }
                        Text("  $read / $total", color = ParentSoft, fontFamily = Fraunces, fontSize = 13.sp, modifier = Modifier.width(70.dp))
                    }
                }
            }
            // últimas lecturas
            Section("Últimos cuentos terminados", Modifier.weight(1f)) {
                if (vm.history.isEmpty()) Text("Aquí aparecerán los próximos cuentos que termine.", color = ParentSoft, fontFamily = Andika, fontSize = 15.sp)
                vm.history.take(12).forEach { (day, id) ->
                    val story = byId[id] ?: return@forEach
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                        Text(LocalDate.ofEpochDay(day).format(dayFormat), color = ParentSoft, fontFamily = Fraunces, fontSize = 13.sp,
                            modifier = Modifier.width(92.dp))
                        Text(story.title, color = ParentInk, fontFamily = Andika, fontSize = 15.sp, maxLines = 1,
                            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        vm.quizStars[id]?.let { n -> repeat(3) { i ->
                            Icon(if (i < n) Icons.Default.Star else Icons.Default.StarBorder, null, tint = GoldDeep, modifier = Modifier.size(16.dp))
                        } }
                    }
                }
            }
        }
        // temas delicados
        Section("Temas delicados", Modifier.fillMaxWidth()) {
            Text("Si un tema todavía no es para tu hijo o hija, ocúltalo: sus cuentos no aparecerán en la biblioteca, ni en la búsqueda, ni como cuento del día. Puedes volver a mostrarlos cuando quieras.",
                color = ParentSoft, fontFamily = Andika, fontSize = 15.sp, modifier = Modifier.padding(bottom = 8.dp))
            vm.topics.forEach { topic ->
                val count = vm.stories.count { topic.id in it.sensitive }
                val hidden = topic.id in vm.hiddenTopics
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    Column(Modifier.weight(1f).padding(end = 16.dp)) {
                        Text(topic.label, color = ParentInk, fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                        Text(vm.stories.filter { topic.id in it.sensitive }.joinToString(" · ") { it.title }, color = ParentSoft,
                            fontFamily = Andika, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    Text(if (hidden) "Oculto ($count)  " else "Visible ($count)  ", color = if (hidden) Amethyst else ParentSoft,
                        fontFamily = Fraunces, fontSize = 14.sp)
                    Switch(checked = !hidden, onCheckedChange = { vm.setTopicHidden(topic.id, !it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = GoldPale, checkedTrackColor = Amethyst))
                }
            }
        }
    }
}

@Composable
private fun StatTile(icon: ImageVector, value: String, label: String, modifier: Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Column(modifier.height(118.dp).shadow(6.dp, shape).clip(shape).paper().goldFrame(16.dp, 1.5.dp).padding(12.dp),
        verticalArrangement = Arrangement.Center) {
        Icon(icon, null, tint = Amethyst, modifier = Modifier.size(24.dp))
        Text(value, color = Plum, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 26.sp, maxLines = 1)
        Text(label, color = ParentSoft, fontFamily = Andika, fontSize = 13.sp, lineHeight = 15.sp, maxLines = 2)
    }
}

@Composable
private fun Section(title: String, modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(modifier.shadow(6.dp, shape).clip(shape).paper().goldFrame(16.dp, 1.5.dp).padding(16.dp)) {
        Text(title, color = Plum, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 19.sp, modifier = Modifier.padding(bottom = 8.dp))
        content()
    }
}
