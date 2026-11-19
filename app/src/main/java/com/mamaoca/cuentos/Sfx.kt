package com.mamaoca.cuentos

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/** Efectos de sonido sintetizados en memoria: no hay archivos de audio en la app. */
enum class Sound { Pop, Page, Fanfare, Right, Wrong, Snap, Splash, Boing, Magic, Drum, Bell, Whistle, Applause, Thunder }

object Sfx {
    private const val RATE = 22050
    private val handler = Handler(Looper.getMainLooper())
    private val buffers: Map<Sound, ShortArray> by lazy {
        mapOf(Sound.Pop to pop(), Sound.Page to swoosh(), Sound.Fanfare to fanfare(), Sound.Right to right(),
            Sound.Wrong to wrong(), Sound.Snap to snap(), Sound.Splash to splash(), Sound.Boing to boing(), Sound.Magic to magic(),
            Sound.Drum to drum(), Sound.Bell to bell(), Sound.Whistle to whistle(), Sound.Applause to applause(), Sound.Thunder to thunder())
    }

    /** Cuánto dura un sonido, en milisegundos (para esperar a que termine en las historias animadas). */
    fun duration(sound: Sound): Long = (buffers[sound]?.size ?: 0) * 1000L / RATE

    fun play(sound: Sound) {
        val data = buffers[sound] ?: return
        runCatching {
            val track = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setTransferMode(AudioTrack.MODE_STATIC).setBufferSizeInBytes(data.size * 2).build()
            track.write(data, 0, data.size)
            track.play()
            handler.postDelayed({ runCatching { track.release() } }, data.size * 1000L / RATE + 200)
        }
    }

    private fun buffer(seconds: Float) = FloatArray((seconds * RATE).toInt())

    private fun tone(out: FloatArray, freq: Float, start: Float, dur: Float, vol: Float, triangle: Boolean) {
        val s0 = (start * RATE).toInt(); val n = (dur * RATE).toInt()
        for (i in 0 until n) {
            val idx = s0 + i; if (idx >= out.size) break
            val t = i.toFloat() / RATE
            val phase = (freq * t) % 1f
            val wave = if (triangle) 4 * abs(phase - 0.5f) - 1 else sin(2 * PI * phase).toFloat()
            val env = min(1f, t / 0.015f) * exp(-4f * t / dur)
            out[idx] += wave * env * vol
        }
    }

    private fun min(a: Float, b: Float) = if (a < b) a else b

    private fun pop() = buffer(0.3f).also {
        tone(it, 660f, 0f, 0.12f, 0.35f, false); tone(it, 990f, 0.06f, 0.18f, 0.28f, false)
    }.toPcm()

    private fun fanfare() = buffer(2.1f).also { out ->
        listOf(523f to 0f, 659f to 0.15f, 784f to 0.3f, 1047f to 0.45f, 784f to 0.7f, 1047f to 0.85f).forEachIndexed { i, (f, t) ->
            tone(out, f, t, if (i > 3) 1.1f else 0.35f, 0.25f, true)
        }
    }.toPcm()

    /** Respuesta correcta: dos notas que suben. */
    private fun right() = buffer(0.6f).also { tone(it, 784f, 0f, 0.2f, 0.3f, true); tone(it, 1175f, 0.12f, 0.45f, 0.3f, true) }.toPcm()
    /** Respuesta equivocada: dos notas suaves que bajan, sin asustar. */
    private fun wrong() = buffer(0.55f).also { tone(it, 330f, 0f, 0.22f, 0.25f, false); tone(it, 262f, 0.16f, 0.35f, 0.25f, false) }.toPcm()
    /** Pieza de rompecabezas que encaja. */
    private fun snap() = buffer(0.15f).also { tone(it, 1400f, 0f, 0.05f, 0.3f, false); tone(it, 700f, 0.02f, 0.1f, 0.25f, true) }.toPcm()
    /** Balde de pintura: un «plop» que baja. */
    private fun splash() = buffer(0.3f).also { out ->
        for (i in out.indices) {
            val t = i.toFloat() / RATE
            out[i] = sin(2 * PI * (500f - 900f * t) * t).toFloat() * exp(-10f * t) * 0.4f
        }
    }.toPcm()

    // ---- sonidos para las historias con calcomanías ----

