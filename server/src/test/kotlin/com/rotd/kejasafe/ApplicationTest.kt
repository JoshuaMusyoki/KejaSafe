package com.rotd.kejasafe

import com.rotd.kejasafe.config.ServerConfig
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApplicationTest {

    @Test
    fun healthReportsOk() = testApplication {
        application { module() }

        val response = client.get("/health")

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("ok"), "unexpected body: ${response.bodyAsText()}")
    }

    @Test
    fun defaultsAreSafeForLocalDevelopment() {
        assertEquals(8080, ServerConfig.port)
        assertTrue(ServerConfig.databaseUrl.startsWith("jdbc:h2:"))
    }
}
