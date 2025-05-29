package me.rayatnia.infrastructure.messaging

import me.rayatnia.domain.events.DomainEvent

interface EventPublisher {
    suspend fun publish(event: DomainEvent)
} 