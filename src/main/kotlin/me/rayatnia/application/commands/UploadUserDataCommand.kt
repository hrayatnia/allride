package me.rayatnia.application.commands

import io.ktor.http.content.*
import java.io.File
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
    
    suspend fun handle(fileItem: PartData.FileItem): Result<FileUploadedEvent> = runCatching {
        val fileName = fileItem.originalFileName ?: throw IllegalArgumentException("Original file name is required")
        if (!fileName.endsWith(".csv")) {
            throw IllegalArgumentException("Only CSV files are allowed")
        }
        
        val fileId = UUID.randomUUID().toString()
        val file = File(uploadDir, "$fileId.csv")
        
        fileItem.streamProvider().use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        
        val event = FileUploadedEvent(
            aggregateId = fileId,
            filePath = file.absolutePath,
            originalFileName = fileName,
            contentType = fileItem.contentType?.toString() ?: "text/csv",
            fileSize = file.length()
        )
        
        eventPublisher.publish(event)
        event
    }
} 