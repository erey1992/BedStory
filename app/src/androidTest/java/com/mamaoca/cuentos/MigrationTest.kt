package com.mamaoca.cuentos

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.lifecycle.ViewModelProvider
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ContentMigrationTest {
    @Test fun allStoriesAndVectorLayersRenderOffline() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val stories = StoryRepository(context).load()
        assertTrue(stories.size >= 20)
        assertTrue(stories.all { it.pages.isNotEmpty() })
        val bitmap = Bitmap.createBitmap(600, 500, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        stories.forEach { story ->
            (listOf(story.cover) + story.pages.map { it.scene }).filterNotNull().forEach { asset ->
                val scene = SceneLoader.load(context, asset)
                assertTrue("Empty scene: $asset", scene.layers.isNotEmpty())
                canvas.drawPicture(scene.background)
                scene.layers.forEach { canvas.drawPicture(it.picture) }
                scene.tint?.let { canvas.drawPicture(it) }
            }
            assertTrue(story.pages.all { page -> page.text.all { it.text.isNotBlank() } })
        }
        bitmap.recycle()
    }
}

class MigrationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun searchFavoritesReadingAndCompletion() {
        compose.waitUntil(30_000) {
            compose.onAllNodesWithText("Empezar a leer").fetchSemanticsNodes().isNotEmpty()
        }
        compose.runOnUiThread {
            val vm = ViewModelProvider(compose.activity)[StoryViewModel::class.java]
            if ("tres-cerditos" in vm.favorites) vm.toggleFavorite("tres-cerditos")
        }
        compose.onNodeWithText("Empezar a leer").performClick()
        compose.mainClock.advanceTimeBy(1200)
        compose.onNodeWithText("Los tres cerditos").performClick()
        compose.mainClock.advanceTimeBy(1200)
        compose.onNodeWithContentDescription("Añadir a favoritos").performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithContentDescription("Quitar de favoritos").assertExists()
        val count = StoryRepository(InstrumentationRegistry.getInstrumentation().targetContext).load().first().pages.size
        repeat(count) {
            compose.onNodeWithContentDescription("Página siguiente").performClick()
            compose.mainClock.advanceTimeBy(1200)
        }
        compose.onNodeWithText("$count / $count").assertExists()
        compose.onNodeWithContentDescription("Terminar cuento").performClick()
        compose.mainClock.advanceTimeBy(1200)
        // tres preguntas: se responden todas bien
        val quiz = StoryRepository(InstrumentationRegistry.getInstrumentation().targetContext).load().first().quiz
        quiz.forEach { q ->
            compose.onNodeWithText(q.question).assertExists()
            compose.onNodeWithText(q.answers[0]).performClick()
            compose.mainClock.advanceTimeBy(1600)
        }
        compose.onNodeWithText("¡Fin!").assertExists()
        compose.onNodeWithText("¡Perfecto! Entendiste todo el cuento").assertExists()
        // terminar un cuento puede dar logros y premios: cada uno se celebra y se cierra con «¡Genial!»
        while (compose.onAllNodesWithText("¡Genial!").fetchSemanticsNodes().isNotEmpty()) {
            compose.onAllNodesWithText("¡Genial!").onFirst().performClick()
            compose.mainClock.advanceTimeBy(600)
        }
        compose.onNodeWithText("Más cuentos").performClick()
        compose.mainClock.advanceTimeBy(1200)
        compose.onNodeWithText("Favoritos").performClick()
        compose.mainClock.advanceTimeByFrame()
        // los leídos quedan en el estante plegado hasta que se despliega
        compose.onNodeWithText("Los tres cerditos").assertDoesNotExist()
        compose.onNodeWithContentDescription("Ver los cuentos ya leídos").performClick()
        compose.mainClock.advanceTimeBy(600)
        compose.onNodeWithText("Los tres cerditos").assertExists()
        compose.onNodeWithText("  Leído  ·  ${count} páginas  ").assertExists()
        compose.onNode(hasSetTextAction()).performTextInput("no existe este cuento")
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithText("No encontramos ese cuento.\nPrueba con otra palabra.").assertExists()
    }
}

class ReadingProgressTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun continueReadAndAbandon() {
        compose.waitUntil(30_000) { compose.onAllNodesWithText("Empezar a leer").fetchSemanticsNodes().isNotEmpty() }
        compose.runOnUiThread {
            val vm = ViewModelProvider(compose.activity)[StoryViewModel::class.java]
            val (a, b) = vm.stories.take(2)
            // empezar A hasta la página 2: queda en curso
            vm.open(a.id); vm.next(); vm.next()
            assertEquals(a.id, vm.inProgress); assertEquals(2, vm.progressPage)
            // al volver a abrirlo, continúa donde se quedó
            vm.navigate("library"); vm.open(a.id); assertEquals(2, vm.page)
            // solo mirar la portada de B no cuenta
            vm.open(b.id); assertEquals(a.id, vm.inProgress)
            // terminar B: leído; A sigue en curso porque B no lo reemplazó hasta pasar de la portada
            repeat(b.pages.size + 1) { vm.next() }
            assertTrue(b.id in vm.read); assertEquals(null, vm.inProgress)
            // releer B y abandonarlo empezando A: B pasa a no leído
            vm.restart(b.id); vm.next(); assertEquals(b.id, vm.inProgress)
            vm.open(a.id); vm.next()
            assertEquals(a.id, vm.inProgress); assertFalse(b.id in vm.read)
        }
    }

    @Test fun achievementsUnlockOnceAndAreCelebrated() {
        compose.waitUntil(30_000) { compose.onAllNodesWithText("Empezar a leer").fetchSemanticsNodes().isNotEmpty() }
        compose.runOnUiThread {
            val vm = ViewModelProvider(compose.activity)[StoryViewModel::class.java]
            vm.now = { java.time.LocalDateTime.of(2030, 1, 1, 21, 0) } // de noche: «Búho nocturno»
            val s = vm.stories.first()
            vm.restart(s.id); repeat(s.pages.size + 1) { vm.next() }
            assertTrue("primer-cuento" in vm.unlocked && "buho-nocturno" in vm.unlocked)
            while (vm.celebrations.isNotEmpty()) vm.dismissCelebration()
            // releerlo no vuelve a celebrar lo ya conseguido
            vm.restart(s.id); repeat(s.pages.size + 1) { vm.next() }
            assertFalse(vm.celebrations.any { it.id == "primer-cuento" || it.id == "buho-nocturno" })
            // tres días seguidos de lectura
            listOf(2, 3).forEach { d ->
                vm.now = { java.time.LocalDateTime.of(2030, 1, d, 10, 0) }
                vm.restart(s.id); vm.next()
            }
            assertTrue("racha-3" in vm.unlocked)
            while (vm.celebrations.isNotEmpty()) vm.dismissCelebration()
        }
    }

    @Test fun quizStarsRewardsAndHiddenTopics() {
        compose.waitUntil(30_000) { compose.onAllNodesWithText("Empezar a leer").fetchSemanticsNodes().isNotEmpty() }
        compose.runOnUiThread {
            val vm = ViewModelProvider(compose.activity)[StoryViewModel::class.java]
            vm.random = object : kotlin.random.Random() { override fun nextBits(bitCount: Int) = 0 } // siempre hay sorpresa
            val s = vm.stories.first { it.quiz.size == 3 }
            vm.restart(s.id); repeat(s.pages.size + 1) { vm.next() }
            assertEquals("quiz", vm.screen)
            val before = vm.ownedRewards.size
            vm.finishQuiz(2)
            assertEquals("finish", vm.screen); assertEquals(2, vm.lastStars); assertEquals(2, vm.quizStars[s.id])
            assertTrue(vm.ownedRewards.size > before); assertTrue(vm.rewardCelebrations.isNotEmpty())
            // un resultado peor no baja el mejor guardado
            vm.restart(s.id); repeat(s.pages.size + 1) { vm.next() }; vm.finishQuiz(1)
            assertEquals(2, vm.quizStars[s.id])
            // ocultar un tema delicado saca sus cuentos de la biblioteca y del cuento del día
            val topic = vm.topics.first()
            vm.query = ""
            vm.setTopicHidden(topic.id, true)
            assertTrue(vm.visibleStories.none { topic.id in it.sensitive })
            assertFalse(vm.daily?.sensitive.orEmpty().contains(topic.id))
            vm.setTopicHidden(topic.id, false)
            assertTrue(vm.visibleStories.any { topic.id in it.sensitive })
        }
    }

    @Test fun dailyPickPrefersUnread() {
        compose.waitUntil(30_000) { compose.onAllNodesWithText("Empezar a leer").fetchSemanticsNodes().isNotEmpty() }
        compose.runOnUiThread {
            val vm = ViewModelProvider(compose.activity)[StoryViewModel::class.java]
            val day = 100_000L
            // el mismo día, la misma recomendación
            vm.pickDaily(day); val first = vm.dailyId
            vm.pickDaily(day); assertEquals(first, vm.dailyId)
            // con todos leídos menos uno, el día siguiente recomienda justo ese
            val last = vm.stories.last()
            vm.stories.dropLast(1).forEach { s -> vm.restart(s.id); repeat(s.pages.size + 1) { vm.next() } }
            vm.pickDaily(day + 1); assertEquals(last.id, vm.dailyId)
            // con todos leídos, recomienda uno cualquiera
            vm.restart(last.id); repeat(last.pages.size + 1) { vm.next() }
            vm.pickDaily(day + 2); assertTrue(vm.stories.any { it.id == vm.dailyId })
        }
    }
}
