package com.example.pdfreader

import androidx.pdf.PdfRect

data class SavedHighlight(val area: PdfRect, val targetColor: Int)

object HighlightStorage {
    fun serialize(highlights: List<SavedHighlight>): String {
        return highlights.joinToString(";") { h ->
            val r = h.area
            "${r.pageNum},${r.left},${r.top},${r.right},${r.bottom},${h.targetColor}"
        }
    }

    fun deserialize(data: String): List<SavedHighlight> {
        if (data.isBlank()) return emptyList()
        val list = mutableListOf<SavedHighlight>()
        for (item in data.split(";")) {
            val parts = item.split(",")
            if (parts.size == 6) {
                try {
                    val pageNum = parts[0].toInt()
                    val left = parts[1].toFloat()
                    val top = parts[2].toFloat()
                    val right = parts[3].toFloat()
                    val bottom = parts[4].toFloat()
                    val color = parts[5].toInt()
                    val pdfRect = PdfRect(pageNum, left, top, right, bottom)
                    list.add(SavedHighlight(pdfRect, color))
                } catch (_: Exception) {
                }
            }
        }
        return list
    }
}