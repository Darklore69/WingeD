package com.example.pdfreader

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object PdfHelper {
    fun getEditableUri(context: Context, rawUri: Uri): Uri {
        if (rawUri.scheme == "file") return rawUri

        val fileName = "pdf_" + rawUri.toString().hashCode() + ".pdf"
        val file = File(context.filesDir, fileName)

        if (file.exists() && file.length() > 0) {
            return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }

        try {
            val tmpFile = File(context.filesDir, "$fileName.tmp")
            context.contentResolver.openInputStream(rawUri)?.use { input ->
                FileOutputStream(tmpFile).use { output ->
                    input.copyTo(output)
                }
            }
            if (tmpFile.exists() && tmpFile.length() > 0) {
                tmpFile.renameTo(file)
                return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return rawUri
    }
}