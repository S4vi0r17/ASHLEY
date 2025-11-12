package com.grupo2.ashley.chat.notifications

import android.Manifest
import android.os.Build
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

/**
 * Request notification permission for Android 13+
 * This composable will automatically request permission when needed
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequestNotificationPermission(
    onPermissionResult: (Boolean) -> Unit = {}
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val notificationPermissionState = rememberPermissionState(
            Manifest.permission.POST_NOTIFICATIONS
        ) { isGranted ->
            onPermissionResult(isGranted)
        }

        var showRationaleDialog by remember { mutableStateOf(false) }

        LaunchedEffect(notificationPermissionState.status) {
            if (!notificationPermissionState.status.isGranted) {
                if (notificationPermissionState.status.shouldShowRationale) {
                    showRationaleDialog = true
                } else {
                    notificationPermissionState.launchPermissionRequest()
                }
            } else {
                onPermissionResult(true)
            }
        }

        if (showRationaleDialog) {
            NotificationPermissionRationaleDialog(
                onDismiss = { showRationaleDialog = false },
                onConfirm = {
                    showRationaleDialog = false
                    notificationPermissionState.launchPermissionRequest()
                }
            )
        }
    } else {
        // Permission not needed on Android 12 and below
        LaunchedEffect(Unit) {
            onPermissionResult(true)
        }
    }
}

@Composable
private fun NotificationPermissionRationaleDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Notificaciones de Chat") },
        text = {
            Text(
                "Para recibir notificaciones de nuevos mensajes, " +
                "necesitamos tu permiso para mostrar notificaciones. " +
                "¿Deseas activar las notificaciones?"
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Permitir")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Ahora no")
            }
        }
    )
}

/**
 * Check if notification permission is granted
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun rememberNotificationPermissionState(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionState = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
        permissionState.status.isGranted
    } else {
        true
    }
}
