package me.rayatnia.infrastructure.workers

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.File
import kotlinx.coroutines.runBlocking
import me.rayatnia.domain.events.FileUploadedEvent
import me.rayatnia.domain.events.ProcessingStatus
import me.rayatnia.domain.events.UserDataProcessedEvent
import me.rayatnia.infrastructure.messaging.EventPublisher
import me.rayatnia.infrastructure.persistence.InMemoryUserDataRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CsvProcessorTest {
    @Test
    fun `should process valid CSV file`() = runBlocking {
        // Create a temporary CSV file
        val csvContent = """
            firstName,lastName,email
            John,Doe,john.doe@example.com
            Jane,Smith,jane.smith@example.com
        """.trimIndent()
        
        val tempFile = File.createTempFile("test", ".csv")
        tempFile.writeText(csvContent)
        
        val repository = InMemoryUserDataRepository()
        val eventPublisher = mockk<EventPublisher>(relaxed = true)
        val processor = CsvProcessor(repository, eventPublisher)
        
        val event = FileUploadedEvent(
            aggregateId = "test-123",
            filePath = tempFile.absolutePath,
            originalFileName = "test.csv",
            contentType = "text/csv",
            fileSize = tempFile.length()
        )
        
        processor.process(event)
        
        // Verify that users were saved
        val users = repository.findAll()
        assertEquals(2, users.size)
        assertTrue(users.any { it.email == "john.doe@example.com" })
        assertTrue(users.any { it.email == "jane.smith@example.com" })
        
        // Verify that success events were published
        coVerify(exactly = 2) {
            eventPublisher.publish(match<UserDataProcessedEvent> { 
                it.status == ProcessingStatus.SUCCESS 
            })
        }
        
        tempFile.delete()
    }
    
    @Test
    fun `should handle invalid CSV rows gracefully`() = runBlocking {
        // Create a temporary CSV file with invalid data
        val csvContent = """
            firstName,lastName,email
            John,Doe,john.doe@example.com
            Invalid,Row,
            Jane,Smith,jane.smith@example.com
        """.trimIndent()
        
        val tempFile = File.createTempFile("test", ".csv")
        tempFile.writeText(csvContent)
        
        val repository = InMemoryUserDataRepository()
        val eventPublisher = mockk<EventPublisher>(relaxed = true)
        val processor = CsvProcessor(repository, eventPublisher)
        
        val event = FileUploadedEvent(
            aggregateId = "test-123",
            filePath = tempFile.absolutePath,
            originalFileName = "test.csv",
            contentType = "text/csv",
            fileSize = tempFile.length()
        )
        
        processor.process(event)
        
        // Verify that valid users were saved
        val users = repository.findAll()
        assertEquals(2, users.size)
        assertTrue(users.any { it.email == "john.doe@example.com" })
        assertTrue(users.any { it.email == "jane.smith@example.com" })
        
        // Verify that both success and failure events were published
        coVerify(exactly = 2) {
            eventPublisher.publish(match<UserDataProcessedEvent> { 
                it.status == ProcessingStatus.SUCCESS 
            })
        }
        coVerify(exactly = 1) {
            eventPublisher.publish(match<UserDataProcessedEvent> { 
                it.status == ProcessingStatus.FAILURE 
            })
        }
        
        tempFile.delete()
    }
} 