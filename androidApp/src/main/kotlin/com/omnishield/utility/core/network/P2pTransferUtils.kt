package com.omnishield.utility.core.network

import android.content.Context
import android.net.Uri
import com.omnishield.utility.core.crypto.CryptoUtils
import com.omnishield.utility.core.storage.StorageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.security.SecureRandom

data class P2pServerInfo(
    val ipAddress: String,
    val port: Int,
    val pinCode: String,
    val qrPayload: String
)

object P2pTransferUtils {

    private val MAGIC = byteArrayOf(0x50, 0x32, 0x50, 0x30) // "P2P0"
    private val random = SecureRandom()

    fun generatePin(): String {
        return String.format("%06d", random.nextInt(1000000))
    }

    /**
     * Spins up an ephemeral ServerSocket, waits for 1 receiver peer, and streams the encrypted file.
     */
    suspend fun serveFile(
        context: Context,
        fileUri: Uri,
        pin: String,
        onBound: (P2pServerInfo) -> Unit,
        onProgress: (bytesSent: Long, totalBytes: Long) -> Unit
    ): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            val fileName = StorageUtils.getFileName(context, fileUri)
            val fileSize = StorageUtils.getFileSize(context, fileUri)
            val localIp = LanScannerUtils.getLocalIpAddress() ?: "127.0.0.1"

            val serverSocket = ServerSocket(0).apply {
                soTimeout = 120_000 // 2-minute connection timeout to prevent battery drain and hanging threads
            }
            val port = serverSocket.localPort
            val qrCodePayload = "OMNIP2P:$localIp:$port:$pin"

            onBound(P2pServerInfo(localIp, port, pin, qrCodePayload))

            // Accept single client connection
            serverSocket.use { server ->
                val client = server.accept()
                client.use { socket ->
                    val dos = DataOutputStream(socket.getOutputStream())
                    val dis = DataInputStream(socket.getInputStream())

                    // Handshake: Write Magic
                    dos.write(MAGIC)
                    dos.flush()

                    // Read Client PIN response
                    val clientPin = dis.readUTF()
                    if (clientPin != pin) {
                        dos.writeBoolean(false)
                        dos.flush()
                        throw SecurityException("Peer provided incorrect PIN code.")
                    }
                    dos.writeBoolean(true)
                    dos.writeUTF(fileName)
                    dos.writeLong(fileSize)
                    dos.flush()

                    // Stream encrypted file using CryptoUtils
                    val inputStream = context.contentResolver.openInputStream(fileUri)
                        ?: throw IllegalArgumentException("Cannot open file stream.")

                    inputStream.use { inStream ->
                        CryptoUtils.encryptStream(
                            inputStream = inStream,
                            outputStream = dos,
                            passphrase = pin.toCharArray(),
                            totalBytes = fileSize,
                            onProgress = onProgress
                        )
                    }
                    dos.flush()
                }
            }
            fileSize
        }
    }

    /**
     * Connects to a peer sender, performs cryptographic PIN authentication, and streams decrypted output to file.
     */
    suspend fun receiveFile(
        context: Context,
        hostIp: String,
        port: Int,
        pin: String,
        onMetadataReceived: (fileName: String, totalBytes: Long) -> Unit,
        onProgress: (bytesReceived: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val socket = Socket()
            socket.connect(InetSocketAddress(hostIp, port), 8000)

            socket.use { sock ->
                val dis = DataInputStream(sock.getInputStream())
                val dos = DataOutputStream(sock.getOutputStream())

                // Verify Server Magic
                val magic = ByteArray(4)
                dis.readFully(magic)
                if (!magic.contentEquals(MAGIC)) {
                    throw SecurityException("Invalid protocol header from peer sender.")
                }

                // Send PIN
                dos.writeUTF(pin)
                dos.flush()

                val authSuccess = dis.readBoolean()
                if (!authSuccess) {
                    throw SecurityException("Authentication rejected: Incorrect transfer PIN.")
                }

                val fileName = dis.readUTF()
                val fileSize = dis.readLong()
                onMetadataReceived(fileName, fileSize)

                val destDir = File(context.cacheDir, "p2p_received").apply { mkdirs() }
                val targetFile = File(destDir, fileName)

                FileOutputStream(targetFile).use { outStream ->
                    CryptoUtils.decryptStream(
                        inputStream = dis,
                        outputStream = outStream,
                        passphrase = pin.toCharArray(),
                        totalBytes = fileSize,
                        onProgress = onProgress
                    )
                }

                targetFile
            }
        }
    }
}
