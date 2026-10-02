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

    /** Where listing photos and KYC uploads are written. Mount this as a volume in Docker. */
    val uploadDir: String = env("HAVENPAD_UPLOAD_DIR") ?: "build/uploads"

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

    /** Server-side key for Places Autocomplete. Never shipped to the app. */
    val googlePlacesApiKey: String? = env("GOOGLE_PLACES_API_KEY")

    private fun env(name: String): String? = System.getenv(name)?.trim()?.ifBlank { null }
}
