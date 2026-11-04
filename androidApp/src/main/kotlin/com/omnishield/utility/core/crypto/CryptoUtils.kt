package com.omnishield.utility.core.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * High-performance, offline-first cryptographic engine implementing:
 * - Symmetric encryption: AES-256-GCM (Galois/Counter Mode with 128-bit authentication tag)
 * - Key derivation: PBKDF2WithHmacSHA256 with 65,536 iterations and 128-bit cryptographically secure random salt
 * - Streaming memory-safe pipeline suitable for multi-gigabyte files
 * - Tamper detection via AEAD authentication tag
 * - Hardware-backed Android KeyStore integration for biometric-gated keys
 */
object CryptoUtils {

    // Magic identifier bytes: 'O', 'M', 'N', 'I'
    private val MAGIC_HEADER = byteArrayOf(0x4F, 0x4D, 0x4E, 0x49)
    private const val FORMAT_VERSION: Byte = 0x01

    private const val ALGORITHM_AES = "AES"
    private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256"

    private const val SALT_SIZE_BYTES = 16
    private const val IV_SIZE_BYTES = 12 // 96 bits is the optimal GCM IV length
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val PBKDF2_ITERATIONS = 65_536
    private const val KEY_LENGTH_BITS = 256
    private const val BUFFER_SIZE = 64 * 1024 // 64 KB streaming buffer

    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val BIOMETRIC_KEY_ALIAS = "OmniShield_Biometric_MasterKey"

    private val secureRandom = SecureRandom()

    /**
     * Generates a cryptographically strong, high-entropy random passphrase for file protection.
     */
    fun generateStrongPassphrase(length: Int = 18, includeSpecial: Boolean = true): String {
        val uppers = "ABCDEFGHJKLMNPQRSTUVWXYZ"
        val lowers = "abcdefghijkmnopqrstuvwxyz"
        val digits = "23456789"
        val specials = "!@#$%^&*()_+-=[]{}|;:,.<>?"

        val charPool = StringBuilder().append(uppers).append(lowers).append(digits)
        if (includeSpecial) charPool.append(specials)

        val poolStr = charPool.toString()
        val password = StringBuilder(length)

        // Ensure at least one from each required category
        password.append(uppers[secureRandom.nextInt(uppers.length)])
        password.append(lowers[secureRandom.nextInt(lowers.length)])
        password.append(digits[secureRandom.nextInt(digits.length)])
        if (includeSpecial) {
            password.append(specials[secureRandom.nextInt(specials.length)])
        }

        while (password.length < length) {
            password.append(poolStr[secureRandom.nextInt(poolStr.length)])
        }

        // Shuffle securely
        val chars = password.toString().toCharArray()
        for (i in chars.indices.reversed()) {
            val j = secureRandom.nextInt(i + 1)
            val temp = chars[i]
            chars[i] = chars[j]
            chars[j] = temp
        }
        return String(chars)
    }

    /**
     * Wipes sensitive cryptographic material from memory.
     */
    fun wipeMemory(chars: CharArray) {
        chars.fill('\u0000')
    }

    fun wipeMemory(bytes: ByteArray) {
        bytes.fill(0.toByte())
    }

    /**
     * Derives a 256-bit AES key from a user passphrase and a cryptographic salt using PBKDF2.
     * Sanitizes intermediate key spec and raw byte arrays immediately.
     */
    fun deriveKeyFromPassword(passphrase: CharArray, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(passphrase, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM)
        val keyBytes = factory.generateSecret(spec).encoded
        val secretKey = SecretKeySpec(keyBytes, ALGORITHM_AES)
        spec.clearPassword()
        wipeMemory(keyBytes)
        return secretKey
    }

    /**
     * Encrypts input stream into output stream using AES-256-GCM in a memory-safe streaming fashion.
     * Header structure: [4 bytes Magic][1 byte Version][16 bytes Salt][12 bytes IV][Ciphertext + 16 bytes Tag]
     */
    @Throws(Exception::class)
    fun encryptStream(
        inputStream: InputStream,
        outputStream: OutputStream,
        passphrase: CharArray,
        totalBytes: Long = -1L,
        onProgress: ((bytesProcessed: Long, totalBytes: Long) -> Unit)? = null
    ) {
        val salt = ByteArray(SALT_SIZE_BYTES)
        val iv = ByteArray(IV_SIZE_BYTES)
        secureRandom.nextBytes(salt)
        secureRandom.nextBytes(iv)

        val secretKey = deriveKeyFromPassword(passphrase, salt)

        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

        // Write file header
        outputStream.write(MAGIC_HEADER)
        outputStream.write(byteArrayOf(FORMAT_VERSION))
        outputStream.write(salt)
        outputStream.write(iv)

        // Stream and encrypt chunks
        val buffer = ByteArray(BUFFER_SIZE)
        var bytesRead: Int
        var totalBytesProcessed = 0L

        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            val encryptedBytes = cipher.update(buffer, 0, bytesRead)
            if (encryptedBytes != null && encryptedBytes.isNotEmpty()) {
                outputStream.write(encryptedBytes)
            }
            totalBytesProcessed += bytesRead
            onProgress?.invoke(totalBytesProcessed, totalBytes)
        }

