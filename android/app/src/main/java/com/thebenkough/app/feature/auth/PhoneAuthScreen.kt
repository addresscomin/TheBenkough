package com.thebenkough.app.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thebenkough.app.data.remote.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private val AuthBackground = Color(0xFF090A0D)
private val AuthPanel = Color(0xFF14171D)
private val AuthText = Color(0xFFF5F3EE)
private val AuthMuted = Color(0xFF9BA3AF)
private val AuthAccent = Color(0xFFC9E879)
private val usernamePattern = Regex("^[a-z0-9_]{3,20}$")
private val emailPattern = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

@Composable
fun AccountAuthScreen(onAuthenticated: () -> Unit) {
    var isCreating by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var messageIsSuccess by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = AuthText,
        unfocusedTextColor = AuthText,
        focusedContainerColor = AuthPanel,
        unfocusedContainerColor = AuthPanel,
        focusedBorderColor = AuthAccent,
        unfocusedBorderColor = Color(0xFF343B45),
        focusedLabelColor = AuthAccent,
        unfocusedLabelColor = AuthMuted,
        cursorColor = AuthAccent
    )

    Surface(modifier = Modifier.fillMaxSize(), color = AuthBackground) {
        Column(
            modifier = Modifier.fillMaxSize().background(AuthBackground)
                .padding(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "THE BENKOUGH",
                color = AuthText,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 3.sp
            )
            Spacer(Modifier.height(9.dp))
            Text(
                "CREATIVE BUSINESS WORKSPACE",
                color = AuthMuted,
                fontSize = 9.sp,
                letterSpacing = 1.8.sp
            )
            Spacer(Modifier.height(38.dp))
            Text(
                if (isCreating) "Make room\nfor your work." else "Good to have\nyou back.",
                color = AuthText,
                fontSize = 36.sp,
                lineHeight = 40.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp
            )
            Spacer(Modifier.height(9.dp))
            Text(
                if (isCreating) "Create a secure account to set up your private workspace."
                else "Sign in to continue to your private workspace.",
                color = AuthMuted,
                fontSize = 13.sp,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(25.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it.trimStart(); message = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Email address") },
                placeholder = { Text("you@example.com", color = AuthMuted.copy(alpha = .65f)) },
                singleLine = true,
                enabled = !busy,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = fieldColors,
                shape = RoundedCornerShape(14.dp)
            )
            if (isCreating) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it.lowercase().filter { ch -> ch.isLetterOrDigit() || ch == '_' }.take(20)
                        message = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Workspace username") },
                    placeholder = { Text("3–20 letters, numbers or _", color = AuthMuted.copy(alpha = .65f)) },
                    singleLine = true,
                    enabled = !busy,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    colors = fieldColors,
                    shape = RoundedCornerShape(14.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; message = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Password") },
                singleLine = true,
                enabled = !busy,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = fieldColors,
                shape = RoundedCornerShape(14.dp)
            )
            if (isCreating) {
                Spacer(Modifier.height(7.dp))
                Text("Use at least 8 characters. Letters, numbers and symbols are accepted.", color = AuthMuted, fontSize = 10.sp)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; message = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Confirm password") },
                    singleLine = true,
                    enabled = !busy,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = fieldColors,
                    shape = RoundedCornerShape(14.dp)
                )
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    val cleanEmail = email.trim().lowercase()
                    val cleanUsername = username.trim().lowercase()
                    when {
                        !emailPattern.matches(cleanEmail) ->
                            message = "Enter a valid email address."
                        isCreating && !usernamePattern.matches(cleanUsername) ->
                            message = "Username must be 3–20 characters: letters, numbers or underscore."
                        password.length < 8 ->
                            message = "Password must contain at least 8 characters."
                        isCreating && password != confirmPassword ->
                            message = "The passwords do not match."
                        else -> scope.launch {
                            busy = true
                            message = null
                            messageIsSuccess = false
                            try {
                                val auth = SupabaseProvider.client.auth
                                if (isCreating) {
                                    auth.signUpWith(Email) {
                                        email = cleanEmail
                                        this.password = password
                                        data = buildJsonObject {
                                            put("username", cleanUsername)
                                            put("display_name", cleanUsername)
                                        }
                                    }
                                    if (auth.currentSessionOrNull() != null) {
                                        onAuthenticated()
                                    } else {
                                        isCreating = false
                                        password = ""
                                        confirmPassword = ""
                                        messageIsSuccess = true
                                        message = "Account request received. Check your email and confirm the address, then sign in here."
                                    }
                                } else {
                                    auth.signInWith(Email) {
                                        email = cleanEmail
                                        this.password = password
                                    }
                                    if (auth.currentSessionOrNull() != null) {
                                        onAuthenticated()
                                    } else {
                                        message = "Sign-in did not create an active session. Please try again."
                                    }
                                }
                            } catch (error: Exception) {
                                val raw = error.message.orEmpty()
                                message = when {
                                    raw.contains("already registered", ignoreCase = true) ->
                                        "This email is already registered. Sign in instead."
                                    raw.contains("invalid login", ignoreCase = true) ||
                                        raw.contains("invalid_credentials", ignoreCase = true) ->
                                        "Email or password is incorrect."
                                    raw.contains("email not confirmed", ignoreCase = true) ->
                                        "Confirm your email from the inbox before signing in."
                                    raw.contains("password", ignoreCase = true) ->
                                        "Password was not accepted. Check the requirements and try again."
                                    else -> "We couldn't complete that request. Check your connection and try again."
                                }
                            } finally {
                                busy = false
                            }
                        }
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AuthAccent,
                    contentColor = Color(0xFF11150B),
                    disabledContainerColor = AuthAccent.copy(alpha = .5f)
                )
            ) {
                Text(
                    if (busy) "PLEASE WAIT…" else if (isCreating) "CREATE ACCOUNT" else "SIGN IN",
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
            }

            Spacer(Modifier.height(17.dp))
            Text(
                if (isCreating) "Already have an account?  Sign in" else "New to Benkough?  Create account",
                color = AuthAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth().clickable(enabled = !busy) {
                    isCreating = !isCreating
                    password = ""
                    confirmPassword = ""
                    message = null
                    messageIsSuccess = false
                },
                textAlign = TextAlign.Center
            )
            message?.let {
                Spacer(Modifier.height(16.dp))
                Text(
                    it,
                    color = if (messageIsSuccess) AuthAccent else Color(0xFFFFB4A9),
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
            Spacer(Modifier.height(30.dp))
            Text(
                "YOUR ACCOUNT IS SECURED BY SUPABASE AUTH. NEVER SHARE YOUR PASSWORD OR EMAIL VERIFICATION LINK.",
                color = AuthMuted.copy(alpha = .65f),
                fontSize = 8.sp,
                lineHeight = 13.sp,
                letterSpacing = .6.sp
            )
        }
    }
}
