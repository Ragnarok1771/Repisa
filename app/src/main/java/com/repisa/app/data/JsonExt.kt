package com.repisa.app.data

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Pequeños atajos para navegar JSON sin definir una clase por cada respuesta. */
operator fun JsonElement?.get(key: String): JsonElement? = (this as? JsonObject)?.get(key)
operator fun JsonElement?.get(index: Int): JsonElement? = (this as? JsonArray)?.getOrNull(index)

val JsonElement?.str: String?
    get() = (this as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content

val JsonElement?.long: Long? get() = str?.toLongOrNull() ?: str?.toDoubleOrNull()?.toLong()
val JsonElement?.int: Int? get() = long?.toInt()
val JsonElement?.arr: List<JsonElement> get() = (this as? JsonArray) ?: emptyList()
val JsonElement?.obj: Map<String, JsonElement> get() = (this as? JsonObject) ?: emptyMap()
