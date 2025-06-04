package me.rayatnia.application.commands

import java.io.File
import java.io.InputStream
import java.util.UUID
import me.rayatnia.domain.events.FileUploadedEvent
import me.rayatnia.infrastructure.messaging.EventPublisher

class UploadUserDataCommand(
    private val eventPublisher: EventPublisher,
    private val uploadDir: String = "uploads"
) {
    init {
        File(uploadDir).mkdirs()
    }
    
    suspend fun handle(originalFileName: String, inputStream: InputStream): Result<FileUploadedEvent> = runCatching {
        if (!originalFileName.endsWith(".csv", ignoreCase = true)) {
            throw IllegalArgumentException("Only CSV files are allowed. Original name: $originalFileName")
        }
        
        val fileId = UUID.randomUUID().toString()
        val savedFile = File(uploadDir, "$fileId-${originalFileName.takeLast(10)}")
        
        inputStream.use { input ->
            savedFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        
        val event = FileUploadedEvent(
            aggregateId = fileId,
            filePath = savedFile.absolutePath,
            originalFileName = originalFileName,
            contentType = "text/csv",
            fileSize = savedFile.length()
        )
        
        eventPublisher.publish(event)
        event
    }
} 