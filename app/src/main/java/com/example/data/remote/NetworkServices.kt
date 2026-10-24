package com.example.data.remote

import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface GeminiRestService {
  @POST("v1beta/models/{model}:generateContent")
  suspend fun generateContent(
    @Path("model") model: String,
    @Query("key") apiKey: String,
    @Body request: GeminiGenerateRequest,
  ): GeminiGenerateResponse
}

interface OpenAiCompatibleService {
  @POST("chat/completions")
  suspend fun chatCompletions(
    @Header("Authorization") bearerToken: String,
    @Body request: OpenAiChatRequest,
  ): OpenAiChatResponse
}

object AiNetworkClient {
  val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = false
    explicitNulls = false
  }

  private val okHttpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
      .connectTimeout(60, TimeUnit.SECONDS)
      .readTimeout(60, TimeUnit.SECONDS)
      .writeTimeout(60, TimeUnit.SECONDS)
      .build()
  }

  val geminiService: GeminiRestService by lazy {
    Retrofit.Builder()
      .baseUrl("https://generativelanguage.googleapis.com/")
      .client(okHttpClient)
      .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
      .build()
      .create(GeminiRestService::class.java)
  }

  val groqService: OpenAiCompatibleService by lazy {
    Retrofit.Builder()
      .baseUrl("https://api.groq.com/openai/v1/")
      .client(okHttpClient)
      .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
      .build()
      .create(OpenAiCompatibleService::class.java)
  }

  val openRouterService: OpenAiCompatibleService by lazy {
    Retrofit.Builder()
      .baseUrl("https://openrouter.ai/api/v1/")
      .client(okHttpClient)
      .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
      .build()
      .create(OpenAiCompatibleService::class.java)
  }
}
