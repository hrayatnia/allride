package me.rayatnia

import io.grpc.Server
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import me.rayatnia.api.configureRouting
import me.rayatnia.api.grpc.UserServiceImpl
import me.rayatnia.application.commands.UploadUserDataCommand
import me.rayatnia.application.queries.GetUserDataQuery
import me.rayatnia.infrastructure.messaging.SqsEventPublisher
import me.rayatnia.infrastructure.persistence.InMemoryUserDataRepository
import me.rayatnia.infrastructure.workers.CsvProcessor
import software.amazon.awssdk.services.sqs.SqsClient
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit
import java.io.File

fun main(args: Array<String>) {
    // Create keystore if it doesn't exist (for development only)
    val keystorePath = System.getenv("SSL_KEYSTORE_PATH") ?: "keystore.jks"
    if (!File(keystorePath).exists()) {
        generateDevelopmentKeyStore(keystorePath)
    }
    
    io.ktor.server.netty.EngineMain.main(args)
}

// Variable to hold the gRPC server instance
private var grpcServer: Server? = null

fun Application.module() {
    val region = environment.config.propertyOrNull("aws.region")?.getString() 
        ?: System.getProperty("AWS_REGION") 
        ?: "us-east-1"
        
    val queueUrl = environment.config.propertyOrNull("aws.sqs.queue_url")?.getString()
        ?: System.getProperty("SQS_QUEUE_URL")
        ?: throw IllegalStateException("SQS queue URL not configured")
        
    val dlqUrl = environment.config.propertyOrNull("aws.sqs.dlq_url")?.getString()
        ?: System.getProperty("SQS_DLQ_URL")
        ?: throw IllegalStateException("SQS DLQ URL not configured")
    
    val grpcPort = environment.config.propertyOrNull("grpc.port")?.getString()?.toInt() ?: 50051

    // Initialize infrastructure
    val sqsClient = SqsClient.builder()
        .region(software.amazon.awssdk.regions.Region.of(region))
        .build()
        
    val json = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "type"
    }
    
    val eventPublisher = SqsEventPublisher(sqsClient, queueUrl, json)
    val repository = InMemoryUserDataRepository()
    
    // Initialize application services
    val uploadCommand = UploadUserDataCommand(eventPublisher)
    val getUserDataQuery = GetUserDataQuery(repository)
    val csvProcessor = CsvProcessor(repository, eventPublisher)
    
    // Configure Ktor
    install(ContentNegotiation) {
        json(json)
    }

    routing {
        get("/") {
            call.respondText("Hello World! Ktor HTTP/2 server running.")
        }
    }
    
    // Configure routes
    configureRouting(uploadCommand, getUserDataQuery)

    // Configure and start gRPC Server with HTTP/2 settings
    val userService = UserServiceImpl(uploadCommand, getUserDataQuery)
    grpcServer = NettyServerBuilder.forPort(grpcPort)
        .addService(userService)
        .build()
        .start()

    log.info("gRPC server started on port $grpcPort")
    
    // Clean up resources on shutdown
    environment.monitor.subscribe(ApplicationStopped) {
        log.info("Shutting down gRPC server...")
        grpcServer?.shutdown()?.awaitTermination(5, TimeUnit.SECONDS)
        log.info("gRPC server shut down.")
        sqsClient.close()
        log.info("SQS client closed.")
    }
}

private fun generateDevelopmentKeyStore(keystorePath: String) {
    val password = System.getenv("SSL_KEYSTORE_PASSWORD") ?: "changeit"
    val alias = System.getenv("SSL_KEY_ALIAS") ?: "sampleAlias"
    
    ProcessBuilder(
        "keytool",
        "-genkeypair",
        "-alias", alias,
        "-keyalg", "RSA",
        "-keysize", "2048",
        "-storetype", "PKCS12",
        "-keystore", keystorePath,
        "-validity", "3650",
        "-storepass", password,
        "-keypass", password,
        "-dname", "CN=localhost, OU=Development, O=AllRide, L=Development, ST=Development, C=US"
    ).start().waitFor()
} 