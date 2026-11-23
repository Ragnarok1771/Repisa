package com.repisa.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

object Http {
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .addInterceptor { chain ->
            val req = chain.request()
            // Solo si la petición no trae ya su propia identificación (Epic necesita la del lanzador).
            chain.proceed(if (req.header("User-Agent") != null) req
                else req.newBuilder().header("User-Agent", "Repisa (Android)").build())
        }
        .build()

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
        coerceInputValues = true
    }

    /** Ejecuta la petición y devuelve el JSON; lanza [HttpException] si el código no es 2xx. */
    suspend fun getJson(request: Request): JsonElement = withContext(Dispatchers.IO) {
        client.newCall(request).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw HttpException(resp.code, body.take(300))
            json.parseToJsonElement(body)
        }
    }

    suspend fun getJson(url: String): JsonElement = getJson(Request.Builder().url(url).build())

    /** true si la URL responde 200 (para comprobar si existe una portada). */
    suspend fun exists(url: String): Boolean = withContext(Dispatchers.IO) {
        try {
            client.newCall(Request.Builder().url(url).head().build()).execute().use { it.isSuccessful }
        } catch (_: IOException) {
            false
        }
    }
}

class HttpException(val code: Int, val body: String) : IOException("HTTP $code: $body")
