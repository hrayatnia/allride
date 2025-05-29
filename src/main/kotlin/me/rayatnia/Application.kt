package me.rayatnia

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import me.rayatnia.api.configureRouting
import me.rayatnia.application.commands.UploadUserDataCommand
import me.rayatnia.application.queries.GetUserDataQuery
import me.rayatnia.infrastructure.messaging.SqsEventPublisher
import me.rayatnia.infrastructure.persistence.InMemoryUserDataRepository
import me.rayatnia.infrastructure.workers.CsvProcessor
import software.amazon.awssdk.services.sqs.SqsClient
import kotlinx.serialization.json.Json

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

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
            call.respondText("Hello World!")
        }
    }
    
    // Configure routes
    configureRouting(uploadCommand, getUserDataQuery)
    
    // Clean up resources on shutdown
    environment.monitor.subscribe(ApplicationStopped) {
        sqsClient.close()
    }
} 