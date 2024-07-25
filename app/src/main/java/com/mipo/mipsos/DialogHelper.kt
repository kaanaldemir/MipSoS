package com.mipo.mipsos

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity

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
}
