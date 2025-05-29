package me.rayatnia.api.grpc

import com.google.protobuf.ByteString
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusException
import io.grpc.StatusRuntimeException
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.rayatnia.application.commands.UploadUserDataCommand
import me.rayatnia.domain.events.FileUploadedEvent
import me.rayatnia.application.queries.GetUserDataQuery
import me.rayatnia.infrastructure.messaging.EventPublisher
import me.rayatnia.infrastructure.persistence.InMemoryUserDataRepository
import me.rayatnia.infrastructure.workers.CsvProcessor
import me.rayatnia.proto.GetAllUsersRequest
import me.rayatnia.proto.GetUserByEmailRequest
import me.rayatnia.proto.GetUserByIdRequest
import me.rayatnia.proto.UploadUserDataRequest
import me.rayatnia.proto.UserServiceGrpcKt
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import io.grpc.Server
import java.util.UUID

class UserServiceTest {
    private lateinit var serverName: String
    private lateinit var channel: ManagedChannel
    private lateinit var client: UserServiceGrpcKt.UserServiceCoroutineStub
    private lateinit var repository: InMemoryUserDataRepository
    private lateinit var uploadCommand: UploadUserDataCommand
    private lateinit var getUserDataQuery: GetUserDataQuery
    private lateinit var serviceImpl: UserServiceImpl
    private lateinit var server: Server
    private lateinit var eventPublisher: EventPublisher
    private lateinit var csvProcessor: CsvProcessor

    @BeforeEach
    fun setUp() = runTest {
        serverName = InProcessServerBuilder.generateName()
        repository = InMemoryUserDataRepository()
        
        eventPublisher = mockk<EventPublisher>(relaxed = true)
        csvProcessor = CsvProcessor(repository, eventPublisher)
        uploadCommand = UploadUserDataCommand(eventPublisher)
        getUserDataQuery = GetUserDataQuery(repository)

        coEvery { eventPublisher.publish(match { it is FileUploadedEvent }) } coAnswers { 
            val event = arg<FileUploadedEvent>(0)
            csvProcessor.process(event)
        }

        serviceImpl = UserServiceImpl(uploadCommand, getUserDataQuery)

        server = InProcessServerBuilder.forName(serverName)
            .directExecutor()
            .addService(serviceImpl)
            .build()
            .start()

        channel = InProcessChannelBuilder.forName(serverName)
            .directExecutor()
            .build()

        client = UserServiceGrpcKt.UserServiceCoroutineStub(channel)
    }

    @AfterEach
    fun tearDown() {
        channel.shutdown()
        server.shutdown()
    }

    @Test
    fun `should get all users`() = runTest {
        val csvContent = """
            firstName,lastName,email
            John,Doe,john.doe.all@example.com
            Jane,Smith,jane.smith.all@example.com
        """.trimIndent()
        
        val request = UploadUserDataRequest.newBuilder()
            .setFileContent(ByteString.copyFromUtf8(csvContent))
            .setOriginalFileName("test_all_users.csv")
            .build()
        
        client.uploadUserData(request)
        this.testScheduler.advanceUntilIdle()
        
        val getAllRequest = GetAllUsersRequest.newBuilder().build()
        val response = client.getAllUsers(getAllRequest)
        
        assertEquals(2, response.usersList.size, "Expected 2 users after processing. Found: ${response.usersList.size}")
    }

    @Test
    fun `should get user by id`() = runTest {
        val userEmailForIdTest = "john.doe.foridtest-${UUID.randomUUID()}@example.com"
        val userFirstName = "JohnId"
        val userLastName = "DoeForId"

        val csvContent = """
            firstName,lastName,email
            $userFirstName,$userLastName,$userEmailForIdTest
        """.trimIndent()
        
        val uploadRequest = UploadUserDataRequest.newBuilder()
            .setFileContent(ByteString.copyFromUtf8(csvContent))
            .setOriginalFileName("user_for_id_test.csv")
            .build()
        
        client.uploadUserData(uploadRequest)
        this.testScheduler.advanceUntilIdle()

        val getUserByEmailReq = GetUserByEmailRequest.newBuilder().setEmail(userEmailForIdTest).build()
        val userByEmailResp = client.getUserByEmail(getUserByEmailReq)
        assertNotNull(userByEmailResp.user, "User for ID test not found by email '$userEmailForIdTest' after upload and processing")
        val userId = userByEmailResp.user.id 
        kotlin.test.assertTrue(userId.isNotEmpty(), "User ID retrieved by email should not be empty")

        val getByIdRequest = GetUserByIdRequest.newBuilder().setId(userId).build()
        val response = client.getUserById(getByIdRequest)
        
        assertNotNull(response.user)
        assertEquals(userFirstName, response.user.firstName)
        assertEquals(userLastName, response.user.lastName)
        assertEquals(userEmailForIdTest, response.user.email)
    }

    @Test
    fun `should handle non-existent user by id`() = runTest {
        val randomNonExistentUuid = UUID.randomUUID().toString()
        val request = GetUserByIdRequest.newBuilder()
            .setId(randomNonExistentUuid)
            .build()
        
        try {
            client.getUserById(request)
            throw AssertionError("Expected a gRPC exception for non-existent ID '$randomNonExistentUuid' but no exception was thrown")
        } catch (e: StatusRuntimeException) {
            assertEquals(Status.NOT_FOUND.code, e.status.code, "Expected NOT_FOUND (StatusRuntimeException) for ID '$randomNonExistentUuid'")
        } catch (e: StatusException) {
            assertEquals(Status.NOT_FOUND.code, Status.fromThrowable(e).code, "Expected NOT_FOUND (StatusException) for ID '$randomNonExistentUuid'")
        }
    }

    @Test
    fun `should get user by email`() = runTest {
        val userEmailForEmailTest = "jane.doe.foremailtest-${UUID.randomUUID()}@example.com"
        val userFirstName = "JaneEmail"
        val userLastName = "DoeForEmail"

        val csvContent = """
            firstName,lastName,email
            $userFirstName,$userLastName,$userEmailForEmailTest
        """.trimIndent()
        
        val uploadRequest = UploadUserDataRequest.newBuilder()
            .setFileContent(ByteString.copyFromUtf8(csvContent))
            .setOriginalFileName("user_for_email_test.csv")
            .build()
        
        client.uploadUserData(uploadRequest)
        this.testScheduler.advanceUntilIdle()
        
        val requestBuilder = GetUserByEmailRequest.newBuilder().setEmail(userEmailForEmailTest)
        val response = client.getUserByEmail(requestBuilder.build())
        
        assertNotNull(response.user, "User not found by email '$userEmailForEmailTest' after upload and processing. Users in repo: ${repository.findAll().joinToString { it.email }}")
        assertEquals(userFirstName, response.user.firstName)
        assertEquals(userLastName, response.user.lastName)
        assertEquals(userEmailForEmailTest, response.user.email)
    }
} 