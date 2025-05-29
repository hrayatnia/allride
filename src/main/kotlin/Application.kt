package me.rayatnia

import io.ktor.server.application.*
import io.ktor.server.config.*
import me.rayatnia.services.MessagingService
import me.rayatnia.services.CachingService
import software.amazon.awssdk.services.sqs.SqsClient

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
        
    val memcachedEndpoint = environment.config.propertyOrNull("memcached.endpoint")?.getString()
        ?: System.getProperty("MEMCACHED_ENDPOINT")
        ?: throw IllegalStateException("Memcached endpoint not configured")
    
    val sqsClient = SqsClient.builder()
        .region(software.amazon.awssdk.regions.Region.of(region))
        .build()
        
    val messagingService = MessagingService(sqsClient, queueUrl, dlqUrl)
    val cachingService = CachingService(memcachedEndpoint)
    
    environment.monitor.subscribe(ApplicationStopped) {
        sqsClient.close()
        cachingService.shutdown()
    }
    
    configureDatabases()
    configureMonitoring()
    configureHTTP()
    configureRouting()
}
