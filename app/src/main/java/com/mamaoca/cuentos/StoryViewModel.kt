package com.mamaoca.cuentos

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime

class StoryViewModel(application: Application, private val saved: SavedStateHandle) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("mamaoca", 0)
    var stories by mutableStateOf<List<Story>>(emptyList()); private set
    var error by mutableStateOf<String?>(null); private set
    var screen by mutableStateOf(saved.get<String>("screen") ?: "home"); private set
    var storyId by mutableStateOf(saved.get<String>("storyId")); private set
    var page by mutableStateOf(saved.get<Int>("page") ?: 0); private set
    var query by mutableStateOf(saved.get<String>("query") ?: "")
    var favoritesOnly by mutableStateOf(false)
    var favorites by mutableStateOf(prefs.getStringSet("favorites", emptySet())!!.toSet()); private set
    var read by mutableStateOf(prefs.getStringSet("read", emptySet())!!.toSet()); private set
    var sound by mutableStateOf(prefs.getBoolean("sound", true)); private set
    var listening by mutableStateOf(false); private set
    /** El cuento empezado y no terminado (solo uno a la vez) y la página donde se quedó. */
    var inProgress by mutableStateOf(prefs.getString("progressId", null)); private set
    var progressPage by mutableStateOf(prefs.getInt("progressPage", 0)); private set
    /** Cuento recomendado hoy: se elige una vez al día. */
    var dailyId by mutableStateOf<String?>(null); private set
    val daily: Story? get() = stories.find { it.id == dailyId }
    val story: Story? get() = stories.find { it.id == storyId }
    /** Temas delicados que papá o mamá ocultaron: sus cuentos no aparecen en la biblioteca. */
    var topics by mutableStateOf<List<Topic>>(emptyList()); private set
    var hiddenTopics by mutableStateOf(prefs.getStringSet("hiddenTopics", emptySet())!!.toSet()); private set
    fun isHidden(story: Story) = story.sensitive.any { it in hiddenTopics }
    val visibleStories: List<Story> get() = stories.filter {
        !isHidden(it) && (!favoritesOnly || it.id in favorites) &&
            (it.title + " " + it.description).searchKey().contains(query.trim().searchKey())
    }.let { list -> shuffleOrder?.let { order -> list.sortedBy { order[it.id] } } ?: list }
        .sortedByDescending { it.id == inProgress } // el cuento por continuar va primero
    /** Orden aleatorio que pide el dado; no se guarda, así que al volver a abrir la app vuelve el orden normal. */
    var shuffleOrder by mutableStateOf<Map<String, Int>?>(null); private set
    fun shuffle() { shuffleOrder = stories.shuffled().withIndex().associate { (i, s) -> s.id to i } }
    /** Los leídos se guardan aparte, en un estante plegable al final de la biblioteca. */
    var showRead by mutableStateOf(false)

    // ---- Registro de lectura para los logros (no se borra al releer un cuento) ----
    internal var now: () -> LocalDateTime = { LocalDateTime.now() }
    internal var random: kotlin.random.Random = kotlin.random.Random.Default
    private val today get() = now().toLocalDate().toEpochDay()
    private var finished = prefs.getStringSet("logFinished", null)?.toSet()
    private var finishCounts = prefs.getStringSet("logCounts", emptySet())!!.associate { it.substringBefore('=') to it.substringAfter('=').toInt() }
    private var readingDays = prefs.getStringSet("logDays", emptySet())!!.map { it.toLong() }.toSet()
    private var dailyDays = prefs.getStringSet("logDaily", emptySet())!!.map { it.toLong() }.toSet()
    /** Logros conseguidos y el día en que se consiguieron. */
    var unlocked by mutableStateOf(prefs.getStringSet("achievements", emptySet())!!
        .associate { it.substringBefore('=') to it.substringAfter('=').toLong() }); private set
    /** Logros recién conseguidos que falta celebrar, en orden. */
    var celebrations by mutableStateOf<List<Achievement>>(emptyList()); private set
    var stats by mutableStateOf(ReadingStats()); private set
    /** Pantalla a la que vuelve «Mis logros». */
    private var returnTo = "home"

    // ---- Preguntas, estrellas y premios ----
    /** Mejor resultado (0 a 3 estrellas) en las preguntas de cada cuento. */
    var quizStars by mutableStateOf(prefs.getStringSet("quizStars", emptySet())!!
        .associate { it.substringBefore('=') to it.substringAfter('=').toInt() }); private set
    /** Estrellas del cuento recién terminado; null si se saltaron las preguntas o el cuento no tiene. */
    var lastStars by mutableStateOf<Int?>(null); private set
    private var firstFinish = false
    var rewards by mutableStateOf(RewardCatalog(emptyList())); private set
    var ownedRewards by mutableStateOf(prefs.getStringSet("rewards", emptySet())!!.toSet()); private set
    /** Premios por celebrar, después de los logros. */
    var rewardCelebrations by mutableStateOf<List<RewardBatch>>(emptyList()); private set
    /** Historial de lecturas terminadas, de la más reciente a la más antigua: día y cuento. */
    var history by mutableStateOf(prefs.getString("logHistory", "")!!.split(',').filter { ':' in it }
        .map { it.substringBefore(':').toLong() to it.substringAfter(':') }); private set
    /** Mejor tiempo (en milisegundos) de cada rompecabezas en cada nivel: «id|nivel» → tiempo. */
    var puzzleRecords by mutableStateOf(prefs.getStringSet("puzzleRecords", emptySet())!!
        .associate { it.substringBefore('=') to it.substringAfter('=').toLong() }); private set
    fun puzzleRecord(id: String, level: Int): Long? = puzzleRecords["$id|$level"]
    /** La medalla más alta ganada con un rompecabezas: la del nivel más difícil que se terminó. */
    fun puzzleMedal(id: String): Medal? =
        Medal.entries.indices.lastOrNull { puzzleRecord(id, it) != null }?.let { Medal.entries[it] }
    /** Guarda un rompecabezas terminado; devuelve el récord que había antes (null si era la primera vez). */
    fun finishPuzzle(id: String, level: Int, millis: Long): Long? {
        val before = puzzleRecord(id, level)
        if (before == null || millis < before) {
            puzzleRecords = puzzleRecords + ("$id|$level" to millis)
            prefs.edit().putStringSet("puzzleRecords", puzzleRecords.map { (k, v) -> "$k=$v" }.toSet()).apply()
        }
        return before
    }
    // ---- Mi personaje: se crea en el perfil y es la primera calcomanía ----
    var avatarSpec by mutableStateOf(AvatarSpec.decode(prefs.getString("avatar", null))); private set
    private var avatarVersion by androidx.compose.runtime.mutableIntStateOf(0)
    /** El personaje como calcomanía, si ya se creó. */
    val avatarReward: Reward? get() {
        avatarVersion // para que la vista se actualice al cambiarlo
        val file = AvatarRenderer.file(getApplication())
        return if (avatarSpec != null && file.exists()) Reward(AVATAR_ID, RewardKind.Sticker, "Mi personaje", "file:" + file.path, null, false) else null
    }
    /** Un premio por su id, incluido el personaje. */
    fun reward(id: String): Reward? = if (id == AVATAR_ID) avatarReward else rewards.byId[id]
    fun saveAvatar(spec: AvatarSpec) {
        val app = getApplication<Application>()
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                val bmp = AvatarRenderer.render(app, AvatarCatalog.load(app), spec) ?: return@withContext
                AvatarRenderer.file(app).outputStream().use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
            }
            StickerArt.forget(AVATAR_ID)
            avatarSpec = spec
            prefs.edit().putString("avatar", spec.encode()).apply()
            avatarVersion++
        }
    }
    /** Premio abierto en un juego (dibujo o rompecabezas). */
    var gameItem by mutableStateOf(saved.get<String>("gameItem")); private set
    val totalFinishes get() = finishCounts.values.sum()
    val readingDayCount get() = readingDays.size
    /** Racha que sigue viva: termina hoy o ayer. */
    val currentStreak: Int get() {
        var day = if (today in readingDays) today else today - 1
        var n = 0
        while (day in readingDays) { n++; day-- }
        return n
    }

    init {
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { StoryRepository(application).load() } }
                .onSuccess {
                    stories = it
                    topics = runCatching { StoryRepository(application).topics() }.getOrDefault(emptyList())
                    rewards = runCatching { withContext(Dispatchers.IO) { RewardCatalog.load(application) } }.getOrDefault(RewardCatalog(emptyList()))
                    pickDaily()
                    if (finished == null) { // primera vez: los cuentos ya leídos cuentan para los logros
                        finished = read
                        finishCounts = read.associateWith { 1 }
                        saveLog()
                    }
                    refreshAchievements()
                    welcomeRewards()
                    if (screen == "reader" || screen == "finish" || screen == "quiz") {
                        val selected = story
                        if (selected == null) navigate("library")
                        else { page = page.coerceIn(0, selected.pages.size); persistNavigation() }
                    }
                }
                .onFailure { error = "No se pudieron abrir los cuentos. Reinicia la aplicación." }
        }
    }

    /** Al empezar el día elige al azar un cuento no leído; si ya los leyó todos, uno cualquiera. */
    fun pickDaily(today: Long = LocalDate.now().toEpochDay()) {
        val keep = prefs.getString("dailyId", null)
        if (prefs.getLong("dailyDay", -1) == today && stories.any { it.id == keep && !isHidden(it) }) { dailyId = keep; return }
        val allowed = stories.filter { !isHidden(it) }
        val pool = allowed.filter { it.id !in read }.ifEmpty { allowed }
        dailyId = pool.randomOrNull()?.id
        prefs.edit().putLong("dailyDay", today).putString("dailyId", dailyId).apply()
    }

    private fun persistNavigation() {
        saved["screen"] = screen; saved["storyId"] = storyId; saved["page"] = page; saved["query"] = query; saved["gameItem"] = gameItem
    }
    fun navigate(destination: String) { listening = false; screen = destination; persistNavigation() }
    /** Abre un cuento; si es el que estaba a medias, continúa en la página donde se quedó. */
    fun open(id: String) {
        storyId = id
        page = if (id == inProgress) progressPage.coerceIn(0, stories.find { it.id == id }?.pages?.size ?: 0) else 0
        navigate("reader")
    }
    /** Empieza desde la portada aunque estuviera a medias. */
    fun restart(id: String) { storyId = id; page = 0; navigate("reader") }

    /** Guarda la página actual. Al empezar otro cuento, el que estaba a medias pierde el progreso y queda como no leído. */
    private fun trackProgress() {
        val id = storyId ?: return
        if (page == 0 && id != inProgress) return // solo mirar la portada no cuenta
        if (inProgress != null && inProgress != id) read = read - inProgress!!
        if (id != inProgress) read = read - id // al releer un cuento, vuelve a estar en curso
        inProgress = id; progressPage = page
        prefs.edit().putString("progressId", id).putInt("progressPage", page).putStringSet("read", read).apply()
    }
    fun toggleFavorite(id: String) {
        favorites = if (id in favorites) favorites - id else favorites + id
        prefs.edit().putStringSet("favorites", favorites).apply()
    }
    fun toggleSound() { sound = !sound; prefs.edit().putBoolean("sound", sound).apply() }
    fun toggleListening() { listening = !listening }
    fun stopListening() { listening = false }
    fun next() {
        val current = story ?: return
        if (page < current.pages.size) { page++; persistNavigation(); trackProgress(); recordActivity() }
        else {
            recordFinish(current, listening)
            read = read + current.id
            if (inProgress == current.id) inProgress = null
            prefs.edit().putStringSet("read", read).remove("progressId").remove("progressPage").apply()
            lastStars = null
            navigate(if (current.quiz.isNotEmpty()) "quiz" else "finish")
            if (current.quiz.isEmpty()) surprise(2)
        }
    }
    fun previous() { if (page > 0) { page--; persistNavigation(); trackProgress() } }
    fun back() {
        if (screen == "quiz") { skipQuiz(); return }
        navigate(when (screen) {
            "library", "games" -> "home"
            "achievements" -> returnTo
            "stickers", "coloringList", "puzzleList" -> "games"
            "coloring" -> "coloringList"
            "puzzle" -> "puzzleList"
            else -> "library"
        })
    }
    fun openGame(screen: String, item: String? = null) { gameItem = item; navigate(screen) }

    /** Termina las preguntas: guarda el mejor resultado y tira la sorpresa con esas estrellas. */
    fun finishQuiz(correct: Int) {
        val id = storyId ?: return
        lastStars = correct
        quizStars = quizStars + (id to maxOf(correct, quizStars[id] ?: 0))
        prefs.edit().putStringSet("quizStars", quizStars.map { (k, v) -> "$k=$v" }.toSet()).apply()
        surprise(correct)
        navigate("finish")
    }
    fun skipQuiz() { lastStars = null; surprise(0); navigate("finish") }

    /**
     * Al terminar un cuento (después de las preguntas): su calcomanía, si tiene y aún no se ganó, y a veces una sorpresa,
     * que primero intenta ser el dibujo para colorear, el rompecabezas o el fondo de ese mismo cuento.
     */
    private fun surprise(stars: Int) {
        val first = firstFinish; firstFinish = false
        val id = storyId ?: return
        grant(RewardRules.storyStickers(rewards, ownedRewards, id))
        RewardRules.storyPuzzle(rewards, ownedRewards, id)?.let { grant(listOf(it)) }
        if (random.nextFloat() < RewardRules.surpriseChance(stars, first)) grant(RewardRules.drawFor(rewards, ownedRewards, random, id))
    }

    /** Da los premios. Cada uno se celebra por separado, uno detrás de otro; [together] los junta en un solo aviso. */
    private fun grant(list: List<Reward>, title: String? = null, together: Boolean = false) {
        if (list.isEmpty()) return
        ownedRewards = ownedRewards + list.map { it.id }
        prefs.edit().putStringSet("rewards", ownedRewards).apply()
        rewardCelebrations = rewardCelebrations + if (together) listOf(RewardBatch(list, title)) else list.map { RewardBatch(listOf(it), title) }
    }
    fun dismissReward() { rewardCelebrations = rewardCelebrations.drop(1) }

    /**
     * La primera vez que hay juegos: un regalo inicial, más premios por los cuentos que ya se habían leído
     * y por los logros que ya se tenían, para que nadie empiece de cero.
     */
    private fun welcomeRewards() {
        // los fondos y dibujos para colorear se cambiaron por otros nuevos: quien tenía premios de antes recibe la misma cantidad de los nuevos
        if (prefs.getBoolean("rewardsInit", false) && !prefs.getBoolean("rewardsV2", false) && rewards.all.isNotEmpty()) {
            prefs.edit().putBoolean("rewardsV2", true).apply()
            val gifts = mutableListOf<Reward>()
            listOf(RewardKind.Background to "b-", RewardKind.Coloring to "c-").forEach { (kind, prefix) ->
                val lost = ownedRewards.count { it.startsWith(prefix) && it !in rewards.byId }
                val owned = { ownedRewards + gifts.map { it.id } }
                gifts += rewards.of(kind).filter { it.starter && it.id !in owned() }
                repeat(lost) { gifts += RewardRules.draw(rewards, owned(), random, kind).filter { it.kind == kind } }
            }
            ownedRewards = ownedRewards.filter { it in rewards.byId }.toSet() // los premios que ya no existen se olvidan
            prefs.edit().putStringSet("rewards", ownedRewards).apply()
            grant(gifts.distinctBy { it.id }, "¡Fondos y dibujos nuevos para tus juegos!", together = true)
        }
        // las calcomanías pintadas llegaron después: los cuentos ya leídos dan la suya, todas juntas en un aviso
        if (prefs.getBoolean("rewardsInit", false) && !prefs.getBoolean("storyStickersInit", false) && rewards.all.isNotEmpty()) {
            prefs.edit().putBoolean("storyStickersInit", true).apply()
            grant(finished.orEmpty().flatMap { RewardRules.storyStickers(rewards, ownedRewards, it) },
                "¡Calcomanías de los cuentos que ya leíste!", together = true)
        }
        // llegaron calcomanías de objetos, animales y personas: los cuentos ya leídos dan las suyas, todas juntas en un aviso
        if (prefs.getBoolean("rewardsInit", false) && !prefs.getBoolean("objectStickersInit", false) && rewards.all.isNotEmpty()) {
            prefs.edit().putBoolean("objectStickersInit", true).apply()
            grant(finished.orEmpty().flatMap { RewardRules.storyStickers(rewards, ownedRewards, it) }.distinctBy { it.id },
                "¡Calcomanías nuevas de los cuentos que ya leíste!", together = true)
        }
        // ahora cada cuento da su rompecabezas: los cuentos ya leídos dan el suyo, todos juntos en un aviso
        if (prefs.getBoolean("rewardsInit", false) && !prefs.getBoolean("storyPuzzlesInit", false) && rewards.all.isNotEmpty()) {
            prefs.edit().putBoolean("storyPuzzlesInit", true)
            .putBoolean("objectStickersInit", true).apply()
            grant(finished.orEmpty().mapNotNull { RewardRules.storyPuzzle(rewards, ownedRewards, it) },
                "¡Rompecabezas de los cuentos que ya leíste!", together = true)
        }
        if (prefs.getBoolean("rewardsInit", false) || rewards.all.isEmpty()) return
        prefs.edit().putBoolean("storyStickersInit", true).putBoolean("rewardsV2", true).putBoolean("storyPuzzlesInit", true)
            .putBoolean("objectStickersInit", true).apply()
        prefs.edit().putBoolean("rewardsInit", true).apply()
        val gifts = rewards.all.filter { it.starter }.toMutableList()
        val owned = { ownedRewards + gifts.map { it.id } }
        repeat(finished.orEmpty().size) { if (random.nextFloat() < 0.5f) gifts += RewardRules.draw(rewards, owned(), random) }
        for (id in unlocked.keys) ACHIEVEMENT_PRIZES[id]?.let { gifts += RewardRules.draw(rewards, owned(), random, it) }
        for (id in finished.orEmpty()) gifts += RewardRules.storyStickers(rewards, owned(), id)
        for (id in finished.orEmpty()) RewardRules.storyPuzzle(rewards, owned(), id)?.let { gifts += it }
        grant(gifts, if (finished.orEmpty().isEmpty()) "¡Bienvenido a los juegos!" else "¡Sorpresas por todo lo que ya leíste!", together = true)
    }

    fun setTopicHidden(topic: String, hidden: Boolean) {
        hiddenTopics = if (hidden) hiddenTopics + topic else hiddenTopics - topic
        prefs.edit().putStringSet("hiddenTopics", hiddenTopics).apply()
        if (daily?.let(::isHidden) == true) { prefs.edit().remove("dailyDay").apply(); pickDaily() }
    }
    fun openAchievements() { returnTo = screen; navigate("achievements") }
    fun dismissCelebration() { celebrations = celebrations.drop(1) }

    /** Pasar una página cuenta como día de lectura. */
    private fun recordActivity() {
        readingDays = readingDays + today
        if (now().hour < 8) prefs.edit().putBoolean("logEarly", true).apply()
        saveLog(); refreshAchievements()
    }

    private fun recordFinish(story: Story, listened: Boolean) {
        firstFinish = story.id !in finished.orEmpty()
        history = (listOf(today to story.id) + history).take(300)
        prefs.edit().putString("logHistory", history.joinToString(",") { (d, id) -> "$d:$id" }).apply()
        finished = finished.orEmpty() + story.id
        finishCounts = finishCounts + (story.id to (finishCounts[story.id] ?: 0) + 1)
        readingDays = readingDays + today
        if (story.id == dailyId) dailyDays = dailyDays + today
        val dayCount = if (prefs.getLong("logDay", -1) == today) prefs.getInt("logDayCount", 0) + 1 else 1
        val edit = prefs.edit().putLong("logDay", today).putInt("logDayCount", dayCount)
            .putInt("logMaxDay", maxOf(dayCount, prefs.getInt("logMaxDay", 0)))
        if (now().hour >= 20) edit.putBoolean("logNight", true)
        if (listened) edit.putBoolean("logListened", true)
        edit.apply()
        saveLog(); refreshAchievements()
    }

    private fun saveLog() {
        prefs.edit().putStringSet("logFinished", finished.orEmpty())
            .putStringSet("logCounts", finishCounts.map { (id, n) -> "$id=$n" }.toSet())
            .putStringSet("logDays", readingDays.map { it.toString() }.toSet())
            .putStringSet("logDaily", dailyDays.map { it.toString() }.toSet()).apply()
    }

    /** Recalcula el progreso y celebra los logros que se acaban de conseguir. */
    private fun refreshAchievements() {
        val done = finished.orEmpty()
        val byId = stories.associateBy { it.id }
        val finishedStories = done.mapNotNull { byId[it] }
        stats = ReadingStats(
            finished = done,
            tagCounts = finishedStories.flatMap { it.tags }.groupingBy { it }.eachCount(),
            finishedPoem = finishedStories.any { it.poem },
            longestStreak = longestStreak(readingDays),
            maxFinishesInDay = prefs.getInt("logMaxDay", 0),
            maxRereads = finishCounts.values.maxOrNull() ?: 0,
            dailyPicksRead = dailyDays.size,
            nightOwl = prefs.getBoolean("logNight", false),
            earlyBird = prefs.getBoolean("logEarly", false),
            listened = prefs.getBoolean("logListened", false),
        )
        val fresh = ACHIEVEMENTS.filter { it.id !in unlocked && it.reached(stats) }
        if (fresh.isEmpty()) return
        unlocked = unlocked + fresh.associate { it.id to today }
        prefs.edit().putStringSet("achievements", unlocked.map { (id, day) -> "$id=$day" }.toSet()).apply()
        celebrations = celebrations + fresh
        // los logros con premio lo entregan enseguida (se celebra después del logro)
        if (prefs.getBoolean("rewardsInit", false)) for (a in fresh) ACHIEVEMENT_PRIZES[a.id]?.let {
            grant(RewardRules.draw(rewards, ownedRewards, random, it), "¡Premio por el logro «${a.title}»!")
        }
    }
}
