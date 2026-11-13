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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
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

private val AuthInk = Color(0xFF050607)
private val AuthWhite = Color(0xFFFFFFFF)
private val AuthMuted = Color(0xFFB9C2CC)
private val AuthLime = Color(0xFFB7E36A)
private val usernamePattern = Regex("^[a-z0-9_]{3,20}$")

private fun authEmailFor(username: String): String =
    "${username.lowercase()}@users.thebenkough.app"

@Composable
fun AccountAuthScreen(onAuthenticated: () -> Unit) {
    var isCreating by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = AuthWhite,
        unfocusedTextColor = AuthWhite,
        focusedBorderColor = AuthLime,
        unfocusedBorderColor = AuthMuted.copy(alpha = .45f),
        focusedLabelColor = AuthLime,
        unfocusedLabelColor = AuthMuted,
        cursorColor = AuthLime
    )

    Surface(modifier = Modifier.fillMaxSize(), color = AuthInk) {
        Column(
            modifier = Modifier.fillMaxSize().background(AuthInk)
                .padding(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("THE BENKOUGH®", color = AuthWhite, fontSize = 14.sp,
                fontWeight = FontWeight.Black, fontFamily = FontFamily.SansSerif, letterSpacing = 2.sp)
            Spacer(Modifier.height(22.dp))
            Text(if (isCreating) "CREATE YOUR\nWORKSPACE." else "YOUR WORKSPACE.\nYOUR ACCESS.",
                color = AuthWhite, fontSize = 32.sp, lineHeight = 37.sp,
                fontWeight = FontWeight.Black, fontFamily = FontFamily.SansSerif)
            Spacer(Modifier.height(10.dp))
            Text("Sign in with your username and password. Your phone number is not required.",
                color = AuthMuted, fontSize = 13.sp, lineHeight = 20.sp)
            Spacer(Modifier.height(22.dp))
            OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it.lowercase().filter { ch -> ch.isLetterOrDigit() || ch == '_' }.take(20)
                    message = null
                },
                modifier = Modifier.fillMaxWidth(), label = { Text("Username") },
                placeholder = { Text("your_username", color = AuthMuted.copy(alpha = .65f)) },
                singleLine = true, enabled = !busy,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii), colors = fieldColors
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password, onValueChange = { password = it; message = null },
                modifier = Modifier.fillMaxWidth(), label = { Text("Password") },
                singleLine = true, enabled = !busy,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), colors = fieldColors
            )
            if (isCreating) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = confirmPassword, onValueChange = { confirmPassword = it; message = null },
                    modifier = Modifier.fillMaxWidth(), label = { Text("Confirm password") },
                    singleLine = true, enabled = !busy,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), colors = fieldColors
                )
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    val cleanUsername = username.trim().lowercase()
                    when {
                        !usernamePattern.matches(cleanUsername) ->
                            message = "Username 3–20 characters ka ho: letters, numbers ya underscore."
                        password.length < 10 -> message = "Password kam se kam 10 characters ka rakho."
                        isCreating && password != confirmPassword -> message = "Dono passwords match nahi karte."
                        else -> scope.launch {
                            busy = true
                            message = null
                            try {
                                val auth = SupabaseProvider.client.auth
                                if (isCreating) {
                                    auth.signUpWith(Email) {
                                        email = authEmailFor(cleanUsername)
                                        this.password = password
                                        data = buildJsonObject {
                                            put("username", cleanUsername)
                                            put("display_name", cleanUsername)
                                        }
                                    }
                                    check(auth.currentSessionOrNull() != null) {
                                        "Signup received, but Supabase Confirm email is still enabled. Turn it off before using username-only accounts."
                                    }
                                } else {
                                    auth.loginWith(Email) {
                                        email = authEmailFor(cleanUsername)
                                        this.password = password
                                    }
                                    check(auth.currentSessionOrNull() != null) { "Sign-in did not return an active session." }
                                }
                                onAuthenticated()
                            } catch (error: Exception) {
                                val raw = error.message.orEmpty()
                                message = when {
                                    raw.contains("already registered", ignoreCase = true) -> "Username already taken. Choose another one."
                                    raw.contains("invalid login", ignoreCase = true) ||
                                        raw.contains("invalid_credentials", ignoreCase = true) -> "Username ya password sahi nahi hai."
                                    else -> raw.take(150).ifBlank { "Request complete nahi hui. Dobara try karo." }
                                }
                            } finally { busy = false }
                        }
                    }
                },
                enabled = !busy, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(3.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AuthLime, contentColor = AuthInk)
            ) {
                Text(if (busy) "PLEASE WAIT…" else if (isCreating) "CREATE ACCOUNT  ↗" else "SIGN IN  ↗",
                    fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            }
            Spacer(Modifier.height(14.dp))
            Text(
                if (isCreating) "ALREADY HAVE AN ACCOUNT?  SIGN IN" else "NEW TO BENKOUGH?  CREATE ACCOUNT",
                color = AuthLime, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp,
                modifier = Modifier.fillMaxWidth().clickable(enabled = !busy) {
                    isCreating = !isCreating
                    password = ""
                    confirmPassword = ""
                    message = null
                }, textAlign = TextAlign.Center
            )
            message?.let {
                Spacer(Modifier.height(14.dp))
                Text(it, color = AuthLime, fontSize = 10.sp, lineHeight = 16.sp)
            }
            Spacer(Modifier.height(18.dp))
            Text(
                "SUPABASE AUTH PASSWORD SESSION. PHONE OTP IS NOT USED. PASSWORD RECOVERY IS UNAVAILABLE UNTIL A VERIFIED RECOVERY EMAIL IS ADDED.",
                color = AuthMuted.copy(alpha = .7f), fontSize = 8.sp, lineHeight = 13.sp
            )
        }
    }
}