        // Finalize cipher and write final tag
        val finalBytes = cipher.doFinal()
        if (finalBytes != null && finalBytes.isNotEmpty()) {
            outputStream.write(finalBytes)
        }
        outputStream.flush()
    }

    /**
     * Safely and fully reads the requested byte buffer from an InputStream, even across fragmented network packets.
     */
    private fun readFully(inputStream: InputStream, buffer: ByteArray): Boolean {
        var totalRead = 0
        while (totalRead < buffer.size) {
            val read = inputStream.read(buffer, totalRead, buffer.size - totalRead)
            if (read == -1) return false
            totalRead += read
        }
        return true
    }

    /**
     * Decrypts an OmniShield encrypted stream into output stream.
     * Validates header, magic bytes, derives key, and verifies GCM authentication tag.
     */
    @Throws(Exception::class)
    fun decryptStream(
        inputStream: InputStream,
        outputStream: OutputStream,
        passphrase: CharArray,
        totalBytes: Long = -1L,
        onProgress: ((bytesProcessed: Long, totalBytes: Long) -> Unit)? = null
    ) {
        // Read Magic Header
        val magic = ByteArray(MAGIC_HEADER.size)
        if (!readFully(inputStream, magic) || !magic.contentEquals(MAGIC_HEADER)) {
            throw IllegalArgumentException("Invalid file format. The file is not an OmniShield encrypted archive.")
        }

        // Read Version
        val version = inputStream.read()
        if (version != FORMAT_VERSION.toInt()) {
            throw IllegalArgumentException("Unsupported file format version: $version")
        }

        // Read Salt
        val salt = ByteArray(SALT_SIZE_BYTES)
        if (!readFully(inputStream, salt)) {
            throw IllegalArgumentException("Corrupted file header: Missing cryptographic salt.")
        }

        // Read IV
        val iv = ByteArray(IV_SIZE_BYTES)
        if (!readFully(inputStream, iv)) {
            throw IllegalArgumentException("Corrupted file header: Missing initialization vector.")
        }

        val secretKey = deriveKeyFromPassword(passphrase, salt)
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

        val headerSize = (MAGIC_HEADER.size + 1 + SALT_SIZE_BYTES + IV_SIZE_BYTES).toLong()
        val dataTotalBytes = if (totalBytes > headerSize) totalBytes - headerSize else -1L

        val buffer = ByteArray(BUFFER_SIZE)
        var totalBytesProcessed = 0L
        var bytesRead: Int

        try {
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                val decryptedBytes = cipher.update(buffer, 0, bytesRead)
                if (decryptedBytes != null && decryptedBytes.isNotEmpty()) {
                    outputStream.write(decryptedBytes)
                }
                totalBytesProcessed += bytesRead
                onProgress?.invoke(totalBytesProcessed, dataTotalBytes)
            }

            val finalBytes = cipher.doFinal()
            if (finalBytes != null && finalBytes.isNotEmpty()) {
                outputStream.write(finalBytes)
            }
            outputStream.flush()
        } catch (e: AEADBadTagException) {
            throw SecurityException("Authentication failed: Incorrect passphrase or file contents have been tampered with.", e)
        }
    }

    /**
     * Inspects a stream header to check if it's an OmniShield encrypted container.
     */
    fun isOmniShieldEncrypted(inputStream: InputStream): Boolean {
        return try {
            val magic = ByteArray(MAGIC_HEADER.size)
            readFully(inputStream, magic) && magic.contentEquals(MAGIC_HEADER)
        } catch (_: Exception) {
            false
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Android KeyStore & Biometric Helper
    // ---------------------------------------------------------------------------------------------

    /**
     * Initializes or returns a hardware-backed Master Key in AndroidKeyStore requiring user biometric authentication.
     */
    fun getOrCreateBiometricMasterKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        if (!keyStore.containsAlias(BIOMETRIC_KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
            val builder = KeyGenParameterSpec.Builder(
                BIOMETRIC_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_LENGTH_BITS)
                .setUserAuthenticationRequired(true)
                .setRandomizedEncryptionRequired(true)

            keyGenerator.init(builder.build())
            return keyGenerator.generateKey()
        }
        val entry = keyStore.getEntry(BIOMETRIC_KEY_ALIAS, null) as KeyStore.SecretKeyEntry
        return entry.secretKey
    }

    /**
     * Creates an initialized Cipher instance for BiometricPrompt.CryptoObject in Encrypt mode.
     */
    fun initBiometricEncryptCipher(): Cipher {
        val key = getOrCreateBiometricMasterKey()
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        return cipher
    }

    /**
     * Creates an initialized Cipher instance for BiometricPrompt.CryptoObject in Decrypt mode using given IV.
     */
    fun initBiometricDecryptCipher(iv: ByteArray): Cipher {
        val key = getOrCreateBiometricMasterKey()
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        return cipher
    }
}
