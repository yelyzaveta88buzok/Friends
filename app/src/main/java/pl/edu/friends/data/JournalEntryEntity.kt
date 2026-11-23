package pl.edu.friends.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "journal_entries")
data class JournalEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dateLabel: String,
    val questionText: String,
    val myAnswer: String,
    val friendAnswer: String,
    val myMoodScore: Int,
    val friendMoodScore: Int,
    val sharedNote: String,
    val timestampMillis: Long = System.currentTimeMillis()
)
