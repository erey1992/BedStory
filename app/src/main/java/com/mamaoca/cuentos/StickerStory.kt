package com.mamaoca.cuentos

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.coroutines.resume
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/** Una calcomanía pegada: posición del centro (0 a 1 del lienzo), alto relativo al lienzo, giro en grados y espejo. */
class PlacedSticker(val rewardId: String, x: Float, y: Float, scale: Float, rotation: Float = 0f, flip: Boolean = false,
                    val uid: Long = nextUid()) {
    var x by mutableFloatStateOf(x)
    var y by mutableFloatStateOf(y)
    var scale by mutableFloatStateOf(scale)
    var rotation by mutableFloatStateOf(rotation)
    var flip by mutableStateOf(flip)
    fun toJson() = JSONObject().put("uid", uid).put("r", rewardId).put("x", x.toDouble()).put("y", y.toDouble())
        .put("s", scale.toDouble()).put("rot", rotation.toDouble()).put("f", flip)
}

private var uidCounter = System.currentTimeMillis()
internal fun nextUid() = ++uidCounter

enum class EffectGroup(val label: String) { Entrance("Entrada"), Move("Movimiento"), Exit("Salida") }

/** Las animaciones que puede tener una calcomanía. [label] lleva un emoji para que los niños las reconozcan sin leer. */
enum class Effect(val group: EffectGroup, val label: String, val ms: Int) {
    FadeIn(EffectGroup.Entrance, "✨ Aparecer", 800),
    PopIn(EffectGroup.Entrance, "🎈 Saltar adentro", 800),
    FlyInLeft(EffectGroup.Entrance, "➡️ Llegar por la izquierda", 1100),
    FlyInRight(EffectGroup.Entrance, "⬅️ Llegar por la derecha", 1100),
    FlyInTop(EffectGroup.Entrance, "⬇️ Caer del cielo", 1000),
    SpinIn(EffectGroup.Entrance, "🌀 Llegar girando", 1000),
    Jump(EffectGroup.Move, "🦘 Saltar", 900),
    Dance(EffectGroup.Move, "💃 Bailar", 1200),
    Spin(EffectGroup.Move, "🔄 Dar una vuelta", 900),
    Pulse(EffectGroup.Move, "💓 Latir", 900),
    Shake(EffectGroup.Move, "〰️ Temblar", 700),
    WalkRight(EffectGroup.Move, "👉 Ir a la derecha", 1300),
    WalkLeft(EffectGroup.Move, "👈 Ir a la izquierda", 1300),
    WalkUp(EffectGroup.Move, "👆 Subir", 1100),
    WalkDown(EffectGroup.Move, "👇 Bajar", 1100),
    FadeOut(EffectGroup.Exit, "💨 Desaparecer", 800),
    ShrinkOut(EffectGroup.Exit, "🤏 Encogerse", 800),
    FlyOutLeft(EffectGroup.Exit, "⬅️ Irse por la izquierda", 1100),
    FlyOutRight(EffectGroup.Exit, "➡️ Irse por la derecha", 1100),
    FlyOutTop(EffectGroup.Exit, "🚀 Salir volando", 1000),
    SpinOut(EffectGroup.Exit, "🌀 Irse girando", 1000),
}

/** Sonidos de la lista; los grabados se guardan como archivo. */
val STORY_SOUNDS = linkedMapOf(
    "pop" to ("💥 Pop" to Sound.Pop), "boing" to ("🤸 Boing" to Sound.Boing), "magic" to ("✨ Magia" to Sound.Magic),
    "drum" to ("🥁 Tambor" to Sound.Drum), "bell" to ("🔔 Campana" to Sound.Bell), "whistle" to ("😗 Silbato" to Sound.Whistle),
    "applause" to ("👏 Aplausos" to Sound.Applause), "thunder" to ("⛈️ Trueno" to Sound.Thunder),
    "fanfare" to ("🎺 Fanfarria" to Sound.Fanfare), "wind" to ("🍃 Viento" to Sound.Page),
)

