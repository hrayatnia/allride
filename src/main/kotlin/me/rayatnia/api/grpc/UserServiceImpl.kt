package me.rayatnia.api.grpc

import com.google.protobuf.ByteString
import com.google.protobuf.Timestamp
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.rayatnia.application.commands.UploadUserDataCommand
import me.rayatnia.application.queries.GetUserDataQuery
import me.rayatnia.domain.model.UserData
import me.rayatnia.proto.*
import java.io.ByteArrayInputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import com.opencsv.CSVReaderBuilder
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.time.ZoneOffset
import java.util.UUID
import utils.Logger

// Helper to convert domain UserData to proto User
fun UserData.toProtoUser(): User = User.newBuilder()
    .setId(this.id.toString())
    .setFirstName(this.firstName)
    .setLastName(this.lastName)
    .setEmail(this.email)
    .setPhoneNumber(this.phoneNumber)
    .setAddress(this.address)
    .setBirthDate(this.birthDate)
    .setStatus(this.status)
    .setCreatedAt(Timestamp.newBuilder()
        .setSeconds(this.createdAt.toEpochSecond(ZoneOffset.UTC))
        .setNanos(this.createdAt.nano)
        .build())
    .build()

class UserServiceImpl(
    private val uploadCommand: UploadUserDataCommand,
    private val getUserDataQuery: GetUserDataQuery
) : UserServiceGrpcKt.UserServiceCoroutineImplBase() {

    private val logger = Logger.withContext(mapOf("service" to "UserService"))

    override suspend fun validateUserData(request: ValidateUserDataRequest): ValidateUserDataResponse {
        logger.info("Received validate user data request", "filename" to request.originalFileName)
        
        return try {
            val inputStream = ByteArrayInputStream(request.fileContent.toByteArray())
            val reader = BufferedReader(InputStreamReader(inputStream))
            val headerLine = reader.readLine()
            val headers = headerLine?.split(",")?.map { it.trim() }
                ?: throw IllegalStateException("CSV file is empty")

            val csvReader = CSVReaderBuilder(reader).build()
            val rows = csvReader.readAll()
            val errors = mutableListOf<ValidationError>()
            
            rows.forEachIndexed { index, row ->
                try {
                    val rowData = headers.zip(row.toList()).toMap()
                    UserData.fromCsvRow(rowData).getOrThrow()
                } catch (e: Exception) {
                    errors.add(ValidationError.newBuilder()
                        .setRow(index + 1)
                        .addAllErrors(listOf(e.message ?: "Unknown error"))
                        .build())
                }
            }

            ValidateUserDataResponse.newBuilder()
                .addAllErrors(errors)
                .setIsValid(errors.isEmpty())
                .setMessage(if (errors.isEmpty()) "Validation successful" else "Validation failed")
                .build()
        } catch (e: Exception) {
            logger.error("Failed to validate user data", e)
            throw Status.INTERNAL
                .withDescription("Failed to validate data: ${e.message}")
                .asException()
        }
    }

    override suspend fun uploadUserData(request: UploadUserDataRequest): UploadUserDataResponse {
        logger.info("Received upload user data request", "filename" to request.originalFileName)
        
        return try {
            val inputStream = ByteArrayInputStream(request.fileContent.toByteArray())
            val result = uploadCommand.handle(request.originalFileName, inputStream)
            
            result.fold(
                onSuccess = { event ->
                    logger.info("Upload successful", "fileId" to event.aggregateId)
                    UploadUserDataResponse.newBuilder()
                        .setMessage("File uploaded successfully")
                        .setFileId(event.aggregateId)
                        .build()
                },
                onFailure = { e ->
                    logger.error("Failed to upload user data", e)
                    throw Status.INTERNAL
                        .withDescription("Failed to process upload: ${e.message}")
                        .asException()
                }
            )
        } catch (e: Exception) {
            logger.error("Failed to upload user data", e)
            throw Status.INTERNAL
                .withDescription("Failed to process upload: ${e.message}")
                .asException()
        }
    }

    override suspend fun getAllUsers(request: GetAllUsersRequest): GetAllUsersResponse {
        logger.info("Received get all users request")
        
        return try {
            val users = getUserDataQuery.getAll()
            logger.info("Retrieved users successfully", "count" to users.size)
            
            GetAllUsersResponse.newBuilder()
                .addAllUsers(users.map { it.toProtoUser() })
                .build()
        } catch (e: Exception) {
            logger.error("Failed to get all users", e)
            throw Status.INTERNAL
                .withDescription("Failed to retrieve users: ${e.message}")
                .asException()
        }
    }

    override suspend fun getUserById(request: GetUserByIdRequest): GetUserResponse {
        logger.info("Received get user by ID request", "userId" to request.id)
        
        return try {
            val user = getUserDataQuery.getById(request.id)
                ?: throw StatusException(Status.NOT_FOUND.withDescription("User not found"))
            
            logger.debug("Retrieved user", "userId" to user.id)
            GetUserResponse.newBuilder()
                .setUser(user.toProtoUser())
                .build()
        } catch (e: StatusException) {
            logger.warn("User not found", "userId" to request.id)
            throw e
        } catch (e: Exception) {
            logger.error("Failed to get user by ID", e, "userId" to request.id)
            throw Status.INTERNAL
                .withDescription("Failed to retrieve user: ${e.message}")
                .asException()
        }
    }

    override suspend fun getUserByEmail(request: GetUserByEmailRequest): GetUserResponse {
        val email = request.email
        if (email.isBlank()) {
            throw StatusException(Status.INVALID_ARGUMENT.withDescription("Email cannot be empty"))
        }
        
        return try {
            val user = getUserDataQuery.getByEmail(email)
                ?: throw StatusException(Status.NOT_FOUND.withDescription("User not found"))
            
            GetUserResponse.newBuilder()
                .setUser(user.toProtoUser())
                .build()
        } catch (e: StatusException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to get user by email", e, "email" to email)
            throw Status.INTERNAL
                .withDescription("Failed to retrieve user: ${e.message}")
                .asException()
        }
    }
} 