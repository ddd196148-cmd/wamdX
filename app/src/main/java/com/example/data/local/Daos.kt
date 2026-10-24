package com.example.data.local

import kotlinx.coroutines.flow.Flow

interface ChatDao {
  fun observeConversations(): Flow<List<ConversationEntity>>
  suspend fun getConversationById(id: Long): ConversationEntity?
  suspend fun insertConversation(conversation: ConversationEntity): Long
  suspend fun updateConversation(conversation: ConversationEntity)
  suspend fun deleteConversation(id: Long)
  suspend fun deleteAllConversations()
  fun observeMessages(conversationId: Long): Flow<List<MessageEntity>>
  suspend fun getMessagesOnce(conversationId: Long): List<MessageEntity>
  suspend fun insertMessage(message: MessageEntity): Long
}

interface BuilderProjectDao {
  fun observeProjects(): Flow<List<BuilderProjectEntity>>
  suspend fun getProjectById(id: Long): BuilderProjectEntity?
  suspend fun upsertProject(project: BuilderProjectEntity): Long
  suspend fun deleteProject(id: Long)
}
