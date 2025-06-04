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
            "email" to "john.doe@example.com",
            "phoneNumber" to "+1234567890",
            "address" to "123 Main St",
            "birthDate" to "1990-01-01",
            "status" to "active"
        )
        
        val result = UserData.fromCsvRow(row)
        assertTrue(result.isSuccess)
        
        val userData = result.getOrNull()!!
        assertEquals("John", userData.firstName)
        assertEquals("Doe", userData.lastName)
        assertEquals("john.doe@example.com", userData.email)
        assertEquals("+1234567890", userData.phoneNumber)
        assertEquals("123 Main St", userData.address)
        assertEquals("1990-01-01", userData.birthDate)
        assertEquals("active", userData.status)
    }
    
    @Test
    fun `should fail when required fields are missing`() {
        val row = mapOf(
            "firstName" to "John",
            "lastName" to "Doe"
            // email and other required fields are missing
        )
        
        val result = UserData.fromCsvRow(row)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("email") ?: false)
    }
} 