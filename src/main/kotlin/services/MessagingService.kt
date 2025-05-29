package me.rayatnia.services

import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.*

class MessagingService(
    private val sqsClient: SqsClient,
    private val queueUrl: String,
    private val dlqUrl: String
) {
    suspend fun sendMessage(message: String) {
        val request = SendMessageRequest.builder()
            .queueUrl(queueUrl)
            .messageBody(message)
            .build()
        
        sqsClient.sendMessage(request)
    }
    
    suspend fun receiveMessages(maxMessages: Int = 10): List<Message> {
        val request = ReceiveMessageRequest.builder()
            .queueUrl(queueUrl)
            .maxNumberOfMessages(maxMessages)
            .waitTimeSeconds(20)
            .build()
            
        return sqsClient.receiveMessage(request).messages()
    }
    
    suspend fun deleteMessage(message: Message) {
        val request = DeleteMessageRequest.builder()
            .queueUrl(queueUrl)
            .receiptHandle(message.receiptHandle())
            .build()
            
        sqsClient.deleteMessage(request)
    }
    
    suspend fun moveToDeadLetter(message: Message) {
        val request = SendMessageRequest.builder()
            .queueUrl(dlqUrl)
            .messageBody(message.body())
            .messageAttributes(message.messageAttributes())
            .build()
            
        sqsClient.sendMessage(request)
        deleteMessage(message)
    }
} 