/** Un paso de la historia: una animación o un sonido de una calcomanía. Su número es su lugar en la lista de la diapositiva. */
class Step(val itemUid: Long, val effect: Effect? = null, val sound: String? = null, val uid: Long = nextUid()) {
    val label: String get() = effect?.label ?: sound?.let { s ->
        if (s.startsWith("rec:")) "🎤 Mi voz" else STORY_SOUNDS[s.removePrefix("sfx:")]?.first ?: "🎵 Sonido"
    } ?: "?"
    fun toJson() = JSONObject().put("uid", uid).put("item", itemUid).put("fx", effect?.name).put("snd", sound)
}

/** Una diapositiva: su fondo, sus calcomanías y los pasos que se reproducen en orden. */
class Slide(background: String? = null) {
    var background by mutableStateOf(background)
    val items = mutableStateListOf<PlacedSticker>()
    val steps = mutableStateListOf<Step>()
    fun stepsOf(item: PlacedSticker) = steps.filter { it.itemUid == item.uid }
    fun remove(item: PlacedSticker) { items.remove(item); steps.removeAll { it.itemUid == item.uid } }
}

/** La historia con calcomanías: varias diapositivas. Se guarda en un archivo para seguir otro día. */
class StickerStory(private val context: Context) {
    private val file = File(context.filesDir, "historia.json")
    val slides = mutableStateListOf<Slide>()

    init {
        runCatching { load() }
        if (slides.isEmpty()) slides.add(migrateOldScene())
    }

    private fun load() {
        val json = JSONObject(file.readText())
        val list = json.getJSONArray("slides")
        for (i in 0 until list.length()) {
            val s = list.getJSONObject(i)
            val slide = Slide(s.optString("bg").ifEmpty { null })
            val items = s.getJSONArray("items")
            for (j in 0 until items.length()) items.getJSONObject(j).let {
                slide.items.add(PlacedSticker(it.getString("r"), it.getDouble("x").toFloat(), it.getDouble("y").toFloat(),
                    it.getDouble("s").toFloat(), it.getDouble("rot").toFloat(), it.getBoolean("f"), it.getLong("uid")))
            }
            val steps = s.getJSONArray("steps")
            for (j in 0 until steps.length()) steps.getJSONObject(j).let {
                slide.steps.add(Step(it.getLong("item"), it.optString("fx").ifEmpty { null }?.let { n -> runCatching { Effect.valueOf(n) }.getOrNull() },
                    it.optString("snd").ifEmpty { null }, it.getLong("uid")))
            }
            slides.add(slide)
        }
        // los números nuevos siguen después de los guardados, para que nunca se repitan
        uidCounter = maxOf(uidCounter, slides.maxOfOrNull { sl -> (sl.items.map { it.uid } + sl.steps.map { it.uid }).maxOrNull() ?: 0L } ?: 0L)
    }

    /** La creación de antes (una sola escena, sin animaciones) pasa a ser la primera diapositiva. */
    private fun migrateOldScene(): Slide {
        val prefs = context.getSharedPreferences("mamaoca", 0)
        return Slide(prefs.getString("stickerBg", null)).apply {
            prefs.getString("stickerScene", "")!!.split(';').filter { it.isNotBlank() }.forEach { line ->
                runCatching {
                    val p = line.split(',')
                    items.add(PlacedSticker(p[0], p[1].toFloat(), p[2].toFloat(), p[3].toFloat(), p[4].toFloat(), p[5].toBoolean()))
                }
            }
        }
    }

    fun encode(): String = JSONObject().put("slides", JSONArray().apply {
        slides.forEach { s ->
            put(JSONObject().put("bg", s.background ?: "")
                .put("items", JSONArray().apply { s.items.forEach { put(it.toJson()) } })
                .put("steps", JSONArray().apply { s.steps.forEach { put(it.toJson()) } }))
        }
    }).toString()

    fun save() {
        runCatching { file.writeText(encode()) }
        // las grabaciones que ya no usa ningún paso se borran
        val used = slides.flatMap { it.steps }.mapNotNull { it.sound?.removePrefix("rec:") }.toSet()
        voiceDir(context).listFiles()?.forEach { if (it.name !in used) it.delete() }
    }
}

internal fun voiceDir(context: Context) = File(context.filesDir, "voces").apply { mkdirs() }

// ---------------- Reproducción ----------------

