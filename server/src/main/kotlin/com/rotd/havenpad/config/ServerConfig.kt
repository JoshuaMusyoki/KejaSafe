package com.rotd.havenpad.config

/**
 * Runtime configuration. Every value falls back to the local development default, so
 * `./gradlew :server:run` keeps working while Docker/production override via environment.
 */
object ServerConfig {
    val port: Int = env("PORT")?.toIntOrNull() ?: 8080
    val host: String = env("HAVENPAD_HOST") ?: "0.0.0.0"

    val jwtSecret: String = env("HAVENPAD_JWT_SECRET") ?: "havenpad-secret-key"
    val jwtIssuer: String = env("HAVENPAD_JWT_ISSUER") ?: "com.rotd.havenpad"
    val jwtAudience: String = env("HAVENPAD_JWT_AUDIENCE") ?: "havenpad-users"

    val databaseUrl: String = env("HAVENPAD_DB_URL")
        ?: "jdbc:h2:file:./build/db/havenpad;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;INIT=CREATE SCHEMA IF NOT EXISTS PUBLIC;"
    val databaseDriver: String = env("HAVENPAD_DB_DRIVER") ?: "org.h2.Driver"

    /** Managed Postgres needs credentials; the local H2 file does not. */
    val databaseUser: String? = env("HAVENPAD_DB_USER")
    val databasePassword: String? = env("HAVENPAD_DB_PASSWORD")

    /**
     * Keep this well below the database's connection limit: Cloud Run multiplies it by
     * the number of running instances.
     */
    val databasePoolSize: Int = env("HAVENPAD_DB_POOL_SIZE")?.toIntOrNull()?.coerceIn(1, 50) ?: 5

    /** Where listing photos are written. Mount this as a volume in Docker. Served at /uploads. */
    val uploadDir: String = env("HAVENPAD_UPLOAD_DIR") ?: "build/uploads"

    /** Identity documents. Kept outside [uploadDir] so they are not publicly served. */
    val kycDir: String = env("HAVENPAD_KYC_DIR") ?: "build/kyc-private"

    /**
     * First-run admin. Override in production.
     * Local default: admin@havenpad.app / HavenPadAdmin1
     */
    val adminEmail: String = env("HAVENPAD_ADMIN_EMAIL") ?: "admin@havenpad.app"
    val adminPassword: String = env("HAVENPAD_ADMIN_PASSWORD") ?: "HavenPadAdmin1"

    /**
     * Browser origins allowed to call the API (comma separated). Empty means any host,
     * which is convenient locally but should be set for a deployed web app.
     */
    val allowedOrigins: List<String> = env("HAVENPAD_ALLOWED_ORIGINS")
        ?.split(",")
        ?.mapNotNull { it.trim().ifBlank { null } }
        .orEmpty()

    val mpesaConsumerKey: String = env("MPESA_CONSUMER_KEY") ?: "2ODATAsyUaOaLd2NgMsR21CLrgiQwyDe8PyYCZJq8lBV4Xni"
    val mpesaConsumerSecret: String = env("MPESA_CONSUMER_SECRET") ?: "9hn3kr3abZ1AI3uPQ1iAs5QMtGXWJEXTIi0yeiosxCk8M4j2tGmDG3sftmF9q6um"
    val mpesaBusinessShortCode: String = env("MPESA_BUSINESS_SHORT_CODE") ?: "174379"
    val mpesaPasskey: String = env("MPESA_PASSKEY") ?: "bfb279f9aa9bdbcf158e97dd71a467cd2e0c893059b10f78e6b72ada1ed2c919"
    /** Must be a public HTTPS URL that Safaricom can reach (e.g. ngrok → /mpesa/callback). */
    val mpesaCallbackUrl: String = env("MPESA_CALLBACK_URL")
        ?: "https://your-api-domain.com/mpesa/callback"

    /**
     * OAuth web client id. The Android app and the website both send this id's ID tokens.
     * Leave empty until Google sign-in is configured; email login and guest browse still work.
     */
    val googleClientId: String = env("HAVENPAD_GOOGLE_CLIENT_ID") ?: ""

    /** Server-side key for Places Autocomplete. Never shipped to the app. */
    val googlePlacesApiKey: String? = env("GOOGLE_PLACES_API_KEY")

    /** Firebase project that owns the Android app. Used by Cloud Messaging HTTP v1. */
    val fcmProjectId: String = env("HAVENPAD_FCM_PROJECT_ID") ?: "havenpad"

    /**
     * Path to a Firebase service-account JSON (Project settings → Service accounts).
     * Without this file, chat still saves; the phone just cannot be woken after it is closed.
     */
    val fcmCredentialsPath: String? = env("HAVENPAD_FCM_CREDENTIALS")
        ?: listOf(
            "firebase-service-account.json",
            "server/firebase-service-account.json",
        ).firstOrNull { java.io.File(it).isFile }

    private fun env(name: String): String? = System.getenv(name)?.trim()?.ifBlank { null }
}
