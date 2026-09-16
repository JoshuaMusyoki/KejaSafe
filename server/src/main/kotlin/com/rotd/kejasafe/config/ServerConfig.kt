package com.rotd.kejasafe.config

/**
 * Runtime configuration. Every value falls back to the local development default, so
 * `./gradlew :server:run` keeps working while Docker/production override via environment.
 */
object ServerConfig {
    val port: Int = env("PORT")?.toIntOrNull() ?: 8080
    val host: String = env("KEJASAFE_HOST") ?: "0.0.0.0"

    val jwtSecret: String = env("KEJASAFE_JWT_SECRET") ?: "kejasafe-secret-key"
    val jwtIssuer: String = env("KEJASAFE_JWT_ISSUER") ?: "com.rotd.kejasafe"
    val jwtAudience: String = env("KEJASAFE_JWT_AUDIENCE") ?: "kejasafe-users"

    val databaseUrl: String = env("KEJASAFE_DB_URL")
        ?: "jdbc:h2:file:./build/db/kejasafe;DB_CLOSE_DELAY=-1;"
    val databaseDriver: String = env("KEJASAFE_DB_DRIVER") ?: "org.h2.Driver"

    /** Managed Postgres needs credentials; the local H2 file does not. */
    val databaseUser: String? = env("KEJASAFE_DB_USER")
    val databasePassword: String? = env("KEJASAFE_DB_PASSWORD")

    /**
     * Keep this well below the database's connection limit: Cloud Run multiplies it by
     * the number of running instances.
     */
    val databasePoolSize: Int = env("KEJASAFE_DB_POOL_SIZE")?.toIntOrNull()?.coerceIn(1, 50) ?: 5

    /** Where listing photos and KYC uploads are written. Mount this as a volume in Docker. */
    val uploadDir: String = env("KEJASAFE_UPLOAD_DIR") ?: "build/uploads"

    /**
     * Browser origins allowed to call the API (comma separated). Empty means any host,
     * which is convenient locally but should be set for a deployed web app.
     */
    val allowedOrigins: List<String> = env("KEJASAFE_ALLOWED_ORIGINS")
        ?.split(",")
        ?.mapNotNull { it.trim().ifBlank { null } }
        .orEmpty()

    private fun env(name: String): String? = System.getenv(name)?.trim()?.ifBlank { null }
}
