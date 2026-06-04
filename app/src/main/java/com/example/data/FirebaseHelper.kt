package com.example.data

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

object FirebaseHelper {
    private var isInitialized = false

    val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance()

    val database: FirebaseDatabase
        get() = FirebaseDatabase.getInstance()

    fun initialize(context: Context) {
        if (!isInitialized) {
            try {
                if (FirebaseApp.getApps(context).isEmpty()) {
                    val apiKey = getFieldOrDefault("FIREBASE_API_KEY", "MockApiKeyForEduTrackAppToPreventStartupCrashes")
                    val appId = getFieldOrDefault("FIREBASE_APPLICATION_ID", "1:1234567890:android:a1b2c3d4e5f6")
                    val dbUrl = getFieldOrDefault("FIREBASE_DATABASE_URL", "https://edutrack-mock-db.firebaseio.com")
                    val projectId = getFieldOrDefault("FIREBASE_PROJECT_ID", "edutrack-mock-db")

                    val options = FirebaseOptions.Builder()
                        .setApiKey(apiKey)
                        .setApplicationId(appId)
                        .setDatabaseUrl(dbUrl)
                        .setProjectId(projectId)
                        .build()

                    FirebaseApp.initializeApp(context, options)
                    Log.d("FirebaseHelper", "Firebase initialized successfully with dynamic options.")
                }
                
                try {
                    // Enable database disk persistence for reliable offline tracking
                    FirebaseDatabase.getInstance().setPersistenceEnabled(true)
                } catch (persistEx: Exception) {
                    Log.w("FirebaseHelper", "Could not set persistence (already initialized?): ${persistEx.message}")
                }
            } catch (e: Exception) {
                Log.e("FirebaseHelper", "Failed to initialize Firebase: ${e.message}")
                e.printStackTrace()
            }
            isInitialized = true
        }
    }

    private fun getFieldOrDefault(fieldName: String, default: String): String {
        return try {
            val field = BuildConfig::class.java.getField(fieldName)
            val value = field.get(null) as? String
            if (!value.isNullOrBlank() && !value.contains("MY_") && !value.startsWith("Mock")) {
                value
            } else {
                default
            }
        } catch (e: Exception) {
            default
        }
    }
}
