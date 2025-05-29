package me.rayatnia.application.queries

import me.rayatnia.domain.model.UserData
import me.rayatnia.infrastructure.persistence.UserDataRepository

class GetUserDataQuery(
    private val repository: UserDataRepository
) {
    suspend fun getAll(): List<UserData> = repository.findAll()
    
    suspend fun getById(id: String): UserData? = repository.findById(id)
    
    suspend fun getByEmail(email: String): UserData? = repository.findByEmail(email)
} 