    /** Muelle: una nota que baja y sube temblando. */
    private fun boing() = buffer(0.7f).also { out ->
        var phase = 0.0
        for (i in out.indices) {
            val t = i.toFloat() / RATE
            val f = 140f + 260f * exp(-5f * t) * (1f + 0.35f * sin(2 * PI * 9 * t).toFloat())
            phase += f / RATE
            out[i] = sin(2 * PI * phase).toFloat() * exp(-3f * t) * 0.45f
        }
    }.toPcm()

    /** Magia: notas agudas que suben, como un destello. */
    private fun magic() = buffer(1.2f).also { out ->
        listOf(1047f, 1319f, 1568f, 2093f, 2637f).forEachIndexed { i, f -> tone(out, f, i * 0.09f, 0.6f, 0.16f, true) }
        val rnd = Random(3)
        repeat(14) { tone(out, 2500f + rnd.nextFloat() * 2500f, 0.2f + rnd.nextFloat() * 0.8f, 0.12f, 0.05f, false) }
    }.toPcm()

    /** Tambor: dos golpes graves. */
    private fun drum() = buffer(0.9f).also { out ->
        val rnd = Random(5)
        listOf(0f, 0.35f).forEach { start ->
            val s0 = (start * RATE).toInt()
            for (i in 0 until (0.4f * RATE).toInt()) {
                val t = i.toFloat() / RATE
                val idx = s0 + i; if (idx >= out.size) break
                val body = sin(2 * PI * (110f - 50f * t) * t).toFloat() * exp(-9f * t)
                val hit = (rnd.nextFloat() * 2 - 1) * exp(-40f * t) * 0.4f
                out[idx] += (body + hit) * 0.6f
            }
        }
    }.toPcm()

    /** Campana: una nota limpia con armónicos que se apaga despacio. */
    private fun bell() = buffer(1.8f).also { out ->
        listOf(880f to 0.3f, 1760f to 0.14f, 2640f to 0.08f, 1320f to 0.06f).forEach { (f, v) -> tone(out, f, 0f, 1.8f, v, false) }
    }.toPcm()

    /** Silbato: un pitido agudo con un trino. */
    private fun whistle() = buffer(0.9f).also { out ->
        var phase = 0.0
        for (i in out.indices) {
            val t = i.toFloat() / RATE
            phase += (1900f + 120f * sin(2 * PI * 18 * t).toFloat()) / RATE
            val env = min(1f, t / 0.03f) * min(1f, (0.9f - t) / 0.08f)
            out[i] = sin(2 * PI * phase).toFloat() * env * 0.3f
        }
    }.toPcm()

    /** Aplausos: muchas palmadas cortas de ruido. */
    private fun applause() = buffer(1.8f).also { out ->
        val rnd = Random(9)
        repeat(90) {
            val s0 = (rnd.nextFloat() * 1.6f * RATE).toInt()
            var low = 0f
            for (i in 0 until (0.03f * RATE).toInt()) {
                val idx = s0 + i; if (idx >= out.size) break
                low += 0.6f * ((rnd.nextFloat() * 2 - 1) - low)
                out[idx] += low * exp(-i / (0.006f * RATE)) * 0.5f
            }
        }
    }.toPcm()

    /** Trueno: ruido muy grave que retumba y se apaga. */
    private fun thunder() = buffer(2.2f).also { out ->
        val rnd = Random(13); var low = 0f; var lower = 0f
        for (i in out.indices) {
            val t = i.toFloat() / RATE
            low += 0.04f * ((rnd.nextFloat() * 2 - 1) - low)
            lower += 0.5f * (low - lower)
            out[i] = lower * 9f * min(1f, t / 0.05f) * exp(-1.6f * t) * (1f + 0.5f * sin(2 * PI * 3 * t).toFloat())
        }
    }.toPcm()

    /** Ruido filtrado con barrido de frecuencia: suena como una hoja que pasa. */
    private fun swoosh() = buffer(0.5f).also { out ->
        val rnd = Random(7); var low = 0f; var band = 0f
        for (i in out.indices) {
            val p = i.toFloat() / out.size
            val cutoff = 0.05f + 0.35f * p
            val noise = rnd.nextFloat() * 2 - 1
            low += cutoff * (noise - low)
            band += 0.5f * (low - band)
            out[i] = (low - band) * 2.6f * sin(PI * p).toFloat().pow(2)
        }
    }.toPcm()

    private fun FloatArray.toPcm() = ShortArray(size) { (this[it].coerceIn(-1f, 1f) * 30000).toInt().toShort() }
}
