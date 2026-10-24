package com.example.data.remote

import com.example.BuildConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

enum class AiProviderType(
  val id: String,
  val displayNameAr: String,
  val displayNameEn: String,
  val rateLimitSummaryAr: String,
  val rateLimitSummaryEn: String,
) {
  GEMINI(
    id = "gemini",
    displayNameAr = "Google Gemini (المزوّد المجاني الافتراضي)",
    displayNameEn = "Google Gemini (Default Free Tier)",
    rateLimitSummaryAr = "طبقة مجانية سخية خاضعة لحدود الطلبات في الدقيقة واليوم (RPM/RPD). يدعم النصوص، الأكواد، الصور، وملفات PDF.",
    rateLimitSummaryEn = "Generous free tier subject to RPM/RPD quotas. Supports text, coding, images, and PDF files.",
  ),
  GROQ(
    id = "groq",
    displayNameAr = "Groq Cloud (نماذج مفتوحة المصدر سريعة)",
    displayNameEn = "Groq Cloud (Fast Open-Source Free Tier)",
    rateLimitSummaryAr = "طبقة مجانية للمطورين بحدود رموز وطلبات يومية للنماذج المفتوحة مثل Llama و Qwen.",
    rateLimitSummaryEn = "Free developer tier with per-minute token & daily request limits on open-source models.",
  ),
  OPENROUTER(
    id = "openrouter",
    displayNameAr = "OpenRouter (نماذج مجانية مفتوحة المصدر)",
    displayNameEn = "OpenRouter (Free Open-Source Models)",
    rateLimitSummaryAr = "يتيح الوصول إلى نماذج مجانية (:free) بحدود استخدام عادلة يحددها المزوّد.",
    rateLimitSummaryEn = "Provides access to community ':free' open-source models with fair-use rate limits.",
  );

  fun apiKey(): String = when (this) {
    GEMINI -> BuildConfig.GEMINI_API_KEY
    GROQ -> BuildConfig.GROQ_API_KEY
    OPENROUTER -> BuildConfig.OPENROUTER_API_KEY
  }

  fun isConfigured(): Boolean {
    val key = apiKey().trim()
    if (key.isEmpty() || key.equals("null", ignoreCase = true)) return false
    return when (this) {
      GEMINI -> key != "MY_GEMINI_API_KEY"
      GROQ -> key != "OPTIONAL_GROQ_API_KEY"
      OPENROUTER -> key != "OPTIONAL_OPENROUTER_API_KEY"
    }
  }

  companion object {
    fun fromId(id: String): AiProviderType = entries.firstOrNull { it.id == id } ?: GEMINI
  }
}

data class AiModelSpec(
  val id: String,
  val provider: AiProviderType,
  val nameAr: String,
  val nameEn: String,
  val badgeAr: String,
  val badgeEn: String,
  val supportsMultimodalBinary: Boolean,
)

object AiCatalog {
  val models: List<AiModelSpec> = listOf(
    AiModelSpec(
      id = "gemini-3.5-flash",
      provider = AiProviderType.GEMINI,
      nameAr = "Gemini 3.5 Flash — سريع ومتوازن للمحادثة والأدوات",
      nameEn = "Gemini 3.5 Flash — Fast & Versatile Everyday AI",
      badgeAr = "مجاني افتراضي",
      badgeEn = "Default Free",
      supportsMultimodalBinary = true,
    ),
    AiModelSpec(
      id = "gemini-3.1-pro-preview",
      provider = AiProviderType.GEMINI,
      nameAr = "Gemini 3.1 Pro — تفكير عميق وبرمجة وبناء تطبيقات",
      nameEn = "Gemini 3.1 Pro — Advanced Coding, Math & Builder",
      badgeAr = "دقة فائقة",
      badgeEn = "Pro Reasoning",
      supportsMultimodalBinary = true,
    ),
    AiModelSpec(
      id = "gemini-3.1-flash-lite-preview",
      provider = AiProviderType.GEMINI,
      nameAr = "Gemini 3.1 Flash Lite — خفيف جداً باستهلاك حصة أقل",
      nameEn = "Gemini 3.1 Flash Lite — Ultra-Light Quota Saver",
      badgeAr = "اقتصادي",
      badgeEn = "Lite",
      supportsMultimodalBinary = true,
    ),
    AiModelSpec(
      id = "llama-3.3-70b-versatile",
      provider = AiProviderType.GROQ,
      nameAr = "Llama 3.3 70B — نموذج مفتوح المصدر متعدد المهام",
      nameEn = "Llama 3.3 70B — Open-Source Versatile Model",
      badgeAr = "مفتوح المصدر",
      badgeEn = "Open Source",
      supportsMultimodalBinary = false,
    ),
    AiModelSpec(
      id = "qwen-qwq-32b",
      provider = AiProviderType.GROQ,
      nameAr = "Qwen QwQ 32B — استنتاج منطقي ورياضيات وبرمجة",
      nameEn = "Qwen QwQ 32B — Reasoning, STEM & Coding",
      badgeAr = "استنتاج",
      badgeEn = "Reasoning",
      supportsMultimodalBinary = false,
    ),
    AiModelSpec(
      id = "deepseek/deepseek-chat:free",
      provider = AiProviderType.OPENROUTER,
      nameAr = "DeepSeek V3 (Free) — محادثة وبرمجة مجانية",
      nameEn = "DeepSeek V3 (Free) — General & Coding Chat",
      badgeAr = "مجاني :free",
      badgeEn = "Free Tier",
      supportsMultimodalBinary = false,
    ),
    AiModelSpec(
      id = "google/gemma-3-27b-it:free",
      provider = AiProviderType.OPENROUTER,
      nameAr = "Gemma 3 27B (Free) — نموذج جوجل المفتوح",
      nameEn = "Gemma 3 27B (Free) — Open Weights Assistant",
      badgeAr = "مفتوح :free",
      badgeEn = "Open Weights",
      supportsMultimodalBinary = false,
    ),
  )

