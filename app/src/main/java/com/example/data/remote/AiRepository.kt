package com.example.data.remote

import com.example.data.local.MessageEntity
import com.example.util.PickedAttachment
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import retrofit2.HttpException

class AiRepository {

  private fun findConfiguredAlternative(current: AiProviderType): AiProviderType? =
    AiProviderType.entries.firstOrNull { it != current && it.isConfigured() }

  private fun buildSystemPrompt(
    memoryEnabled: Boolean,
    memoryNotes: String,
    extraRoleInstruction: String? = null,
  ): String = buildString {
    appendLine(
      "You are WamdX AI Studio (ومض إكس ستوديو الذكي), a helpful, knowledgeable, general-purpose conversational AI assistant."
    )
    appendLine(
      "Respond naturally in the same language the user writes in (defaulting to clear, fluent Modern Standard Arabic when the user writes in Arabic, or English/other languages when requested)."
    )
    appendLine(
      "You excel at everyday questions, teaching, step-by-step math and science, creative and academic writing, accurate translation, summarization, brainstorming, and clean programming."
    )
    appendLine(
      "Never force app-building unless the user asks for an app or website. Format code inside triple-backtick markdown blocks with language tags."
    )
    if (memoryEnabled && memoryNotes.isNotBlank()) {
      appendLine("\n[User-Provided Cross-Session Memory Preferences]:")
      appendLine(memoryNotes)
    }
    if (!extraRoleInstruction.isNullOrBlank()) {
      appendLine("\n[Task Context]:")
      appendLine(extraRoleInstruction)
    }
  }

  suspend fun verifyConnection(modelId: String): ConnectionStatus = withContext(Dispatchers.IO) {
    val spec = AiCatalog.findModel(modelId)
    val provider = spec.provider
    if (!provider.isConfigured()) {
      return@withContext ConnectionStatus.MissingKey(provider)
    }

    val start = System.currentTimeMillis()
    try {
      when (provider) {
        AiProviderType.GEMINI -> {
          val req = GeminiGenerateRequest(
            contents = listOf(
              GeminiContent(role = "user", parts = listOf(GeminiPart(text = "Reply with OK")))
            )
          )
          val res = AiNetworkClient.geminiService.generateContent(spec.id, provider.apiKey(), req)
          val txt = res.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
          if (txt.isNullOrBlank()) {
            ConnectionStatus.Failed("Empty response from ${spec.id}")
          } else {
            ConnectionStatus.Connected(spec.id, System.currentTimeMillis() - start)
          }
        }

        AiProviderType.GROQ -> {
          val req = OpenAiChatRequest(
            model = spec.id,
            messages = listOf(OpenAiMessage("user", "Reply with OK")),
          )
          val res = AiNetworkClient.groqService.chatCompletions("Bearer ${provider.apiKey()}", req)
          val txt = res.choices?.firstOrNull()?.message?.content
          if (txt.isNullOrBlank()) {
            ConnectionStatus.Failed("Empty response from Groq")
          } else {
            ConnectionStatus.Connected(spec.id, System.currentTimeMillis() - start)
          }
        }

        AiProviderType.OPENROUTER -> {
          val req = OpenAiChatRequest(
            model = spec.id,
            messages = listOf(OpenAiMessage("user", "Reply with OK")),
          )
          val res = AiNetworkClient.openRouterService.chatCompletions(
            "Bearer ${provider.apiKey()}",
            req,
          )
          val txt = res.choices?.firstOrNull()?.message?.content
          if (txt.isNullOrBlank()) {
            ConnectionStatus.Failed("Empty response from OpenRouter")
          } else {
            ConnectionStatus.Connected(spec.id, System.currentTimeMillis() - start)
          }
        }
      }
    } catch (e: HttpException) {
      if (e.code() == 429) {
        ConnectionStatus.RateLimited(
          message = "HTTP 429: Free-tier quota/rate limit reached",
          fallbackProvider = findConfiguredAlternative(provider),
        )
      } else {
        ConnectionStatus.Failed("HTTP ${e.code()}: ${e.message()}")
      }
    } catch (e: Exception) {
      ConnectionStatus.Failed(e.localizedMessage ?: "Network error")
    }
  }

