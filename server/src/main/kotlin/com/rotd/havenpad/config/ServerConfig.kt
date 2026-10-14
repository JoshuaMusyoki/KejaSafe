package com.rotd.havenpad.config

import java.io.File
import java.util.Properties

/**
 * Runtime configuration. Priority order for configuration values:
 * 1. System environment variables (e.g. Docker, Cloud Run, GitHub Actions)
 * 2. System properties (e.g. -DMPESA_CONSUMER_KEY=...)
 * 3. local.properties file (for local development/testing)
 * 4. Fallback defaults (sandbox defaults for M-Pesa)
 */
object ServerConfig {
    private val localProperties: Properties by lazy {
        Properties().apply {
            val candidateFiles = listOf(
                File("local.properties"),
                File("../local.properties"),
                File(System.getProperty("user.dir"), "local.properties"),
                File(System.getProperty("user.dir"), "../local.properties")
            )
            val file = candidateFiles.firstOrNull { it.exists() && it.isFile }
            if (file != null) {
                try {
                    file.inputStream().use { load(it) }
                } catch (e: Exception) {
                    println("ServerConfig: Failed to load local.properties: ${e.message}")
                }
            }
        }
    }

    private fun isPlaceholder(value: String): Boolean {
        val v = value.trim()
        return v.isBlank() ||
            v.startsWith("YOUR_", ignoreCase = true) ||
            v.startsWith("YOUR-", ignoreCase = true) ||
            v.contains("your-ngrok", ignoreCase = true) ||
            v.contains("<your-", ignoreCase = true) ||
            v.contains("your-api-domain", ignoreCase = true) ||
            v.equals("CHANGE_ME", ignoreCase = true)
    }

    private fun configValue(key: String): String? {
        val sysEnv = System.getenv(key)?.trim()?.ifBlank { null }
        if (sysEnv != null && !isPlaceholder(sysEnv)) return sysEnv

        val sysProp = System.getProperty(key)?.trim()?.ifBlank { null }
        if (sysProp != null && !isPlaceholder(sysProp)) return sysProp

        val localProp = localProperties.getProperty(key)?.trim()?.ifBlank { null }
        if (localProp != null && !isPlaceholder(localProp)) return localProp

        return null
    }

    val port: Int = configValue("PORT")?.toIntOrNull() ?: 8080
    val host: String = configValue("HAVENPAD_HOST") ?: "0.0.0.0"

    val jwtSecret: String = configValue("HAVENPAD_JWT_SECRET") ?: "havenpad-secret-key"
    val jwtIssuer: String = configValue("HAVENPAD_JWT_ISSUER") ?: "com.rotd.havenpad"
    val jwtAudience: String = configValue("HAVENPAD_JWT_AUDIENCE") ?: "havenpad-users"

    val databaseUrl: String = configValue("HAVENPAD_DB_URL")
        ?: "jdbc:h2:file:./build/db/havenpad;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;INIT=CREATE SCHEMA IF NOT EXISTS PUBLIC;"
    val databaseDriver: String = configValue("HAVENPAD_DB_DRIVER") ?: "org.h2.Driver"

    /** Managed Postgres needs credentials; the local H2 file does not. */
    val databaseUser: String? = configValue("HAVENPAD_DB_USER")
    val databasePassword: String? = configValue("HAVENPAD_DB_PASSWORD")

    /**
     * Keep this well below the database's connection limit: Cloud Run multiplies it by
     * the number of running instances.
     */
    val databasePoolSize: Int = configValue("HAVENPAD_DB_POOL_SIZE")?.toIntOrNull()?.coerceIn(1, 50) ?: 5

    /** Where listing photos are written. Mount this as a volume in Docker. Served at /uploads. */
    val uploadDir: String = configValue("HAVENPAD_UPLOAD_DIR") ?: "build/uploads"

    /** Identity documents. Kept outside [uploadDir] so they are not publicly served. */
    val kycDir: String = configValue("HAVENPAD_KYC_DIR") ?: "build/kyc-private"

    /**
     * First-run admin credentials. Loaded from local.properties or environment variables.
     */
    val adminEmail: String = configValue("HAVENPAD_ADMIN_EMAIL").orEmpty()
    val adminPassword: String = configValue("HAVENPAD_ADMIN_PASSWORD").orEmpty()

