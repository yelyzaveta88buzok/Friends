package pl.edu.friends.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import pl.edu.friends.data.TimeCapsuleEntity
import pl.edu.friends.databinding.ItemCapsuleBinding
import pl.edu.friends.util.AppLocale
import pl.edu.friends.util.AppTheme
import pl.edu.friends.util.BestiesLogicHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

class CapsuleAdapter(
    private val onCapsuleClick: (TimeCapsuleEntity, Boolean) -> Unit,
    private val onDeleteClick: (TimeCapsuleEntity) -> Unit
) : RecyclerView.Adapter<CapsuleAdapter.CapsuleViewHolder>() {

    private val items = mutableListOf<TimeCapsuleEntity>()
    private val dateFmt = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    @Suppress("NotifyDataSetChanged")
    fun submitList(newItems: List<TimeCapsuleEntity>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CapsuleViewHolder {
        val binding = ItemCapsuleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CapsuleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CapsuleViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class CapsuleViewHolder(private val binding: ItemCapsuleBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(capsule: TimeCapsuleEntity) {
            val now = System.currentTimeMillis()
            val unlocked = BestiesLogicHelper.isCapsuleUnlocked(capsule, now)
            val theme = AppTheme.theme
            val ctx = binding.root.context
            val dateStr = dateFmt.format(Date(capsule.unlockAtMillis))

            val authorPrefix = AppLocale.t("Від", "From", "Od")
            binding.tvCapsuleAuthorChip.text = "$authorPrefix: ${capsule.author}"
            binding.tvCapsuleUnlockDate.text = dateStr

            if (unlocked) {
                binding.cardCapsuleItem.setCardBackgroundColor("#FFFFFF".toColorInt())
                binding.cardCapsuleItem.strokeColor = "#10B981".toColorInt()
                binding.tvCapsuleBadgePill.background = AppTheme.pill(ctx, "#ECFDF5", 999f)
                binding.tvCapsuleBadgePill.setTextColor("#059669".toColorInt())
                binding.tvCapsuleBadgePill.text = AppLocale.t("✨ ВІДКРИТО", "✨ UNLOCKED", "✨ OTWARTA")

                binding.tvCapsuleStatusIcon.background = AppTheme.pill(ctx, "#10B981", 15f)
                binding.tvCapsuleStatusIcon.text = "🔓"

                binding.tvCapsuleTitle.visibility = View.VISIBLE
                binding.tvCapsuleTitle.text = capsule.title
                binding.tvCapsuleMaskedRow.visibility = View.GONE
                binding.tvCapsuleContent.text = "«${capsule.message}»"
                binding.tvCapsuleContent.setTextColor("#374151".toColorInt())

                binding.progressCapsuleWait.setIndicatorColor("#10B981".toColorInt())
                binding.progressCapsuleWait.progress = 100
                binding.tvCapsuleCountdownChip.background = AppTheme.pill(ctx, "#ECFDF5", 999f)
                binding.tvCapsuleCountdownChip.setTextColor("#059669".toColorInt())
                binding.tvCapsuleCountdownChip.text = AppLocale.t("Читати лист ›", "Read letter ›", "Czytaj list ›")
            } else {
                val diffMs = (capsule.unlockAtMillis - now).coerceAtLeast(1L)
                val daysLeft = ceil(diffMs.toDouble() / (1000.0 * 60.0 * 60.0 * 24.0)).toInt().coerceAtLeast(1)
                val totalMs = (capsule.unlockAtMillis - capsule.createdAtMillis).coerceAtLeast(1L)
                val elapsedMs = (now - capsule.createdAtMillis).coerceAtLeast(0L)
                val waitPct = ((elapsedMs * 100L) / totalMs).toInt().coerceIn(14, 92)

                binding.cardCapsuleItem.setCardBackgroundColor("#F8FAFC".toColorInt())
                binding.cardCapsuleItem.strokeColor = theme.primaryColor
                binding.tvCapsuleBadgePill.background = AppTheme.pill(ctx, theme.softTintHex, 999f)
                binding.tvCapsuleBadgePill.setTextColor(theme.primaryColor)
                binding.tvCapsuleBadgePill.text = AppLocale.t("🔒 ЗАПЕЧАТАНО", "🔒 SEALED", "🔒 ZAMKNIĘTA")

                binding.tvCapsuleStatusIcon.background = AppTheme.pill(ctx, theme.primaryHex, 15f)
                binding.tvCapsuleStatusIcon.text = "🔒"

                binding.tvCapsuleTitle.visibility = View.GONE
                binding.tvCapsuleMaskedRow.visibility = View.VISIBLE
                binding.tvCapsuleContent.text = AppLocale.t(
                    "Назва і таємний лист приховані до дати відкриття",
                    "Title and secret letter are hidden until unlock date",
                    "Tytuł i tajny list są ukryte do daty otwarcia"
                )
                binding.tvCapsuleContent.setTextColor("#6B7280".toColorInt())

                binding.progressCapsuleWait.setIndicatorColor(theme.primaryColor)
                binding.progressCapsuleWait.progress = waitPct
                binding.tvCapsuleCountdownChip.background = AppTheme.pill(ctx, theme.softTintHex, 999f)
                binding.tvCapsuleCountdownChip.setTextColor(theme.primaryColor)
                binding.tvCapsuleCountdownChip.text = AppLocale.t(
                    "⏳ Ще $daysLeft дн.",
                    "⏳ $daysLeft d left",
                    "⏳ Jeszcze $daysLeft dni"
                )
            }

            binding.root.setOnClickListener {
                onCapsuleClick(capsule, unlocked)
            }
            binding.btnDeleteCapsule.setOnClickListener {
                onDeleteClick(capsule)
            }
        }
    }
}
