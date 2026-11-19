package com.mamaoca.cuentos

import android.content.Context
import org.json.JSONArray
import java.text.Normalizer

data class Segment(val speaker: String?, val text: String)
/** [image] es una ilustración pintada (assets/art); si no hay, se dibuja la escena vectorial [scene] o, sin ella, un adorno. */
data class StoryPage(val scene: String?, val text: List<Segment>, val image: String? = null)
/** Pregunta de comprensión: [answers] trae primero la correcta (en pantalla se barajan). */
data class QuizQuestion(val question: String, val answers: List<String>)
/** Tema delicado que se puede ocultar desde el rincón de papás. */
data class Topic(val id: String, val label: String)
data class Story(val id: String, val title: String, val description: String,
                 val color: String, val cover: String?, val pages: List<StoryPage>, val coverImage: String? = null,
                 /** Poesía: cada segmento es una estrofa y se respetan los versos. */ val poem: Boolean = false,
                 /** Categorías (animales, magia...) para los logros. */ val tags: Set<String> = emptySet(),
                 val quiz: List<QuizQuestion> = emptyList(),
                 /** Temas delicados que trata (muerte, adopción...). */ val sensitive: Set<String> = emptySet())

fun String.searchKey(): String = Normalizer.normalize(this, Normalizer.Form.NFD)
    .replace("\\p{M}+".toRegex(), "").lowercase(java.util.Locale.ROOT)

internal fun org.json.JSONObject.strings(key: String): List<String> =
    optJSONArray(key)?.let { a -> List(a.length()) { a.getString(it) } } ?: emptyList()

private fun org.json.JSONObject.optStringOrNull(key: String) = if (isNull(key)) null else getString(key)

class StoryRepository(private val context: Context) {
    fun load(): List<Story> {
        val json = JSONArray(context.assets.open("stories.json").bufferedReader().use { it.readText() })
        return List(json.length()) { i ->
            val s = json.getJSONObject(i)
            val pages = s.getJSONArray("pages")
            Story(s.getString("id"), s.getString("title"), s.getString("description"),
                s.getString("color"), s.optStringOrNull("cover"), List(pages.length()) { p ->
                    val page = pages.getJSONObject(p)
                    val text = page.getJSONArray("text")
                    StoryPage(page.optStringOrNull("scene"), List(text.length()) { t ->
                        val seg = text.getJSONObject(t)
                        Segment(if (seg.isNull("speaker")) null else seg.getString("speaker"), seg.getString("text"))
                    }, page.optStringOrNull("image"))
                }, s.optStringOrNull("coverImage"), s.optBoolean("poem", false),
                s.strings("tags").toSet(),
                s.optJSONArray("quiz")?.let { q -> List(q.length()) { i ->
                    val item = q.getJSONObject(i)
                    QuizQuestion(item.getString("question"), item.strings("answers"))
                } } ?: emptyList(),
                s.strings("sensitive").toSet())
        }
    }

    fun topics(): List<Topic> {
        val json = JSONArray(context.assets.open("topics.json").bufferedReader().use { it.readText() })
        return List(json.length()) { json.getJSONObject(it).let { t -> Topic(t.getString("id"), t.getString("label")) } }
    }
}
