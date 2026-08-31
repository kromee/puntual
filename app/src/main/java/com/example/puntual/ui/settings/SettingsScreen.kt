package com.example.puntual.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.puntual.R
import com.example.puntual.ui.components.PuntualElevatedCard
import com.example.puntual.ui.components.PuntualScreenShell
import com.example.puntual.ui.theme.PuntualGreen
import com.example.puntual.ui.theme.TextPrimary
import com.example.puntual.domain.model.sanitizeDisplayName
import com.example.puntual.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onSignOut: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> viewModel.onNotificationPermissionResult(granted) },
    )

    PuntualScreenShell(
        userDisplayName = sanitizeDisplayName(uiState.displayNameDraft),
        screenTitle = stringResource(R.string.nav_settings),
        brandFirst = true,
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
        ) {
            PuntualElevatedCard {
                Text(
                    text = stringResource(R.string.settings_profile),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = uiState.displayNameDraft,
                    onValueChange = viewModel::onDisplayNameChange,
                    label = { Text(stringResource(R.string.settings_display_name)) },
                    placeholder = { Text(stringResource(R.string.settings_name_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedPlaceholderColor = TextSecondary,
                        unfocusedPlaceholderColor = TextSecondary,
                        focusedBorderColor = PuntualGreen,
                        unfocusedBorderColor = TextSecondary,
                        cursorColor = PuntualGreen,
                    ),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.settings_name_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = viewModel::saveDisplayName,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.settings_save_name))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            PuntualElevatedCard {
                Text(
                    text = stringResource(R.string.settings_periods),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.settings_periods_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
                if (uiState.activePeriodTitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(
                            R.string.settings_active_period,
                            uiState.activePeriodTitle,
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = PuntualGreen,
                        fontWeight = FontWeight.Medium,
                    )
                }
                uiState.periods.filter { !it.isActive }.forEach { period ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${period.title} · ${period.rangeLabel}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = viewModel::openClosePeriodDialog,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.settings_close_period))
                }
                val closeSuccess = uiState.closePeriodSuccess
                if (closeSuccess != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = closeSuccess,
                        style = MaterialTheme.typography.bodyMedium,
                        color = PuntualGreen,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            PuntualElevatedCard {
                Text(
                    text = "Ausencias justificadas",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Registra vacaciones, permisos o incapacidades para explicar días sin check-in en el historial.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = viewModel::openAbsenceDialog,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Agregar ausencia")
                }
                val absenceSuccess = uiState.absenceSuccess
                if (absenceSuccess != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = absenceSuccess,
                        style = MaterialTheme.typography.bodyMedium,
                        color = PuntualGreen,
                    )
                }
                if (uiState.absences.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(8.dp))
                    uiState.absences.take(5).forEach { absence ->
                        AbsenceListItem(
                            absence = absence,
                            onDelete = viewModel::deleteAbsence,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            PuntualElevatedCard {
                Text(
                    text = stringResource(R.string.settings_schedule),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.settings_expected_time),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                Text(
                    text = if (uiState.hasExpectedTime) {
                        uiState.expectedTimeLabel
                    } else {
                        stringResource(R.string.settings_no_time)
                    },
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 40.sp),
                    color = if (uiState.hasExpectedTime) PuntualGreen else TextSecondary,
                    fontWeight = FontWeight.Light,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = viewModel::openTimePicker,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (uiState.hasExpectedTime) {
                            stringResource(R.string.settings_change_time)
                        } else {
                            stringResource(R.string.settings_set_time)
                        },
                    )
                }
                if (uiState.hasExpectedTime) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = viewModel::clearExpectedTime,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.settings_clear_time))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.settings_permissions),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            when (val permission = uiState.notificationsPermission) {
                PermissionUiState.NotApplicable -> {
                    PuntualElevatedCard {
                        Text(
                            text = stringResource(R.string.settings_permission_legacy),
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary,
                        )
                    }
                }
                is PermissionUiState.Applicable -> {
                    PermissionCard(
                        title = stringResource(R.string.settings_permission_notifications),
                        description = stringResource(R.string.settings_permission_notifications_desc),
                        granted = permission.granted,
                        onRequest = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Cerrar sesión")
            }
        }
    }

    if (uiState.showTimePicker) {
        TimePickerDialog(
            initialHour = uiState.expectedHour,
            initialMinute = uiState.expectedMinute,
            onDismiss = viewModel::dismissTimePicker,
            onConfirm = viewModel::onTimeSelected,
        )
    }

    if (uiState.showClosePeriodDialog) {
        ClosePeriodDialog(
            endDate = uiState.closePeriodEndDate,
            newStartDate = uiState.closePeriodNewStartDate,
            newTitle = uiState.closePeriodNewTitle,
            errorMessage = uiState.closePeriodError,
            isLoading = uiState.isClosingPeriod,
            onEndDateSelected = viewModel::onClosePeriodEndDateSelected,
            onNewStartDateSelected = viewModel::onClosePeriodNewStartDateSelected,
            onNewTitleChange = viewModel::onClosePeriodNewTitleChange,
            onDismiss = viewModel::dismissClosePeriodDialog,
            onConfirm = viewModel::confirmClosePeriod,
        )
    }

    if (uiState.showAbsenceDialog) {
        AbsenceDialog(
            startDate = uiState.absenceStartDate,
            endDate = uiState.absenceEndDate,
            type = uiState.absenceType,
            reason = uiState.absenceReason,
            errorMessage = uiState.absenceError,
            isLoading = uiState.isSavingAbsence,
            onStartDateSelected = viewModel::onAbsenceStartDateSelected,
            onEndDateSelected = viewModel::onAbsenceEndDateSelected,
            onTypeSelected = viewModel::onAbsenceTypeSelected,
            onReasonChange = viewModel::onAbsenceReasonChange,
            onDismiss = viewModel::dismissAbsenceDialog,
            onConfirm = viewModel::confirmAbsence,
        )
    }
}

@Composable
private fun AbsenceListItem(
    absence: AbsenceListItemUi,
    onDelete: (Long) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = absence.typeLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = absence.rangeLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
                Text(
                    text = "${absence.statusLabel} · ${absence.reason}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
            TextButton(onClick = { onDelete(absence.id) }) {
                Text("Quitar")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(timePickerState.hour, timePickerState.minute)
                },
            ) {
                Text(stringResource(R.string.settings_save_time))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
        text = {
            TimePicker(state = timePickerState)
        },
    )
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    granted: Boolean,
    onRequest: () -> Unit,
) {
    PuntualElevatedCard {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (granted) {
                stringResource(R.string.settings_permission_granted)
            } else {
                stringResource(R.string.settings_permission_denied)
            },
            color = if (granted) PuntualGreen else MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Medium,
        )
        if (!granted) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = onRequest, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_permission_request))
            }
        }
    }
}
