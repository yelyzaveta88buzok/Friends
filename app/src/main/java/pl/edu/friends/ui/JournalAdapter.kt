package pl.edu.friends.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import pl.edu.friends.data.JournalEntryEntity
import pl.edu.friends.databinding.ItemJournalEntryBinding
import pl.edu.friends.util.BestiesLogicHelper

class JournalAdapter(
    private val onDeleteClick: (JournalEntryEntity) -> Unit
) : RecyclerView.Adapter<JournalAdapter.JournalViewHolder>() {

    private val items = mutableListOf<JournalEntryEntity>()

    fun submitList(newItems: List<JournalEntryEntity>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JournalViewHolder {
        val binding = ItemJournalEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return JournalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: JournalViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class JournalViewHolder(private val binding: ItemJournalEntryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: JournalEntryEntity) {
            val myEmoji = BestiesLogicHelper.moodEmoji(entry.myMoodScore)
            val myLabel = BestiesLogicHelper.moodLabel(entry.myMoodScore)
            val friendEmoji = BestiesLogicHelper.moodEmoji(entry.friendMoodScore)

            binding.tvAuthorAvatar.text = "✨"
            binding.tvJournalDate.text = "📅 ${entry.dateLabel}"
            binding.tvJournalQuestion.text = entry.questionText
            binding.tvMoodBadge.text = "$myEmoji $myLabel (${entry.myMoodScore}/5)"

            binding.tvMyAnswerAndMood.text = entry.myAnswer
            binding.tvFriendAnswerAndMood.text =
                "💬 Kacper ($friendEmoji ${entry.friendMoodScore}/5): ${entry.friendAnswer}"
            binding.tvSharedNote.text = "📌 ${entry.sharedNote}"

            binding.btnDeleteJournal.setOnClickListener {
                onDeleteClick(entry)
            }
        }
    }
}
