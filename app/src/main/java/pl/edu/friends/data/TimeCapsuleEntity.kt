package pl.edu.friends.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "time_capsules")
data class TimeCapsuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val message: String,
    val author: String = "Ty",
    val unlockAtMillis: Long,
    val createdAtMillis: Long = System.currentTimeMillis()
)
