package com.repisa.app.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.repisa.app.MainActivity
import com.repisa.app.RepisaApp
import com.repisa.app.data.Http
import com.repisa.app.model.Game
import com.repisa.app.model.Source
import kotlinx.serialization.builtins.ListSerializer
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * La app de verdad, con una biblioteca de ejemplo, para ver cada paso del tutorial y de la guía de inicio
 * (solo con -Drender.out=carpeta -Drender.tour=1).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "es-w1280dp-h720dp-mdpi")
class TourRender {
    @get:Rule val compose = createEmptyComposeRule()

    private val sample = listOf(
        367520 to "Hollow Knight", 2161700 to "Persona 3 Reload", 1245620 to "ELDEN RING", 2379780 to "Balatro",
        1145360 to "Hades", 413150 to "Stardew Valley", 504230 to "Celeste", 379720 to "DOOM", 1113000 to "Persona 4 Golden",
        638970 to "Yakuza 0", 632470 to "Disco Elysium", 588650 to "Dead Cells", 620 to "Portal 2", 205100 to "Dishonored",
        8870 to "BioShock Infinite", 200260 to "Batman: Arkham City GOTY", 1057090 to "Ori and the Will of the Wisps", 268910 to "Cuphead",
    )

    private fun settle(ms: Long = 400) {
        repeat((ms / 50).toInt()) { shadowOf(Looper.getMainLooper()).idle(); Thread.sleep(50) }
        compose.waitForIdle()
    }

    @Test fun tour() {
        val out = System.getProperty("render.out") ?: return
        if (System.getProperty("render.tour") == null) return
        val app = ApplicationProvider.getApplicationContext<RepisaApp>()
        app.getSharedPreferences("repisa", 0).edit().putString("language", "es-ES").putBoolean("onboarding_done", true)
            .putLong("first_use", System.currentTimeMillis() - 30L * 86400000).commit()
        val now = System.currentTimeMillis()
        val games = sample.mapIndexed { i, (id, name) ->
            Game("steam:$id", Source.STEAM, name, "pc", playtimeMinutes = (40 - i) * 97, metaFetched = true, factsFetched = true,
                coverUrl = "https://cdn.cloudflare.steamstatic.com/steam/apps/$id/library_600x900.jpg",
                lastPlayed = if (i < 5) now - i * 86400000L else now - (60L + i) * 86400000L, status = if (i < 4) "PLAYING" else null)
        }
        File(app.filesDir, "library.json").writeText(Http.json.encodeToString(ListSerializer(Game.serializer()), games))
        com.repisa.app.i18n.L10n.init(app)

        val scenario = ActivityScenario.launch(MainActivity::class.java)
        settle(6000) // carátulas
        val vm = app.library()
        File(out).mkdirs()
        fun shot(name: String) {
            scenario.onActivity { a ->
                val v = a.window.decorView
                val bmp = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
                v.draw(Canvas(bmp))
                File(out, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
        }
        // Guía de inicio: sus 7 pantallas.
        for (s in 0..6) {
            scenario.onActivity { vm.onboarding.value = false }
            settle(100)
            scenario.onActivity { vm.settings.onboardingStep = s; vm.onboarding.value = true }
            settle(500)
            shot("guia_%02d".format(s + 1))
        }
        scenario.onActivity { vm.onboarding.value = false }
        // Tutorial: cada paso.
        for (i in com.repisa.app.ui.Tour.steps.indices) {
            scenario.onActivity { vm.tour.value = i }
            settle(1200)
            shot("tutorial_%02d".format(i + 1))
        }
        scenario.onActivity { vm.endTour() }
        // Estadísticas: el botón de «Tu año en Repisa» (aún no disponible).
        settle(300)
    }

    /** Tema personalizado con foto de fondo, la página de temas y la lista de idiomas (-Drender.photo=imagen.jpg). */
    @Test fun photoTheme() {
        val out = System.getProperty("render.out") ?: return
        val photo = System.getProperty("render.photo") ?: return
        val app = ApplicationProvider.getApplicationContext<RepisaApp>()
        File(photo).copyTo(File(app.filesDir, "theme_background.jpg"), overwrite = true)
        app.getSharedPreferences("repisa", 0).edit().putString("language", "es-419").putBoolean("onboarding_done", true)
            .putString("shelf_theme", "CUSTOM").putString("custom_theme", "PLANKS|${0xFF2E3A2A}|${0xFF8C5A30}|${0xFFFF66E0}|true|30|55").commit()
        val now = System.currentTimeMillis()
        val games = sample.mapIndexed { i, (id, name) ->
            Game("steam:$id", Source.STEAM, name, "pc", playtimeMinutes = (40 - i) * 97, metaFetched = true, factsFetched = true,
                coverUrl = "https://cdn.cloudflare.steamstatic.com/steam/apps/$id/library_600x900.jpg",
                lastPlayed = if (i < 5) now - i * 86400000L else now - (60L + i) * 86400000L)
        }
        File(app.filesDir, "library.json").writeText(Http.json.encodeToString(ListSerializer(Game.serializer()), games))
        com.repisa.app.i18n.L10n.init(app)
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        settle(6000)
        val vm = app.library()
        File(out).mkdirs()
        fun shot(name: String) = scenario.onActivity { a ->
            val v = a.window.decorView
            val bmp = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
            v.draw(Canvas(bmp))
            File(out, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        shot("foto_repisa")
        scenario.onActivity { vm.debugPage = "THEMES"; vm.inSettings.value = true }
        settle(1500)
        shot("foto_temas")
        compose.onNode(androidx.compose.ui.test.hasText("Filtro del color", substring = true)).performScrollTo()
        settle(600)
        shot("foto_controles")
        compose.onAllNodes(androidx.compose.ui.test.hasText("🎨"))[1].performScrollTo().performClick()
        settle(800)
        shot("rueda")
        scenario.onActivity { vm.inSettings.value = false }
        settle(300)
        scenario.onActivity { vm.debugPage = "LANGUAGE"; vm.inSettings.value = true }
        settle(1000)
        shot("idiomas")
    }
}
