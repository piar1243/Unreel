package com.example.welive.settings

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

object AppSecurity {
    fun generateSalt(): String {
        val bytes = ByteArray(SALT_BYTE_COUNT)
        SecureRandom().nextBytes(bytes)
        return Base64.getEncoder().withoutPadding().encodeToString(bytes)
    }

    fun hashPin(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest((salt + ":" + pin).toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().withoutPadding().encodeToString(hashBytes)
    }

    fun verifyPin(pin: String, salt: String, expectedHash: String): Boolean {
        if (salt.isBlank() || expectedHash.isBlank()) return false
        return hashPin(pin, salt) == expectedHash
    }

    private const val SALT_BYTE_COUNT = 16
}
