package com.thebenkough.app.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import io.github.jan.supabase.auth.providers.builtin.Phone
import kotlinx.coroutines.launch
import java.security.SecureRandom

private val AuthInk = Color(0xFF050607)
private val AuthPanel = Color(0xFF111820)
private val AuthWhite = Color(0xFFFFFFFF)
private val AuthMuted = Color(0xFFB9C2CC)
private val AuthLime = Color(0xFFB7E36A)
private val CodeAlphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789"
private val SecureRandomSource = SecureRandom()

private fun createAccessCode(length: Int = 16): String =
    buildString(length) {
        repeat(length) { append(CodeAlphabet[SecureRandomSource.nextInt(CodeAlphabet.length)]) }
    }

private fun normalizeIndianPhone(input: String): String? {
    val digits = input.filter(Char::isDigit)
    if (digits.length == 10) return "+91$digits"
    if (digits.length == 12 && digits.startsWith("91")) return "+$digits"
    return null
}

@Composable
fun PhoneAuthScreen(onAuthenticated: () -> Unit) {
    var createAccount by remember { mutableStateOf(false) }
    var phone by remember { mutableStateOf("") }
    var accessCode by remember { mutableStateOf("") }
    var issuedCode by remember { mutableStateOf<String?>(null) }
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
            modifier = Modifier.fillMaxSize().background(AuthInk).padding(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "THE BENKOUGH®",
                color = AuthWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(22.dp))
            Text(
                if (createAccount) "CREATE YOUR\nSECURE ACCESS." else "YOUR WORKSPACE.\nYOUR ACCESS.",
                color = AuthWhite,
                fontSize = 32.sp,
                lineHeight = 37.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Use your phone number and private access code. No SMS is sent. Your code is your account password.",
                color = AuthMuted,
                fontSize = 13.sp,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(22.dp))
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it.filter { ch -> ch.isDigit() || ch == '+' }.take(13) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Phone number (India)") },
                placeholder = { Text("98765 43210", color = AuthMuted.copy(alpha = .65f)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                colors = fieldColors
            )
            Spacer(Modifier.height(12.dp))
            if (!createAccount) {
                OutlinedTextField(
                    value = accessCode,
                    onValueChange = { accessCode = it.take(64) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Private access code") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = fieldColors
                )
            } else {
                Text(
                    "A unique 16-character code will be generated on this device and used as your Supabase account password. Save it safely; THE BENKOUGH cannot recover it without a verified recovery method.",
                    color = AuthMuted,
                    fontSize = 11.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.fillMaxWidth().background(AuthPanel, RoundedCornerShape(8.dp)).padding(13.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    val normalizedPhone = normalizeIndianPhone(phone)
                    if (normalizedPhone == null) {
                        message = "Enter a valid 10-digit Indian mobile number."
                    } else if (!createAccount && accessCode.length < 12) {
                        message = "Enter your complete private access code."
                    } else {
                        scope.launch {
                            busy = true
                            message = null
                            issuedCode = null
                            try {
                                if (createAccount) {
                                    val newCode = createAccessCode()
                                    SupabaseProvider.client.auth.signUpWith(Phone) {
                                        phone = normalizedPhone
                                        password = newCode
                                    }
                                    if (SupabaseProvider.client.auth.currentSessionOrNull() == null) {
                                        message = "Account created, but Supabase has phone confirmation enabled. Turn off Confirm phone to use this no-SMS flow."
                                    } else {
                                        issuedCode = newCode
                                    }
                                } else {
                                    SupabaseProvider.client.auth.signInWith(Phone) {
                                        phone = normalizedPhone
                                        password = accessCode
                                    }
                                }
                                if (!createAccount) onAuthenticated()
                            } catch (error: Exception) {
                                message = when {
                                    error.message.orEmpty().contains("already", ignoreCase = true) ->
                                        "This phone number already has an account. Switch to Sign in."
                                    error.message.orEmpty().contains("phone", ignoreCase = true) &&
                                        error.message.orEmpty().contains("confirm", ignoreCase = true) ->
                                        "Phone confirmation is enabled in Supabase. Disable Confirm phone for this no-SMS password flow."
                                    else -> error.message ?: "Could not complete account access."
                                }
                            } finally {
                                busy = false
                            }
                        }
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AuthLime,
                    contentColor = Color(0xFF101318),
                    disabledContainerColor = AuthLime.copy(alpha = .55f)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    when {
                        busy -> "PLEASE WAIT…"
                        createAccount -> "CREATE SECURE ACCOUNT"
                        else -> "SIGN IN SECURELY"
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = .8.sp
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (createAccount) "Already registered?  " else "New to THE BENKOUGH?  ",
                    color = AuthMuted,
                    fontSize = 12.sp
                )
                Text(
                    if (createAccount) "SIGN IN" else "CREATE ACCOUNT",
                    color = AuthLime,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 2.dp).clickable {
                        createAccount = !createAccount
                        accessCode = ""
                        issuedCode = null
                        message = null
                    }
                )
            }
            message?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = Color(0xFFFFB7A8), fontSize = 11.sp, lineHeight = 16.sp)
            }
            issuedCode?.let { code ->
                Spacer(Modifier.height(12.dp))
                Column(
                    modifier = Modifier.fillMaxWidth().background(AuthPanel, RoundedCornerShape(10.dp)).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Text("THE BENKOUGH — ACCOUNT ACCESS CODE", color = AuthLime, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(code, color = AuthWhite, fontSize = 23.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
                    Text("Generated by THE BENKOUGH. Save this code privately. It is not sent by SMS and cannot prove phone ownership.", color = AuthMuted, fontSize = 10.sp, lineHeight = 15.sp)
                    Button(
                        onClick = onAuthenticated,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AuthLime, contentColor = AuthInk)
                    ) { Text("I SAVED MY CODE — CONTINUE", fontWeight = FontWeight.Black, fontSize = 11.sp) }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "NO SMS  /  NO PAID OTP  /  SUPABASE PHONE-PASSWORD AUTH",
                color = AuthMuted.copy(alpha = .7f),
                fontSize = 8.sp,
                letterSpacing = .7.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
