package com.example.puntual.ui.home

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.puntual.R
import com.example.puntual.ui.settings.MotivationalQuoteDialog
import com.example.puntual.ui.theme.LateOrange
import com.example.puntual.ui.theme.OnTimeGreen

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() }
    var pendingAction by remember { mutableStateOf<SensitiveHomeAction?>(null) }
    var pendingEditHour by remember { mutableStateOf(0) }
    var pendingEditMinute by remember { mutableStateOf(0) }
    var biometricError by remember { mutableStateOf<String?>(null) }
    val canUseBiometrics = remember(context) {
        BiometricManager.from(context).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG,
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }
    val promptInfo = remember {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("Autorizar acción")
            .setSubtitle("Confirma tu identidad para continuar en Puntuall")
            .setNegativeButtonText("Cancelar")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()
    }
    val biometricPrompt = remember(activity) {
        activity?.let { fragmentActivity ->
            BiometricPrompt(
                fragmentActivity,
                ContextCompat.getMainExecutor(fragmentActivity),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        when (pendingAction) {
                            SensitiveHomeAction.REGISTER_CHECK_IN ->
                                viewModel.onRegisterClick(identityVerified = true)
                            SensitiveHomeAction.SAVE_EDITED_TIME ->
                                viewModel.onEditTimeSelected(
                                    hour = pendingEditHour,
                                    minute = pendingEditMinute,
                                    identityVerified = true,
                                )
                            null -> Unit
                        }
                        pendingAction = null
                        biometricError = null
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        if (pendingAction == SensitiveHomeAction.SAVE_EDITED_TIME) {
                            viewModel.dismissEditTimePicker()
                        }
                        pendingAction = null
                        biometricError = errString.toString()
                    }

                    override fun onAuthenticationFailed() {
                        biometricError = "No se pudo validar la huella. Intenta de nuevo."
                    }
                },
            )
        }
    }

    when (val state = uiState) {
        HomeUiState.Loading -> {
            HomeScreenLayout(userDisplayName = "") {
                HomeLoadingContent()
            }
        }
        is HomeUiState.Ready -> {
            HomeScreenLayout(userDisplayName = state.userName) {
                fun authorizeOrRun(
                    action: SensitiveHomeAction,
                    hour: Int = 0,
                    minute: Int = 0,
                    fallback: () -> Unit,
                ) {
                    if (state.biometricUnlockEnabled && canUseBiometrics && biometricPrompt != null) {
                        pendingAction = action
                        pendingEditHour = hour
                        pendingEditMinute = minute
                        biometricError = null
                        biometricPrompt.authenticate(promptInfo)
                    } else {
                        fallback()
                    }
                }

                when {
                    !state.isWeekday -> WeekendCheckInCard(state)
                    state.alreadyCheckedInToday -> SuccessCheckInCard(
                        state = state,
                        viewModel = viewModel,
                    )
                    else -> PendingCheckInCard(
                        state = state,
                        onRegisterClick = {
                            authorizeOrRun(
                                action = SensitiveHomeAction.REGISTER_CHECK_IN,
                                fallback = viewModel::onRegisterClick,
                            )
                        },
                    )
                }
            }
            if (state.showEditTimePicker) {
                EditCheckInTimeDialog(
                    initialHour = state.editTimeHour,
                    initialMinute = state.editTimeMinute,
                    onDismiss = viewModel::dismissEditTimePicker,
                    onConfirm = { hour, minute ->
                        if (state.biometricUnlockEnabled && canUseBiometrics && biometricPrompt != null) {
                            pendingAction = SensitiveHomeAction.SAVE_EDITED_TIME
                            pendingEditHour = hour
                            pendingEditMinute = minute
                            biometricError = null
                            biometricPrompt.authenticate(promptInfo)
                        } else {
                            viewModel.onEditTimeSelected(hour, minute)
                        }
                    },
                )
            }
            if (state.showMotivationalDialog) {
                MotivationalQuoteDialog(
                    isLoading = state.motivationalQuoteLoading,
                    quoteText = state.motivationalQuoteText,
                    author = state.motivationalQuoteAuthor,
                    onDismiss = viewModel::dismissMotivationalDialog,
                )
            }
            biometricError?.let { error ->
                ActionBiometricErrorDialog(
                    message = error,
                    onDismiss = { biometricError = null },
                )
            }
        }
    }
}

@Composable
private fun PendingCheckInCard(
    state: HomeUiState.Ready,
    onRegisterClick: () -> Unit,
) {
    val primaryTime = if (state.hasExpectedTime) {
        state.expectedTimeLabel
    } else {
        stringResource(R.string.home_no_time_configured)
    }
    val subtitle = if (state.hasExpectedTime) {
        stringResource(R.string.home_card_subtitle)
    } else {
        stringResource(R.string.home_card_subtitle_no_time)
    }
    CheckInCard(
        primaryTimeLabel = primaryTime,
        subtitleLabel = subtitle,
        centerLine1 = stringResource(R.string.home_ready_message),
        centerLine2 = state.errorMessage,
        centerLine2Color = MaterialTheme.colorScheme.error,
        buttonText = stringResource(R.string.home_register),
        onButtonClick = onRegisterClick,
        buttonLoading = state.isRegistering,
    )
}

@Composable
private fun SuccessCheckInCard(
    state: HomeUiState.Ready,
    viewModel: HomeViewModel,
) {
    val delayColor = if (state.todayDelayLabel == "A tiempo") OnTimeGreen else LateOrange
    val delayLine = if (state.hasExpectedTime) {
        state.todayDelayLabel
    } else {
        stringResource(R.string.home_no_delay_without_time)
    }
    CheckInCard(
        primaryTimeLabel = state.todayTimeLabel.orEmpty(),
        subtitleLabel = stringResource(R.string.home_card_checked_subtitle),
        centerLine1 = stringResource(R.string.home_success_line1),
        centerLine2 = stringResource(R.string.home_success_line2),
        centerLine3 = delayLine,
        centerLine3Color = if (state.hasExpectedTime) delayColor else OnTimeGreen,
        buttonText = stringResource(R.string.home_accept),
        onButtonClick = {},
        secondaryButtonText = stringResource(R.string.home_edit_time),
        onSecondaryButtonClick = viewModel::openEditTimePicker,
        tertiaryButtonText = stringResource(R.string.home_view_quote),
        onTertiaryButtonClick = viewModel::onViewQuoteClick,
    )
}

@Composable
private fun ActionBiometricErrorDialog(
    message: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Validación requerida") },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Aceptar")
            }
        },
    )
}

@Composable
private fun WeekendCheckInCard(state: HomeUiState.Ready) {
    val primaryTime = if (state.hasExpectedTime) {
        state.expectedTimeLabel
    } else {
        stringResource(R.string.home_no_time_configured)
    }
    CheckInCard(
        primaryTimeLabel = primaryTime,
        subtitleLabel = stringResource(R.string.home_card_subtitle),
        centerLine1 = stringResource(R.string.home_weekend_message),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditCheckInTimeDialog(
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
        title = { Text(stringResource(R.string.home_edit_time_title)) },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(timePickerState.hour, timePickerState.minute) },
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
            Text(
                text = stringResource(R.string.home_edit_time_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            TimePicker(state = timePickerState)
        },
    )
}

private enum class SensitiveHomeAction {
    REGISTER_CHECK_IN,
    SAVE_EDITED_TIME,
}

private tailrec fun Context.findFragmentActivity(): FragmentActivity? =
    when (this) {
        is FragmentActivity -> this
        is ContextWrapper -> baseContext.findFragmentActivity()
        else -> null
    }
