package com.omnishield.utility.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

enum class PortRiskLevel {
    CRITICAL, HIGH, MEDIUM, LOW, INFO
}

data class ScannedPort(
    val port: Int,
    val serviceName: String,
    val isOpen: Boolean,
    val riskLevel: PortRiskLevel,
    val description: String
)

object PortScannerUtils {

    val TARGET_PORTS = listOf(
        ScannedPort(21, "FTP", false, PortRiskLevel.HIGH, "File Transfer Protocol (Unencrypted plaintext credentials)"),
        ScannedPort(22, "SSH", false, PortRiskLevel.LOW, "Secure Shell remote access daemon"),
        ScannedPort(23, "Telnet", false, PortRiskLevel.CRITICAL, "Insecure remote terminal (Plaintext transmission)"),
        ScannedPort(53, "DNS", false, PortRiskLevel.INFO, "Domain Name System server"),
        ScannedPort(80, "HTTP", false, PortRiskLevel.MEDIUM, "Standard unencrypted web server"),
        ScannedPort(110, "POP3", false, PortRiskLevel.HIGH, "Post Office Protocol (Plaintext email)"),
        ScannedPort(143, "IMAP", false, PortRiskLevel.HIGH, "Internet Message Access Protocol"),
        ScannedPort(443, "HTTPS", false, PortRiskLevel.INFO, "Secure TLS/SSL encrypted web server"),
        ScannedPort(1080, "SOCKS5", false, PortRiskLevel.MEDIUM, "SOCKS proxy gateway"),
        ScannedPort(3306, "MySQL", false, PortRiskLevel.HIGH, "MySQL Database exposed listener"),
        ScannedPort(5432, "PostgreSQL", false, PortRiskLevel.HIGH, "PostgreSQL Database exposed listener"),
        ScannedPort(5555, "Wireless ADB", false, PortRiskLevel.CRITICAL, "Android Debug Bridge (Remote shell root exploit risk)"),
        ScannedPort(6379, "Redis", false, PortRiskLevel.HIGH, "Redis in-memory store (Often unauthenticated)"),
        ScannedPort(8080, "HTTP-Proxy", false, PortRiskLevel.MEDIUM, "Alternate web service or proxy"),
        ScannedPort(8443, "HTTPS-Alt", false, PortRiskLevel.INFO, "Alternate secure TLS/SSL web interface"),
        ScannedPort(9090, "Web-Admin", false, PortRiskLevel.MEDIUM, "Administrative management console")
    )

    /**
     * Concurrently scans predefined target ports on a host IP with timeout.
     */
    suspend fun scanPorts(
        host: String,
        timeoutMs: Int = 250,
        onProgress: (scanned: Int, total: Int, port: ScannedPort) -> Unit
    ): List<ScannedPort> = withContext(Dispatchers.IO) {
        val total = TARGET_PORTS.size
        var scanned = 0
        val results = mutableListOf<ScannedPort>()

        coroutineScope {
            val deferred = TARGET_PORTS.map { target ->
                async {
                    val isOpen = probePort(host, target.port, timeoutMs)
                    val result = target.copy(isOpen = isOpen)
                    synchronized(results) {
                        scanned++
                        if (isOpen) results.add(result)
                        onProgress(scanned, total, result)
                    }
                }
            }
            deferred.awaitAll()
        }

        results.sortedBy { it.port }
    }

    private fun probePort(host: String, port: Int, timeoutMs: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), timeoutMs)
                true
            }
        } catch (_: Exception) {
            false
        }
    }
}
