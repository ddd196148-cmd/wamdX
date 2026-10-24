package com.example.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext

class WamdXDatabase private constructor(context: Context) :
  SQLiteOpenHelper(context.applicationContext, "wamdx_studio.db", null, 1) {

  private val chatInvalidation = MutableSharedFlow<Unit>(extraBufferCapacity = 16)
  private val projectInvalidation = MutableSharedFlow<Unit>(extraBufferCapacity = 16)

  override fun onConfigure(db: SQLiteDatabase) {
    super.onConfigure(db)
    db.setForeignKeyConstraintsEnabled(true)
  }

  override fun onCreate(db: SQLiteDatabase) {
    db.execSQL(
      """
      CREATE TABLE IF NOT EXISTS conversations (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        title TEXT NOT NULL,
        createdAt INTEGER NOT NULL,
        updatedAt INTEGER NOT NULL,
        providerId TEXT NOT NULL,
        modelId TEXT NOT NULL
      )
      """.trimIndent()
    )

    db.execSQL(
      """
      CREATE TABLE IF NOT EXISTS messages (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        conversationId INTEGER NOT NULL,
        role TEXT NOT NULL,
        content TEXT NOT NULL,
        attachmentName TEXT,
        attachmentMimeType TEXT,
        timestamp INTEGER NOT NULL,
        modelUsed TEXT,
        FOREIGN KEY(conversationId) REFERENCES conversations(id) ON DELETE CASCADE
      )
      """.trimIndent()
    )

    db.execSQL("CREATE INDEX IF NOT EXISTS index_messages_conversationId ON messages(conversationId)")

    db.execSQL(
      """
      CREATE TABLE IF NOT EXISTS builder_projects (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        name TEXT NOT NULL,
        description TEXT NOT NULL,
        htmlCode TEXT NOT NULL,
        cssCode TEXT NOT NULL,
        jsCode TEXT NOT NULL,
        lastPrompt TEXT NOT NULL,
        createdAt INTEGER NOT NULL,
        updatedAt INTEGER NOT NULL
      )
      """.trimIndent()
    )
  }

  override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    db.execSQL("DROP TABLE IF EXISTS messages")
    db.execSQL("DROP TABLE IF EXISTS conversations")
    db.execSQL("DROP TABLE IF EXISTS builder_projects")
    onCreate(db)
  }

  fun chatDao(): ChatDao = chatDaoImpl
  fun builderProjectDao(): BuilderProjectDao = builderProjectDaoImpl

  private val chatDaoImpl = object : ChatDao {
    override fun observeConversations(): Flow<List<ConversationEntity>> =
      chatInvalidation
        .onStart { emit(Unit) }
        .map { queryConversationsSync() }
        .flowOn(Dispatchers.IO)

    override suspend fun getConversationById(id: Long): ConversationEntity? =
      withContext(Dispatchers.IO) {
        readableDatabase.rawQuery(
          "SELECT id, title, createdAt, updatedAt, providerId, modelId FROM conversations WHERE id = ? LIMIT 1",
          arrayOf(id.toString()),
        ).use { cursor ->
          if (cursor.moveToFirst()) cursor.toConversation() else null
        }
      }

    override suspend fun insertConversation(conversation: ConversationEntity): Long =
      withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
          if (conversation.id > 0L) put("id", conversation.id)
          put("title", conversation.title)
          put("createdAt", conversation.createdAt)
          put("updatedAt", conversation.updatedAt)
          put("providerId", conversation.providerId)
          put("modelId", conversation.modelId)
        }
        val rowId = writableDatabase.insertWithOnConflict(
          "conversations",
          null,
          values,
          SQLiteDatabase.CONFLICT_REPLACE,
        )
        chatInvalidation.tryEmit(Unit)
        rowId
      }

    override suspend fun updateConversation(conversation: ConversationEntity) =
      withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
          put("title", conversation.title)
          put("createdAt", conversation.createdAt)
          put("updatedAt", conversation.updatedAt)
          put("providerId", conversation.providerId)
          put("modelId", conversation.modelId)
        }
        writableDatabase.update(
          "conversations",
          values,
          "id = ?",
          arrayOf(conversation.id.toString()),
        )
        chatInvalidation.tryEmit(Unit)
        Unit
      }

    override suspend fun deleteConversation(id: Long) = withContext(Dispatchers.IO) {
      writableDatabase.delete("conversations", "id = ?", arrayOf(id.toString()))
      chatInvalidation.tryEmit(Unit)
      Unit
    }

    override suspend fun deleteAllConversations() = withContext(Dispatchers.IO) {
      writableDatabase.delete("conversations", null, null)
      chatInvalidation.tryEmit(Unit)
      Unit
    }

    override fun observeMessages(conversationId: Long): Flow<List<MessageEntity>> =
      chatInvalidation
        .onStart { emit(Unit) }
        .map { queryMessagesSync(conversationId) }
        .flowOn(Dispatchers.IO)

    override suspend fun getMessagesOnce(conversationId: Long): List<MessageEntity> =
      withContext(Dispatchers.IO) { queryMessagesSync(conversationId) }

    override suspend fun insertMessage(message: MessageEntity): Long =
      withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
          if (message.id > 0L) put("id", message.id)
          put("conversationId", message.conversationId)
          put("role", message.role)
          put("content", message.content)
          put("attachmentName", message.attachmentName)
          put("attachmentMimeType", message.attachmentMimeType)
          put("timestamp", message.timestamp)
          put("modelUsed", message.modelUsed)
        }
        val rowId = writableDatabase.insertWithOnConflict(
          "messages",
          null,
          values,
          SQLiteDatabase.CONFLICT_REPLACE,
        )
        chatInvalidation.tryEmit(Unit)
        rowId
      }
  }

  private val builderProjectDaoImpl = object : BuilderProjectDao {
    override fun observeProjects(): Flow<List<BuilderProjectEntity>> =
      projectInvalidation
        .onStart { emit(Unit) }
        .map { queryProjectsSync() }
        .flowOn(Dispatchers.IO)

    override suspend fun getProjectById(id: Long): BuilderProjectEntity? =
      withContext(Dispatchers.IO) {
        readableDatabase.rawQuery(
          "SELECT id, name, description, htmlCode, cssCode, jsCode, lastPrompt, createdAt, updatedAt FROM builder_projects WHERE id = ? LIMIT 1",
          arrayOf(id.toString()),
        ).use { cursor ->
          if (cursor.moveToFirst()) cursor.toProject() else null
        }
      }

    override suspend fun upsertProject(project: BuilderProjectEntity): Long =
      withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
          if (project.id > 0L) put("id", project.id)
          put("name", project.name)
          put("description", project.description)
          put("htmlCode", project.htmlCode)
          put("cssCode", project.cssCode)
          put("jsCode", project.jsCode)
          put("lastPrompt", project.lastPrompt)
          put("createdAt", project.createdAt)
          put("updatedAt", project.updatedAt)
        }
        val rowId = writableDatabase.insertWithOnConflict(
          "builder_projects",
          null,
          values,
          SQLiteDatabase.CONFLICT_REPLACE,
        )
        projectInvalidation.tryEmit(Unit)
        rowId
      }

    override suspend fun deleteProject(id: Long) = withContext(Dispatchers.IO) {
      writableDatabase.delete("builder_projects", "id = ?", arrayOf(id.toString()))
      projectInvalidation.tryEmit(Unit)
      Unit
    }
  }

  private fun queryConversationsSync(): List<ConversationEntity> {
    val list = mutableListOf<ConversationEntity>()
    readableDatabase.rawQuery(
      "SELECT id, title, createdAt, updatedAt, providerId, modelId FROM conversations ORDER BY updatedAt DESC",
      null,
    ).use { cursor ->
      while (cursor.moveToNext()) {
        list += cursor.toConversation()
      }
    }
    return list
  }

  private fun queryMessagesSync(conversationId: Long): List<MessageEntity> {
    val list = mutableListOf<MessageEntity>()
    readableDatabase.rawQuery(
      "SELECT id, conversationId, role, content, attachmentName, attachmentMimeType, timestamp, modelUsed FROM messages WHERE conversationId = ? ORDER BY timestamp ASC, id ASC",
      arrayOf(conversationId.toString()),
    ).use { cursor ->
      while (cursor.moveToNext()) {
        list += cursor.toMessage()
      }
    }
    return list
  }

  private fun queryProjectsSync(): List<BuilderProjectEntity> {
    val list = mutableListOf<BuilderProjectEntity>()
    readableDatabase.rawQuery(
      "SELECT id, name, description, htmlCode, cssCode, jsCode, lastPrompt, createdAt, updatedAt FROM builder_projects ORDER BY updatedAt DESC",
      null,
    ).use { cursor ->
      while (cursor.moveToNext()) {
        list += cursor.toProject()
      }
    }
    return list
  }

  private fun Cursor.toConversation() = ConversationEntity(
    id = getLong(0),
    title = getString(1),
    createdAt = getLong(2),
    updatedAt = getLong(3),
    providerId = getString(4),
    modelId = getString(5),
  )

  private fun Cursor.toMessage() = MessageEntity(
    id = getLong(0),
    conversationId = getLong(1),
    role = getString(2),
    content = getString(3),
    attachmentName = if (isNull(4)) null else getString(4),
    attachmentMimeType = if (isNull(5)) null else getString(5),
    timestamp = getLong(6),
    modelUsed = if (isNull(7)) null else getString(7),
  )

  private fun Cursor.toProject() = BuilderProjectEntity(
    id = getLong(0),
    name = getString(1),
    description = getString(2),
    htmlCode = getString(3),
    cssCode = getString(4),
    jsCode = getString(5),
    lastPrompt = getString(6),
    createdAt = getLong(7),
    updatedAt = getLong(8),
  )

  companion object {
    @Volatile private var INSTANCE: WamdXDatabase? = null

    fun getInstance(context: Context): WamdXDatabase {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: WamdXDatabase(context).also { INSTANCE = it }
      }
    }
  }
}
