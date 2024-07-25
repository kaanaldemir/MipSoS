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
        builder.setTitle("Permissions Required")
            .setMessage("This app needs certain permissions to function properly. No permissions will be used to collect data or for any malicious purposes. Please grant the required permissions.")
            .setCancelable(false)
            .setPositiveButton("Proceed") { dialog, _ ->
                dialog.dismiss()
                checkAndRequestPermissions()
            }
            .setNegativeButton("Exit") { _, _ ->
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
        dialogTitle.text = "Permissions Required"

        val friendlyPermissions = getPermissionFriendlyNames(deniedPermissions)
        val adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, friendlyPermissions)
        permissionsListView.adapter = adapter

        builder.setView(dialogView)
            .setMessage("This app needs the following permissions to function properly. Please grant all permissions.")
            .setCancelable(false)
            .setPositiveButton("Grant Permissions") { dialog, _ ->
                dialog.dismiss()
                checkAndRequestPermissions()
            }
            .setNegativeButton("Exit") { _, _ ->
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
                Manifest.permission.RECORD_AUDIO -> friendlyNames.add("Microphone")
                Manifest.permission.SEND_SMS -> friendlyNames.add("SMS")
                Manifest.permission.READ_CONTACTS -> friendlyNames.add("Contacts")
                else -> friendlyNames.add(permission)
            }
        }

        if (includesLocation) {
            friendlyNames.add("Location")
        }

        return friendlyNames.toList()
    }
}