  suspend fun sendChatTurn(
    modelId: String,
    history: List<MessageEntity>,
    userPrompt: String,
    attachment: PickedAttachment?,
    memoryEnabled: Boolean,
    memoryNotes: String,
    extraRoleInstruction: String? = null,
  ): AiOutcome<String> = withContext(Dispatchers.IO) {
    val spec = AiCatalog.findModel(modelId)
    val provider = spec.provider
    if (!provider.isConfigured()) {
      val keyName = when (provider) {
        AiProviderType.GEMINI -> "GEMINI_API_KEY"
        AiProviderType.GROQ -> "GROQ_API_KEY"
        AiProviderType.OPENROUTER -> "OPENROUTER_API_KEY"
      }
      return@withContext AiOutcome.Error(
        messageAr = "المفتاح $keyName غير مُعدّ بعد. يرجى إضافته في لوحة Secrets داخل AI Studio أو التبديل إلى مزوّد آخر مُعدّ من الإعدادات.",
        messageEn = "$keyName is not configured yet. Please set it in the AI Studio Secrets panel or switch to a configured provider in Settings.",
        suggestedFallback = findConfiguredAlternative(provider),
      )
    }

    if (attachment?.base64Data != null && !spec.supportsMultimodalBinary) {
      return@withContext AiOutcome.Error(
        messageAr = "النموذج المختار (${spec.id}) يدعم الملفات النصية والبرمجية فقط ولا يدعم الصور أو PDF. حوّل إلى Gemini 3.5 Flash أو Gemini 3.1 Pro لتحليل الصور وملفات PDF.",
        messageEn = "Selected model (${spec.id}) supports text/code files only, not binary images or PDFs. Switch to Gemini 3.5 Flash or Gemini 3.1 Pro for multimodal vision/PDF analysis.",
        suggestedFallback = if (AiProviderType.GEMINI.isConfigured()) AiProviderType.GEMINI else null,
      )
    }

    val systemPrompt = buildSystemPrompt(memoryEnabled, memoryNotes, extraRoleInstruction)
    val recentHistory = history.takeLast(14)
    val startMs = System.currentTimeMillis()

    try {
      val replyText = when (provider) {
        AiProviderType.GEMINI -> {
          val contents = mutableListOf<GeminiContent>()
          for (msg in recentHistory) {
            val geminiRole = if (msg.role == "assistant") "model" else "user"
            contents += GeminiContent(
              role = geminiRole,
              parts = listOf(GeminiPart(text = msg.content)),
            )
          }
          val currentParts = mutableListOf<GeminiPart>()
          val enrichedText = buildString {
            append(userPrompt)
            if (attachment?.textExtract != null) {
              append("\n\n--- Attached File: ${attachment.name} ---\n")
              append(attachment.textExtract)
            }
          }
          currentParts += GeminiPart(text = enrichedText)
          if (attachment?.base64Data != null) {
            currentParts += GeminiPart(
              inlineData = GeminiInlineData(
                mimeType = attachment.mimeType,
                data = attachment.base64Data,
              )
            )
          }
          contents += GeminiContent(role = "user", parts = currentParts)

          val req = GeminiGenerateRequest(
            contents = contents,
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemPrompt))),
            generationConfig = GeminiGenerationConfig(temperature = 0.7f),
          )
          val res = AiNetworkClient.geminiService.generateContent(spec.id, provider.apiKey(), req)
          res.candidates?.firstOrNull()?.content?.parts
            ?.mapNotNull { it.text }
            ?.joinToString("\n")
            ?.trim()
        }

