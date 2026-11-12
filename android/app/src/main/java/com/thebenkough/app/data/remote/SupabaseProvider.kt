package com.thebenkough.app.data.remote

import com.thebenkough.app.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

/**
 * Shared Supabase client for the Android app.
 *
 * Only the publishable key is shipped in the client. Never add a service-role
 * or secret key to this app. RLS is the authorization boundary.
 */
object SupabaseProvider {
    val client = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY
    ) {
        install(Auth)
        install(Postgrest)
        install(Storage)
    }
}
