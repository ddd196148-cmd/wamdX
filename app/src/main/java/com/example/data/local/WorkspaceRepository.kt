package com.example.data.local

import kotlinx.coroutines.flow.Flow

class WorkspaceRepository(
  private val chatDao: ChatDao,
  private val projectDao: BuilderProjectDao,
) {
  val conversations: Flow<List<ConversationEntity>> = chatDao.observeConversations()
  val projects: Flow<List<BuilderProjectEntity>> = projectDao.observeProjects()

  fun observeMessages(conversationId: Long): Flow<List<MessageEntity>> =
    chatDao.observeMessages(conversationId)

  suspend fun getMessagesOnce(conversationId: Long): List<MessageEntity> =
    chatDao.getMessagesOnce(conversationId)

  suspend fun createConversation(
    title: String,
    providerId: String,
    modelId: String,
  ): Long {
    val now = System.currentTimeMillis()
    return chatDao.insertConversation(
      ConversationEntity(
        title = title,
        createdAt = now,
        updatedAt = now,
        providerId = providerId,
        modelId = modelId,
      )
    )
  }

  suspend fun touchConversation(conversationId: Long, newTitle: String? = null) {
    val existing = chatDao.getConversationById(conversationId) ?: return
    chatDao.updateConversation(
      existing.copy(
        title = newTitle ?: existing.title,
        updatedAt = System.currentTimeMillis(),
      )
    )
  }

  suspend fun addMessage(
    conversationId: Long,
    role: String,
    content: String,
    attachmentName: String? = null,
    attachmentMimeType: String? = null,
    modelUsed: String? = null,
  ): Long {
    val msgId = chatDao.insertMessage(
      MessageEntity(
        conversationId = conversationId,
        role = role,
        content = content,
        attachmentName = attachmentName,
        attachmentMimeType = attachmentMimeType,
        modelUsed = modelUsed,
      )
    )
    touchConversation(conversationId)
    return msgId
  }

  suspend fun deleteConversation(id: Long) = chatDao.deleteConversation(id)

  suspend fun deleteAllConversations() = chatDao.deleteAllConversations()

  suspend fun saveProject(project: BuilderProjectEntity): Long =
    projectDao.upsertProject(project.copy(updatedAt = System.currentTimeMillis()))

  suspend fun getProjectById(id: Long): BuilderProjectEntity? = projectDao.getProjectById(id)

  suspend fun deleteProject(id: Long) = projectDao.deleteProject(id)
}
