package me.rayatnia

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import me.rayatnia.application.commands.UploadUserDataCommand
import me.rayatnia.application.queries.GetUserDataQuery
import me.rayatnia.infrastructure.messaging.SqsEventPublisher
import me.rayatnia.infrastructure.persistence.InMemoryUserDataRepository
import software.amazon.awssdk.services.sqs.SqsClient
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.BeforeTest

class ApplicationTest {
    @BeforeTest
    fun setup() {
        // Create test keystore if it doesn't exist
        val keystorePath = "keystore.jks"
        if (!File(keystorePath).exists()) {
            ProcessBuilder(
                "keytool",
                "-genkeypair",
                "-alias", "sampleAlias",
                "-keyalg", "RSA",
                "-keysize", "2048",
                "-storetype", "PKCS12",
                "-keystore", keystorePath,
                "-validity", "3650",
                "-storepass", "changeit",
                "-keypass", "changeit",
                "-dname", "CN=localhost, OU=Test, O=AllRide, L=Test, ST=Test, C=US"
            ).start().waitFor()
        }
    }

    @Test
    fun testRoot() = testApplication {
        environment {
            config = MapApplicationConfig().apply {
                put("ktor.deployment.port", "8080")
                put("ktor.deployment.sslPort", "8443")
                put("ktor.security.ssl.keyStore", "keystore.jks")
                put("ktor.security.ssl.keyAlias", "sampleAlias")
                put("ktor.security.ssl.keyStorePassword", "changeit")
                put("ktor.security.ssl.privateKeyPassword", "changeit")
                put("ktor.http.version", "2.0")
                put("aws.region", "us-east-1")
                put("aws.sqs.queue_url", "http://localhost:4566/000000000000/allride-main-queue")
                put("aws.sqs.dlq_url", "http://localhost:4566/000000000000/allride-dlq")
                put("memcached.endpoint", "localhost:11211")
            }
        }
        
        application {
            module()
        }
        
        client.get("/").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("Hello World! Ktor HTTP/2 server running.", bodyAsText())
        }
    }
}
