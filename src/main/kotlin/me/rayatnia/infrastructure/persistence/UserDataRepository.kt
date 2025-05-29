package me.rayatnia.infrastructure.persistence

import me.rayatnia.domain.model.UserData

interface UserDataRepository {
    suspend fun save(userData: UserData)
    suspend fun findAll(): List<UserData>
    suspend fun findById(id: String): UserData?
    suspend fun findByEmail(email: String): UserData?
    suspend fun saveAll(users: List<UserData>)
} 