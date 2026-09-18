package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getUserProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getUserProfileSync(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfileEntity)

    @Update
    suspend fun updateProfile(profile: UserProfileEntity)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesForConversation(conversationId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC LIMIT 50")
    fun getRecentMessages(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages WHERE conversationId = :conversationId")
    suspend fun clearConversation(conversationId: String)
}

@Dao
interface GrammarCorrectionDao {
    @Query("SELECT * FROM grammar_corrections ORDER BY timestamp DESC")
    fun getAllCorrections(): Flow<List<GrammarCorrectionEntity>>

    @Query("SELECT * FROM grammar_corrections WHERE isMastered = 0 ORDER BY timestamp DESC")
    fun getPendingCorrections(): Flow<List<GrammarCorrectionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCorrection(correction: GrammarCorrectionEntity): Long

    @Query("UPDATE grammar_corrections SET isMastered = 1, practiceCount = practiceCount + 1 WHERE id = :id")
    suspend fun markAsMastered(id: Long)

    @Query("UPDATE grammar_corrections SET practiceCount = practiceCount + 1 WHERE id = :id")
    suspend fun incrementPractice(id: Long)

    @Query("SELECT COUNT(*) FROM grammar_corrections")
    fun getCorrectionsCount(): Flow<Int>
}

@Dao
interface VocabularyDao {
    @Query("SELECT * FROM vocabulary_items ORDER BY timestamp DESC")
    fun getAllVocabulary(): Flow<List<VocabularyEntity>>

    @Query("SELECT * FROM vocabulary_items WHERE category = :category ORDER BY timestamp DESC")
    fun getVocabularyByCategory(category: String): Flow<List<VocabularyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<VocabularyEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: VocabularyEntity): Long

    @Query("UPDATE vocabulary_items SET isMastered = :mastered WHERE id = :id")
    suspend fun setMastered(id: Long, mastered: Boolean)

    @Query("SELECT COUNT(*) FROM vocabulary_items WHERE isMastered = 1")
    fun getMasteredCount(): Flow<Int>
}

@Dao
interface DailyLessonDao {
    @Query("SELECT * FROM daily_lessons WHERE date = :date LIMIT 1")
    fun getLessonRecord(date: String): Flow<DailyLessonRecordEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateLesson(lesson: DailyLessonRecordEntity)

    @Query("SELECT * FROM daily_lessons ORDER BY timestamp DESC LIMIT 14")
    fun getRecentLessons(): Flow<List<DailyLessonRecordEntity>>
}
