package com.omnishield.utility.core.vault

import android.content.Context
import com.omnishield.utility.core.crypto.CryptoUtils
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.SecureRandom
import java.util.UUID

enum class NoteCategory(val displayName: String) {
    SEED_PHRASE("Seed Phrase"),
    PASSWORD("Credentials"),
    SSH_KEY("Server / SSH Key"),
    FINANCIAL("Financial / Card"),
    PERSONAL("Confidential Note")
}

data class SecretNote(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val category: NoteCategory,
    val timestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false
)

sealed class VaultUnlockResult {
    data class Success(val notes: List<SecretNote>, val isDecoy: Boolean) : VaultUnlockResult()
    data object IncorrectPin : VaultUnlockResult()
    data object NotConfigured : VaultUnlockResult()
}

object NotesVaultUtils {

    private const val VAULT_DIR = "secure_vault"
    private const val CONFIG_FILE = "vault_config.json"
    private const val REAL_VAULT_FILE = "real_vault.enc"
    private const val DECOY_VAULT_FILE = "decoy_vault.enc"

    fun isConfigured(context: Context): Boolean {
        val file = File(getVaultDir(context), CONFIG_FILE)
        return file.exists()
    }

    /**
     * Completely wipes the encrypted vault and configuration for reset/reinitialization.
     */
    fun resetVault(context: Context): Boolean {
        val dir = getVaultDir(context)
        return dir.deleteRecursively()
    }

    /**
     * Configures the vault with a primary Master PIN and an emergency Duress Decoy PIN.
     */
    fun setupVault(context: Context, masterPin: String, duressPin: String) {
        if (masterPin == duressPin) {
            throw IllegalArgumentException("Duress PIN must be different from Master PIN.")
        }

        val dir = getVaultDir(context)
        val salt = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        val saltHex = salt.joinToString("") { "%02x".format(it) }

        val masterHash = hashPin(masterPin, salt)
        val duressHash = hashPin(duressPin, salt)

        val configObj = JSONObject().apply {
            put("salt", saltHex)
            put("masterHash", masterHash)
            put("duressHash", duressHash)
        }

        File(dir, CONFIG_FILE).writeText(configObj.toString(2))

        // Initialize empty real vault and a plausible decoy note in decoy vault
        saveVaultNotes(context, masterPin, emptyList(), isDecoy = false)
        val decoyNotes = listOf(
            SecretNote(
                title = "Shopping List",
                content = "- Almond milk\n- Organic eggs\n- Sourdough bread\n- Olive oil",
                category = NoteCategory.PERSONAL
            ),
            SecretNote(
                title = "Gym Workout Schedule",
                content = "Mon: Chest & Triceps\nWed: Back & Biceps\nFri: Legs & Core",
                category = NoteCategory.PERSONAL
            )
        )
        saveVaultNotes(context, duressPin, decoyNotes, isDecoy = true)
    }

    /**
     * Authenticates user entered PIN. If master PIN matches, opens real vault.
     * If duress PIN matches, opens decoy vault seamlessly.
     */
    fun unlockVault(context: Context, pin: String): VaultUnlockResult {
        val configFile = File(getVaultDir(context), CONFIG_FILE)
        if (!configFile.exists()) return VaultUnlockResult.NotConfigured

        val config = JSONObject(configFile.readText())
        val salt = hexToBytes(config.getString("salt"))
        val masterHash = config.getString("masterHash")
        val duressHash = config.getString("duressHash")

        val inputHash = hashPin(pin, salt)

        return when (inputHash) {
            masterHash -> {
                val notes = loadVaultNotes(context, pin, isDecoy = false)
                VaultUnlockResult.Success(notes, isDecoy = false)
            }
            duressHash -> {
                val notes = loadVaultNotes(context, pin, isDecoy = true)
                VaultUnlockResult.Success(notes, isDecoy = true)
            }
            else -> VaultUnlockResult.IncorrectPin
        }
    }

    fun saveVaultNotes(context: Context, pin: String, notes: List<SecretNote>, isDecoy: Boolean) {
        val array = JSONArray()
        for (note in notes) {
            val obj = JSONObject().apply {
                put("id", note.id)
                put("title", note.title)
                put("content", note.content)
                put("category", note.category.name)
                put("timestamp", note.timestamp)
                put("isPinned", note.isPinned)
            }
            array.put(obj)
        }

        val jsonBytes = array.toString(2).toByteArray(Charsets.UTF_8)
        val targetFile = File(getVaultDir(context), if (isDecoy) DECOY_VAULT_FILE else REAL_VAULT_FILE)

        val outStream = ByteArrayOutputStream()
        CryptoUtils.encryptStream(
            inputStream = ByteArrayInputStream(jsonBytes),
            outputStream = outStream,
            passphrase = pin.toCharArray(),
            totalBytes = jsonBytes.size.toLong()
        )

        targetFile.writeBytes(outStream.toByteArray())
    }

    private fun loadVaultNotes(context: Context, pin: String, isDecoy: Boolean): List<SecretNote> {
        val file = File(getVaultDir(context), if (isDecoy) DECOY_VAULT_FILE else REAL_VAULT_FILE)
        if (!file.exists()) return emptyList()

        return try {
            val decryptedOut = ByteArrayOutputStream()
            CryptoUtils.decryptStream(
                inputStream = ByteArrayInputStream(file.readBytes()),
                outputStream = decryptedOut,
                passphrase = pin.toCharArray(),
                totalBytes = file.length()
            )

            val jsonStr = decryptedOut.toString(Charsets.UTF_8.name())
            val array = JSONArray(jsonStr)
            val notes = mutableListOf<SecretNote>()

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val catName = obj.optString("category", NoteCategory.PERSONAL.name)
                val category = try {
                    NoteCategory.valueOf(catName)
                } catch (_: Exception) {
                    NoteCategory.PERSONAL
                }
                notes.add(
                    SecretNote(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        content = obj.getString("content"),
                        category = category,
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        isPinned = obj.optBoolean("isPinned", false)
                    )
                )
            }
            notes
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun hashPin(pin: String, salt: ByteArray): String {
        val key = CryptoUtils.deriveKeyFromPassword(pin.toCharArray(), salt)
        return key.encoded.joinToString("") { "%02x".format(it) }
    }

    private fun getVaultDir(context: Context): File {
        return File(context.filesDir, VAULT_DIR).apply { mkdirs() }
    }

    private fun hexToBytes(hex: String): ByteArray {
        val bytes = ByteArray(hex.length / 2)
        for (i in bytes.indices) {
            bytes[i] = hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
        return bytes
    }
}
