package com.example.pdfreader

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object PdfHelper {
    fun getEditableUri(context: Context, uri: Uri): Uri {
        // If it's already a file URI in our files dir, return it
        if (uri.scheme == "file") return uri
        
        try {
            // Create a local copy that androidx.pdf can edit directly without prompting
            val file = File(context.filesDir, "working_copy.pdf")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            return Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            return uri
        }
    }
}