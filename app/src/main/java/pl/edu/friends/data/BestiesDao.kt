package pl.edu.friends.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface BestiesDao {

    @Query("SELECT * FROM friendship_profile WHERE id = 1 LIMIT 1")
    fun getProfile(): LiveData<FriendshipProfileEntity?>

    @Query("SELECT * FROM friendship_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfileSync(): FriendshipProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: FriendshipProfileEntity)

    @Query("SELECT * FROM time_capsules ORDER BY unlockAtMillis ASC")
    fun getAllCapsules(): LiveData<List<TimeCapsuleEntity>>

    @Query("SELECT * FROM time_capsules ORDER BY unlockAtMillis ASC")
    suspend fun getAllCapsulesSync(): List<TimeCapsuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCapsule(capsule: TimeCapsuleEntity): Long

    @Delete
    suspend fun deleteCapsule(capsule: TimeCapsuleEntity)

    @Query("SELECT * FROM chat_messages ORDER BY timestampMillis ASC")
    fun getAllMessages(): LiveData<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Update
    suspend fun updateMessage(message: ChatMessageEntity)

    @Query("SELECT * FROM quiz_answers WHERE gameType = :gameType ORDER BY timestampMillis DESC")
    fun getQuizAnswersByType(gameType: String): LiveData<List<QuizAnswerEntity>>

    @Query("SELECT * FROM quiz_answers WHERE gameType = 'NEVER' ORDER BY timestampMillis DESC")
    fun getNeverGameAnswers(): LiveData<List<QuizAnswerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizAnswer(answer: QuizAnswerEntity): Long

    @Query("SELECT * FROM journal_entries ORDER BY timestampMillis DESC")
    fun getAllJournalEntries(): LiveData<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE dateLabel = :dateLabel LIMIT 1")
    suspend fun getJournalEntryByDate(dateLabel: String): JournalEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournalEntry(entry: JournalEntryEntity): Long

    @Delete
    suspend fun deleteJournalEntry(entry: JournalEntryEntity)
}
