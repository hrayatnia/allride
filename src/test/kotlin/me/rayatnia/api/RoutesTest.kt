package me.rayatnia.api

import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import me.rayatnia.domain.model.UserData
import me.rayatnia.infrastructure.messaging.EventPublisher
import me.rayatnia.infrastructure.persistence.InMemoryUserDataRepository
import me.rayatnia.application.commands.UploadUserDataCommand
import me.rayatnia.application.queries.GetUserDataQuery
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import io.mockk.mockk
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*

class RoutesTest {
    @Test
    fun `should upload CSV file successfully`() = testApplication {
        val repository = InMemoryUserDataRepository()
        val eventPublisher = mockk<EventPublisher>(relaxed = true)
        val uploadCommand = UploadUserDataCommand(eventPublisher)
        val getUserDataQuery = GetUserDataQuery(repository)
        
        application {
            install(ContentNegotiation) {
                json()
            }
            configureRouting(uploadCommand, getUserDataQuery)
        }
        
        val csvContent = """
            firstName,lastName,email
            John,Doe,john.doe@example.com
        """.trimIndent()
        
        val response = client.post("/api/v1/users/upload") {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("file", csvContent.toByteArray(), Headers.build {
                            append(HttpHeaders.ContentType, "text/csv")
                            append(HttpHeaders.ContentDisposition, "filename=test.csv")
                        })
                    }
                )
            )
        }
        
        assertEquals(HttpStatusCode.Accepted, response.status)
        val responseText = response.bodyAsText()
        assertTrue(responseText.contains("File uploaded successfully"))
    }
    
    @Test
    fun `should get all users`() = testApplication {
        val repository = InMemoryUserDataRepository()
        val testUser = UserData(
            firstName = "John",
            lastName = "Doe",
            email = "john.doe@example.com"
        )
        repository.save(testUser)
        
        val eventPublisher = mockk<EventPublisher>(relaxed = true)
        val uploadCommand = UploadUserDataCommand(eventPublisher)
        val getUserDataQuery = GetUserDataQuery(repository)
        
        application {
            install(ContentNegotiation) {
                json()
            }
            configureRouting(uploadCommand, getUserDataQuery)
        }
        
        val response = client.get("/api/v1/users")
        assertEquals(HttpStatusCode.OK, response.status)
        val responseText = response.bodyAsText()
        assertTrue(responseText.contains("john.doe@example.com"))
    }
} 