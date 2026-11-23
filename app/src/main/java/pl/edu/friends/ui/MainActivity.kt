package pl.edu.friends.ui

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import pl.edu.friends.R
import pl.edu.friends.databinding.ActivityMainBinding
import pl.edu.friends.databinding.DialogAddCapsuleBinding
import pl.edu.friends.databinding.DialogQuickPlusSheetBinding
import pl.edu.friends.util.AppLocale
import pl.edu.friends.util.AppTheme
import pl.edu.friends.util.RoomPrefs

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: BestiesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLocale.init(this)
        AppTheme.init(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    replaceFragment(HomeFragment())
                    true
                }
                R.id.nav_chat -> {
                    replaceFragment(ChatFragment())
                    true
                }
                R.id.nav_games -> {
                    replaceFragment(GamesFragment())
                    true
                }
                R.id.nav_journal -> {
                    replaceFragment(JournalFragment())
                    true
                }
                else -> false
            }
        }

        binding.btnCenterPlus.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            showQuickPlusBottomSheet()
        }

        AppLocale.currentLang.observe(this) {
            val menu = binding.bottomNavigation.menu
            menu.findItem(R.id.nav_home)?.title = AppLocale.t("Головна", "Home", "Główna")
            menu.findItem(R.id.nav_chat)?.title = AppLocale.t("Чат", "Chat", "Czat")
            menu.findItem(R.id.nav_games)?.title = AppLocale.t("Ігри", "Games", "Gry")
            menu.findItem(R.id.nav_journal)?.title = AppLocale.t("Настрій", "Mood", "Nastrój")
        }

        AppTheme.currentTheme.observe(this) { theme ->
            binding.root.setBackgroundColor(theme.pageBgColor)
            binding.bottomNavDivider.setBackgroundColor("#E5E7EB".toColorInt())
            binding.bottomNavigation.setBackgroundColor("#FFFFFF".toColorInt())
            binding.btnCenterPlus.setCardBackgroundColor(theme.primaryColor)

            val navColors = ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_checked),
                    intArrayOf()
                ),
                intArrayOf(
                    theme.primaryColor,
                    "#9CA3AF".toColorInt()
                )
            )
            binding.bottomNavigation.itemIconTintList = navColors
            binding.bottomNavigation.itemTextColor = navColors
        }
    }

    private fun showQuickPlusBottomSheet() {
        val dialog = BottomSheetDialog(this)
        val sheetBinding = DialogQuickPlusSheetBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)

        val theme = AppTheme.theme
        sheetBinding.tvQuickSheetTitle.text = AppLocale.t("⚡ Швидка дія", "⚡ Quick Action", "⚡ Szybka akcja")
        sheetBinding.tvQuickHighFiveTitle.text = AppLocale.t("Дай п'ять", "High Five", "Przybij piątkę")
        sheetBinding.tvQuickHighFiveSub.text = AppLocale.t("У чат • +10 XP", "To chat • +10 XP", "Na czat • +10 XP")
        sheetBinding.tvQuickCoffeeTitle.text = AppLocale.t("На каву", "Grab Coffee", "Na kawę")
        sheetBinding.tvQuickCoffeeSub.text = AppLocale.t("Покликати • +10 XP", "Invite • +10 XP", "Zaproś • +10 XP")
        sheetBinding.tvQuickPhotoTitle.text = AppLocale.t("Фото в чат", "Photo to Chat", "Zdjęcie na czat")
        sheetBinding.tvQuickPhotoSub.text = AppLocale.t("Обрати з галереї", "Pick from gallery", "Wybierz z galerii")
        sheetBinding.tvQuickCapsuleTitle.text = AppLocale.t("Нова капсула", "New Capsule", "Nowa kapsuła")
        sheetBinding.tvQuickCapsuleSub.text = AppLocale.t("Лист у майбутнє 🔒", "Seal for future 🔒", "List w przyszłość 🔒")

        sheetBinding.cardQuickCapsule.setCardBackgroundColor(theme.softTintColor)
        sheetBinding.cardQuickCapsule.strokeColor = theme.primaryColor
        sheetBinding.tvQuickCapsuleSub.setTextColor(theme.primaryColor)

        sheetBinding.btnCloseQuickSheet.setOnClickListener { dialog.dismiss() }

        sheetBinding.cardQuickHighFive.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            viewModel.addQuickActionXp()
            viewModel.sendChatMessage(
                sender = "Ty",
                text = AppLocale.t(
                    "✋ Даю п'ять всій нашій компанії! Ви найкращі! 🔥",
                    "✋ High-five to our whole squad! You rock! 🔥",
                    "✋ Przybijam piątkę całej naszej ekipie! 🔥"
                )
            )
            Toast.makeText(
                this,
                AppLocale.t("✋ «Дай п'ять» надіслано в чат! (+10 XP)", "✋ High-five sent to chat! (+10 XP)", "✋ Wysłano piątkę na czat! (+10 XP)"),
                Toast.LENGTH_SHORT
            ).show()
            dialog.dismiss()
        }

        sheetBinding.cardQuickCoffee.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            viewModel.addQuickActionXp()
            viewModel.sendChatMessage(
                sender = "Ty",
                text = AppLocale.t(
                    "☕ Хто сьогодні йде на каву? Збираємося!",
                    "☕ Who's in for coffee today?",
                    "☕ Kto wpada dzisiaj na kawę?"
                )
            )
            Toast.makeText(
                this,
                AppLocale.t("☕ Запрошення на каву надіслано! (+10 XP)", "☕ Coffee invite sent! (+10 XP)", "☕ Zaproszenie na kawę wysłane! (+10 XP)"),
                Toast.LENGTH_SHORT
            ).show()
            dialog.dismiss()
        }

        sheetBinding.cardQuickPhoto.setOnClickListener {
            dialog.dismiss()
            val currentFrag = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
            if (currentFrag is ChatFragment) {
                currentFrag.openPhotoFromQuickPlus()
            } else {
                RoomPrefs.openPhotoPickerNext = true
                navigateToTab(R.id.nav_chat)
            }
        }

        sheetBinding.cardQuickCapsule.setOnClickListener {
            dialog.dismiss()
            openQuickCapsuleDialog()
        }

        dialog.show()
    }

    fun openQuickCapsuleDialog() {
        showCreateCapsuleDialog()
    }

    private fun showCreateCapsuleDialog() {
        val dialogBinding = DialogAddCapsuleBinding.inflate(layoutInflater)
        dialogBinding.tilCapsuleTitle.hint = AppLocale.t(
            "Назва капсули (напр. Літо 2026)",
            "Capsule title (e.g. Summer 2026)",
            "Tytuł kapsuły (np. Lato 2026)"
        )
        dialogBinding.tilCapsuleDays.hint = AppLocale.t(
            "Через скільки днів відкрити? (0 = одразу)",
            "Unlock in how many days? (0 = now)",
            "Za ile dni odblokować? (0 = od razu)"
        )
        dialogBinding.tilCapsuleMessage.hint = AppLocale.t(
            "Таємне повідомлення для компанії",
            "Secret message for the future",
            "Tajna wiadomość do odczytania w przyszłości"
        )

        AlertDialog.Builder(this)
            .setTitle(AppLocale.t("✉️ Нова Капсула Часу (+30 XP)", "✉️ New Time Capsule (+30 XP)", "✉️ Nowa Kapsuła Czasu (+30 XP)"))
            .setView(dialogBinding.root)
            .setPositiveButton(AppLocale.t("Запечатати 🔒", "Seal 🔒", "Zapieczętuj 🔒")) { _, _ ->
                val title = dialogBinding.etCapsuleTitle.text?.toString()?.trim().orEmpty()
                val daysStr = dialogBinding.etCapsuleDaysUntilUnlock.text?.toString()?.trim().orEmpty()
                val message = dialogBinding.etCapsuleMessage.text?.toString()?.trim().orEmpty()

                if (title.isNotEmpty() && message.isNotEmpty()) {
                    val days = daysStr.toIntOrNull()?.coerceAtLeast(0) ?: 7
                    viewModel.addCapsule(
                        title = title,
                        message = message,
                        author = viewModel.profile.value?.membersCsv?.split(",")?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() } ?: AppLocale.t("Я", "Me", "Ja"),
                        daysUntilUnlock = days
                    )
                    Toast.makeText(
                        this,
                        AppLocale.t(
                            "✉️ Капсулу запечатано 🔒 Відкриття через $days дн. (+30 XP)",
                            "✉️ Capsule sealed 🔒 Unlocks in $days days (+30 XP)",
                            "✉️ Kapsuła zapieczętowana 🔒 Otwarcie za $days dni (+30 XP)"
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                    val currentFrag = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
                    if (currentFrag is ChatFragment) {
                        currentFrag.openCapsulesFromQuickPlus()
                    } else {
                        RoomPrefs.openEnvelopesNext = true
                        navigateToTab(R.id.nav_chat)
                    }
                }
            }
            .setNegativeButton(AppLocale.t("Скасувати", "Cancel", "Anuluj"), null)
            .show()
    }

    fun navigateToTab(itemId: Int) {
        binding.bottomNavigation.selectedItemId = itemId
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
