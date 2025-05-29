package me.rayatnia.api.grpc

import com.google.protobuf.ByteString
import com.google.protobuf.Timestamp
import io.grpc.Status
import io.grpc.StatusException
import me.rayatnia.application.commands.UploadUserDataCommand
import me.rayatnia.application.queries.GetUserDataQuery
import me.rayatnia.domain.model.UserData
import me.rayatnia.proto.*
import java.io.File
import java.io.FileOutputStream
import java.time.ZoneOffset
import java.util.UUID

// Helper to convert domain UserData to proto User
fun UserData.toProtoUser(): User = User.newBuilder()
    .setId(this.id.toString())
    .setFirstName(this.firstName)
    .setLastName(this.lastName)
    .setEmail(this.email)
    .setCreatedAt(Timestamp.newBuilder()
        .setSeconds(this.createdAt.toEpochSecond(ZoneOffset.UTC))
        .setNanos(this.createdAt.nano)
        .build())
    .build()

class UserServiceImpl(
    private val uploadCommand: UploadUserDataCommand,
    private val getUserDataQuery: GetUserDataQuery
) : UserServiceGrpcKt.UserServiceCoroutineImplBase() {

    override suspend fun uploadUserData(request: UploadUserDataRequest): UploadUserDataResponse {
        val tempFileForCommand = File.createTempFile("grpc-upload-", ".csv") // Still used by command
        try {
            // Convert ByteString to InputStream for the command
            request.fileContent.newInput().use { inputStream ->
                val result = uploadCommand.handle(request.originalFileName, inputStream)
                
                result.fold(
                    onSuccess = { event ->
                        return UploadUserDataResponse.newBuilder()
                            .setMessage("File uploaded successfully via gRPC")
                            .setFileId(event.aggregateId)
                            .build()
                    },
                    onFailure = { error ->
                        // Log error appropriately
                        throw StatusException(Status.INTERNAL.withDescription("Failed to process file: ${error.message}"))
                    }
                )
            }
        } catch (e: Exception) {
            // Log error if not already a StatusException
            if (e is StatusException) throw e
            throw StatusException(Status.INTERNAL.withDescription("Error during file upload: ${e.message}"))
        } finally {
            // The command saves the file, so this tempFile (if used for intermediate storage) isn't strictly needed
            // if the command directly consumes the inputStream. The current command saves its own file.
            // Let's remove the direct usage of tempFile here as the command handles file persistence.
        }
        // Should not be reached if result.fold is exhaustive
        throw StatusException(Status.UNKNOWN.withDescription("Unknown error in uploadUserData"))
    }

    override suspend fun getUserById(request: GetUserByIdRequest): GetUserResponse {
        val userId = request.id
        if (userId.isNullOrBlank()) {
            throw StatusException(Status.INVALID_ARGUMENT.withDescription("User ID cannot be empty"))
        }
        val user = getUserDataQuery.getById(userId)
        return if (user != null) {
            GetUserResponse.newBuilder().setUser(user.toProtoUser()).build()
        } else {
            // To indicate not found, gRPC typically uses an empty response or a specific error status.
            // Returning an empty response is common if the field is optional.
            // For a required single user, throwing NOT_FOUND is clearer.
            throw StatusException(Status.NOT_FOUND.withDescription("User with ID $userId not found"))
        }
    }

    override suspend fun getUserByEmail(request: GetUserByEmailRequest): GetUserResponse {
        val email = request.email
        if (email.isNullOrBlank()) {
            throw StatusException(Status.INVALID_ARGUMENT.withDescription("Email cannot be empty"))
        }
        val user = getUserDataQuery.getByEmail(email)
        return if (user != null) {
            GetUserResponse.newBuilder().setUser(user.toProtoUser()).build()
        } else {
            throw StatusException(Status.NOT_FOUND.withDescription("User with email $email not found"))
        }
    }

    override suspend fun getAllUsers(request: GetAllUsersRequest): GetAllUsersResponse {
        val users = getUserDataQuery.getAll()
        val protoUsers = users.map { it.toProtoUser() }
        return GetAllUsersResponse.newBuilder().addAllUsers(protoUsers).build()
    }
} 