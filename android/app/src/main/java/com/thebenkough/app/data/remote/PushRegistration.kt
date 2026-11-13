package com.thebenkough.app.data.remote

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.thebenkough.app.BuildConfig
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

object PushRegistration {
    fun isConfigured(): Boolean =
        BuildConfig.FIREBASE_APP_ID.isNotBlank() &&
        BuildConfig.FIREBASE_API_KEY.isNotBlank() &&
        BuildConfig.FIREBASE_PROJECT_ID.isNotBlank() &&
        BuildConfig.FIREBASE_SENDER_ID.isNotBlank()

    private fun firebaseApp(context: Context): FirebaseApp {
        FirebaseApp.getApps(context).firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME }?.let { return it }
        check(isConfigured()) {
            "Firebase project config is missing. Add BENKOUGH_FIREBASE_APP_ID, API_KEY, PROJECT_ID and SENDER_ID to Gradle properties."
        }
        val options = FirebaseOptions.Builder()
            .setApplicationId(BuildConfig.FIREBASE_APP_ID)
            .setApiKey(BuildConfig.FIREBASE_API_KEY)
            .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
            .setGcmSenderId(BuildConfig.FIREBASE_SENDER_ID)
            .build()
        return FirebaseApp.initializeApp(context, options)
            ?: error("Firebase could not initialize. Check the Firebase Android app configuration.")
    }

    suspend fun registerCurrentDevice(context: Context): String = withContext(Dispatchers.IO) {
        val userId = SupabaseProvider.client.auth.currentUserOrNull()?.id
            ?: error("Sign in before registering push notifications.")
        val app = firebaseApp(context)
        val token = Tasks.await(FirebaseMessaging.getInstance(app).token)
        saveTokenForUser(userId, token)
        "PUSH TOKEN REGISTERED"
    }

    suspend fun saveCurrentUserToken(token: String) = withContext(Dispatchers.IO) {
        val userId = SupabaseProvider.client.auth.currentUserOrNull()?.id ?: return@withContext
        saveTokenForUser(userId, token)
    }

    private suspend fun saveTokenForUser(userId: String, token: String) {
        val row = JsonObject(
            mapOf(
                "user_id" to JsonPrimitive(userId),
                "fcm_token" to JsonPrimitive(token),
                "platform" to JsonPrimitive("android")
            )
        )
        SupabaseProvider.client.postgrest["device_tokens"].insert(
            row,
            upsert = true,
            onConflict = "fcm_token"
        )
    }
}
