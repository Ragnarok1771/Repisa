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

    data class Doc(val id: String, val name: String, val isDir: Boolean, val uri: Uri)

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
                arrayOf(Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME, Document.COLUMN_MIME_TYPE),
                null, null, null,
            )?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getString(0) ?: continue
                    val name = c.getString(1) ?: continue
                    out += Doc(id, name, c.getString(2) == Document.MIME_TYPE_DIR,
                        DocumentsContract.buildDocumentUriUsingTree(treeUri, id))
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

    fun readText(doc: Doc): String? = runCatching {
        context.contentResolver.openInputStream(doc.uri)?.bufferedReader()?.use { it.readText() }
    }.getOrNull()

    companion object {
        fun displayName(context: Context, uriString: String): String = runCatching {
            val id = DocumentsContract.getTreeDocumentId(Uri.parse(uriString))
            id.substringAfter(':').ifEmpty { "Almacenamiento" }.let { "/$it" }
        }.getOrDefault(uriString)
    }
}
