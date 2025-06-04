package me.rayatnia

import io.ktor.server.application.*
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient

fun Application.configureDatabases() {
    val region = System.getenv("AWS_REGION") ?: "us-east-1"
    val sqsClient = SqsClient.builder()
        .region(Region.of(region))
        .build()
        
    environment.monitor.subscribe(ApplicationStopped) {
        sqsClient.close()
    }
}
