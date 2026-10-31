package com.nova.runtime.ai.native.extraction

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nova.runtime.ai.model.DocumentExtractionResult
import com.nova.runtime.ai.model.OcrEngine
import com.nova.runtime.ai.model.OcrResult
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AndroidDocumentTextExtractorTest {
    private lateinit var context: Context
    private lateinit var extractor: AndroidDocumentTextExtractor

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        extractor = AndroidDocumentTextExtractor(context, StubOcrEngine())
    }

    @Test
    fun digitalPdfReadsPastOldTwentyPageLimitAndHonorsExplicitPageBudget() = runTest {
        com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(context)
        val file = File(context.cacheDir, "long-digital.pdf")
        com.tom_roush.pdfbox.pdmodel.PDDocument().use { pdf ->
            for (number in 1..25) {
                val page = com.tom_roush.pdfbox.pdmodel.PDPage()
                pdf.addPage(page)
                com.tom_roush.pdfbox.pdmodel.PDPageContentStream(pdf, page).use { stream ->
                    stream.beginText()
                    stream.setFont(com.tom_roush.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 12f)
                    stream.newLineAtOffset(40f, 700f)
                    stream.showText("Engineering reference page $number with useful readable operating instructions.")
                    if (number == 25) {
                        stream.newLineAtOffset(0f, -20f)
                        stream.showText("Final access code QUARTZ-921.")
                    }
                    stream.endText()
                }
            }
            pdf.save(file)
        }
        val full = extractor.extract(file.toURI().toString(), "application/pdf", "pdf") as DocumentExtractionResult.Success
        assertTrue(full.text.contains("QUARTZ-921"))
        val limited = extractor.extract(file.toURI().toString(), "application/pdf", "pdf", maxPages = 1) as DocumentExtractionResult.Success
        assertTrue(!limited.text.contains("QUARTZ-921"))
    }

    @Test
    fun extract_docxPreservesRunsEntitiesAndParagraphs() = runTest {
        val file = File(context.cacheDir, "structured.docx")
        java.util.zip.ZipOutputStream(file.outputStream()).use { zip ->
            zip.putNextEntry(java.util.zip.ZipEntry("word/document.xml"))
            zip.write("""
                <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body>
                <w:p><w:r><w:t>War</w:t></w:r><w:r><w:t>ranty &amp; safety</w:t></w:r></w:p>
                <w:p><w:r><w:t>Second paragraph: caf&#233;.</w:t></w:r></w:p>
                </w:body></w:document>
            """.trimIndent().toByteArray())
            zip.closeEntry()
        }
        val result = extractor.extract(file.toURI().toString(), "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx")
        assertTrue(result is DocumentExtractionResult.Success)
        assertEquals("Warranty & safety\n\nSecond paragraph: café.", (result as DocumentExtractionResult.Success).text)
    }

    @Test
    fun extract_readsUnicodeAndTailBeyondPreviousLimit() = runTest {
        val file = File(context.cacheDir, "unicode.txt")
        val body = "Useful background. ".repeat(2400) + "The final warranty code is QUARTZ-921."
        file.writeBytes(byteArrayOf(0xff.toByte(), 0xfe.toByte()) + body.toByteArray(Charsets.UTF_16LE))
        val result = extractor.extract(file.toURI().toString(), "text/plain", "txt")
        assertTrue(result is DocumentExtractionResult.Success)
        assertTrue((result as DocumentExtractionResult.Success).text.endsWith("QUARTZ-921."))
    }

    @Test
    fun extract_plainTextFile_returnsSuccess() =
        runTest {
            val file = File(context.cacheDir, "menu.txt")
            file.writeText(
                """
                MONDAY
                Lunch: dal rice
                """.trimIndent(),
            )

            val result = extractor.extract(
                uri = file.toURI().toString(),
                mimeType = "text/plain",
                extension = "txt",
            )

            assertTrue(result is DocumentExtractionResult.Success)
            assertTrue((result as DocumentExtractionResult.Success).text.contains("dal rice"))
        }

    @Test
    fun extract_emptyTextFile_returnsEmpty() =
        runTest {
            val file = File(context.cacheDir, "blank.txt")
            file.writeText("   \n  ")

            val result = extractor.extract(
                uri = file.toURI().toString(),
                mimeType = "text/plain",
                extension = "txt",
            )

            assertEquals(DocumentExtractionResult.Empty, result)
        }

    @Test
    fun extract_htmlStripsMarkup() =
        runTest {
            val file = File(context.cacheDir, "timetable.html")
            file.writeText(
                """
                <html><body>
                <table><tr><td>Monday</td><td>Lec-AAA1111</td></tr></table>
                </body></html>
                """.trimIndent(),
            )

            val result = extractor.extract(
                uri = file.toURI().toString(),
                mimeType = "text/html",
                extension = "html",
            )

            assertTrue(result is DocumentExtractionResult.Success)
            val text = (result as DocumentExtractionResult.Success).text
            assertTrue(text.contains("AAA1111"))
            assertTrue(!text.contains("<table"))
        }

    @Test
    fun extract_unsupportedDocExtension_returnsUnsupported() =
        runTest {
            val result = extractor.extract(
                uri = "content://docs/legacy.doc",
                mimeType = "application/msword",
                extension = "doc",
            )

            assertEquals(DocumentExtractionResult.Unsupported, result)
        }

    private class StubOcrEngine : OcrEngine {
        override suspend fun recognize(imageBytes: ByteArray): OcrResult =
            OcrResult.Failure("unused in plain-text tests")
    }
}
