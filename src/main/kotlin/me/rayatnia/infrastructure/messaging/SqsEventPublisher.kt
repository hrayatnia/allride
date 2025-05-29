package me.rayatnia.infrastructure.messaging

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.rayatnia.domain.events.DomainEvent
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.SendMessageRequest

class SqsEventPublisher(
    private val sqsClient: SqsClient,
    private val queueUrl: String,
    private val json: Json = Json { 
        ignoreUnknownKeys = true 
        classDiscriminator = "type"
    }
) : EventPublisher {
    
    override suspend fun publish(event: DomainEvent) {
        val messageBody = json.encodeToString(event)
        val request = SendMessageRequest.builder()
            .queueUrl(queueUrl)
            .messageBody(messageBody)
            .build()
            
        sqsClient.sendMessage(request)
    }
} 