package com.example.pdfreader

import android.graphics.RectF
import android.util.SparseArray
import androidx.pdf.ExperimentalPdfApi
import androidx.pdf.view.PdfView
import androidx.pdf.viewer.fragment.PdfViewerFragment

@OptIn(ExperimentalPdfApi::class)
class ReaderPdfFragment : PdfViewerFragment() {

    interface ViewportListener {
        fun onViewport(firstVisible: Int, pageLocations: SparseArray<RectF>, viewHeight: Int)
    }

    var viewportListener: ViewportListener? = null
    var pdfViewRef: PdfView? = null

    override fun onPdfViewCreated(pdfView: PdfView) {
        super.onPdfViewCreated(pdfView)
        pdfViewRef = pdfView
        
        // Show built-in toolbox and FAB
        isToolboxVisible = true
        
        pdfView.addOnViewportChangedListener(object : PdfView.OnViewportChangedListener {
            override fun onViewportChanged(
                firstVisiblePage: Int,
                visiblePagesCount: Int,
                pageLocations: SparseArray<RectF>,
                zoomLevel: Float
            ) {
                viewportListener?.onViewport(firstVisiblePage, pageLocations, pdfView.height)
            }
        })

        (activity as? MainActivity)?.setupPdfViewSelectionMenu(pdfView)
    }
}