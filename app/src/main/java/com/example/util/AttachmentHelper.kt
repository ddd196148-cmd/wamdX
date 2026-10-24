package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PickedAttachment(
  val name: String,
  val mimeType: String,
  val textExtract: String? = null,
  val base64Data: String? = null,
  val sizeBytes: Long = 0L,
)

object AttachmentHelper {
  private val textExtensions = setOf(
    "txt", "md", "csv", "json", "xml", "html", "htm", "css", "js", "ts",
    "kt", "kts", "java", "py", "cpp", "c", "h", "sql", "yaml", "yml", "log"
  )

  suspend fun readAttachment(context: Context, uri: Uri): Result<PickedAttachment> =
    withContext(Dispatchers.IO) {
      try {
        val resolver = context.contentResolver
        var fileName = "attachment"
        var fileSize = 0L

        resolver.query(uri, null, null, null, null)?.use { cursor ->
          if (cursor.moveToFirst()) {
            val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (nameIdx >= 0) fileName = cursor.getString(nameIdx) ?: fileName
            if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) fileSize = cursor.getLong(sizeIdx)
          }
        }

        val rawMime = resolver.getType(uri) ?: guessMimeFromName(fileName)
        val ext = fileName.substringAfterLast('.', "").lowercase()

        when {
          rawMime.startsWith("image/") -> {
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
              ?: return@withContext Result.failure(IllegalArgumentException("Unable to read image"))
            val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
              ?: return@withContext Result.failure(IllegalArgumentException("Invalid image format"))
            val scaled = scaleBitmapDown(decoded, 1280)
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 82, out)
            val b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            Result.success(
              PickedAttachment(
                name = fileName,
                mimeType = "image/jpeg",
                base64Data = b64,
                sizeBytes = fileSize,
              )
            )
          }

          rawMime == "application/pdf" || ext == "pdf" -> {
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
              ?: return@withContext Result.failure(IllegalArgumentException("Unable to read PDF"))
            if (bytes.size > 6 * 1024 * 1024) {
              return@withContext Result.failure(
                IllegalArgumentException("PDF exceeds 6 MB mobile analysis limit")
              )
            }
            val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            Result.success(
              PickedAttachment(
                name = fileName,
                mimeType = "application/pdf",
                base64Data = b64,
                sizeBytes = bytes.size.toLong(),
              )
            )
          }

          rawMime.startsWith("text/") ||
            rawMime.contains("json") ||
            rawMime.contains("xml") ||
            rawMime.contains("javascript") ||
            ext in textExtensions -> {
            val rawText = resolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
              it.readText()
            } ?: return@withContext Result.failure(IllegalArgumentException("Unable to read text file"))
            val trimmed = if (rawText.length > 40_000) {
              rawText.take(40_000) + "\n...[Truncated at 40,000 chars]..."
            } else {
              rawText
            }
            Result.success(
              PickedAttachment(
                name = fileName,
                mimeType = "text/plain",
                textExtract = trimmed,
                sizeBytes = rawText.length.toLong(),
              )
            )
          }

          else -> Result.failure(
            IllegalArgumentException(
              "Unsupported file type ($rawMime). Please attach text, code, CSV, JSON, PDF, or an image."
            )
          )
        }
      } catch (e: Exception) {
        Result.failure(e)
      }
    }

  private fun scaleBitmapDown(bitmap: Bitmap, maxDim: Int): Bitmap {
    val w = bitmap.width
    val h = bitmap.height
    if (w <= maxDim && h <= maxDim) return bitmap
    val ratio = w.toFloat() / h.toFloat()
    val (newW, newH) = if (w >= h) {
      maxDim to (maxDim / ratio).toInt().coerceAtLeast(1)
    } else {
      (maxDim * ratio).toInt().coerceAtLeast(1) to maxDim
    }
    return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
  }

  private fun guessMimeFromName(name: String): String {
    return when (name.substringAfterLast('.', "").lowercase()) {
      "png" -> "image/png"
      "jpg", "jpeg" -> "image/jpeg"
      "webp" -> "image/webp"
      "pdf" -> "application/pdf"
      else -> "text/plain"
    }
  }
}
