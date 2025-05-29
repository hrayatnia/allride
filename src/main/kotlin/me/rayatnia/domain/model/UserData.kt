package me.rayatnia.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class UserData(
    @Transient val id: UUID = UUID.randomUUID(),
    val firstName: String,
    val lastName: String,
    val email: String,
    @Transient val createdAt: LocalDateTime = LocalDateTime.now()
) {
    companion object {
        fun fromCsvRow(row: Map<String, String>): Result<UserData> = runCatching {
            UserData(
                firstName = row["firstName"] ?: throw IllegalArgumentException("firstName is required"),
                lastName = row["lastName"] ?: throw IllegalArgumentException("lastName is required"),
                email = row["email"] ?: throw IllegalArgumentException("email is required")
            )
        }
    }
} 