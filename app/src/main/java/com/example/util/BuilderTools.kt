package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CodeValidationReport(
  val isClean: Boolean,
  val issuesAr: List<String>,
  val issuesEn: List<String>,
)

object BuilderTools {

  fun buildStandaloneHtml(
    title: String,
    htmlBodyOrDoc: String,
    cssCode: String,
    jsCode: String,
    desktopViewport: Boolean = false,
  ): String {
    val viewportContent = if (desktopViewport) {
      "width=1280, initial-scale=0.35, minimum-scale=0.2, maximum-scale=2.0, user-scalable=yes"
    } else {
      "width=device-width, initial-scale=1.0, maximum-scale=2.0"
    }

    // Strict CSP inside preview sandbox preventing access to file:// or unintended parent origins
    val cspMeta =
      "<meta http-equiv=\"Content-Security-Policy\" content=\"default-src 'unsafe-inline' 'unsafe-eval' data: blob: https://fonts.googleapis.com https://fonts.gstatic.com https://cdn.jsdelivr.net https://cdnjs.cloudflare.com;\">"

    val trimmedHtml = htmlBodyOrDoc.trim()
    val bodyContent = if (trimmedHtml.contains("<body", ignoreCase = true)) {
      val afterOpen = trimmedHtml.substringAfter("<body").substringAfter(">")
      afterOpen.substringBeforeLast("</body>")
    } else if (trimmedHtml.contains("<html", ignoreCase = true)) {
      trimmedHtml
        .replace(Regex("<!DOCTYPE[^>]*>", RegexOption.IGNORE_CASE), "")
        .replace(Regex("</?html[^>]*>", RegexOption.IGNORE_CASE), "")
        .replace(Regex("<head[\\s\\S]*?</head>", RegexOption.IGNORE_CASE), "")
    } else {
      trimmedHtml
    }

    val safeTitle = title.replace("<", "&lt;").replace(">", "&gt;")

    return buildString {
      appendLine("<!DOCTYPE html>")
      appendLine("<html>")
      appendLine("<head>")
      appendLine("  <meta charset=\"UTF-8\">")
      appendLine("  <meta name=\"viewport\" content=\"$viewportContent\">")
      appendLine("  $cspMeta")
      appendLine("  <title>$safeTitle</title>")
      appendLine("  <style>")
      appendLine("    *, *::before, *::after { box-sizing: border-box; }")
      appendLine(cssCode)
      appendLine("  </style>")
      appendLine("</head>")
      appendLine("<body>")
      appendLine(bodyContent)
      appendLine("  <script>")
      appendLine("    try {")
      appendLine(jsCode)
      appendLine("    } catch (err) {")
      appendLine("      console.error('WamdX Preview Runtime Error:', err);")
      appendLine("    }")
      appendLine("  </script>")
      appendLine("</body>")
      appendLine("</html>")
    }
  }

  fun validateProject(html: String, css: String, js: String): CodeValidationReport {
    val issuesAr = mutableListOf<String>()
    val issuesEn = mutableListOf<String>()

    if (html.isBlank()) {
      issuesAr += "كود HTML فارغ — لا توجد عناصر واجهة للعرض."
      issuesEn += "HTML markup is empty — no UI elements to render."
    }

    val openBracesCss = css.count { it == '{' }
    val closeBracesCss = css.count { it == '}' }
    if (openBracesCss != closeBracesCss) {
      issuesAr += "عدد الأقواس المتعرجة في CSS غير متطابق ($openBracesCss مفتوح مقابل $closeBracesCss مغلق)."
      issuesEn += "Unbalanced curly braces in CSS ($openBracesCss open vs $closeBracesCss closed)."
    }

    val openBracesJs = js.count { it == '{' }
    val closeBracesJs = js.count { it == '}' }
    if (openBracesJs != closeBracesJs) {
      issuesAr += "تحذير: قد توجد أقواس غير متوازنة في JavaScript ($openBracesJs مقابل $closeBracesJs)."
      issuesEn += "Warning: Potentially unbalanced braces in JavaScript ($openBracesJs vs $closeBracesJs)."
    }

    val openParensJs = js.count { it == '(' }
    val closeParensJs = js.count { it == ')' }
    if (openParensJs != closeParensJs) {
      issuesAr += "تحذير: قد توجد أقواس دائرية غير مكتملة في JavaScript."
      issuesEn += "Warning: Potentially unbalanced parentheses in JavaScript."
    }

    if (html.contains("<script src=\"file:", ignoreCase = true) || js.contains("file://")) {
      issuesAr += "تم رصد محاولة وصول محلي غير مسموح بها (file://) وتم حظرها في البيئة المعزولة."
      issuesEn += "Detected disallowed local file:// reference; blocked by sandbox."
    }

    return CodeValidationReport(
      isClean = issuesAr.isEmpty(),
      issuesAr = issuesAr,
      issuesEn = issuesEn,
    )
  }

