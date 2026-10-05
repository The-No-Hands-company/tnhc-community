package com.tnhc.community.ui

import androidx.compose.foundation.layout.*
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.tnhc.community.data.CommunityRepository
import com.tnhc.community.data.MemberProfile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun AccountScreen(repository: CommunityRepository) {
    val session by repository.session.collectAsState()
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    var profile by remember { mutableStateOf<MemberProfile?>(null) }
    var displayName by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var interests by remember { mutableStateOf("") }
    var visibility by remember { mutableStateOf("members") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showSignInPassword by remember { mutableStateOf(false) }
    var newPassword by remember { mutableStateOf("") }
    var showNewPassword by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var profileAttempt by remember { mutableIntStateOf(0) }

    LaunchedEffect(session?.userId, profileAttempt) {
        profile = null
        if (session != null) {
            try {
                repository.loadProfile().also {
                    profile = it
                    displayName = it.displayName.orEmpty()
                    bio = it.bio.orEmpty()
                    interests = it.interests.joinToString(", ")
                    visibility = it.visibility
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { message = "Your profile could not load. Check your connection and try again." }
        }
    }

    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Your TNHC account", style = MaterialTheme.typography.headlineMedium)
        Text("Meet people through the things you care about.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (session == null) {
            Text("New accounts require an invitation from TNHC.")
            OutlinedTextField(email, { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("account-email"))
            OutlinedTextField(
                password,
                { password = it },
                label = { Text("Password") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                visualTransformation = if (showSignInPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    PasswordVisibilityToggle(
                        visible = showSignInPassword,
                        testTag = "account-password-visibility-toggle",
                        onToggle = { showSignInPassword = !showSignInPassword },
                    )
                },
                modifier = Modifier.fillMaxWidth().testTag("account-password"),
            )
            Button(enabled = !busy && email.isNotBlank() && password.isNotBlank(), onClick = {
                busy = true; message = null
                scope.launch {
                    try { repository.signIn(email.trim(), password); keyboard?.hide() }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { message = "Sign-in failed. Check your details or ask TNHC for an invitation." }
                    finally { busy = false }
                }
            }, modifier = Modifier.fillMaxWidth().testTag("account-sign-in")) { Text(if (busy) "Signing in…" else "Sign in") }
        } else if (profile == null) {
            if (message == null) CircularProgressIndicator() else {
                Text(message!!, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { message = null; profileAttempt++ }) { Text("Try again") }
            }
        } else {
            Text("@${profile!!.handle}", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(displayName, { displayName = it }, label = { Text("Display name") }, modifier = Modifier.fillMaxWidth().testTag("account-display-name"))
            OutlinedTextField(bio, { bio = it }, label = { Text("About you") }, minLines = 3, modifier = Modifier.fillMaxWidth().testTag("account-bio"))
            OutlinedTextField(interests, { interests = it }, label = { Text("Interests") }, supportingText = { Text("Separate interests with commas") }, modifier = Modifier.fillMaxWidth().testTag("account-interests"))
            Text("Profile visibility", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(visibility == "members", { visibility = "members" }, label = { Text("Members") })
                FilterChip(visibility == "private", { visibility = "private" }, label = { Text("Private") })
            }
            Button(enabled = !busy, onClick = {
                busy = true; message = null
                scope.launch {
                    try {
                        repository.updateProfile(displayName, bio, interests.split(',').map(String::trim).filter(String::isNotEmpty).distinct(), visibility)
                        keyboard?.hide()
                        message = "Profile saved."
                    } catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { message = "Your profile could not be saved. Check your connection and try again." }
                    finally { busy = false }
                }
            }, modifier = Modifier.fillMaxWidth().testTag("account-save")) { Text("Save profile") }
            HorizontalDivider()
            Text("Password", style = MaterialTheme.typography.titleLarge)
            Text("Choose at least 12 characters.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                newPassword,
                { newPassword = it },
                label = { Text("New password") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                visualTransformation = if (showNewPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    PasswordVisibilityToggle(
                        visible = showNewPassword,
                        testTag = "account-new-password-visibility-toggle",
                        onToggle = { showNewPassword = !showNewPassword },
                    )
                },
                modifier = Modifier.fillMaxWidth().testTag("account-new-password"),
            )
            OutlinedButton(enabled = !busy && newPassword.length >= 12, onClick = {
                busy = true; message = null
                scope.launch {
                    try { repository.setPassword(newPassword); keyboard?.hide(); newPassword = ""; message = "Password updated." }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { message = "Password could not be updated. Check your connection and try again." }
                    finally { busy = false }
                }
            }, modifier = Modifier.fillMaxWidth().testTag("account-set-password")) { Text("Update password") }
            OutlinedButton(enabled = !busy, onClick = {
                busy = true; message = null
                scope.launch {
                    try { repository.signOut(); keyboard?.hide() }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { message = "Sign-out failed. Please try again." }
                    finally { busy = false }
                }
            }, modifier = Modifier.fillMaxWidth().testTag("account-sign-out")) { Text("Sign out") }
        }
    }
        message?.let { feedback ->
            Snackbar(Modifier.align(Alignment.TopCenter).padding(horizontal = 16.dp, vertical = 8.dp)) { Text(feedback) }
        }
    }
}

@Composable
private fun PasswordVisibilityToggle(visible: Boolean, testTag: String, onToggle: () -> Unit) {
    IconButton(onClick = onToggle, modifier = Modifier.testTag(testTag)) {
        Icon(
            imageVector = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
            contentDescription = if (visible) "Hide password" else "Show password",
        )
    }
}
