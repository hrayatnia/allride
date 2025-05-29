package me.rayatnia

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
    @Test
    fun testRoot() = testApplication {
        environment {
            config = MapApplicationConfig().apply {
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
            assertEquals("Hello World!", bodyAsText())
        }
    }
}
