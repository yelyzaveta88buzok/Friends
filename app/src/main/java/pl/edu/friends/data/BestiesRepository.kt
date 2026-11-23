package pl.edu.friends.data

import androidx.lifecycle.LiveData

class BestiesRepository(private val dao: BestiesDao) {

    companion object {
        const val MAX_FRIENDS_PER_CODE = 5
    }

    val profile: LiveData<FriendshipProfileEntity?> = dao.getProfile()
    val allCapsules: LiveData<List<TimeCapsuleEntity>> = dao.getAllCapsules()
    val allMessages: LiveData<List<ChatMessageEntity>> = dao.getAllMessages()
    val neverGameAnswers: LiveData<List<QuizAnswerEntity>> = dao.getNeverGameAnswers()
    val allJournalEntries: LiveData<List<JournalEntryEntity>> = dao.getAllJournalEntries()

    suspend fun getProfileSync(): FriendshipProfileEntity? = dao.getProfileSync()

    suspend fun ensureProfileExists() {
        if (dao.getProfileSync() == null) {
            dao.saveProfile(
                FriendshipProfileEntity(
                    id = 1,
                    startDateMillis = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun saveProfile(profile: FriendshipProfileEntity) {
        dao.saveProfile(profile)
    }

    suspend fun addXp(points: Int) {
        val current = dao.getProfileSync()
        if (current != null) {
            dao.saveProfile(current.copy(xpPoints = current.xpPoints + points))
        }
    }

    suspend fun updateMoods(myMood: String, friendMood: String) {
        val current = dao.getProfileSync()
        if (current != null) {
            dao.saveProfile(current.copy(myMood = myMood, friendMood = friendMood))
        }
    }

    suspend fun updateStartDate(newStartDateMillis: Long) {
        val current = dao.getProfileSync()
        if (current != null) {
            dao.saveProfile(current.copy(startDateMillis = newStartDateMillis))
        }
    }

    suspend fun addFriendToRoom(friendName: String): Boolean {
        val current = dao.getProfileSync() ?: return false
        val members = current.membersCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
        if (members.size >= MAX_FRIENDS_PER_CODE) {
            return false
        }
        if (members.none { it.equals(friendName, ignoreCase = true) }) {
            members.add(friendName)
            val checkedIn = current.checkedInMembersCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
            checkedIn.add(friendName)
            dao.saveProfile(
                current.copy(
                    membersCsv = members.joinToString(","),
                    checkedInMembersCsv = checkedIn.distinct().joinToString(","),
                    xpPoints = current.xpPoints + 25
                )
            )
        }
        return true
    }

    suspend fun removeFriendFromRoom(friendName: String) {
        val current = dao.getProfileSync() ?: return
        val members = current.membersCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() && !it.equals(friendName, ignoreCase = true) }
        val checkedIn = current.checkedInMembersCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() && !it.equals(friendName, ignoreCase = true) }
        dao.saveProfile(
            current.copy(
                membersCsv = members.joinToString(","),
                checkedInMembersCsv = checkedIn.joinToString(",")
            )
        )
    }

    suspend fun checkInAllFriendsToday() {
        val current = dao.getProfileSync() ?: return
        val members = current.membersCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val alreadyAll = members.isNotEmpty() && current.checkedInMembersCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.size >= members.size
        val nextStreak = if (alreadyAll) current.streakDays else current.streakDays + 1
        dao.saveProfile(
            current.copy(
                checkedInMembersCsv = members.joinToString(","),
                streakDays = nextStreak,
                xpPoints = current.xpPoints + 15
            )
        )
    }

    suspend fun insertCapsule(capsule: TimeCapsuleEntity) {
        dao.insertCapsule(capsule)
        addXp(30)
    }

    suspend fun deleteCapsule(capsule: TimeCapsuleEntity) {
        dao.deleteCapsule(capsule)
    }

    suspend fun insertMessage(message: ChatMessageEntity) {
        dao.insertMessage(message)
    }

    suspend fun toggleMessageLike(message: ChatMessageEntity) {
        dao.updateMessage(message.copy(isLiked = !message.isLiked))
    }

    suspend fun insertQuizAnswer(answer: QuizAnswerEntity) {
        dao.insertQuizAnswer(answer)
        addXp(20)
    }

    suspend fun insertJournalEntry(entry: JournalEntryEntity) {
        dao.insertJournalEntry(entry)
        addXp(30)
    }

    suspend fun upsertDailyMoodEntry(
        dateLabel: String,
        myMoodScore: Int,
        friendMoodScore: Int,
        note: String
    ) {
        val existing = dao.getJournalEntryByDate(dateLabel)
        if (existing != null) {
            dao.insertJournalEntry(
                existing.copy(
                    myMoodScore = myMoodScore,
                    friendMoodScore = friendMoodScore,
                    myAnswer = "Mood $myMoodScore/5",
                    friendAnswer = "Mood $friendMoodScore/5",
                    sharedNote = note,
                    timestampMillis = System.currentTimeMillis()
                )
            )
            addXp(10)
        } else {
            dao.insertJournalEntry(
                JournalEntryEntity(
                    dateLabel = dateLabel,
                    questionText = "Daily Mood",
                    myAnswer = "Mood $myMoodScore/5",
                    friendAnswer = "Mood $friendMoodScore/5",
                    myMoodScore = myMoodScore,
                    friendMoodScore = friendMoodScore,
                    sharedNote = note
                )
            )
            addXp(15)
        }
    }

    suspend fun deleteJournalEntry(entry: JournalEntryEntity) {
        dao.deleteJournalEntry(entry)
    }
}
