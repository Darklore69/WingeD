package com.example.pdfreader

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object PdfHelper {
    fun getEditableUri(context: Context, rawUri: Uri): Uri {
        if (rawUri.scheme == "file") return rawUri

        val fileName = "pdf_" + rawUri.toString().hashCode() + ".pdf"
        val file = File(context.filesDir, fileName)

        if (file.exists() && file.length() > 0) {
            return Uri.fromFile(file)
        }

        try {
            context.contentResolver.openInputStream(rawUri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            if (file.exists() && file.length() > 0) {
                return Uri.fromFile(file)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return rawUri
    }
}