/** Cómo se ve una calcomanía en este momento de la reproducción: desplazada, más grande o pequeña, girada y con transparencia. */
data class Pose(val dx: Float = 0f, val dy: Float = 0f, val scale: Float = 1f, val rot: Float = 0f, val alpha: Float = 1f)

private fun easeOut(t: Float) = 1 - (1 - t) * (1 - t) * (1 - t)
private fun easeBack(t: Float): Float { val c = 1.9f; val u = t - 1; return 1 + (c + 1) * u * u * u + c * u * u }

/** La pose de [item] a mitad de [effect] (t de 0 a 1), partiendo de su posición de reposo [base]. */
fun poseOf(effect: Effect, t: Float, item: PlacedSticker, base: Pose): Pose {
    val e = easeOut(t)
    return when (effect) {
        Effect.FadeIn -> base.copy(alpha = t)
        Effect.PopIn -> base.copy(scale = easeBack(t).coerceAtLeast(0f), alpha = (t * 3).coerceAtMost(1f))
        Effect.FlyInLeft -> base.copy(dx = base.dx - (item.x + base.dx + 0.3f) * (1 - e), alpha = 1f)
        Effect.FlyInRight -> base.copy(dx = base.dx + (1.3f - item.x - base.dx) * (1 - e), alpha = 1f)
        Effect.FlyInTop -> base.copy(dy = base.dy - (item.y + base.dy + 0.4f) * (1 - easeBack(t).coerceAtMost(1f)), alpha = 1f)
        Effect.SpinIn -> base.copy(rot = -360f * (1 - e), scale = e, alpha = 1f)
        Effect.Jump -> base.copy(dy = base.dy - 0.12f * abs(sin(2 * PI * t)).toFloat())
        Effect.Dance -> base.copy(rot = 16f * sin(4 * PI * t).toFloat(), dy = base.dy - 0.03f * abs(sin(4 * PI * t)).toFloat())
        Effect.Spin -> base.copy(rot = 360f * e)
        Effect.Pulse -> base.copy(scale = 1f + 0.22f * abs(sin(2 * PI * t)).toFloat())
        Effect.Shake -> base.copy(dx = base.dx + 0.018f * sin(12 * PI * t).toFloat())
        Effect.WalkRight -> base.copy(dx = base.dx + 0.22f * e, dy = base.dy - 0.025f * abs(sin(6 * PI * t)).toFloat())
        Effect.WalkLeft -> base.copy(dx = base.dx - 0.22f * e, dy = base.dy - 0.025f * abs(sin(6 * PI * t)).toFloat())
        Effect.WalkUp -> base.copy(dy = base.dy - 0.2f * e)
        Effect.WalkDown -> base.copy(dy = base.dy + 0.2f * e)
        Effect.FadeOut -> base.copy(alpha = 1 - t)
        Effect.ShrinkOut -> base.copy(scale = 1 - e)
        Effect.FlyOutLeft -> base.copy(dx = base.dx - (item.x + base.dx + 0.3f) * easeOut(t * t))
        Effect.FlyOutRight -> base.copy(dx = base.dx + (1.3f - item.x - base.dx) * easeOut(t * t))
        Effect.FlyOutTop -> base.copy(dy = base.dy - (item.y + base.dy + 0.4f) * easeOut(t * t))
        Effect.SpinOut -> base.copy(rot = 360f * e, scale = 1 - e)
    }
}

/** Dónde queda la calcomanía al terminar [effect]: los movimientos la cambian de sitio, las salidas la esconden. */
fun restAfter(effect: Effect, item: PlacedSticker, base: Pose): Pose = when (effect.group) {
    EffectGroup.Exit -> base.copy(alpha = 0f)
    EffectGroup.Entrance -> base.copy(alpha = 1f)
    EffectGroup.Move -> poseOf(effect, 1f, item, base).copy(rot = 0f, scale = 1f)
}

/** Reproduce las diapositivas: guarda la pose de cada calcomanía para que el lienzo la dibuje. */
class StoryPlayer(private val context: Context) {
    var playing by mutableStateOf(false); private set
    var slideIndex by mutableStateOf(0); private set
    /** El paso que se está reproduciendo (para resaltar su número). */
    var currentStep by mutableStateOf<Step?>(null); private set
    val poses = mutableStateMapOf<Long, Pose>()

