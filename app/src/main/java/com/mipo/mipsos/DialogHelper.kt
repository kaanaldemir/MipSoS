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
        builder.setTitle("Permissions Required")
            .setMessage("This app needs certain permissions to function properly. No permissions will be used to collect data or for any malicious purposes. Please grant the required permissions.")
            .setCancelable(false)
            .setPositiveButton("Proceed") { dialog, _ ->
                dialog.dismiss()
                onProceed()
            }
            .setNegativeButton("Exit") { _, _ ->
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
        dialogTitle.text = "Permissions Required"

        val friendlyPermissions = getPermissionFriendlyNames(deniedPermissions)
        val adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, friendlyPermissions)
        permissionsListView.adapter = adapter

        builder.setView(dialogView)
            .setMessage("This app needs the following permissions to function properly. Please grant all permissions.")
            .setCancelable(false)
            .setPositiveButton("Grant Permissions") { dialog, _ ->
                dialog.dismiss()
                onGrantPermissions()
            }
            .setNegativeButton("Exit") { _, _ ->
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
        dialogTitle.text = "Permissions Required"

        val friendlyPermissions = getPermissionFriendlyNames(deniedPermissions.toSet())
        val adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, friendlyPermissions)
        permissionsListView.adapter = adapter

        builder.setView(dialogView)
            .setMessage("Some permissions were permanently denied. Please go to app settings to grant all required permissions.")
            .setCancelable(false)
            .setPositiveButton("Open Settings") { dialog, _ ->
                dialog.dismiss()
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.fromParts("package", context.packageName, null)
                appSettingsLauncher.launch(intent)
            }
            .setNegativeButton("Exit") { _, _ ->
                onExit()
            }
        val alertDialog = builder.create()
        alertDialog.show()
    }
}
