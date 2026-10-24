package com.example.data.local

data class ConversationEntity(
  val id: Long = 0,
  val title: String,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
  val providerId: String = "gemini",
  val modelId: String = "gemini-3.5-flash",
)

data class MessageEntity(
  val id: Long = 0,
  val conversationId: Long,
  val role: String, // "user" or "assistant"
  val content: String,
  val attachmentName: String? = null,
  val attachmentMimeType: String? = null,
  val timestamp: Long = System.currentTimeMillis(),
  val modelUsed: String? = null,
)

data class BuilderProjectEntity(
  val id: Long = 0,
  val name: String,
  val description: String,
  val htmlCode: String,
  val cssCode: String,
  val jsCode: String,
  val lastPrompt: String,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
)
