package com.nova.runtime.ai.native.extraction

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import com.nova.runtime.ai.io.readBoundedBytes
import com.nova.runtime.ai.model.DocumentExtractionResult
import com.nova.runtime.ai.model.DocumentOcrTextCleaner
import com.nova.runtime.ai.model.DocumentTextExtractor
import com.nova.runtime.ai.model.OcrEngine
import com.nova.runtime.ai.model.OcrResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream

/**
 * Local document text extraction (DPS §7):
 * - PDFs: digital text layer via PdfBox, then page OCR fallback for scans.
 * - Images (PNG/JPEG/WebP…): ML Kit OCR for timetable photos and scans.
 * - DOCX: unzip `word/document.xml` and strip tags.
 * - Plain-text / HTML: read directly; HTML tags stripped to plain schedule text.
 *
 * Legacy `.doc` is not extractable — [DocumentTextExtractor.isExtractable] returns false.
 */
class AndroidDocumentTextExtractor(
    private val context: Context,
    private val ocrEngine: OcrEngine,
) : DocumentTextExtractor {
    override suspend fun extract(
        uri: String,
        mimeType: String,
        extension: String,
        maxPages: Int,
        maxChars: Int,
    ): DocumentExtractionResult =
        withContext(Dispatchers.IO) {
            runCatching {
                when {
                    isPlainText(mimeType, extension) ->
                        readPlainText(uri, maxChars, mimeType, extension)
                    DocumentTextExtractor.isImage(mimeType, extension) ->
                        extractImageText(uri, maxChars)
                    isPdf(mimeType, extension) -> extractPdfText(uri, maxPages, maxChars)
                    isDocx(mimeType, extension) -> extractDocxText(uri, maxChars)
                    else -> DocumentExtractionResult.Unsupported
                }
            }.getOrElse { e ->
                Log.w(TAG, "Extract threw uri=$uri: ${e.message}")
                DocumentExtractionResult.Unreadable(e.message ?: "extract_exception")
            }
        }

    private fun readPlainText(
        uri: String,
        maxChars: Int,
        mimeType: String,
        extension: String,
    ): DocumentExtractionResult {
        val stream = openInputStream(uri) ?: return DocumentExtractionResult.Unreadable("open_failed")
        return stream.use { input ->
            val raw = com.nova.runtime.ai.io.readDocumentText(input, maxChars)
            if (raw.startsWith("%PDF-") || raw.startsWith("PK\u0003\u0004")) {
                return@use DocumentExtractionResult.Unreadable("binary_content_mislabeled_as_text")
            }
            val plain = if (isHtml(mimeType, extension) || looksLikeHtml(raw)) {
                stripHtmlToPlain(raw)
            } else raw
            if (plain.isBlank()) DocumentExtractionResult.Empty else DocumentExtractionResult.Success(plain)
        }
    }

    private suspend fun extractImageText(
        uri: String,
        maxChars: Int,
    ): DocumentExtractionResult {
        val bytes =
            openInputStream(uri)?.use { it.readBoundedBytes(10 * 1024 * 1024) } ?: run {
                Log.w(TAG, "Image open failed: $uri")
                return DocumentExtractionResult.Unreadable("open_failed_or_image_too_large")
            }
        if (bytes.isEmpty()) return DocumentExtractionResult.Empty
        return when (val result = ocrEngine.recognize(bytes)) {
            is OcrResult.Success -> {
                val text = DocumentOcrTextCleaner.cleanScheduleText(result.text).take(maxChars)
                Log.i(TAG, "Image OCR uri=$uri chars=${text.length}")
                if (text.isBlank()) {
                    DocumentExtractionResult.Empty
                } else {
                    DocumentExtractionResult.Success(text)
                }
            }
            is OcrResult.Failure -> {
                Log.w(TAG, "Image OCR failed uri=$uri: ${result.message}")
                DocumentExtractionResult.Unreadable("ocr_failed:${result.message}")
            }
        }
    }

    private fun isHtml(
        mimeType: String,
        extension: String,
    ): Boolean =
        mimeType.equals("text/html", ignoreCase = true) ||
            extension.equals("html", ignoreCase = true) ||
            extension.equals("htm", ignoreCase = true)

    private fun looksLikeHtml(raw: String): Boolean =
        raw.contains("<html", ignoreCase = true) ||
            raw.contains("<!doctype", ignoreCase = true) ||
            raw.contains("<table", ignoreCase = true)

    private fun stripHtmlToPlain(raw: String): String {
        var text = raw.replace(Regex("""(?is)<(script|style)\b[^>]*>.*?</\1>"""), " ")
        text = text.replace(Regex("""(?i)</(?:tr|p|div|li|h[1-6])\s*>"""), "\n")
        text = text.replace(Regex("""(?i)<br\s*/?>"""), "\n")
        text = text.replace(Regex("""(?i)</td\s*>"""), " | ")
        text = text.replace(Regex("""(?is)<[^>]+>"""), " ")
        text =
            text
                .replace("&nbsp;", " ", ignoreCase = true)
                .replace("&amp;", "&", ignoreCase = true)
                .replace("&lt;", "<", ignoreCase = true)
                .replace("&gt;", ">", ignoreCase = true)
                .replace("&quot;", "\"", ignoreCase = true)
        return text.lines()
            .map { it.replace(Regex("""\s+"""), " ").trim() }
            .filter { it.isNotBlank() }
            .joinToString("\n")
    }

    private fun openInputStream(uri: String): java.io.InputStream? {
        val parsed = Uri.parse(uri)
        if (parsed.scheme == "file") {
            val path = parsed.path ?: return null
            return runCatching { FileInputStream(File(path)) }.getOrNull()
        }
        return runCatching { context.contentResolver.openInputStream(parsed) }.getOrNull()
    }

    private fun openFileDescriptor(uri: String): ParcelFileDescriptor? {
        val parsed = Uri.parse(uri)
        if (parsed.scheme == "file") {
            val path = parsed.path ?: return null
            return runCatching {
                ParcelFileDescriptor.open(File(path), ParcelFileDescriptor.MODE_READ_ONLY)
            }.getOrNull()
        }
        return runCatching { context.contentResolver.openFileDescriptor(parsed, "r") }.getOrNull()
    }

    private suspend fun extractPdfText(
        uri: String,
        maxPages: Int,
        maxChars: Int,
    ): DocumentExtractionResult {
        val digital = extractPdfDigitalText(uri, maxChars, maxPages)
        if (digital is DocumentExtractionResult.Success &&
            digital.text.length >= MIN_DIGITAL_PDF_CHARS
        ) {
            Log.i(TAG, "PDF digital text uri=$uri chars=${digital.text.length}")
            return digital
        }
        val ocr = extractPdfViaOcr(uri, maxPages, maxChars)
        return when {
            ocr is DocumentExtractionResult.Success &&
                (
                    digital !is DocumentExtractionResult.Success ||
                        ocr.text.length >= digital.text.length
                ) -> ocr
            digital is DocumentExtractionResult.Success -> digital
            ocr is DocumentExtractionResult.Success -> ocr
            digital is DocumentExtractionResult.Unreadable -> digital
            ocr is DocumentExtractionResult.Unreadable -> ocr
            digital is DocumentExtractionResult.Empty || ocr is DocumentExtractionResult.Empty ->
                DocumentExtractionResult.Empty
            else -> ocr
        }
    }

    private fun extractPdfDigitalText(
        uri: String,
        maxChars: Int,
        maxPages: Int,
    ): DocumentExtractionResult {
        ensurePdfBoxLoaded()
        val stream =
            openInputStream(uri)
                ?: return DocumentExtractionResult.Unreadable("open_failed")
        return stream.use { input ->
            runCatching {
                com.tom_roush.pdfbox.pdmodel.PDDocument.load(input).use { doc ->
                    val stripper =
                        com.tom_roush.pdfbox.text.PDFTextStripper().apply {
                            sortByPosition = true
                            startPage = 1
                            endPage = doc.numberOfPages.coerceAtMost(maxPages)
                        }
                    val text =
                        stripper.getText(doc)
                            .replace(Regex("""[ \t]+\n"""), "\n")
                            .replace(Regex("""\n{3,}"""), "\n\n")
                            .trim()
                            .take(maxChars)
                    if (text.isBlank()) {
                        DocumentExtractionResult.Empty
                    } else {
                        DocumentExtractionResult.Success(text)
                    }
                }
            }.getOrElse { e ->
                Log.w(TAG, "PDF digital extract failed uri=$uri: ${e.message}")
                DocumentExtractionResult.Unreadable("pdfbox_failed:${e.message}")
            }
        }
    }

    private suspend fun extractPdfViaOcr(
        uri: String,
        maxPages: Int,
        maxChars: Int,
    ): DocumentExtractionResult {
        val descriptor =
            openFileDescriptor(uri) ?: run {
                Log.w(TAG, "PDF open failed (no fd): $uri")
                return DocumentExtractionResult.Unreadable("open_failed")
            }
        try {
            val renderer = PdfRenderer(descriptor)
            try {
                val text = StringBuilder()
                val totalPages = renderer.pageCount
                val pageBudget = maxPages.coerceAtLeast(1).coerceAtMost(MAX_OCR_PAGES)
                val pageCount = minOf(totalPages, pageBudget)
                var pagesWithText = 0
                var ocrFailures = 0
                for (pageIndex in 0 until pageCount) {
                    if (text.length >= maxChars) break
                    val pageText = renderPageAndRecognize(renderer, pageIndex)
                    when {
                        !pageText.isNullOrBlank() -> {
                            pagesWithText++
                            if (text.isNotEmpty()) text.append('\n')
                            text.append(pageText.trim())
                        }
                        pageText == null -> ocrFailures++
                    }
                }
                val cleaned =
                    DocumentOcrTextCleaner.cleanScheduleText(text.toString())
                        .take(maxChars)
                Log.i(
                    TAG,
                    "PDF OCR uri=$uri totalPages=$totalPages processed=$pageCount " +
                        "pagesWithText=$pagesWithText ocrFailures=$ocrFailures chars=${cleaned.length}",
                )
                return when {
                    cleaned.isNotBlank() -> DocumentExtractionResult.Success(cleaned)
                    pagesWithText == 0 && ocrFailures > 0 && text.isEmpty() ->
                        DocumentExtractionResult.Unreadable("ocr_failed")
                    else -> DocumentExtractionResult.Empty
                }
            } finally {
                renderer.close()
            }
        } finally {
            runCatching { descriptor.close() }
        }
    }

    private fun extractDocxText(
        uri: String,
        maxChars: Int,
    ): DocumentExtractionResult {
        val stream =
            openInputStream(uri)
                ?: return DocumentExtractionResult.Unreadable("open_failed")
        return stream.use { input ->
            runCatching {
                java.util.zip.ZipInputStream(input.buffered()).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        if (entry.name == "word/document.xml") {
                            val plain = readWordXml(zip, maxChars)
                            return@use if (plain.isBlank()) {
                                DocumentExtractionResult.Empty
                            } else {
                                DocumentExtractionResult.Success(plain)
                            }
                        }
                        entry = zip.nextEntry
                    }
                    DocumentExtractionResult.Empty
                }
            }.getOrElse { e ->
                Log.w(TAG, "DOCX extract failed uri=$uri: ${e.message}")
                DocumentExtractionResult.Unreadable("docx_failed:${e.message}")
            }
        }
    }

    private fun readWordXml(input: java.io.InputStream, maxChars: Int): String {
        val parser = android.util.Xml.newPullParser()
        parser.setFeature(org.xmlpull.v1.XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        parser.setInput(input, null)
        val result = StringBuilder()
        var inText = false
        var event = parser.eventType
        while (event != org.xmlpull.v1.XmlPullParser.END_DOCUMENT && result.length < maxChars) {
            when (event) {
                org.xmlpull.v1.XmlPullParser.START_TAG -> when (parser.name) {
                    "t" -> inText = true
                    "tab" -> result.append('\t')
                    "br", "cr" -> result.append('\n')
                }
                org.xmlpull.v1.XmlPullParser.TEXT -> if (inText) {
                    result.append(parser.text.take((maxChars - result.length).coerceAtLeast(0)))
                }
                org.xmlpull.v1.XmlPullParser.END_TAG -> when (parser.name) {
                    "t" -> inText = false
                    "p" -> result.append("\n\n")
                    "tc" -> {
                        while (result.isNotEmpty() && result.last().isWhitespace()) result.setLength(result.length - 1)
                        result.append(" | ")
                    }
                    "tr" -> {
                        if (result.endsWith(" | ")) result.setLength(result.length - 3)
                        result.append('\n')
                    }
                }
            }
            event = parser.next()
        }
        return result.toString().take(maxChars).replace(Regex("[ \t]+\n"), "\n")
            .replace(Regex("\n{3,}"), "\n\n").trim()
    }

    private fun ensurePdfBoxLoaded() {
        if (pdfBoxReady) return
        synchronized(this) {
            if (pdfBoxReady) return
            runCatching {
                com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(context)
                pdfBoxReady = true
            }.onFailure { e ->
                Log.w(TAG, "PDFBox init failed: ${e.message}")
            }
        }
    }

    private suspend fun renderPageAndRecognize(
        renderer: PdfRenderer,
        pageIndex: Int,
    ): String? {
        val page = renderer.openPage(pageIndex)
        val pageBytes =
            try {
                val scale = (TARGET_RENDER_WIDTH_PX.toFloat() / page.width).coerceIn(MIN_SCALE, MAX_SCALE)
                val width = (page.width * scale).toInt().coerceAtLeast(1)
                val height = (page.height * scale).toInt().coerceAtLeast(1)
                // Cap total pixels to avoid OOM on very tall pages while keeping OCR-readable DPI.
                val maxPixels = MAX_RENDER_PIXELS.toLong()
                val pixels = width.toLong() * height.toLong()
                val (finalWidth, finalHeight) =
                    if (pixels > maxPixels) {
                        val shrink = kotlin.math.sqrt(maxPixels.toDouble() / pixels.toDouble())
                        ((width * shrink).toInt().coerceAtLeast(1)) to
                            ((height * shrink).toInt().coerceAtLeast(1))
                    } else {
                        width to height
                    }
                val bitmap = Bitmap.createBitmap(finalWidth, finalHeight, Bitmap.Config.ARGB_8888)
                // PdfRenderer leaves undrawn regions transparent; OCR needs a white page background.
                Canvas(bitmap).drawColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                bitmap.recycle()
                stream.toByteArray()
            } finally {
                page.close()
            }
        return when (val result = ocrEngine.recognize(pageBytes)) {
            is OcrResult.Success -> result.text
            is OcrResult.Failure -> null
        }
    }

    private fun isPdf(
        mimeType: String,
        extension: String,
    ): Boolean =
        mimeType.equals("application/pdf", ignoreCase = true) ||
            extension.equals("pdf", ignoreCase = true)

    private fun isDocx(
        mimeType: String,
        extension: String,
    ): Boolean {
        val ext = extension.lowercase()
        val mime = mimeType.lowercase()
        return ext == "docx" || mime.contains("wordprocessingml")
    }

    private fun isPlainText(
        mimeType: String,
        extension: String,
    ): Boolean =
        mimeType.startsWith("text/", ignoreCase = true) ||
            extension.lowercase() in DocumentTextExtractor.PLAIN_TEXT_EXTENSIONS

    @Volatile
    private var pdfBoxReady: Boolean = false

    private companion object {
        const val TAG = "NOVA/DocumentOCR"

        /** Wider render target improves small timetable/menu OCR without unbounded memory. */
        const val TARGET_RENDER_WIDTH_PX = 1600
        const val MIN_SCALE = 1.25f
        const val MAX_SCALE = 4f
        const val MAX_RENDER_PIXELS = 6_000_000
        const val MIN_DIGITAL_PDF_CHARS = 40
        const val MAX_OCR_PAGES = 24
    }
}
