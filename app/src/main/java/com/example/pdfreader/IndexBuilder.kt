package com.example.pdfreader

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineNode
import com.tom_roush.pdfbox.text.PDFTextStripper

data class TocEntry(val title: String, val page: Int, val level: Int)

data class IndexResult(val entries: List<TocEntry>, val auto: Boolean)

object IndexBuilder {

    fun build(
        context: Context,
        uri: Uri,
        isCancelled: () -> Boolean,
        onProgress: (page: Int, total: Int) -> Unit
    ): IndexResult {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            PDDocument.load(stream).use { doc ->
                val outlineResult = buildFromOutline(doc)
                if (outlineResult.isNotEmpty()) {
                    return IndexResult(outlineResult, false)
                }
                val autoResult = buildFromTextAnalysis(doc, isCancelled, onProgress)
                return IndexResult(autoResult, true)
            }
        }
        return IndexResult(emptyList(), false)
    }

    private fun buildFromOutline(doc: PDDocument): List<TocEntry> {
        val cat = doc.documentCatalog ?: return emptyList()
        val outline = cat.documentOutline ?: return emptyList()
        val list = mutableListOf<TocEntry>()
        walkOutline(doc, outline, 0, list)
        return list
    }

    private fun walkOutline(doc: PDDocument, node: PDOutlineNode, level: Int, out: MutableList<TocEntry>) {
        if (level > 8 || out.size > 3000) return
        var current: PDOutlineItem? = node.firstChild
        while (current != null) {
            val title = current.title?.trim()
            if (!title.isNullOrBlank()) {
                val page = current.findDestinationPage(doc)
                if (page != null) {
                    val pageNum = doc.pages.indexOf(page)
                    if (pageNum >= 0) {
                        out.add(TocEntry(title, pageNum, level))
                    }
                }
            }
            walkOutline(doc, current, level + 1, out)
            current = current.nextSibling
        }
    }

    private fun buildFromTextAnalysis(
        doc: PDDocument,
        isCancelled: () -> Boolean,
        onProgress: (page: Int, total: Int) -> Unit
    ): List<TocEntry> {
        val total = doc.numberOfPages
        val stripper = PDFTextStripper()
        val out = mutableListOf<TocEntry>()
        val maxPagesToScan = minOf(total, 1500)

        for (i in 0 until maxPagesToScan) {
            if (isCancelled()) break
            onProgress(i + 1, maxPagesToScan)
            stripper.startPage = i + 1
            stripper.endPage = i + 1
            val text = stripper.getText(doc) ?: continue
            val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
            if (lines.isNotEmpty()) {
                val firstLine = lines.first()
                if (firstLine.length in 3..80 && isHeadingCandidate(firstLine)) {
                    out.add(TocEntry(firstLine, i, 0))
                }
            }
        }
        return out
    }

    private fun isHeadingCandidate(line: String): Boolean {
        if (line.matches(Regex("^(chapter|section|part|unit)\\s+\\d+.*", RegexOption.IGNORE_CASE))) return true
        if (line.matches(Regex("^\\d+(\\.\\d+)*\\s+[A-Z].*"))) return true
        if (line.length in 4..50 && line.all { it.isUpperCase() || it.isWhitespace() || it.isDigit() }) return true
        return false
    }

    fun serialize(entries: List<TocEntry>): String {
        return entries.joinToString("\n") { "${it.page}|${it.level}|${it.title.replace("|", "/")}" }
    }

    fun deserialize(data: String): List<TocEntry> {
        if (data.isBlank()) return emptyList()
        val list = mutableListOf<TocEntry>()
        for (line in data.lines()) {
            val parts = line.split("|", limit = 3)
            if (parts.size == 3) {
                try {
                    val page = parts[0].toInt()
                    val level = parts[1].toInt()
                    val title = parts[2]
                    list.add(TocEntry(title, page, level))
                } catch (_: Exception) {
                }
            }
        }
        return list
    }
}