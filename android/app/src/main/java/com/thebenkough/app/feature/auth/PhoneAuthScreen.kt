package com.thebenkough.app.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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

private val AuthBackground = Color(0xFF050608)
private val AuthPanel = Color(0xCC151D27)
private val AuthText = Color(0xFFF4F7FB)
private val AuthMuted = Color(0xFF929EAD)
private val AuthAccent = Color(0xFF77E6D2)
private val AuthViolet = Color(0xFF9B8CFF)
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
        focusedTextColor = AuthText, unfocusedTextColor = AuthText,
        focusedContainerColor = Color(0xFF0C1118), unfocusedContainerColor = Color(0xFF0C1118),
        focusedBorderColor = AuthAccent, unfocusedBorderColor = Color(0xFF34404D),
        focusedLabelColor = AuthAccent, unfocusedLabelColor = AuthMuted, cursorColor = AuthAccent
    )

    Surface(Modifier.fillMaxSize(), color = AuthBackground) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.radialGradient(listOf(Color(0xFF18323A), Color(0xFF111326), AuthBackground), radius = 1050f)
            )
        ) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    Modifier.height(76.dp).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            Modifier.background(Brush.linearGradient(listOf(Color(0xFF234B50), Color(0xFF30265A))), RoundedCornerShape(22.dp))
                                .border(1.dp, Color(0x8877E6D2), RoundedCornerShape(22.dp)).padding(15.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = "The Benkough logo", tint = AuthAccent, modifier = Modifier.height(34.dp))
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text("THE BENKOUGH", color = AuthText, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 3.sp)
                Text("CREATIVE OPERATING SYSTEM", color = AuthMuted, fontSize = 9.sp, letterSpacing = 2.sp)
                Spacer(Modifier.height(28.dp))

                Column(
                    Modifier.fillMaxWidth().background(AuthPanel, RoundedCornerShape(30.dp))
                        .border(1.dp, Brush.linearGradient(listOf(Color(0x8877E6D2), Color(0x22FFFFFF), Color(0x889B8CFF))), RoundedCornerShape(30.dp))
                        .padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(13.dp)
                ) {
                    Text(if (isCreating) "CREATE YOUR SPACE" else "WELCOME BACK", color = AuthAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text(
                        if (isCreating) "Build your\nworkspace." else "Your work.\nOne home.",
                        color = AuthText, fontSize = 34.sp, lineHeight = 37.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp
                    )
                    Text(
                        if (isCreating) "Set up a secure account for your private workspace."
                        else "Sign in to continue where your ideas take shape.",
                        color = AuthMuted, fontSize = 12.sp, lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(3.dp))
                    OutlinedTextField(
                        value = email, onValueChange = { email = it.trimStart(); message = null },
                        modifier = Modifier.fillMaxWidth(), label = { Text("Email address") },
                        placeholder = { Text("you@example.com", color = AuthMuted.copy(alpha = .65f)) },
                        singleLine = true, enabled = !busy,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = fieldColors, shape = RoundedCornerShape(16.dp)
                    )
                    if (isCreating) {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it.lowercase().filter { ch -> ch.isLetterOrDigit() || ch == '_' }.take(20); message = null },
                            modifier = Modifier.fillMaxWidth(), label = { Text("Workspace username") },
                            placeholder = { Text("3–20 letters, numbers or _", color = AuthMuted.copy(alpha = .65f)) },
                            singleLine = true, enabled = !busy,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                            colors = fieldColors, shape = RoundedCornerShape(16.dp)
                        )
                    }
                    OutlinedTextField(
                        value = password, onValueChange = { password = it; message = null },
                        modifier = Modifier.fillMaxWidth(), label = { Text("Password") },
                        singleLine = true, enabled = !busy,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = fieldColors, shape = RoundedCornerShape(16.dp)
                    )
                    if (isCreating) {
                        Text("At least 8 characters.", color = AuthMuted, fontSize = 10.sp)
                        OutlinedTextField(
                            value = confirmPassword, onValueChange = { confirmPassword = it; message = null },
                            modifier = Modifier.fillMaxWidth(), label = { Text("Confirm password") },
                            singleLine = true, enabled = !busy,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            colors = fieldColors, shape = RoundedCornerShape(16.dp)
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Button(
                        onClick = {
                            val cleanEmail = email.trim().lowercase()
                            val cleanUsername = username.trim().lowercase()
                            when {
                                !emailPattern.matches(cleanEmail) -> message = "Enter a valid email address."
                                isCreating && !usernamePattern.matches(cleanUsername) -> message = "Username must be 3–20 characters: letters, numbers or underscore."
                                password.length < 8 -> message = "Password must contain at least 8 characters."
                                isCreating && password != confirmPassword -> message = "The passwords do not match."
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
                                            if (auth.currentSessionOrNull() != null) onAuthenticated()
                                            else {
                                                isCreating = false
                                                password = ""
                                                confirmPassword = ""
                                                messageIsSuccess = true
                                                message = "Account request received. Check your email and confirm the address, then sign in here."
                                            }
                                        } else {
                                            auth.signInWith(Email) { email = cleanEmail; this.password = password }
                                            if (auth.currentSessionOrNull() != null) onAuthenticated()
                                            else message = "Sign-in did not create an active session. Please try again."
                                        }
                                    } catch (error: Exception) {
                                        val raw = error.message.orEmpty()
                                        message = when {
                                            raw.contains("already registered", true) -> "This email is already registered. Sign in instead."
                                            raw.contains("invalid login", true) || raw.contains("invalid_credentials", true) -> "Email or password is incorrect."
                                            raw.contains("email not confirmed", true) -> "Confirm your email from the inbox before signing in."
                                            raw.contains("password", true) -> "Password was not accepted. Check the requirements and try again."
                                            else -> "We couldn't complete that request. Check your connection and try again."
                                        }
                                    } finally { busy = false }
                                }
                            }
                        },
                        enabled = !busy, modifier = Modifier.fillMaxWidth().height(58.dp),
                        shape = RoundedCornerShape(17.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AuthAccent, contentColor = Color(0xFF071310),
                            disabledContainerColor = AuthAccent.copy(alpha = .5f)
                        )
                    ) {
                        Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.height(19.dp))
                        Spacer(Modifier.height(0.dp))
                        Text(if (busy) "PLEASE WAIT…" else if (isCreating) "CREATE ACCOUNT" else "SIGN IN SECURELY", fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                    }
                    Text(
                        if (isCreating) "Already have an account?  Sign in" else "New to Benkough?  Create account",
                        color = AuthAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth().clickable(enabled = !busy) {
                            isCreating = !isCreating
                            password = ""; confirmPassword = ""; message = null; messageIsSuccess = false
                        },
                        textAlign = TextAlign.Center
                    )
                    message?.let {
                        Text(it, color = if (messageIsSuccess) AuthAccent else Color(0xFFFFA99D), fontSize = 12.sp, lineHeight = 18.sp)
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text("PRIVATE BY DESIGN  ·  SECURED WITH SUPABASE AUTH", color = AuthMuted.copy(alpha = .7f), fontSize = 8.sp, letterSpacing = 1.sp, textAlign = TextAlign.Center)
            }
        }
    }
}
