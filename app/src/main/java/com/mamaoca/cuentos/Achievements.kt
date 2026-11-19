package com.mamaoca.cuentos

/**
 * Lo que se sabe del historial de lectura, para calcular los logros.
 * [finished] son los cuentos terminados alguna vez (aunque luego se releyeran sin acabar).
 */
data class ReadingStats(
    val finished: Set<String> = emptySet(),
    val tagCounts: Map<String, Int> = emptyMap(),
    val finishedPoem: Boolean = false,
    val longestStreak: Int = 0,
    val maxFinishesInDay: Int = 0,
    val maxRereads: Int = 0,
    val dailyPicksRead: Int = 0,
    val nightOwl: Boolean = false,
    val earlyBird: Boolean = false,
    val listened: Boolean = false,
)

/** Un logro: se consigue cuando [progress] llega a [goal]. Su insignia va en assets/badges/<id>.webp (o .png). */
class Achievement(val id: String, val title: String, val description: String, val goal: Int,
                  val progress: (ReadingStats) -> Int) {
    fun reached(stats: ReadingStats) = progress(stats) >= goal
}

private fun count(n: Int, id: String, title: String, description: String) =
    Achievement(id, title, description, n) { it.finished.size }
private fun tag(n: Int, tag: String, id: String, title: String, description: String) =
    Achievement(id, title, description, n) { it.tagCounts[tag] ?: 0 }
private fun streak(n: Int, id: String, title: String, description: String) =
    Achievement(id, title, description, n) { it.longestStreak }
private fun flag(id: String, title: String, description: String, get: (ReadingStats) -> Boolean) =
    Achievement(id, title, description, 1) { if (get(it)) 1 else 0 }

val ACHIEVEMENTS = listOf(
    count(1, "primer-cuento", "¡Mi primer cuento!", "Termina tu primer cuento."),
    count(5, "lector-curioso", "Lector curioso", "Lee 5 cuentos distintos."),
    count(10, "raton-biblioteca", "Ratón de biblioteca", "Lee 10 cuentos distintos."),
    count(25, "devoralibros", "Devoralibros", "Lee 25 cuentos distintos."),
    count(50, "gran-lector", "Gran lector", "Lee 50 cuentos distintos."),
    count(100, "maestro-mama-oca", "Maestro de Mamá Oca", "Lee 100 cuentos distintos."),
    tag(5, "animales", "amigo-animales", "Amigo de los animales", "Lee 5 cuentos de animales."),
    tag(5, "realeza", "invitado-corte", "Invitado de la corte", "Lee 5 cuentos de reyes, princesas y príncipes."),
    tag(5, "magia", "aprendiz-mago", "Aprendiz de mago", "Lee 5 cuentos de magia y hadas."),
    tag(3, "dragones", "domador-dragones", "Domador de dragones", "Lee 3 cuentos con dragones."),
    tag(4, "mar", "lobo-de-mar", "Lobo de mar", "Lee 4 cuentos del mar."),
    tag(3, "espacio", "astronauta", "Astronauta", "Lee 3 cuentos de estrellas y otros mundos."),
    tag(5, "historia", "viajero-tiempo", "Viajero del tiempo", "Lee 5 cuentos de otras épocas."),
    tag(10, "valores", "corazon-valiente", "Corazón valiente", "Lee 10 cuentos sobre emociones y sentimientos."),
    tag(5, "marti", "amigo-marti", "Amigo de Martí", "Lee los 5 cuentos de José Martí."),
    flag("pequeno-poeta", "Pequeño poeta", "Lee un cuento en verso.") { it.finishedPoem },
    streak(3, "racha-3", "Tres días de cuento", "Lee 3 días seguidos."),
    streak(5, "racha-5", "Cinco días seguidos", "Lee 5 días seguidos."),
    streak(10, "racha-10", "Diez días mágicos", "Lee 10 días seguidos."),
    flag("buho-nocturno", "Búho nocturno", "Termina un cuento después de las 8 de la noche.") { it.nightOwl },
    flag("gallo-madrugador", "Gallo madrugador", "Lee un cuento antes de las 8 de la mañana.") { it.earlyBird },
    flag("oidos-atentos", "Oídos atentos", "Escucha un cuento entero con la narración.") { it.listened },
    Achievement("maraton", "Maratón de cuentos", "Termina 3 cuentos en un mismo día.", 3) { it.maxFinishesInDay },
    Achievement("cuento-del-dia", "Fiel al cuento del día", "Lee el cuento del día en 3 días distintos.", 3) { it.dailyPicksRead },
    Achievement("otra-vez", "¡Otra vez, otra vez!", "Termina el mismo cuento 3 veces.", 3) { it.maxRereads },
)

/** La racha más larga de días consecutivos dentro de [days] (días contados desde 1970). */
fun longestStreak(days: Set<Long>): Int {
    var best = 0
    for (d in days) if (d - 1 !in days) {
        var len = 1
        while (d + len in days) len++
        best = maxOf(best, len)
    }
    return best
}
