
package com.mantstock.app.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

object SupabaseManager {
    val client = createSupabaseClient(
        supabaseUrl = "https://qsqaiqjdnqugobtsymna.supabase.co",
        supabaseKey = "sb_publishable_wR-ztb9NcmGIjGn8k6PFbA_t9ag-ywf"
    ) {
        install(Auth)
        install(Postgrest)
        install(Storage)
    }
}
