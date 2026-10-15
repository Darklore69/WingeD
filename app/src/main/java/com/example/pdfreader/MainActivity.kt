package com.example.pdfreader

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.SparseArray
import android.view.View
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity(), ReaderPdfFragment.ViewportListener {

    private lateinit var fragment: ReaderPdfFragment
    private lateinit var pdfContainer: View
    private lateinit var corner: ImageView
    private lateinit var btnMark: ImageView
    private lateinit var title: TextView
    private lateinit var emptyState: View
    private lateinit var pageIndicator: TextView

    private var currentUri: Uri? = null
    private var bookmarkedPage = -1
    private var currentPage = 0
    private var pendingScrollPage = -1
    private var cornerSizePx = 0f

    private val prefs by lazy { getSharedPreferences("pdf_prefs", Context.MODE_PRIVATE) }

    private val openDocument =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                try {
                    contentResolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {
                }
                prefs.edit().putString("last_uri", uri.toString()).apply()
                openPdf(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply the saved theme choice (System / Light / Dark) before the screen is built
        AppCompatDelegate.setDefaultNightMode(savedNightMode())
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // White status-bar icons on the coloured top bar
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false

        // Keep the toolbar below the status bar and the content above the navigation bar
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            findViewById<View>(R.id.topBar).setPadding(0, bars.top, 0, 0)
            v.setPadding(0, 0, 0, bars.bottom)
            insets
        }

        pdfContainer = findViewById(R.id.pdf_container)
        corner = findViewById(R.id.bookmarkCorner)
        btnMark = findViewById(R.id.btnMark)
        title = findViewById(R.id.title)
        emptyState = findViewById(R.id.emptyState)
        pageIndicator = findViewById(R.id.pageIndicator)
        cornerSizePx = 72f * resources.displayMetrics.density

        fragment = supportFragmentManager.findFragmentById(R.id.pdf_container) as ReaderPdfFragment
        fragment.viewportListener = this

        val pick = View.OnClickListener { openDocument.launch(arrayOf("application/pdf")) }
        findViewById<View>(R.id.btnOpen).setOnClickListener(pick)
        findViewById<View>(R.id.btnOpenEmpty).setOnClickListener(pick)
        btnMark.setOnClickListener { toggleBookmark() }
        findViewById<View>(R.id.btnGoto).setOnClickListener { goToBookmark() }
        findViewById<View>(R.id.btnTheme).setOnClickListener { showThemeMenu(it) }

        applyPdfInversion()

        val viewUri = if (intent?.action == Intent.ACTION_VIEW) intent.data else null
        val lastUri = prefs.getString("last_uri", null)?.let { Uri.parse(it) }
        when {
            viewUri != null -> openPdf(viewUri)
            lastUri != null -> openPdf(lastUri)
        }
    }

    // ---------- Theme ----------

    private fun savedNightMode(): Int = when (prefs.getInt("theme_mode", 0)) {
        1 -> AppCompatDelegate.MODE_NIGHT_NO
        2 -> AppCompatDelegate.MODE_NIGHT_YES
        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }

    private fun isNight(): Boolean =
        (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES

    private fun showThemeMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        val mode = prefs.getInt("theme_mode", 0)
        val labels = listOf("System default", "Light", "Dark")
        labels.forEachIndexed { i, label ->
            popup.menu.add(0, i, i, label).apply {
                isCheckable = true
                isChecked = (mode == i)
            }
        }
        popup.menu.setGroupCheckable(0, true, true)
        popup.menu.add(1, 10, 10, "Invert PDF colours in dark mode").apply {
            isCheckable = true
            isChecked = prefs.getBoolean("invert", true)
        }
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                0, 1, 2 -> {
                    savePosition()
                    prefs.edit().putInt("theme_mode", item.itemId).apply()
                    AppCompatDelegate.setDefaultNightMode(savedNightMode())
                }
                10 -> {
                    prefs.edit().putBoolean("invert", !item.isChecked).apply()
                    applyPdfInversion()
                }
            }
            true
        }
        popup.show()
    }

    // Inverts the PDF pages (softened so it's easy on the eyes) when dark mode is active
    private fun applyPdfInversion() {
        val invert = isNight() && prefs.getBoolean("invert", true)
        if (invert) {
            val s = -0.88f
            val t = 235f
            val matrix = ColorMatrix(
                floatArrayOf(
                    s, 0f, 0f, 0f, t,
                    0f, s, 0f, 0f, t,
                    0f, 0f, s, 0f, t,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(matrix) }
            pdfContainer.setLayerType(View.LAYER_TYPE_HARDWARE, paint)
        } else {
            pdfContainer.setLayerType(View.LAYER_TYPE_NONE, null)
        }
    }

    // ---------- PDF ----------

    private fun bookmarkKey(uri: Uri) = "bm_" + uri.toString().hashCode()
    private fun positionKey(uri: Uri) = "pos_" + uri.toString().hashCode()

    private fun openPdf(uri: Uri) {
        savePosition()
        currentUri = uri
        bookmarkedPage = prefs.getInt(bookmarkKey(uri), -1)
        val saved = prefs.getInt(positionKey(uri), 0)
        pendingScrollPage = if (saved > 0) saved else -1
        corner.visibility = View.GONE
        pageIndicator.visibility = View.GONE
        emptyState.visibility = View.GONE
        title.text = displayName(uri)
        updateMarkIcon()
        fragment.documentUri = uri
    }

    private fun displayName(uri: Uri): String {
        return try {
            contentResolver.query(uri, null, null, null, null)?.use { c ->
                val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (i >= 0 && c.moveToFirst()) c.getString(i).removeSuffix(".pdf") else null
            } ?: "PDF Reader"
        } catch (_: Exception) {
            "PDF Reader"
        }
    }

    private fun updateMarkIcon() {
        val marked = bookmarkedPage >= 0 && bookmarkedPage == currentPage
        btnMark.setImageResource(
            if (marked) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark_outline
        )
    }

    // Called by the PDF viewer whenever you scroll or zoom
    override fun onViewport(firstVisible: Int, pageLocations: SparseArray<RectF>, viewHeight: Int) {
        var page = firstVisible
        val mid = viewHeight / 2f
        for (i in 0 until pageLocations.size()) {
            val r = pageLocations.valueAt(i)
            if (r.top <= mid && r.bottom >= mid) {
                page = pageLocations.keyAt(i)
                break
            }
        }
        currentPage = page
        pageIndicator.text = "Page ${page + 1}"
        pageIndicator.visibility = View.VISIBLE
        updateMarkIcon()

        if (pendingScrollPage >= 0) {
            val p = pendingScrollPage
            pendingScrollPage = -1
            fragment.pdfViewRef?.scrollToPage(p)
            return
        }

        val rect = if (bookmarkedPage >= 0) pageLocations.get(bookmarkedPage) else null
        if (rect == null) {
            corner.visibility = View.GONE
        } else {
            corner.translationX = rect.right - cornerSizePx
            corner.translationY = rect.top
            corner.visibility = View.VISIBLE
        }
    }

    private fun toggleBookmark() {
        val uri = currentUri
        if (uri == null) {
            Toast.makeText(this, "Open a PDF first", Toast.LENGTH_SHORT).show()
            return
        }
        if (bookmarkedPage == currentPage) {
            bookmarkedPage = -1
            prefs.edit().remove(bookmarkKey(uri)).apply()
            corner.visibility = View.GONE
            Toast.makeText(this, "Bookmark removed", Toast.LENGTH_SHORT).show()
        } else {
            bookmarkedPage = currentPage
            prefs.edit().putInt(bookmarkKey(uri), currentPage).apply()
            Toast.makeText(this, "Bookmarked page ${currentPage + 1}", Toast.LENGTH_SHORT).show()
        }
        updateMarkIcon()
    }

    private fun goToBookmark() {
        if (currentUri == null) {
            Toast.makeText(this, "Open a PDF first", Toast.LENGTH_SHORT).show()
            return
        }
        if (bookmarkedPage < 0) {
            Toast.makeText(this, "No bookmark set in this PDF", Toast.LENGTH_SHORT).show()
        } else {
            fragment.pdfViewRef?.scrollToPage(bookmarkedPage)
        }
    }

    private fun savePosition() {
        val uri = currentUri ?: return
        prefs.edit().putInt(positionKey(uri), currentPage).apply()
    }

    override fun onPause() {
        super.onPause()
        savePosition()
    }
}