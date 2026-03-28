package com.example.saltyoffshore.auth

import com.example.saltyoffshore.config.AppConstants
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

/**
 * Singleton Supabase client.
 * Matching iOS SupabaseManager pattern with lazy initialization.
 */
object SupabaseClientProvider {

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = AppConstants.supabaseUrl,
            supabaseKey = AppConstants.supabaseKey
        ) {
            install(Auth)
            install(Postgrest)
            install(Realtime)
        }
    }
}
