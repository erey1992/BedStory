package com.mamaoca.cuentos

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

private val RightGreen = Color(0xFF2E9E5B)
private val WrongRed = Color(0xFFD64550)

/** Tres preguntas al terminar el cuento. Cada acierto es una estrella. */
@Composable
fun QuizScreen(vm: StoryViewModel, story: Story, narrator: Narrator, play: () -> Unit) {
    var index by remember(story.id) { mutableIntStateOf(0) }
    var correct by remember(story.id) { mutableIntStateOf(0) }
    var chosen by remember(story.id, index) { mutableStateOf<Int?>(null) }
    val question = story.quiz[index]
    // las respuestas se barajan, pero siempre igual para la misma pregunta
    val order = remember(story.id, index) { question.answers.indices.shuffled(Random(story.id.hashCode() * 31 + index)) }
    val shake = remember(story.id, index) { Animatable(0f) }
    DisposableEffect(Unit) { onDispose { narrator.stop() } }
    fun speak() = narrator.speak(listOf(Segment(null, question.question)) + order.mapIndexed { i, a ->
        Segment(null, "${"ABC"[i]}: ${question.answers[a]}") }) {}
    LaunchedEffect(story.id, index) { if (vm.listening) { delay(500); speak() } }
    LaunchedEffect(chosen) {
        val pick = chosen ?: return@LaunchedEffect
        narrator.stop()
        val right = pick == 0
        if (vm.sound) Sfx.play(if (right) Sound.Right else Sound.Wrong)
        if (!right) {
            repeat(3) { shake.animateTo(1f, tween(60)); shake.animateTo(-1f, tween(60)) }
            shake.animateTo(0f, tween(60))
        }
        if (right) correct++
        delay(if (right) 1200 else 2200) // si se equivocó, un momento para ver la respuesta correcta
        if (index < story.quiz.lastIndex) index++ else vm.finishQuiz(correct)
    }
    BoxWithConstraints(Modifier.fillMaxSize().velvet(Plum), contentAlignment = Alignment.Center) {
        val fit = minOf(1f, maxHeight / 470.dp, maxWidth / 760.dp)
        Column(Modifier.wrapContentSize(unbounded = true).graphicsLayer { scaleX = fit; scaleY = fit }.width(720.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text("¿Cuánto recuerdas de «${story.title}»?", style = TextStyle(brush = GoldBrush, fontFamily = Fraunces,
                fontWeight = FontWeight.Bold, fontSize = 22.sp, textAlign = TextAlign.Center))
            Row(Modifier.padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Pregunta ${index + 1} de ${story.quiz.size}", color = GoldPale, fontFamily = Fraunces, fontSize = 16.sp)
                repeat(story.quiz.size) { i ->
                    Box(Modifier.size(12.dp).clip(CircleShape).background(when {
                        i < index -> GoldLeaf; i == index -> GoldPale; else -> Color(0x44FFFFFF) }))
                }
            }
            Row(Modifier.fillMaxWidth().shadow(10.dp, RoundedCornerShape(18.dp)).clip(RoundedCornerShape(18.dp)).paper()
                .goldFrame(18.dp, 2.dp).padding(horizontal = 22.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(question.question, color = Plum, fontFamily = Andika, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp,
                    modifier = Modifier.weight(1f))
                Box(Modifier.size(48.dp).clip(CircleShape).background(GoldBrush).goldBorder(CircleShape, 1.5.dp)
                    .clickable(role = Role.Button) { speak() }.semantics { contentDescription = "Escuchar la pregunta" },
                    contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.VolumeUp, null, tint = Plum) }
            }
            Column(Modifier.padding(top = 16.dp).graphicsLayer { translationX = shake.value * 10.dp.toPx() },
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                order.forEachIndexed { i, a ->
                    val state = when {
                        chosen == null -> 0
                        a == 0 -> 1 // la correcta se ilumina siempre al responder
                        a == chosen -> 2
                        else -> 3
                    }
                    AnswerButton("ABC"[i], question.answers[a], state) { if (chosen == null) { play(); chosen = a } }
                }
            }
            Text("Saltar preguntas", color = GoldPale.copy(alpha = 0.7f), fontFamily = Fraunces, fontSize = 14.sp,
                modifier = Modifier.padding(top = 14.dp).clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button) { vm.skipQuiz() }
                    .padding(horizontal = 10.dp, vertical = 4.dp))
        }
    }
}

/** [state]: 0 sin responder, 1 correcta, 2 la equivocada que se eligió, 3 las demás. */
@Composable
private fun AnswerButton(letter: Char, text: String, state: Int, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    val back = when (state) { 1 -> RightGreen; 2 -> WrongRed; else -> Velvet }
    Row(Modifier.fillMaxWidth().height(64.dp).graphicsLayer { alpha = if (state == 3) 0.45f else 1f }
        .shadow(6.dp, shape).clip(shape).velvet(back).border(2.dp, if (state == 1) GoldPale else GoldLeaf.copy(alpha = 0.6f), shape)
        .clickable(role = Role.Button, enabled = state == 0, onClick = onClick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(GoldBrush), contentAlignment = Alignment.Center) {
            when (state) {
                1 -> Icon(Icons.Default.Check, null, tint = RightGreen)
                2 -> Icon(Icons.Default.Close, null, tint = WrongRed)
                else -> Text(letter.toString(), color = Plum, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        }
        Text(text, color = GoldPale, fontFamily = Andika, fontWeight = FontWeight.Bold, fontSize = 21.sp, maxLines = 2)
    }
}

/** Las estrellas del final: las ganadas en oro, las que faltan en contorno. */
@Composable
fun StarRow(stars: Int, big: Dp = 52.dp, small: Dp = 40.dp) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(3) { i ->
            val pop = remember { Animatable(0f) }
            LaunchedEffect(Unit) { delay(1000L + i * 250L); pop.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 300f)) }
            Icon(if (i < stars) Icons.Default.Star else Icons.Default.StarBorder, null,
                tint = if (i < stars) GoldLeaf else GoldPale.copy(alpha = 0.5f),
                modifier = Modifier.size(if (i == 1) big else small).graphicsLayer {
                    scaleX = pop.value; scaleY = pop.value; rotationZ = (1 - pop.value) * -180f })
        }
    }
}

fun starMessage(stars: Int?) = when (stars) {
    3 -> "¡Perfecto! Entendiste todo el cuento"
    2 -> "¡Muy bien! Casi todo"
    1 -> "¡Buen intento! La próxima vez, más"
    0 -> "Escucha el cuento otra vez y vuelve a intentarlo"
    else -> null
}
