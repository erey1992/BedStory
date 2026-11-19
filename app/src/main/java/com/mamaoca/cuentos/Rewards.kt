package com.mamaoca.cuentos

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

/** Los cuatro tipos de premio que se desbloquean leyendo. [one] se usa en «Desbloqueaste un fondo». */
enum class RewardKind(val key: String, val one: String) {
    Sticker("stickers", "una calcomanía"),
    Background("backgrounds", "un fondo"),
    Coloring("coloring", "un dibujo para colorear"),
    Puzzle("puzzles", "un rompecabezas"),
}

/**
 * Un premio: las calcomanías dibujadas son vectoriales ([svg]); las pintadas y el resto, una imagen de assets ([image]).
 * [story] es el cuento al que pertenece (su calcomanía, su dibujo para colorear, su rompecabezas...).
 */
data class Reward(val id: String, val kind: RewardKind, val title: String?, val image: String?, val svg: String?, val starter: Boolean,
                  val story: String? = null)

class RewardCatalog(val all: List<Reward>) {
    val byId = all.associateBy { it.id }
    fun of(kind: RewardKind) = all.filter { it.kind == kind }

    companion object {
        fun load(context: Context): RewardCatalog {
            val json = JSONObject(context.assets.open("rewards.json").bufferedReader().use { it.readText() })
            return RewardCatalog(RewardKind.entries.flatMap { kind ->
                val list: JSONArray = json.optJSONArray(kind.key) ?: JSONArray()
                List(list.length()) { i ->
                    val r = list.getJSONObject(i)
                    Reward(r.getString("id"), kind, r.optString("title").ifEmpty { null }, r.optString("image").ifEmpty { null },
                        r.optString("svg").ifEmpty { null }, r.optBoolean("starter", false), r.optString("story").ifEmpty { null })
                }
            })
        }
    }
}

/** Algunos logros traen un premio sorpresa del tipo indicado (calcomanías no: esas se ganan leyendo cada cuento). */
val ACHIEVEMENT_PRIZES = mapOf(
    "primer-cuento" to RewardKind.Coloring,
    "lector-curioso" to RewardKind.Background,
    "raton-biblioteca" to RewardKind.Puzzle,
    "devoralibros" to RewardKind.Coloring,
    "gran-lector" to RewardKind.Background,
    "maestro-mama-oca" to RewardKind.Puzzle,
    "amigo-animales" to RewardKind.Background,
    "aprendiz-mago" to RewardKind.Coloring,
    "pequeno-poeta" to RewardKind.Puzzle,
    "racha-3" to RewardKind.Puzzle,
    "racha-5" to RewardKind.Coloring,
    "racha-10" to RewardKind.Background,
    "maraton" to RewardKind.Coloring,
    "oidos-atentos" to RewardKind.Puzzle,
)

/**
 * Reglas de las sorpresas. No todos los cuentos dan premio: al terminar uno nuevo hay una probabilidad,
 * más alta cuantas más estrellas se ganen en las preguntas. Releer también puede dar premio, pero menos.
 */
object RewardRules {
    fun surpriseChance(stars: Int, firstTime: Boolean): Float =
        if (firstTime) listOf(0.35f, 0.45f, 0.55f, 0.7f)[stars.coerceIn(0, 3)] else 0.15f

    private val weights = mapOf(RewardKind.Sticker to 45, RewardKind.Background to 15, RewardKind.Coloring to 20, RewardKind.Puzzle to 20)

    /** Las calcomanías propias de un cuento (su personaje, un objeto, un animal...) que aún no se tienen: se ganan siempre al terminarlo. */
    fun storyStickers(catalog: RewardCatalog, owned: Set<String>, story: String): List<Reward> =
        catalog.all.filter { it.kind == RewardKind.Sticker && it.story == story && it.id !in owned }

    /** El rompecabezas de un cuento, si aún no se tiene: también se gana siempre al terminarlo. */
    fun storyPuzzle(catalog: RewardCatalog, owned: Set<String>, story: String): Reward? =
        catalog.all.firstOrNull { it.kind == RewardKind.Puzzle && it.story == story && it.id !in owned }

    /** Sorpresa al terminar un cuento: primero algo de ese mismo cuento (su dibujo, su rompecabezas o su fondo); si ya se tiene todo, cualquier otra cosa. */
    fun drawFor(catalog: RewardCatalog, owned: Set<String>, random: Random, story: String): List<Reward> {
        val own = catalog.all.filter { it.story == story && it.kind != RewardKind.Sticker && it.id !in owned }
        return if (own.isNotEmpty()) listOf(own.random(random)) else draw(catalog, owned, random)
    }

    /** Elige un premio que aún no se tiene. Las calcomanías vienen de a tres. Vacío si ya se tiene todo. */
    fun draw(catalog: RewardCatalog, owned: Set<String>, random: Random, kind: RewardKind? = null): List<Reward> {
        // las calcomanías de un cuento no salen al azar: se ganan leyendo ese cuento
        val locked = RewardKind.entries.associateWith { k ->
            catalog.of(k).filter { it.id !in owned && !(k == RewardKind.Sticker && it.story != null) }
        }
        val chosen = kind?.takeIf { locked[it]!!.isNotEmpty() } ?: run {
            val open = weights.filterKeys { locked[it]!!.isNotEmpty() }
            if (open.isEmpty()) return emptyList()
            var roll = random.nextInt(open.values.sum())
            open.entries.first { (_, w) -> (roll < w).also { roll -= w } }.key
        }
        return locked[chosen]!!.shuffled(random).take(if (chosen == RewardKind.Sticker) 3 else 1)
    }
}

/** Medallas de los rompecabezas: una por nivel de dificultad (12, 24 y 40 piezas). */
enum class Medal(val label: String) { Bronze("bronce"), Silver("plata"), Gold("oro") }

/** Un lote de premios que se celebra junto. [title] cambia el encabezado (por ejemplo, el regalo de bienvenida). */
data class RewardBatch(val rewards: List<Reward>, val title: String? = null)
