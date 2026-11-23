package pl.edu.friends.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.edu.friends.data.*

class BestiesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BestiesRepository

    val profile: LiveData<FriendshipProfileEntity?>
    val allCapsules: LiveData<List<TimeCapsuleEntity>>
    val allMessages: LiveData<List<ChatMessageEntity>>
    val neverGameAnswers: LiveData<List<QuizAnswerEntity>>
    val allJournalEntries: LiveData<List<JournalEntryEntity>>

    init {
        val dao = AppDatabase.getDatabase(application).bestiesDao()
        repository = BestiesRepository(dao)
        profile = repository.profile
        allCapsules = repository.allCapsules
        allMessages = repository.allMessages
        neverGameAnswers = repository.neverGameAnswers
        allJournalEntries = repository.allJournalEntries
    }

    fun addQuickActionXp() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addXp(10)
        }
    }

    fun updateMoods(myMood: String, friendMood: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateMoods(myMood, friendMood)
        }
    }

    fun updateFriendshipStartDate(newStartDateMillis: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateStartDate(newStartDateMillis)
        }
    }

    fun addFriendToRoom(friendName: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val added = repository.addFriendToRoom(friendName)
            withContext(Dispatchers.Main) {
                onResult(added)
            }
        }
    }

    fun removeFriendFromRoom(friendName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.removeFriendFromRoom(friendName)
        }
    }

    fun checkInToday() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.checkInAllFriendsToday()
        }
    }

    fun addCapsule(title: String, message: String, author: String, daysUntilUnlock: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val unlockAt = now + (daysUntilUnlock.toLong() * 24L * 60L * 60L * 1000L)
            repository.insertCapsule(
                TimeCapsuleEntity(
                    title = title,
                    message = message,
                    author = author,
                    unlockAtMillis = unlockAt,
                    createdAtMillis = now
                )
            )
        }
    }

    fun deleteCapsule(capsule: TimeCapsuleEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCapsule(capsule)
        }
    }

    fun sendChatMessage(sender: String, text: String, hasPhoto: Boolean = false, photoCaption: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertMessage(
                ChatMessageEntity(
                    sender = sender,
                    text = text,
                    hasPhoto = hasPhoto,
                    photoCaption = photoCaption
                )
            )
        }
    }

    fun toggleLike(message: ChatMessageEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleMessageLike(message)
        }
    }

    fun submitGameAnswer(gameType: String, questionText: String, myAnswer: String, friendAnswer: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertQuizAnswer(
                QuizAnswerEntity(
                    gameType = gameType,
                    questionText = questionText,
                    myAnswer = myAnswer,
                    friendAnswer = friendAnswer
                )
            )
        }
    }

    fun addJournalEntry(
        dateLabel: String,
        questionText: String,
        myAnswer: String,
        friendAnswer: String,
        myMoodScore: Int,
        friendMoodScore: Int,
        sharedNote: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertJournalEntry(
                JournalEntryEntity(
                    dateLabel = dateLabel,
                    questionText = questionText,
                    myAnswer = myAnswer,
                    friendAnswer = friendAnswer,
                    myMoodScore = myMoodScore,
                    friendMoodScore = friendMoodScore,
                    sharedNote = sharedNote
                )
            )
        }
    }

    fun recordDailyMood(
        dateLabel: String,
        myMoodScore: Int,
        friendMoodScore: Int,
        note: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.upsertDailyMoodEntry(dateLabel, myMoodScore, friendMoodScore, note)
        }
    }

    fun deleteJournalEntry(entry: JournalEntryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteJournalEntry(entry)
        }
    }
}
