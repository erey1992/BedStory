package com.mamaoca.cuentos

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class Narrator(context: Context) {
    private val handler = Handler(Looper.getMainLooper())
    private var generation = 0
    private var completion: (() -> Unit)? = null
    private var segments = emptyList<Segment>()
    var ready by mutableStateOf(false); private set
    var message by mutableStateOf<String?>(null); private set
    var activeSegment by mutableIntStateOf(-1); private set
    private lateinit var engine: TextToSpeech
    private var closed = false
    private var initialized = false

    init {
        engine = TextToSpeech(context.applicationContext) { status ->
            handler.post {
                if (!closed) {
                    initialized = status == TextToSpeech.SUCCESS
                    if (initialized) refreshVoices()
                    else message = "No hay un motor de voz disponible. Configura la síntesis de voz en Android."
                }
            }
        }
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) = Unit
            override fun onDone(id: String?) {
                handler.post {
                    if (id == "$generation:$activeSegment") {
                        if (activeSegment + 1 < segments.size) speakSegment(activeSegment + 1)
                        else { val done = completion; completion = null; activeSegment = -1; done?.invoke() }
                    }
                }
            }
            @Deprecated("Android callback")
            override fun onError(id: String?) {
                handler.post {
                    if (id == "$generation:$activeSegment") {
                        stop(); message = "No se pudo reproducir la narración. Comprueba la voz instalada."
                    }
                }
            }
        })
    }
    fun refreshVoices() {
        if (closed || !initialized) return
        val voices = engine.voices.orEmpty().filter { it.locale.language == "es" && !it.isNetworkConnectionRequired }.sortedBy { it.name }
        ready = voices.isNotEmpty()
        if (ready) { engine.voice = voices.first(); engine.setSpeechRate(0.85f); message = null }
        else message = "Instala una voz en español sin conexión en los ajustes de voz de Android."
    }
    fun speak(text: List<Segment>, onDone: () -> Unit) {
        stop()
        if (!ready || text.isEmpty()) return
        message = null; segments = text; completion = onDone
        speakSegment(0)
    }
    private fun speakSegment(index: Int) {
        activeSegment = index
        val segment = segments[index]
        val hash = (segment.speaker ?: "Narrador").searchKey().hashCode().toLong().let { kotlin.math.abs(it) }
        val voices = engine.voices.orEmpty().filter { it.locale.language == "es" && !it.isNetworkConnectionRequired }.sortedBy { it.name }
        if (voices.isNotEmpty()) engine.voice = voices[(hash % voices.size).toInt()]
        engine.setPitch(if (segment.speaker == null) 1f else 0.85f + (hash % 6) * 0.08f)
        engine.setSpeechRate(if (segment.speaker == null) 0.85f else 0.9f)
        if (engine.speak(segment.text, TextToSpeech.QUEUE_FLUSH, null, "$generation:$index") == TextToSpeech.ERROR) {
            stop(); message = "No se pudo iniciar la narración. Comprueba la voz instalada."
        }
    }
    fun stop() { generation++; completion = null; activeSegment = -1; engine.stop() }
    fun close() { closed = true; stop(); engine.shutdown(); handler.removeCallbacksAndMessages(null) }
}