        AiProviderType.GROQ, AiProviderType.OPENROUTER -> {
          val messages = mutableListOf<OpenAiMessage>()
          messages += OpenAiMessage(role = "system", content = systemPrompt)
          for (msg in recentHistory) {
            messages += OpenAiMessage(
              role = if (msg.role == "assistant") "assistant" else "user",
              content = msg.content,
            )
          }
          val enrichedText = buildString {
            append(userPrompt)
            if (attachment?.textExtract != null) {
              append("\n\n--- Attached File: ${attachment.name} ---\n")
              append(attachment.textExtract)
            }
          }
          messages += OpenAiMessage(role = "user", content = enrichedText)

          val req = OpenAiChatRequest(model = spec.id, messages = messages)
          val service = if (provider == AiProviderType.GROQ) {
            AiNetworkClient.groqService
          } else {
            AiNetworkClient.openRouterService
          }
          val res = service.chatCompletions("Bearer ${provider.apiKey()}", req)
          res.choices?.firstOrNull()?.message?.content?.trim()
        }
      }

      if (replyText.isNullOrBlank()) {
        AiOutcome.Error(
          messageAr = "لم يُرجع المزوّد نصاً صالحاً لهذه الرسالة. جرّب إعادة صياغة الطلب.",
          messageEn = "The AI provider returned an empty response. Please try rephrasing your prompt.",
        )
      } else {
        AiOutcome.Success(
          data = replyText,
          modelUsed = spec.id,
          latencyMs = System.currentTimeMillis() - startMs,
        )
      }
    } catch (e: Exception) {
      mapExceptionToOutcome(e, provider)
    }
  }

  suspend fun generateOrModifyWebApp(
    modelId: String,
    userInstruction: String,
    currentHtml: String,
    currentCss: String,
    currentJs: String,
    isArabicUi: Boolean,
  ): AiOutcome<GeneratedWebAppPayload> = withContext(Dispatchers.IO) {
    val spec = AiCatalog.findModel(modelId)
    val provider = spec.provider
    if (!provider.isConfigured()) {
      return@withContext AiOutcome.Error(
        messageAr = "يرجى إعداد مفتاح API للمزوّد المختار في لوحة Secrets أولاً لبناء التطبيق.",
        messageEn = "Please configure the API key for the selected provider in the Secrets panel first.",
        suggestedFallback = findConfiguredAlternative(provider),
      )
    }

    val hasExisting = currentHtml.isNotBlank() || currentCss.isNotBlank() || currentJs.isNotBlank()
    val builderSystemInstruction = buildString {
      appendLine("You are the WamdX AI App & Website Builder engine.")
      appendLine("Generate complete, self-contained, interactive, modern HTML, CSS, and vanilla JavaScript.")
      appendLine("Rules:")
      appendLine("1. htmlCode must contain ONLY the inner markup inside <body> (no <html> or <head> wrapper needed, though clean semantic tags are required).")
      appendLine("2. cssCode must contain complete responsive styling with attractive modern aesthetics, accessible contrast, and smooth layout.")
      appendLine("3. jsCode must contain working, bug-free vanilla JavaScript event listeners and logic without external build steps.")
      appendLine("4. If the user writes in Arabic or requests an Arabic app, set dir=\"rtl\" on the root container and localize labels into natural Arabic while keeping code syntax clean.")
      appendLine("5. When modifying an existing project, PRESERVE all existing working features and state unless explicitly asked to replace them.")
      appendLine("6. Return strictly valid JSON with keys: projectName, summary, htmlCode, cssCode, jsCode.")
    }

    val fullPrompt = buildString {
      if (hasExisting) {
        appendLine("Modify the existing web application according to this follow-up instruction while preserving existing working features:")
        appendLine("Instruction: $userInstruction")
        appendLine("\n--- CURRENT HTML ---\n$currentHtml")
        appendLine("\n--- CURRENT CSS ---\n$currentCss")
        appendLine("\n--- CURRENT JS ---\n$currentJs")
      } else {
        appendLine("Build a complete interactive web application or website for:")
        appendLine(userInstruction)
        appendLine(if (isArabicUi) "Write `summary` in Arabic." else "Write `summary` in English.")
      }
    }

    val startMs = System.currentTimeMillis()
    try {
      val rawJson = when (provider) {
        AiProviderType.GEMINI -> {
          val schema = buildJsonObject {
            put("type", "OBJECT")
            putJsonObject("properties") {
              putJsonObject("projectName") { put("type", "STRING") }
              putJsonObject("summary") { put("type", "STRING") }
              putJsonObject("htmlCode") { put("type", "STRING") }
              putJsonObject("cssCode") { put("type", "STRING") }
              putJsonObject("jsCode") { put("type", "STRING") }
            }
            putJsonArray("required") {
              add(kotlinx.serialization.json.JsonPrimitive("projectName"))
              add(kotlinx.serialization.json.JsonPrimitive("summary"))
              add(kotlinx.serialization.json.JsonPrimitive("htmlCode"))
              add(kotlinx.serialization.json.JsonPrimitive("cssCode"))
              add(kotlinx.serialization.json.JsonPrimitive("jsCode"))
            }
          }
          val req = GeminiGenerateRequest(
            contents = listOf(GeminiContent(role = "user", parts = listOf(GeminiPart(text = fullPrompt)))),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = builderSystemInstruction))),
            generationConfig = GeminiGenerationConfig(
              responseMimeType = "application/json",
              responseSchema = schema,
              temperature = 0.4f,
            ),
          )
          val res = AiNetworkClient.geminiService.generateContent(spec.id, provider.apiKey(), req)
          res.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
        }

        AiProviderType.GROQ, AiProviderType.OPENROUTER -> {
          val req = OpenAiChatRequest(
            model = spec.id,
            messages = listOf(
              OpenAiMessage("system", "$builderSystemInstruction\nRespond ONLY with a raw JSON object."),
              OpenAiMessage("user", fullPrompt),
            ),
            temperature = 0.4,
          )
          val svc = if (provider == AiProviderType.GROQ) {
            AiNetworkClient.groqService
          } else {
            AiNetworkClient.openRouterService
          }
          val res = svc.chatCompletions("Bearer ${provider.apiKey()}", req)
          res.choices?.firstOrNull()?.message?.content
        }
      }

      if (rawJson.isNullOrBlank()) {
        return@withContext AiOutcome.Error(
          messageAr = "لم يتم استلام كود صالح من النموذج. حاول مرة أخرى.",
          messageEn = "No valid application payload received from the model. Please retry.",
        )
      }

      val cleanedJson = extractJsonObject(rawJson)
      val parsed = AiNetworkClient.json.decodeFromString<GeneratedWebAppPayload>(cleanedJson)
      if (parsed.htmlCode.isBlank() && parsed.jsCode.isBlank()) {
        return@withContext AiOutcome.Error(
          messageAr = "أرجع النموذج مشروعاً فارغاً. حاول توضيح فكرة التطبيق أكثر.",
          messageEn = "The model returned empty code fields. Try describing the desired features in more detail.",
        )
      }

      AiOutcome.Success(
        data = parsed,
        modelUsed = spec.id,
        latencyMs = System.currentTimeMillis() - startMs,
      )
    } catch (e: Exception) {
      mapExceptionToOutcome(e, provider)
    }
  }

  private fun extractJsonObject(raw: String): String {
    val trimmed = raw.trim()
      .removePrefix("```json")
      .removePrefix("```")
      .removeSuffix("```")
      .trim()
    val firstBrace = trimmed.indexOf('{')
    val lastBrace = trimmed.lastIndexOf('}')
    return if (firstBrace >= 0 && lastBrace > firstBrace) {
      trimmed.substring(firstBrace, lastBrace + 1)
    } else {
      trimmed
    }
  }

  private fun mapExceptionToOutcome(
    e: Exception,
    currentProvider: AiProviderType,
  ): AiOutcome.Error {
    val fallback = findConfiguredAlternative(currentProvider)
    return when (e) {
      is HttpException -> {
        when (e.code()) {
          429 -> AiOutcome.Error(
            messageAr = "تم استنفاد حد الطلبات المجاني المؤقت (HTTP 429) لدى المزوّد الحالي.${if (fallback != null) " يمكنك التبديل فوراً إلى ${fallback.displayNameAr} من الإعدادات." else " انتظر قليلاً ثم أعد المحاولة."}",
            messageEn = "Free-tier rate limit or quota reached (HTTP 429).${if (fallback != null) " Switch to ${fallback.displayNameEn} in Settings or retry shortly." else " Please wait a moment and try again."}",
            isRateLimit = true,
            suggestedFallback = fallback,
          )
          400, 401, 403 -> AiOutcome.Error(
            messageAr = "رفض الخادم الطلب (HTTP ${e.code()}). تحقق من صلاحية مفتاح API في لوحة Secrets أو جرّب نموذجاً آخر.",
            messageEn = "Provider rejected the request (HTTP ${e.code()}). Verify your API key in the AI Studio Secrets panel or select another model.",
            suggestedFallback = fallback,
          )
          else -> AiOutcome.Error(
            messageAr = "خطأ من خادم المزوّد (HTTP ${e.code()}). يرجى المحاولة مرة أخرى.",
            messageEn = "Provider server error (HTTP ${e.code()}). Please try again.",
            suggestedFallback = fallback,
          )
        }
      }
      is SocketTimeoutException -> AiOutcome.Error(
        messageAr = "انتهت مهلة الاتصال بالخادم (Timeout). قد يكون الطلب طويلاً؛ جرّب نموذجاً أسرع مثل Gemini 3.5 Flash أو أعد المحاولة.",
        messageEn = "Request timed out. Try a faster model like Gemini 3.5 Flash or retry.",
      )
      is UnknownHostException -> AiOutcome.Error(
        messageAr = "لا يوجد اتصال بالإنترنت. تحقق من الشبكة ثم أعد المحاولة.",
        messageEn = "No internet connection detected. Check your network and try again.",
      )
      else -> AiOutcome.Error(
        messageAr = "تعذّر إكمال الطلب: ${e.localizedMessage ?: "خطأ غير معروف"}",
        messageEn = "Request failed: ${e.localizedMessage ?: "Unexpected error"}",
      )
    }
  }
}
