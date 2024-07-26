package com.mipo.mipsos

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionHelper(
    private val context: Context,
    private val requestPermissionsLauncher: ActivityResultLauncher<Array<String>>,
    private val appSettingsLauncher: ActivityResultLauncher<Intent>,
    private val initializeApp: () -> Unit,
    private val handleDeniedPermissions: (Map<String, Boolean>) -> Unit
) {

    fun showPermissionExplanationDialog() {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(context.getString(R.string.permissions_required))
            .setMessage(context.getString(R.string.permissions_explanation))
            .setCancelable(false)
            .setPositiveButton(context.getString(R.string.proceed)) { dialog, _ ->
                dialog.dismiss()
                checkAndRequestPermissions()
            }
            .setNegativeButton(context.getString(R.string.exit)) { _, _ ->
                (context as AppCompatActivity).finish()
            }
        val alertDialog = builder.create()
        alertDialog.show()
    }

    fun checkAndRequestPermissions() {
        val permissionsToRequest = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_CONTACTS
        )

        val permissionsNeeded = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsNeeded.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsNeeded.toTypedArray())
        } else {
            initializeApp()
        }
    }

    fun handleDeniedPermissions(permissions: Map<String, Boolean>) {
        val deniedPermissions = permissions.filterValues { !it }
        val permanentlyDeniedPermissions = deniedPermissions.keys.filter { permission ->
            !ActivityCompat.shouldShowRequestPermissionRationale(context as AppCompatActivity, permission)
        }

        if (permanentlyDeniedPermissions.isNotEmpty()) {
            showAppSettingsDialog(permanentlyDeniedPermissions)
        } else {
            showPermissionDeniedDialog(deniedPermissions.keys)
        }
    }

    private fun showPermissionDeniedDialog(deniedPermissions: Set<String>) {
        val builder = AlertDialog.Builder(context)
        val dialogView = (context as AppCompatActivity).layoutInflater.inflate(R.layout.dialog_permissions, null)
        val permissionsListView: ListView = dialogView.findViewById(R.id.permissionsListView)
        val dialogTitle: TextView = dialogView.findViewById(R.id.dialogTitle)
        dialogTitle.text = context.getString(R.string.permissions_required)

        val friendlyPermissions = getPermissionFriendlyNames(deniedPermissions)
        val adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, friendlyPermissions)
        permissionsListView.adapter = adapter

        builder.setView(dialogView)
            .setMessage(context.getString(R.string.permissions_needed))
            .setCancelable(false)
            .setPositiveButton(context.getString(R.string.grant_permissions)) { dialog, _ ->
                dialog.dismiss()
                checkAndRequestPermissions()
            }
            .setNegativeButton(context.getString(R.string.exit)) { _, _ ->
                (context as AppCompatActivity).finish()
            }
        val alertDialog = builder.create()
        alertDialog.show()
    }

    private fun showAppSettingsDialog(deniedPermissions: List<String>) {
        val builder = AlertDialog.Builder(context)
        val dialogView = (context as AppCompatActivity).layoutInflater.inflate(R.layout.dialog_permissions, null)
        val permissionsListView: ListView = dialogView.findViewById(R.id.permissionsListView)
        val dialogTitle: TextView = dialogView.findViewById(R.id.dialogTitle)
        dialogTitle.text = context.getString(R.string.permissions_required)

        val friendlyPermissions = getPermissionFriendlyNames(deniedPermissions.toSet())
        val adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, friendlyPermissions)
        permissionsListView.adapter = adapter

        builder.setView(dialogView)
            .setMessage(context.getString(R.string.permissions_permanently_denied))
            .setCancelable(false)
            .setPositiveButton(context.getString(R.string.open_settings)) { dialog, _ ->
                dialog.dismiss()
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.fromParts("package", context.packageName, null)
                appSettingsLauncher.launch(intent)
            }
            .setNegativeButton(context.getString(R.string.exit)) { _, _ ->
                (context as AppCompatActivity).finish()
            }
        val alertDialog = builder.create()
        alertDialog.show()
    }

    private fun getPermissionFriendlyNames(permissions: Set<String>): List<String> {
        val friendlyNames = mutableSetOf<String>()
        var includesLocation = false

        permissions.forEach { permission ->
            when (permission) {
                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION -> includesLocation = true
                Manifest.permission.RECORD_AUDIO -> friendlyNames.add(context.getString(R.string.microphone))
                Manifest.permission.SEND_SMS -> friendlyNames.add(context.getString(R.string.sms))
                Manifest.permission.READ_CONTACTS -> friendlyNames.add(context.getString(R.string.contacts))
                else -> friendlyNames.add(permission)
            }
        }

        if (includesLocation) {
            friendlyNames.add(context.getString(R.string.location))
        }

        return friendlyNames.toList()
    }
}
