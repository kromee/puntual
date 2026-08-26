package com.example.puntual.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.puntual.domain.model.AbsenceType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AbsenceDialog(
    startDate: LocalDate,
    endDate: LocalDate,
    type: AbsenceType,
    reason: String,
    errorMessage: String?,
    isLoading: Boolean,
    onStartDateSelected: (LocalDate) -> Unit,
    onEndDateSelected: (LocalDate) -> Unit,
    onTypeSelected: (AbsenceType) -> Unit,
    onReasonChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var showTypeMenu by remember { mutableStateOf(false) }

    if (showStartPicker) {
        AbsenceDatePickerDialog(
            initialDate = startDate,
            onDismiss = { showStartPicker = false },
            onConfirm = { date ->
                onStartDateSelected(date)
                showStartPicker = false
            },
        )
    }
    if (showEndPicker) {
        AbsenceDatePickerDialog(
            initialDate = endDate,
            onDismiss = { showEndPicker = false },
            onConfirm = { date ->
                onEndDateSelected(date)
                showEndPicker = false
            },
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar ausencia") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                Text(
                    text = "Marca días sin check-in para que aparezcan como justificados en el historial.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { showTypeMenu = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                ) {
                    Text(type.label)
                }
                DropdownMenu(
                    expanded = showTypeMenu,
                    onDismissRequest = { showTypeMenu = false },
                ) {
                    AbsenceType.values().forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label) },
                            onClick = {
                                onTypeSelected(option)
                                showTypeMenu = false
                            },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showStartPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                ) {
                    Text("Inicio: ${formatAbsenceDate(startDate)}")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showEndPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                ) {
                    Text("Fin: ${formatAbsenceDate(endDate)}")
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = onReasonChange,
                    label = { Text("Motivo / comentario") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    maxLines = 3,
                )
            }
        },
        confirmButton = {
            TextButton(enabled = !isLoading, onClick = onConfirm) {
                Text(if (isLoading) "Guardando..." else "Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancelar")
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AbsenceDatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    val initialMillis = initialDate
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = state.selectedDateMillis ?: return@TextButton
                    val date = Instant.ofEpochMilli(millis)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    onConfirm(date)
                },
            ) {
                Text("Aceptar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
    ) {
        DatePicker(state = state)
    }
}

private fun formatAbsenceDate(date: LocalDate): String =
    "${date.dayOfMonth}/${date.monthValue}/${date.year}"