    /**
     * Browser origins allowed to call the API (comma separated). Empty means any host,
     * which is convenient locally but should be set for a deployed web app.
     */
    val allowedOrigins: List<String> = configValue("HAVENPAD_ALLOWED_ORIGINS")
        ?.split(",")
        ?.mapNotNull { it.trim().ifBlank { null } }
        .orEmpty()

    // -------------------------------------------------------------------------
    // M-PESA DARAJA CONFIGURATION
    // -------------------------------------------------------------------------

    /**
     * M-Pesa Environment Mode: "sandbox" (default) or "production" / "live".
     */
    val mpesaEnv: String = configValue("MPESA_ENV") ?: configValue("MPESA_ENVIRONMENT") ?: "sandbox"

    val isMpesaProduction: Boolean
        get() = mpesaEnv.equals("production", ignoreCase = true) || mpesaEnv.equals("live", ignoreCase = true)

    val mpesaBaseUrl: String
        get() = if (isMpesaProduction) "https://api.safaricom.co.ke" else "https://sandbox.safaricom.co.ke"

    val mpesaAuthUrl: String
        get() = "$mpesaBaseUrl/oauth/v1/generate?grant_type=client_credentials"

    val mpesaStkPushUrl: String
        get() = "$mpesaBaseUrl/mpesa/stkpush/v1/processrequest"

    val mpesaStkQueryUrl: String
        get() = "$mpesaBaseUrl/mpesa/stkpushquery/v1/query"

    val mpesaConsumerKey: String = configValue("MPESA_CONSUMER_KEY")
        ?: ""

    val mpesaConsumerSecret: String = configValue("MPESA_CONSUMER_SECRET")
        ?: ""

    val mpesaBusinessShortCode: String = configValue("MPESA_BUSINESS_SHORT_CODE")
        ?: ""

    val mpesaPasskey: String = configValue("MPESA_PASSKEY")
        ?: ""

    /**
     * Command ID for STK Push:
     * - CustomerPayBillOnline (Paybill)
     * - CustomerBuyGoodsOnline (Till Number / Buy Goods)
     */
    val mpesaCommandId: String = configValue("MPESA_COMMAND_ID")
        ?: "CustomerPayBillOnline"

    /**
     * Public HTTPS callback URL for Safaricom Daraja STK Push callbacks.
     * Must be a valid HTTPS domain reachable by Safaricom servers.
     * Examples:
     * - Local testing: "https://<subdomain>.ngrok-free.app/mpesa/callback"
     * - Production: "https://api.havenpad.app/mpesa/callback" or "https://<cloud-run-url>/mpesa/callback"
     */
    val mpesaCallbackUrl: String = configValue("MPESA_CALLBACK_URL")
        ?: "https://your-api-domain.com/mpesa/callback"

    /**
     * Optional secret token used to validate incoming HTTPS callbacks from Safaricom.
     * If set, incoming callback requests must contain `?token=YOUR_SECRET_TOKEN` or `X-Mpesa-Token: YOUR_SECRET_TOKEN`.
     */
    val mpesaCallbackSecretToken: String? = configValue("MPESA_CALLBACK_SECRET_TOKEN")

    // -------------------------------------------------------------------------
    // OTHER THIRD-PARTY SERVICES
    // -------------------------------------------------------------------------

    /**
     * OAuth web client id. The Android app and the website both send this id's ID tokens.
     * Leave empty until Google sign-in is configured; email login and guest browse still work.
     */
    val googleClientId: String = configValue("HAVENPAD_GOOGLE_CLIENT_ID") ?: ""

    /**
     * OAuth iOS client id. Same value as Info.plist GIDClientID.
     * Accepted as a second ID-token audience so iOS sign-in verifies beside the web client.
     */
    val googleIosClientId: String = configValue("HAVENPAD_GOOGLE_IOS_CLIENT_ID") ?: ""

    /** Server-side key for Places Autocomplete. Never shipped to the app. */
    val googlePlacesApiKey: String? = configValue("GOOGLE_PLACES_API_KEY")

    /** Firebase project that owns the Android app. Used by Cloud Messaging HTTP v1. */
    val fcmProjectId: String = configValue("HAVENPAD_FCM_PROJECT_ID") ?: "havenpad"

    /** Path to Firebase service account JSON */
    val fcmCredentialsPath: String? = configValue("HAVENPAD_FCM_CREDENTIALS")
        ?: listOf(
            "firebase-service-account.json",
            "server/firebase-service-account.json",
        ).firstOrNull { File(it).isFile }
}

