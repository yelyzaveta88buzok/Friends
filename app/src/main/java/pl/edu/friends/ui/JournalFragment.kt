package pl.edu.friends.ui

import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import pl.edu.friends.R
import pl.edu.friends.data.BestiesRepository
import pl.edu.friends.data.FriendshipProfileEntity
import pl.edu.friends.data.JournalEntryEntity
import pl.edu.friends.databinding.FragmentJournalBinding
import pl.edu.friends.util.AppLocale
import pl.edu.friends.util.AppTheme
import pl.edu.friends.util.RoomPrefs
import pl.edu.friends.util.RoomTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

class JournalFragment : Fragment() {

    private var _binding: FragmentJournalBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BestiesViewModel by activityViewModels()
    private var cachedEntries: List<JournalEntryEntity> = emptyList()
    private var latestProfile: FriendshipProfileEntity? = null
    private var selectedMyMoodScore: Int = 5

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJournalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.warmthWaveChart.onDaySelected = { _, pt ->
            if (_binding != null) {
                updateSelectedDayTooltip(pt)
            }
        }

        val moodButtons = listOf(
            1 to binding.btnMood1,
            2 to binding.btnMood2,
            3 to binding.btnMood3,
            4 to binding.btnMood4,
            5 to binding.btnMood5
        )
        moodButtons.forEach { (score, btn) ->
            btn.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                selectTodayMood(score)
            }
        }

        viewModel.allJournalEntries.observe(viewLifecycleOwner) { entries ->
            cachedEntries = entries
            entries.firstOrNull()?.let { latest ->
                selectedMyMoodScore = latest.myMoodScore.coerceIn(1, 5)
            }
            highlightSelectedMoodButton(selectedMyMoodScore)
            updateWarmthStatsAndWave(entries)
            renderFriendsWarmthChips()
            renderDailyMoodLimitUi()
        }

        viewModel.profile.observe(viewLifecycleOwner) { profile ->
            latestProfile = profile
            renderFriendsWarmthChips()
        }

        AppLocale.currentLang.observe(viewLifecycleOwner) {
            applyLocalization()
            highlightSelectedMoodButton(selectedMyMoodScore)
            updateWarmthStatsAndWave(cachedEntries)
            renderFriendsWarmthChips()
            renderDailyMoodLimitUi()
        }

        AppTheme.currentTheme.observe(viewLifecycleOwner) { theme ->
            applyTheme(theme)
        }
    }

    private fun applyTheme(theme: RoomTheme) {
        val ctx = requireContext()
        binding.journalRootScroll.setBackgroundColor(theme.pageBgColor)
        binding.layoutAtmosphereWall.setBackgroundColor("#FFFFFF".toColorInt())
        binding.tvJournalHeaderTitle.setTextColor("#111827".toColorInt())
        binding.tvJournalHeaderSub.setTextColor("#6B7280".toColorInt())
        binding.tvWarmthTempBadge.background = AppTheme.pill(ctx, theme.softTintHex, 999f)
        binding.tvWarmthTempBadge.setTextColor(theme.primaryColor)
        binding.cardMoodChartSection.strokeColor = theme.cardStrokeColor
        binding.tvChartTitle.setTextColor("#111827".toColorInt())
        binding.tvChartLegend.setTextColor("#6B7280".toColorInt())
        binding.tvSelectedDayTooltip.background = AppTheme.pill(ctx, theme.softTintHex, 14f)
        binding.tvSelectedDayTooltip.setTextColor(theme.primaryColor)
        binding.warmthWaveChart.setTheme(theme)
        highlightSelectedMoodButton(selectedMyMoodScore)
        renderFriendsWarmthChips()
        renderDailyMoodLimitUi()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun emojiFor(score: Int): String = when (score.coerceIn(1, 5)) {
        5 -> "😍"
        4 -> "😊"
        3 -> "😐"
        2 -> "😔"
        else -> "😢"
    }

    private fun updateSelectedDayTooltip(pt: DayWarmthPoint) {
        val myInt = pt.myScore.roundToInt().coerceIn(1, 5)
        val frInt = pt.friendScore.roundToInt().coerceIn(1, 5)
        val youWord = AppLocale.t("Ти", "You", "Ty")
        val frWord = AppLocale.t("Друзі", "Friends", "Przyjaciele")
        binding.tvSelectedDayTooltip.text =
            "📅 ${pt.dayLabel}  •  $youWord: ${emojiFor(myInt)} $myInt/5  •  $frWord: ${emojiFor(frInt)} $frInt/5"
    }

    private fun selectTodayMood(score: Int) {
        val ctx = requireContext()
        val used = RoomPrefs.getTodayMoodCount(ctx)
        if (used >= RoomPrefs.MAX_DAILY_MOOD_REACTIONS) {
            Toast.makeText(
                ctx,
                AppLocale.t(
                    "⏳ Сьогодні вже 5/5 реакцій! Нові відкриються завтра ✨",
                    "⏳ Daily limit 5/5 reached! New ones unlock tomorrow ✨",
                    "⏳ Dzisiejszy limit 5/5 wykorzystany! Kolejne jutro ✨"
                ),
                Toast.LENGTH_SHORT
            ).show()
            renderDailyMoodLimitUi()
            return
        }

        val nowUsed = RoomPrefs.incrementTodayMoodCount(ctx)
        selectedMyMoodScore = score
        highlightSelectedMoodButton(score)
        renderDailyMoodLimitUi()

        val dateStr = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
        val friendScore = listOf(4, 5, 5, 4).random()
        val moodLabels = mapOf(
            1 to AppLocale.t("😢 Втома", "😢 Tired", "😢 Zmęczenie"),
            2 to AppLocale.t("😔 Сумно", "😔 Down", "😔 Smutek"),
            3 to AppLocale.t("😐 Спокійно", "😐 Calm", "😐 Spokój"),
            4 to AppLocale.t("😊 Тепло", "😊 Happy", "😊 Pogodnie"),
            5 to AppLocale.t("🤩 Супер!", "🤩 Awesome!", "🤩 Super!")
        )
        viewModel.updateMoods(
            myMood = moodLabels[score] ?: AppLocale.t("🤩 Супер!", "🤩 Awesome!", "🤩 Super!"),
            friendMood = AppLocale.t("☕ Затишно", "☕ Cozy", "☕ Przytulnie")
        )
        viewModel.recordDailyMood(
            dateLabel = dateStr,
            myMoodScore = score,
            friendMoodScore = friendScore,
            note = AppLocale.t("Настрій дня", "Daily mood", "Nastrój dnia")
        )

        Toast.makeText(
            ctx,
            AppLocale.t(
                "✨ Настрій ($score/5) оновлено! ($nowUsed/${RoomPrefs.MAX_DAILY_MOOD_REACTIONS} сьогодні)",
                "✨ Mood ($score/5) updated! ($nowUsed/${RoomPrefs.MAX_DAILY_MOOD_REACTIONS} today)",
                "✨ Nastrój ($score/5) zaktualizowany! ($nowUsed/${RoomPrefs.MAX_DAILY_MOOD_REACTIONS} dziś)"
            ),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun renderDailyMoodLimitUi() {
        val ctx = requireContext()
        val theme = AppTheme.theme
        val used = RoomPrefs.getTodayMoodCount(ctx)
        val max = RoomPrefs.MAX_DAILY_MOOD_REACTIONS
        val limitReached = used >= max

        val todayWord = AppLocale.t("сьогодні", "today", "dziś")
        binding.tvDailyMoodLimitBadge.text = "$used / $max $todayWord"
        binding.tvDailyMoodLimitBadge.background = AppTheme.pill(
            ctx,
            if (limitReached) "#FEE2E2" else theme.softTintHex,
            999f
        )
        binding.tvDailyMoodLimitBadge.setTextColor(
            if (limitReached) "#DC2626".toColorInt() else theme.primaryColor
        )

        binding.tvPickMoodLabel.text = if (limitReached) {
            AppLocale.t(
                "✅ Ліміт на сьогодні вичерпано (5/5)",
                "✅ Today's limit reached (5/5)",
                "✅ Dzisiejszy limit osiągnięty (5/5)"
            )
        } else {
            AppLocale.t(
                "✨ Як ти почуваєшся сьогодні?",
                "✨ How is your vibe today?",
                "✨ Jaki masz dziś nastrój?"
            )
        }

        binding.layoutDailyMoodSlots.removeAllViews()
        for (i in 0 until max) {
            val filled = i < used
            val bar = View(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(0, dp(5), 1f).apply {
                    if (i < max - 1) marginEnd = dp(5)
                }
                background = AppTheme.pill(
                    ctx,
                    if (filled) theme.primaryHex else "#E5E7EB",
                    999f
                )
            }
            binding.layoutDailyMoodSlots.addView(bar)
        }

        val btnAlpha = if (limitReached) 0.48f else 1f
        listOf(
            binding.btnMood1,
            binding.btnMood2,
            binding.btnMood3,
            binding.btnMood4,
            binding.btnMood5
        ).forEach { btn ->
            btn.alpha = btnAlpha
        }
    }

    private fun highlightSelectedMoodButton(activeScore: Int) {
        val theme = AppTheme.theme
        val ctx = requireContext()
        val pairs = listOf(
            Triple(1, binding.btnMood1, binding.tvMoodLbl1),
            Triple(2, binding.btnMood2, binding.tvMoodLbl2),
            Triple(3, binding.btnMood3, binding.tvMoodLbl3),
            Triple(4, binding.btnMood4, binding.tvMoodLbl4),
            Triple(5, binding.btnMood5, binding.tvMoodLbl5)
        )
        pairs.forEach { (score, box, lbl) ->
            val isActive = score == activeScore
            if (isActive) {
                box.background = AppTheme.pill(ctx, theme.softTintHex, 14f, 1.5f, theme.primaryHex)
                lbl.setTextColor(theme.primaryColor)
            } else {
                box.setBackgroundResource(R.drawable.bg_timer_box)
                lbl.setTextColor(ctx.getColor(R.color.text_secondary))
            }
        }
    }

    private fun updateWarmthStatsAndWave(entries: List<JournalEntryEntity>) {
        val dayLabelsDefault = listOf(
            AppLocale.t("Пн", "Mon", "Pn"),
            AppLocale.t("Вт", "Tue", "Wt"),
            AppLocale.t("Ср", "Wed", "Śr"),
            AppLocale.t("Чт", "Thu", "Cz"),
            AppLocale.t("Пт", "Fri", "Pt"),
            AppLocale.t("Сб", "Sat", "Sb"),
            AppLocale.t("Нд", "Sun", "Nd")
        )
        val fallbackPoints = listOf(
            DayWarmthPoint(dayLabelsDefault[0], 4f, 4f),
            DayWarmthPoint(dayLabelsDefault[1], 5f, 4f),
            DayWarmthPoint(dayLabelsDefault[2], 3f, 4f),
            DayWarmthPoint(dayLabelsDefault[3], 4f, 5f),
            DayWarmthPoint(dayLabelsDefault[4], 5f, 4f),
            DayWarmthPoint(dayLabelsDefault[5], 4f, 5f),
            DayWarmthPoint(dayLabelsDefault[6], selectedMyMoodScore.toFloat(), 5f)
        )

        val recent = entries.distinctBy { it.dateLabel }.take(7).reversed()
        val wavePoints = if (recent.isEmpty()) {
            fallbackPoints
        } else {
            val missing = (7 - recent.size).coerceAtLeast(0)
            val padded = fallbackPoints.take(missing).toMutableList()
            recent.forEachIndexed { idx, item ->
                val shortLabel = item.dateLabel.take(5).ifEmpty { dayLabelsDefault[(missing + idx) % 7] }
                padded.add(
                    DayWarmthPoint(
                        dayLabel = shortLabel,
                        myScore = item.myMoodScore.coerceIn(1, 5).toFloat(),
                        friendScore = item.friendMoodScore.coerceIn(1, 5).toFloat()
                    )
                )
            }
            padded
        }

        binding.warmthWaveChart.setData(wavePoints, animate = true)

        val avg = wavePoints.map { (it.myScore + it.friendScore) / 2.0 }.average()
        val avgFmt = String.format(Locale.US, "%.1f", avg)
        val warmDays = wavePoints.count { ((it.myScore + it.friendScore) / 2f) >= 3.5f }
        val syncPct = wavePoints.map { 100.0 - (abs(it.myScore - it.friendScore) * 15.0) }
            .average().roundToInt().coerceIn(50, 100)

        binding.tvWarmthTempBadge.text = AppLocale.t(
            "⭐ Середнє: $avgFmt / 5",
            "⭐ Avg: $avgFmt / 5",
            "⭐ Średnia: $avgFmt / 5"
        )

        val avgTitle = AppLocale.t("Середнє", "Average", "Średnia")
        val warmTitle = AppLocale.t("Теплих днів", "Warm days", "Ciepłych dni")
        val syncTitle = AppLocale.t("Синхрон", "Vibe sync", "Zgodność")

        binding.tvStatAvg.text = "⭐ $avgFmt / 5\n$avgTitle"
        binding.tvStatWarmDays.text = "☀️ $warmDays ${AppLocale.t("із", "of", "z")} 7\n$warmTitle"
        binding.tvStatSync.text = "🤝 $syncPct%\n$syncTitle"
    }

    private fun renderFriendsWarmthChips() {
        binding.layoutFriendsWarmthChips.removeAllViews()
        val ctx = requireContext()
        val theme = AppTheme.theme
        val members = latestProfile?.membersCsv
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.take(BestiesRepository.MAX_FRIENDS_PER_CODE)
            ?: listOf("Ліза", "Кацпер", "Аня")

        val friendScores = listOf(selectedMyMoodScore, 5, 4, 5, 4)

        members.forEachIndexed { idx, name ->
            val score = friendScores[idx % friendScores.size].coerceIn(1, 5)
            val avatarEmoji = RoomPrefs.getAvatar(ctx, name, idx)
            val chipBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(99).toFloat()
                setColor(if (idx == 0) "#FFFFFF".toColorInt() else theme.softTintColor)
                setStroke(dp(1), if (idx == 0) theme.primaryColor else theme.secondaryColor)
            }
            val chip = TextView(ctx).apply {
                background = chipBg
                setPadding(dp(12), dp(6), dp(12), dp(6))
                text = "$avatarEmoji $name • ${emojiFor(score)} $score/5"
                textSize = 12f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(theme.primaryDarkColor)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    if (idx < members.size - 1) marginEnd = dp(8)
                }
            }
            binding.layoutFriendsWarmthChips.addView(chip)
        }
    }

    private fun applyLocalization() {
        binding.tvJournalHeaderTitle.text = AppLocale.t(
            "Настрій компанії",
            "Squad Mood",
            "Nastrój ekipy"
        )
        binding.tvJournalHeaderSub.text = AppLocale.t(
            "Хвиля настрою вашої компанії за 7 днів",
            "Your friends squad's mood wave over 7 days",
            "Fala nastroju Waszej paczki z ostatnich 7 dni"
        )
        binding.tvChartTitle.text = AppLocale.t(
            "📈 Хвиля настрою (7 днів)",
            "📈 Mood Wave (Last 7 Days)",
            "📈 Fala nastroju (7 dni)"
        )
        binding.tvChartLegend.text = AppLocale.t(
            "━━ Ти   •   ╌╌ Друзі (натисни на день)",
            "━━ You   •   ╌╌ Friends (tap any day)",
            "━━ Ty   •   ╌╌ Przyjaciele (dotknij dzień)"
        )
        renderDailyMoodLimitUi()
    }

    override fun onResume() {
        super.onResume()
        renderDailyMoodLimitUi()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
