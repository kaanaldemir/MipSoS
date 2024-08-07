package com.mipo.mipsos

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

class DialogHelper(private val context: Context) {

    fun showPermissionExplanationDialog(
        onProceed: () -> Unit,
        onExit: () -> Unit
    ) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(context.getString(R.string.permissions_disclaimer_title))
            .setMessage(context.getString(R.string.permissions_disclaimer_message))
            .setCancelable(false)
            .setPositiveButton(context.getString(R.string.proceed)) { dialog, _ ->
                dialog.dismiss()
                onProceed()
            }
            .setNegativeButton(context.getString(R.string.exit)) { _, _ ->
                onExit()
            }
        val alertDialog = builder.create()
        alertDialog.show()
    }

    fun showPermissionDeniedDialog(
        deniedPermissions: Set<String>,
        getPermissionFriendlyNames: (Set<String>) -> List<String>,
        onGrantPermissions: () -> Unit,
        onExit: () -> Unit
    ) {
        val builder = AlertDialog.Builder(context)
        val dialogView = (context as AppCompatActivity).layoutInflater.inflate(R.layout.dialog_permissions, null)
        val permissionsListView: ListView = dialogView.findViewById(R.id.permissionsListView)
        val dialogTitle: TextView = dialogView.findViewById(R.id.dialogTitle)
        dialogTitle.text = context.getString(R.string.permissions_required_title)

        val friendlyPermissions = getPermissionFriendlyNames(deniedPermissions)
        val adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, friendlyPermissions)
        permissionsListView.adapter = adapter

        builder.setView(dialogView)
            .setMessage(context.getString(R.string.permissions_required_message))
            .setCancelable(false)
            .setPositiveButton(context.getString(R.string.grant_permissions)) { dialog, _ ->
                dialog.dismiss()
                onGrantPermissions()
            }
            .setNegativeButton(context.getString(R.string.exit)) { _, _ ->
                onExit()
            }
        val alertDialog = builder.create()
        alertDialog.show()
    }

    fun showAppSettingsDialog(
        deniedPermissions: List<String>,
        getPermissionFriendlyNames: (Set<String>) -> List<String>,
        appSettingsLauncher: ActivityResultLauncher<Intent>,
        onExit: () -> Unit
    ) {
        val builder = AlertDialog.Builder(context)
        val dialogView = (context as AppCompatActivity).layoutInflater.inflate(R.layout.dialog_permissions, null)
        val permissionsListView: ListView = dialogView.findViewById(R.id.permissionsListView)
        val dialogTitle: TextView = dialogView.findViewById(R.id.dialogTitle)
        dialogTitle.text = context.getString(R.string.permissions_required_title)

        val friendlyPermissions = getPermissionFriendlyNames(deniedPermissions.toSet())
        val adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, friendlyPermissions)
        permissionsListView.adapter = adapter

        builder.setView(dialogView)
            .setMessage(context.getString(R.string.app_settings_message))
            .setCancelable(false)
            .setPositiveButton(context.getString(R.string.open_settings)) { dialog, _ ->
                dialog.dismiss()
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.fromParts("package", context.packageName, null)
                appSettingsLauncher.launch(intent)
            }
            .setNegativeButton(context.getString(R.string.exit)) { _, _ ->
                onExit()
            }
        val alertDialog = builder.create()
        alertDialog.show()
    }

    fun showContactLimitWarningDialog(onProceed: () -> Unit) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(context.getString(R.string.warning_title))
            .setMessage(context.getString(R.string.contact_limit_warning_message))
            .setCancelable(false)
            .setPositiveButton(context.getString(R.string.yes)) { dialog, _ ->
                dialog.dismiss()
                onProceed()
            }
            .setNegativeButton(context.getString(R.string.no)) { dialog, _ ->
                dialog.dismiss()
            }
        val alertDialog = builder.create()
        alertDialog.show()
    }


    fun showIntervalSelectionDialog(onIntervalSelected: (Long) -> Unit) {
        val intervals = arrayOf(
            context.getString(R.string.interval_15_seconds),
            context.getString(R.string.interval_1_minutes),
            context.getString(R.string.interval_10_minutes)
        )
        val intervalValues = arrayOf(15000L, 60000L, 600000L)

        val builder = AlertDialog.Builder(context)
        builder.setTitle(context.getString(R.string.select_interval))
            .setItems(intervals) { dialog, which ->
                onIntervalSelected(intervalValues[which])
                dialog.dismiss()
            }
            .setNegativeButton(context.getString(R.string.cancel_interval)) { dialog, _ ->
                dialog.dismiss()
            }
        val alertDialog = builder.create()
        alertDialog.show()
    }

    fun promptEnableLocation(onProceed: () -> Unit) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(context.getString(R.string.enable_location))
            .setMessage(context.getString(R.string.enable_location_message))
            .setCancelable(false)
            .setPositiveButton(context.getString(R.string.enable)) { dialog, _ ->
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                (context as AppCompatActivity).startActivityForResult(intent, MainActivity.ENABLE_LOCATION_REQUEST)
            }
            .setNegativeButton(context.getString(R.string.proceed_without_location)) { dialog, _ ->
                dialog.dismiss()
                onProceed()
            }
        val alertDialog = builder.create()
        alertDialog.show()
    }

    fun showLanguageChangeDialog(
        messageEditText: EditText,
        messageHelper: MessageHelper,
        sharedPrefHelper: SharedPrefHelper,
        onRestart: () -> Unit
    ) {
        val languages = arrayOf("English", "Türkçe", "العربية")
        val languageCodes = arrayOf("en", "tr", "ar")
        val currentLangCode = sharedPrefHelper.getLanguage()
        val currentLangIndex = languageCodes.indexOf(currentLangCode)

        val builder = AlertDialog.Builder(context)
        builder.setTitle(context.getString(R.string.change_language))
            .setSingleChoiceItems(languages, currentLangIndex) { dialog, which ->
                val selectedLanguage = languageCodes[which]
                val defaultMessageEn = "Emergency! Please send help to my location."
                val defaultMessageTr = "Acil Durum! Lütfen konumuma yardım gönderin."
                val defaultMessageAr = "حالة طوارئ! يرجى إرسال المساعدة إلى موقعي."

                val currentMessage = messageEditText.text.toString()
                if (currentMessage == defaultMessageEn || currentMessage == defaultMessageTr || currentMessage == defaultMessageAr || currentMessage.isEmpty()) {
                    val newDefaultMessage = when (selectedLanguage) {
                        "en" -> defaultMessageEn
                        "tr" -> defaultMessageTr
                        "ar" -> defaultMessageAr
                        else -> defaultMessageEn
                    }
                    messageEditText.setText(newDefaultMessage)
                    messageHelper.saveMessage(newDefaultMessage)
                }

                sharedPrefHelper.saveLanguage(selectedLanguage)
                dialog.dismiss()
                onRestart()
            }
            .setNegativeButton(context.getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
        val alertDialog = builder.create()
        alertDialog.show()
    }

    fun showThemeModeDialog(
        sharedPrefHelper: SharedPrefHelper,
        onModeSelected: (Int) -> Unit
    ) {
        val modes = arrayOf(
            context.getString(R.string.night_mode),
            context.getString(R.string.day_mode),
            context.getString(R.string.follow_system_mode)
        )
        val currentMode = sharedPrefHelper.getThemeMode()
        val currentModeIndex = when (currentMode) {
            AppCompatDelegate.MODE_NIGHT_NO -> 1
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> 2
            else -> 0
        }

        val builder = AlertDialog.Builder(context)
        builder.setTitle(context.getString(R.string.select_theme_mode))
            .setSingleChoiceItems(modes, currentModeIndex) { dialog, which ->
                val selectedMode = when (which) {
                    1 -> AppCompatDelegate.MODE_NIGHT_NO
                    2 -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    else -> AppCompatDelegate.MODE_NIGHT_YES
                }
                sharedPrefHelper.saveThemeMode(selectedMode)
                onModeSelected(selectedMode)
                dialog.dismiss()
            }
            .setNegativeButton(context.getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
        val alertDialog = builder.create()
        alertDialog.show()
    }
}
