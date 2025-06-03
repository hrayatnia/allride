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
import io.ktor.server.plugins.defaultheaders.*
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
import utils.Logger

fun main() {
    Logger.info("Starting AllRide application...")
    
    try {
        embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
            Logger.info("Configuring application modules...")
            module()
            Logger.info("Application modules configured successfully")
        }.start(wait = true)
    } catch (e: Exception) {
        Logger.error("Failed to start application", e)
        throw e
    }
}

// Variable to hold the gRPC server instance
private var grpcServer: Server? = null

fun Application.module() {
    val logger = Logger.withContext(mapOf(
        "environment" to (environment.config.propertyOrNull("ktor.environment")?.getString() ?: "development"),
        "developmentMode" to environment.developmentMode
    ))
    
    try {
        logger.info("Configuring serialization...")
        configureSerialization()
        logger.info("Serialization configured successfully")

        logger.info("Configuring security...")
        configureSecurity()
        logger.info("Security configured successfully")

        logger.info("Configuring routing...")
        routing {
            get("/") {
                call.respondText("Hello World! Ktor HTTP/2 server running.")
            }
        }
        logger.info("Routing configured successfully")

        logger.info("Application configuration completed successfully")
    } catch (e: Exception) {
        logger.error("Failed to configure application", e)
        throw e
    }
}

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
        })
    }
}

fun Application.configureSecurity() {
    install(DefaultHeaders) {
        header("X-Engine", "Ktor")
        header("X-Content-Type-Options", "nosniff")
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