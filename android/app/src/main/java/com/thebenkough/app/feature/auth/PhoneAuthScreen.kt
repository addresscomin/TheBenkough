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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thebenkough.app.data.remote.SupabaseProvider
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.OTP
import kotlinx.coroutines.launch

private val AuthInk = Color(0xFF050607)
private val AuthPanel = Color(0xFF111820)
private val AuthWhite = Color(0xFFFFFFFF)
private val AuthMuted = Color(0xFFB9C2CC)
private val AuthLime = Color(0xFFB7E36A)

private fun normalizeIndianPhone(input: String): String? {
    val digits = input.filter(Char::isDigit)
    if (digits.length == 10) return "+91$digits"
    if (digits.length == 12 && digits.startsWith("91")) return "+$digits"
    return null
}

@Composable
fun PhoneAuthScreen(onAuthenticated: () -> Unit) {
    var phone by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var otpSent by remember { mutableStateOf(false) }
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

    val normalizedPhone = normalizeIndianPhone(phone)
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
                if (otpSent) "VERIFY YOUR\nPHONE NUMBER." else "YOUR WORKSPACE.\nVERIFIED ACCESS.",
                color = AuthWhite,
                fontSize = 32.sp,
                lineHeight = 37.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "A one-time verification code will be sent to your mobile number. Enter the code received on that same phone to prove ownership.",
                color = AuthMuted,
                fontSize = 13.sp,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(22.dp))
            OutlinedTextField(
                value = phone,
                onValueChange = {
                    phone = it.filter { ch -> ch.isDigit() || ch == '+' }.take(13)
                    otpSent = false
                    otp = ""
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Phone number (India)") },
                placeholder = { Text("98765 43210", color = AuthMuted.copy(alpha = .65f)) },
                singleLine = true,
                enabled = !busy && !otpSent,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                colors = fieldColors
            )
            if (otpSent) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = otp,
                    onValueChange = { otp = it.filter(Char::isDigit).take(6) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("6-digit SMS code") },
                    placeholder = { Text("000000", color = AuthMuted.copy(alpha = .65f)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    colors = fieldColors
                )
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    if (normalizedPhone == null) {
                        message = "Enter a valid 10-digit Indian mobile number."
                    } else if (otpSent && otp.length != 6) {
                        message = "Enter the complete 6-digit code received by SMS."
                    } else {
                        scope.launch {
                            busy = true
                            message = null
                            try {
                                if (!otpSent) {
                                    SupabaseProvider.client.auth.signInWith(OTP) {
                                        phone = normalizedPhone
                                    }
                                    otpSent = true
                                    message = "CODE SENT · CHECK YOUR PHONE"
                                } else {
                                    SupabaseProvider.client.auth.verifyPhoneOtp(
                                        type = OtpType.Phone.SMS,
                                        phone = normalizedPhone,
                                        token = otp
                                    )
                                    check(SupabaseProvider.client.auth.currentSessionOrNull() != null) {
                                        "Phone code verified, but no active session was returned."
                                    }
                                    onAuthenticated()
                                }
                            } catch (error: Exception) {
                                message = if (!otpSent) {
                                    "SMS COULD NOT BE SENT · CHECK SUPABASE SMS PROVIDER SETTINGS · ${error.message?.take(80) ?: "TRY AGAIN"}"
                                } else {
                                    "CODE INVALID OR EXPIRED · ${error.message?.take(65) ?: "REQUEST A NEW CODE"}"
                                }
                            } finally {
                                busy = false
                            }
                        }
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(3.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AuthLime, contentColor = AuthInk)
            ) {
                Text(
                    when {
                        busy -> "PLEASE WAIT…"
                        otpSent -> "VERIFY & CONTINUE  ↗"
                        else -> "SEND VERIFICATION CODE  ↗"
                    },
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
            if (otpSent) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "CHANGE NUMBER",
                    color = AuthLime,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.fillMaxWidth().clickable(enabled = !busy) {
                        otpSent = false
                        otp = ""
                        message = null
                    },
                    textAlign = TextAlign.Center
                )
                Text(
                    "RESEND CODE",
                    color = AuthMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.fillMaxWidth().clickable(enabled = !busy) {
                        scope.launch {
                            busy = true
                            try {
                                SupabaseProvider.client.auth.signInWith(OTP) { phone = normalizedPhone ?: "" }
                                message = "NEW CODE SENT"
                            } catch (error: Exception) {
                                message = "RESEND FAILED · ${error.message?.take(60) ?: "TRY AGAIN"}"
                            } finally { busy = false }
                        }
                    },
                    textAlign = TextAlign.Center
                )
            }
            message?.let {
                Spacer(Modifier.height(14.dp))
                Text(it, color = AuthLime, fontSize = 10.sp, lineHeight = 16.sp)
            }
            Spacer(Modifier.height(18.dp))
            Text(
                "PHONE OWNERSHIP IS VERIFIED BY THE SMS OTP. SMS DELIVERY REQUIRES A CONFIGURED SUPABASE SMS PROVIDER; THE APP NEVER GENERATES OR DISPLAYS A FAKE CODE.",
                color = AuthMuted.copy(alpha = .7f),
                fontSize = 8.sp,
                lineHeight = 13.sp
            )
        }
    }
}
