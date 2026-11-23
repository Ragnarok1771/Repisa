package com.repisa.app.i18n

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import com.repisa.app.R
import java.util.Locale

/** Idiomas disponibles. [tag] es la etiqueta BCP-47 con la que Android elige los textos. */
enum class AppLanguage(val tag: String, val flag: String, val nativeName: String) {
    ES_ES("es-ES", "🇪🇸", "Español (España)"),
    ES_419("es-419", "🇲🇽", "Español (Latinoamérica)"),
    EN_US("en-US", "🇺🇸", "English (US)"),
    EN_GB("en-GB", "🇬🇧", "English (UK)"),
    FR("fr-FR", "🇫🇷", "Français"),
    DE("de-DE", "🇩🇪", "Deutsch"),
    IT("it-IT", "🇮🇹", "Italiano"),
    PT_BR("pt-BR", "🇧🇷", "Português (Brasil)"),
    PT_PT("pt-PT", "🇵🇹", "Português (Portugal)");

    val locale: Locale get() = Locale.forLanguageTag(tag)

    companion object {
        fun fromTag(tag: String?) = entries.firstOrNull { it.tag == tag }

        /** Sugerencia para la pantalla de bienvenida según el idioma del sistema. */
        fun guess(system: Locale = Locale.getDefault()): AppLanguage = when (system.language) {
            "es" -> if (system.country in setOf("ES", "")) ES_ES else ES_419
            "en" -> if (system.country == "GB") EN_GB else EN_US
            "fr" -> FR
            "de" -> DE
            "it" -> IT
            "pt" -> if (system.country == "PT") PT_PT else PT_BR
            else -> EN_US
        }
    }
}

/**
 * Textos traducidos fuera de Compose (mensajes del ViewModel, errores...).
 * El idioma se guarda en preferencias y se aplica envolviendo el contexto de la app y de la actividad.
 */
object L10n {
    private lateinit var ctx: Context
    var current: AppLanguage? = null
        private set

    /** Cambia cada vez que se elige idioma: la interfaz se recompone con él sin reiniciar la actividad. */
    val version = kotlinx.coroutines.flow.MutableStateFlow(0)

    /** [base] (normalmente la actividad) con los textos del idioma elegido, sin perder lo demás. */
    fun localize(base: Context): Context {
        val lang = current ?: return base
        val config = Configuration(base.resources.configuration).apply { setLocale(lang.locale) }
        val res = base.createConfigurationContext(config).resources
        return object : android.content.ContextWrapper(base) {
            override fun getResources(): android.content.res.Resources = res
        }
    }

    private fun saved(context: Context): AppLanguage? = AppLanguage.fromTag(
        context.getSharedPreferences("repisa", Context.MODE_PRIVATE).getString("language", null)
    )

    /** Devuelve [base] con el idioma elegido (o el del sistema si aún no se eligió). */
    fun wrap(base: Context): Context {
        val lang = saved(base) ?: return base
        Locale.setDefault(lang.locale)
        val config = Configuration(base.resources.configuration).apply { setLocale(lang.locale) }
        return base.createConfigurationContext(config)
    }

    fun init(app: Context) {
        current = saved(app)
        ctx = wrap(app)
    }

    fun setLanguage(app: Context, lang: AppLanguage) {
        app.getSharedPreferences("repisa", Context.MODE_PRIVATE).edit().putString("language", lang.tag).apply()
        init(app)
        version.value++
    }

    fun s(@StringRes id: Int, vararg args: Any): String = ctx.getString(id, *args)
    fun plural(@PluralsRes id: Int, n: Int, vararg args: Any): String = ctx.resources.getQuantityString(id, n, *args)

    fun games(n: Int) = plural(R.plurals.games_count, n, n)

    /** Número con decimales según el idioma (coma en español, punto en inglés...). */
    fun decimal(v: Number, digits: Int = 1): String = String.format(Locale.getDefault(), "%.${digits}f", v.toDouble())

    /** Nombre visible de un género (internamente se guardan con su nombre en español). */
    fun genre(key: String): String {
        val keys = ctx.resources.getStringArray(R.array.genre_keys)
        val i = keys.indexOf(key)
        if (i < 0) return key
        val id = ctx.resources.getIdentifier("genre_$i", "string", ctx.packageName)
        return if (id != 0) ctx.getString(id) else key
    }
}
