package com.repisa.app.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document

/**
 * Acceso rápido a carpetas elegidas con el selector de Android (Storage Access Framework).
 * Consultar el ContentResolver directamente es mucho más rápido que DocumentFile.listFiles().
 */
class SafTree(private val context: Context, val treeUri: Uri) {

    data class Doc(val id: String, val name: String, val isDir: Boolean, val uri: Uri, val size: Long = -1)

    val root: Doc by lazy {
        val id = DocumentsContract.getTreeDocumentId(treeUri)
        Doc(id, id.substringAfterLast('/').substringAfterLast(':'), true,
            DocumentsContract.buildDocumentUriUsingTree(treeUri, id))
    }

    fun children(parent: Doc = root): List<Doc> {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parent.id)
        val out = mutableListOf<Doc>()
        runCatching {
            context.contentResolver.query(
                childrenUri,
                arrayOf(Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME, Document.COLUMN_MIME_TYPE, Document.COLUMN_SIZE),
                null, null, null,
            )?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getString(0) ?: continue
                    val name = c.getString(1) ?: continue
                    out += Doc(id, name, c.getString(2) == Document.MIME_TYPE_DIR,
                        DocumentsContract.buildDocumentUriUsingTree(treeUri, id),
                        if (c.isNull(3)) -1 else c.getLong(3))
                }
            }
        }
        return out
    }

    fun child(parent: Doc, name: String): Doc? = children(parent).firstOrNull { it.name.equals(name, ignoreCase = true) }

    /** Navega por una ruta relativa: "gamelists/snes". */
    fun path(vararg parts: String): Doc? {
        var cur: Doc? = root
        for (p in parts) cur = cur?.let { child(it, p) } ?: return null
        return cur
    }

    /** Los primeros [max] bytes de un archivo (para leer cabeceras binarias). */
    fun readBytes(doc: Doc, max: Int = 4096): ByteArray? = runCatching {
        context.contentResolver.openInputStream(doc.uri)?.use { input ->
            val buf = ByteArray(max)
            var n = 0
            while (n < max) { val r = input.read(buf, n, max - n); if (r < 0) break; n += r }
            buf.copyOf(n)
        }
    }.getOrNull()

    fun readText(doc: Doc): String? = runCatching {
        context.contentResolver.openInputStream(doc.uri)?.bufferedReader()?.use { it.readText() }?.removePrefix("\uFEFF")
    }.getOrNull()

    /**
     * Busca una carpeta por nombre dentro del árbol (por niveles, la más cercana primero), para que baste con
     * elegir la carpeta principal: "RetroArch" encuentra "RetroArch/playlists" aunque esté más adentro.
     */
    fun findDir(name: String, maxDepth: Int = 5, skip: Set<String> = emptySet(), limit: Int = 3000): Doc? {
        if (root.name.equals(name, true)) return root
        var level = listOf(root)
        var visited = 0
        repeat(maxDepth) {
            val next = mutableListOf<Doc>()
            for (d in level) {
                for (c in children(d)) {
                    if (!c.isDir || c.name.startsWith(".")) continue
                    if (c.name.equals(name, true)) return c
                    if (c.name.lowercase() !in skip) next += c
                }
                if (++visited > limit) return null
            }
            level = next
        }
        return null
    }

    fun open(doc: Doc): java.io.InputStream? = runCatching { context.contentResolver.openInputStream(doc.uri) }.getOrNull()

    companion object {
        fun displayName(context: Context, uriString: String): String = runCatching {
            val id = DocumentsContract.getTreeDocumentId(Uri.parse(uriString))
            id.substringAfter(':').ifEmpty { "Almacenamiento" }.let { "/$it" }
        }.getOrDefault(uriString)
    }
}