  suspend fun writeStandaloneHtmlToUri(
    context: Context,
    uri: Uri,
    title: String,
    html: String,
    css: String,
    js: String,
  ): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      val content = buildStandaloneHtml(title, html, css, js, desktopViewport = false)
      context.contentResolver.openOutputStream(uri)?.use { stream ->
        stream.write(content.toByteArray(Charsets.UTF_8))
      } ?: return@withContext Result.failure(IllegalStateException("Could not open output stream"))
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun writeProjectZipToUri(
    context: Context,
    uri: Uri,
    projectName: String,
    html: String,
    css: String,
    js: String,
  ): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      context.contentResolver.openOutputStream(uri)?.use { stream ->
        writeZipEntries(stream, projectName, html, css, js)
      } ?: return@withContext Result.failure(IllegalStateException("Could not open ZIP output stream"))
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun createShareableZipIntent(
    context: Context,
    projectName: String,
    html: String,
    css: String,
    js: String,
  ): Result<Intent> = withContext(Dispatchers.IO) {
    try {
      val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
      val safeSlug = projectName
        .replace(Regex("[^a-zA-Z0-9\\u0600-\\u06FF_-]"), "_")
        .take(32)
        .ifBlank { "wamdx_project" }
      val zipFile = File(exportDir, "${safeSlug}.zip")
      FileOutputStream(zipFile).use { fos ->
        writeZipEntries(fos, projectName, html, css, js)
      }
      val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        zipFile,
      )
      val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/zip"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
      Result.success(Intent.createChooser(intent, projectName))
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  private fun writeZipEntries(
    outputStream: OutputStream,
    projectName: String,
    html: String,
    css: String,
    js: String,
  ) {
    val indexFileHtml = buildString {
      appendLine("<!DOCTYPE html>")
      appendLine("<html>")
      appendLine("<head>")
      appendLine("  <meta charset=\"UTF-8\">")
      appendLine("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
      appendLine("  <title>${projectName.replace("<", "&lt;")}</title>")
      appendLine("  <link rel=\"stylesheet\" href=\"styles.css\">")
      appendLine("</head>")
      appendLine("<body>")
      appendLine(
        if (html.contains("<body", ignoreCase = true)) {
          html.substringAfter("<body").substringAfter(">").substringBeforeLast("</body>")
        } else {
          html
        }
      )
      appendLine("  <script src=\"app.js\"></script>")
      appendLine("</body>")
      appendLine("</html>")
    }

    val readme = buildString {
      appendLine("# $projectName")
      appendLine()
      appendLine("Generated with **WamdX AI Studio**.")
      appendLine()
      appendLine("## How to Run")
      appendLine("1. Extract this ZIP archive.")
      appendLine("2. Open `index.html` in any modern web browser, or open `standalone.html` for a single-file bundle.")
    }

    val standalone = buildStandaloneHtml(projectName, html, css, js, desktopViewport = false)

    ZipOutputStream(outputStream).use { zip ->
      fun putText(name: String, text: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(text.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
      }
      putText("index.html", indexFileHtml)
      putText("styles.css", css)
      putText("app.js", js)
      putText("standalone.html", standalone)
      putText("README.md", readme)
    }
  }
}
