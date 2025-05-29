package me.rayatnia.domain.events

import java.time.LocalDateTime
import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
sealed interface DomainEvent {
    val eventId: String
    val timestamp: String
    val aggregateId: String
}

@Serializable
data class FileUploadedEvent(
    override val eventId: String = UUID.randomUUID().toString(),
    override val timestamp: String = LocalDateTime.now().toString(),
    override val aggregateId: String,
    val filePath: String,
    val originalFileName: String,
    val contentType: String,
    val fileSize: Long
) : DomainEvent

@Serializable
data class UserDataProcessedEvent(
    override val eventId: String = UUID.randomUUID().toString(),
    override val timestamp: String = LocalDateTime.now().toString(),
    override val aggregateId: String,
    val userId: String,
    val status: ProcessingStatus,
    val errorMessage: String? = null
) : DomainEvent

enum class ProcessingStatus {
    SUCCESS,
    FAILURE
} 