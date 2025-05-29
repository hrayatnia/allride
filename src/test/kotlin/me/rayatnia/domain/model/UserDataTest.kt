package me.rayatnia.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UserDataTest {
    @Test
    fun `should create UserData from valid CSV row`() {
        val row = mapOf(
            "firstName" to "John",
            "lastName" to "Doe",
            "email" to "john.doe@example.com"
        )
        
        val result = UserData.fromCsvRow(row)
        assertTrue(result.isSuccess)
        
        val userData = result.getOrNull()!!
        assertEquals("John", userData.firstName)
        assertEquals("Doe", userData.lastName)
        assertEquals("john.doe@example.com", userData.email)
    }
    
    @Test
    fun `should fail when required fields are missing`() {
        val row = mapOf(
            "firstName" to "John",
            "lastName" to "Doe"
            // email is missing
        )
        
        val result = UserData.fromCsvRow(row)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("email") ?: false)
    }
} 