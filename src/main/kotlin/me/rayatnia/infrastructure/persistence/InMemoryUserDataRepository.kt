package me.rayatnia.infrastructure.persistence

import me.rayatnia.domain.model.UserData
import java.util.concurrent.ConcurrentHashMap

class InMemoryUserDataRepository : UserDataRepository {
    private val store = ConcurrentHashMap<String, UserData>()
    private val emailIndex = ConcurrentHashMap<String, String>()
    
    override suspend fun save(userData: UserData) {
        store[userData.id.toString()] = userData
        emailIndex[userData.email] = userData.id.toString()
    }
    
    override suspend fun findAll(): List<UserData> = store.values.toList()
    
    override suspend fun findById(id: String): UserData? = store[id]
    
    override suspend fun findByEmail(email: String): UserData? = emailIndex[email]?.let { store[it] }
    
    override suspend fun saveAll(users: List<UserData>) {
        users.forEach { save(it) }
    }
} 