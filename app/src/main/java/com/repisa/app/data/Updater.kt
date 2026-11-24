package com.repisa.app.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File

/**
 * Busca versiones nuevas en las releases de GitHub del proyecto y las instala:
 * descarga el APK y abre el instalador de Android («¿Quieres actualizar esta aplicación?").
 * Las versiones nuevas se instalan encima sin perder datos porque van firmadas con la misma clave.
 */
object Updater {
    /** Repositorio con las releases (si se renombra, GitHub redirige el nombre antiguo). */
    const val REPO = "Ragnarok1771/Pruebas"

    data class Release(val version: String, val notes: String, val apkUrl: String, val apkName: String)

    /** Última release publicada, o null si no hay o no se puede consultar (sin red, repositorio privado...). */
    suspend fun latest(): Release? = withContext(Dispatchers.IO) {
        val req = Request.Builder().url("https://api.github.com/repos/$REPO/releases/latest")
            .header("Accept", "application/vnd.github+json").build()
        Http.client.newCall(req).execute().use { r ->
            if (!r.isSuccessful) return@withContext null
            val json = Http.json.parseToJsonElement(r.body?.string().orEmpty())
            val tag = json["tag_name"].str ?: return@withContext null
            val apk = json["assets"].arr.firstOrNull { it["name"].str?.endsWith(".apk") == true } ?: return@withContext null
            Release(tag.removePrefix("v"), json["body"].str.orEmpty(), apk["browser_download_url"].str ?: return@withContext null,
                apk["name"].str ?: "Repisa.apk")
        }
    }

    /** "0.12.0" > "0.11.4" (compara número a número). */
    fun isNewer(remote: String, local: String): Boolean {
        fun parts(v: String) = v.split('.', '-').map { it.toIntOrNull() ?: 0 }
        val a = parts(remote); val b = parts(local)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }; val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    suspend fun download(context: Context, release: Release, onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs(); listFiles()?.forEach { it.delete() } }
        val out = File(dir, release.apkName)
        Http.client.newCall(Request.Builder().url(release.apkUrl).build()).execute().use { r ->
            if (!r.isSuccessful) error("HTTP ${r.code}")
            val body = r.body ?: error("sin datos")
            val total = body.contentLength().takeIf { it > 0 }
            body.byteStream().use { input ->
                out.outputStream().use { output ->
                    val buf = ByteArray(64 * 1024)
                    var done = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        output.write(buf, 0, n)
                        done += n
                        total?.let { onProgress(done.toFloat() / it) }
                    }
                }
            }
        }
        out
    }

    /** ¿Puede Repisa abrir el instalador? (Android pide permitir «instalar apps desconocidas» una vez). */
    fun canInstall(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    fun permissionIntent(context: Context): Intent =
        Intent(AndroidSettings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun install(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", apk)
        context.startActivity(
            Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
