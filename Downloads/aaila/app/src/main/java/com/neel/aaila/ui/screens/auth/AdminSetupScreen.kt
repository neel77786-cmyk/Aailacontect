package com.neel.aaila.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.FamilyRestroom
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neel.aaila.R
import com.neel.aaila.ui.components.Phase1NoticeBanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSetupScreen(
    viewModel: AuthViewModel,
    onBackToLogin: () -> Unit,
    onContinueToPreviewDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.adminSetupState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.admin_setup_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackToLogin,
                        modifier = Modifier.testTag("admin_setup_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 580.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "First-Time Admin Setup",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.admin_setup_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.88f)
                            )
                        }
                    }
                }

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.familyName,
                            onValueChange = viewModel::onFamilyNameChanged,
                            label = { Text(stringResource(R.string.label_family_name)) },
                            placeholder = { Text(stringResource(R.string.hint_family_name)) },
                            leadingIcon = { Icon(Icons.Rounded.FamilyRestroom, contentDescription = null) },
                            isError = uiState.familyNameError != null,
                            supportingText = uiState.familyNameError?.let { { Text(it) } },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_family_name_input")
                        )

                        OutlinedTextField(
                            value = uiState.adminName,
                            onValueChange = viewModel::onAdminNameChanged,
                            label = { Text(stringResource(R.string.label_admin_name)) },
                            placeholder = { Text(stringResource(R.string.hint_admin_name)) },
                            leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                            isError = uiState.adminNameError != null,
                            supportingText = uiState.adminNameError?.let { { Text(it) } },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_name_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = uiState.familyId,
                                onValueChange = viewModel::onSetupFamilyIdChanged,
                                label = { Text(stringResource(R.string.label_create_family_id)) },
                                placeholder = { Text(stringResource(R.string.hint_family_id)) },
                                leadingIcon = { Icon(Icons.Rounded.Badge, contentDescription = null) },
                                isError = uiState.familyIdError != null,
                                supportingText = uiState.familyIdError?.let { { Text(it) } },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Characters,
                                    imeAction = ImeAction.Next
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("admin_family_id_input")
                            )

                            FilledTonalButton(
                                onClick = viewModel::generateSuggestedFamilyId,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .height(52.dp)
                                    .testTag("admin_generate_id_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = uiState.password,
                            onValueChange = viewModel::onSetupPasswordChanged,
                            label = { Text(stringResource(R.string.label_password)) },
                            leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = viewModel::toggleSetupPasswordVisibility) {
                                    Icon(
                                        imageVector = if (uiState.passwordVisible) {
                                            Icons.Rounded.VisibilityOff
                                        } else {
                                            Icons.Rounded.Visibility
                                        },
                                        contentDescription = stringResource(R.string.cd_toggle_password)
                                    )
                                }
                            },
                            visualTransformation = if (uiState.passwordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            isError = uiState.passwordError != null,
                            supportingText = uiState.passwordError?.let { { Text(it) } },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_password_input")
                        )

                        OutlinedTextField(
                            value = uiState.confirmPassword,
                            onValueChange = viewModel::onSetupConfirmPasswordChanged,
                            label = { Text(stringResource(R.string.label_confirm_password)) },
                            leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                            visualTransformation = if (uiState.passwordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            isError = uiState.confirmPasswordError != null,
                            supportingText = uiState.confirmPasswordError?.let { { Text(it) } },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_confirm_password_input")
                        )
                    }
                }

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Family Privacy Controls",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.label_approval_toggle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp)
                            )
                            Switch(
                                checked = uiState.requireAdminApproval,
                                onCheckedChange = viewModel::onRequireAdminApprovalChanged,
                                modifier = Modifier.testTag("admin_approval_switch")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.label_directory_toggle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp)
                            )
                            Switch(
                                checked = uiState.privateDirectoryOnly,
                                onCheckedChange = viewModel::onPrivateDirectoryChanged,
                                modifier = Modifier.testTag("admin_directory_switch")
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = uiState.backendNotice != null) {
                    Phase1NoticeBanner(
                        title = "Registration Notice",
                        message = uiState.backendNotice.orEmpty(),
                        isWarningTone = true
                    )
                }

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.validateAndSubmitAdminSetup(onSuccess = onContinueToPreviewDashboard)
                    },
                    enabled = !uiState.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("admin_setup_submit_button"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = if (uiState.isLoading) "Creating Family Space..." else stringResource(R.string.action_create_family_preview),
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                if (uiState.readyForPreview) {
                    OutlinedButton(
                        onClick = onContinueToPreviewDashboard,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("admin_setup_enter_dashboard_button"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "Open Family Dashboard Preview",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                TextButton(
                    onClick = onBackToLogin,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(text = stringResource(R.string.action_back_to_login))
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