  fun modelsForProvider(provider: AiProviderType): List<AiModelSpec> =
    models.filter { it.provider == provider }

  fun findModel(modelId: String): AiModelSpec =
    models.firstOrNull { it.id == modelId } ?: models.first()
}

sealed interface ConnectionStatus {
  data object Unverified : ConnectionStatus
  data object Checking : ConnectionStatus
  data class Connected(val modelId: String, val latencyMs: Long) : ConnectionStatus
  data class MissingKey(val provider: AiProviderType) : ConnectionStatus
  data class RateLimited(val message: String, val fallbackProvider: AiProviderType?) : ConnectionStatus
  data class Failed(val reason: String) : ConnectionStatus
}

sealed interface AiOutcome<out T> {
  data class Success<T>(val data: T, val modelUsed: String, val latencyMs: Long) : AiOutcome<T>
  data class Error(
    val messageAr: String,
    val messageEn: String,
    val isRateLimit: Boolean = false,
    val suggestedFallback: AiProviderType? = null,
  ) : AiOutcome<Nothing>
}

// --- Gemini REST DTOs ---
@Serializable
data class GeminiGenerateRequest(
  val contents: List<GeminiContent>,
  val generationConfig: GeminiGenerationConfig? = null,
  val systemInstruction: GeminiContent? = null,
)

@Serializable
data class GeminiContent(
  val role: String? = null,
  val parts: List<GeminiPart>,
)

@Serializable
data class GeminiPart(
  val text: String? = null,
  val inlineData: GeminiInlineData? = null,
)

@Serializable
data class GeminiInlineData(
  val mimeType: String,
  val data: String,
)

@Serializable
data class GeminiGenerationConfig(
  val responseMimeType: String? = null,
  val responseSchema: JsonObject? = null,
  val temperature: Float? = null,
)

@Serializable
data class GeminiGenerateResponse(
  val candidates: List<GeminiCandidate>? = null,
)

@Serializable
data class GeminiCandidate(
  val content: GeminiContent? = null,
  val finishReason: String? = null,
)

// --- OpenAI-Compatible REST DTOs (Groq / OpenRouter) ---
@Serializable
data class OpenAiChatRequest(
  val model: String,
  val messages: List<OpenAiMessage>,
  val temperature: Double = 0.7,
)

@Serializable
data class OpenAiMessage(
  val role: String,
  val content: String,
)

@Serializable
data class OpenAiChatResponse(
  val choices: List<OpenAiChoice>? = null,
)

@Serializable
data class OpenAiChoice(
  val message: OpenAiMessage? = null,
)

// --- Structured App Builder Output ---
@Serializable
data class GeneratedWebAppPayload(
  @SerialName("projectName") val projectName: String = "WamdX Web App",
  @SerialName("summary") val summary: String = "",
  @SerialName("htmlCode") val htmlCode: String = "",
  @SerialName("cssCode") val cssCode: String = "",
  @SerialName("jsCode") val jsCode: String = "",
)