    /** Antes de empezar: las calcomanías con animación de entrada están escondidas. */
    private fun reset(slide: Slide) {
        poses.clear()
        slide.items.forEach { item ->
            val first = slide.stepsOf(item).firstOrNull { it.effect != null }
            poses[item.uid] = Pose(alpha = if (first?.effect?.group == EffectGroup.Entrance) 0f else 1f)
        }
    }

    suspend fun playAll(story: StickerStory, sound: Boolean, from: Int = 0) {
        playing = true
        try {
            for (i in from until story.slides.size) {
                slideIndex = i
                val slide = story.slides[i]
                reset(slide)
                delay(500)
                for (step in slide.steps.toList()) runStep(slide, step, sound)
                currentStep = null
                delay(1300)
            }
        } finally {
            playing = false; currentStep = null; poses.clear()
        }
    }

    /** Prueba un solo paso sobre la calcomanía, para que se vea cómo queda al elegirlo. */
    suspend fun preview(slide: Slide, step: Step, sound: Boolean) {
        poses.clear()
        try { runStep(slide, step, sound) } finally { delay(250); poses.clear(); currentStep = null }
    }

    private suspend fun runStep(slide: Slide, step: Step, sound: Boolean) {
        val item = slide.items.firstOrNull { it.uid == step.itemUid } ?: return
        currentStep = step
        val base = poses[item.uid] ?: Pose()
        step.effect?.let { fx ->
            animate(0f, 1f, animationSpec = tween(fx.ms)) { t, _ -> poses[item.uid] = poseOf(fx, t, item, base) }
            poses[item.uid] = restAfter(fx, item, base)
        }
        step.sound?.let { playSound(it, sound) }
    }

    private suspend fun playSound(key: String, enabled: Boolean) {
        if (!enabled) { delay(400); return }
        if (key.startsWith("rec:")) playVoice(File(voiceDir(context), key.removePrefix("rec:")))
        else STORY_SOUNDS[key.removePrefix("sfx:")]?.second?.let { Sfx.play(it); delay(Sfx.duration(it).coerceAtMost(2500)) }
    }

    /** Reproduce una grabación y espera a que termine (como mucho 15 segundos). */
    suspend fun playVoice(file: File) {
        if (!file.exists()) return
        val player = MediaPlayer()
        try {
            player.setDataSource(file.path); player.prepare()
            withTimeoutOrNull(15_000) {
                suspendCancellableCoroutine { cont ->
                    player.setOnCompletionListener { if (cont.isActive) cont.resume(Unit) }
                    cont.invokeOnCancellation { runCatching { player.stop() } }
                    player.start()
                }
            }
        } catch (_: Exception) {
        } finally { player.release() }
    }
}

/** Graba la voz del niño (como mucho 10 segundos) en la carpeta de voces de la app. No sale del teléfono. */
class VoiceRecorder(private val context: Context) {
    var recording by mutableStateOf(false); private set
    /** Si la grabación se corta sola al llegar a 10 segundos, recibe el archivo. */
    var onAutoStop: ((String?) -> Unit)? = null
    private var recorder: MediaRecorder? = null
    private var file: File? = null

    fun start(): Boolean = runCatching {
        val f = File(voiceDir(context), "voz-${System.currentTimeMillis()}.m4a")
        @Suppress("DEPRECATION")
        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
        r.setAudioSource(MediaRecorder.AudioSource.MIC)
        r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        r.setAudioSamplingRate(44100); r.setAudioEncodingBitRate(96000)
        r.setMaxDuration(10_000)
        r.setOutputFile(f.path)
        r.setOnInfoListener { _, what, _ -> if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) onAutoStop?.invoke(stop()) }
        r.prepare(); r.start()
        recorder = r; file = f; recording = true
    }.isSuccess

    /** Termina la grabación y devuelve el nombre del archivo (o null si fue demasiado corta o falló). */
    fun stop(): String? {
        val r = recorder ?: return null
        recorder = null; recording = false
        val ok = runCatching { r.stop() }.isSuccess
        r.release()
        return file?.takeIf { ok && it.length() > 0 }?.name.also { if (it == null) file?.delete() }
    }
}
