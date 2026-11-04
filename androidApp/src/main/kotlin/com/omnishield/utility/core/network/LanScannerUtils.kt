package com.omnishield.utility.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.FileReader
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket

data class DiscoveredDevice(
    val ipAddress: String,
    val macAddress: String,
    val hostName: String,
    val latencyMs: Long,
    val isCurrentDevice: Boolean = false,
    val vendorHint: String = "Generic Device"
)

object LanScannerUtils {

    /**
     * Resolves current device's local IPv4 address on active network interface.
     */
    fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                val addrs = intf.inetAddresses
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }

    /**
     * Scans the local /24 subnet (e.g. 192.168.1.1 to 192.168.1.254) using concurrent socket ping.
     */
    suspend fun scanSubnet(
        onProgress: (scanned: Int, total: Int, found: DiscoveredDevice?) -> Unit
    ): List<DiscoveredDevice> = withContext(Dispatchers.IO) {
        val localIp = getLocalIpAddress() ?: return@withContext emptyList()
        val subnetPrefix = localIp.substringBeforeLast('.') + "."

        val arpTable = readArpTable()
        val results = mutableListOf<DiscoveredDevice>()
        val totalIps = 254
        var scannedCount = 0

        // Chunk scans in batches of 32 concurrent probes to be polite to mobile radio & battery
        val ipBatches = (1..totalIps).chunked(32)

        for (batch in ipBatches) {
            coroutineScope {
                val deferredList = batch.map { host ->
                    async {
                        val targetIp = "$subnetPrefix$host"
                        val device = probeHost(targetIp, targetIp == localIp, arpTable)
                        synchronized(results) {
                            scannedCount++
                            if (device != null) {
                                results.add(device)
                            }
                            onProgress(scannedCount, totalIps, device)
                        }
                    }
                }
                deferredList.awaitAll()
            }
        }

        results.sortedBy { ipToLong(it.ipAddress) }
    }

    private fun probeHost(
        ip: String,
        isCurrent: Boolean,
        arpTable: Map<String, String>
    ): DiscoveredDevice? {
        val startTime = System.currentTimeMillis()
        var reachable = false

        if (isCurrent) {
            reachable = true
        } else {
            // Probe 1: isReachable ICMP / TCP port 7 echo
            try {
                val address = InetAddress.getByName(ip)
                if (address.isReachable(200)) {
                    reachable = true
                }
            } catch (_: Exception) {}

            // Probe 2: Fast TCP SYN connect on common LAN ports (80, 443, 53, 445, 8080)
            if (!reachable) {
                val probePorts = intArrayOf(80, 443, 445, 53, 8080, 5555)
                for (port in probePorts) {
                    try {
                        Socket().use { socket ->
                            socket.connect(InetSocketAddress(ip, port), 120)
                            reachable = true
                        }
                        if (reachable) break
                    } catch (_: Exception) {}
                }
            }

            // Probe 3: Check if ARP table contains this IP
            if (!reachable && arpTable.containsKey(ip)) {
                val mac = arpTable[ip] ?: ""
                if (mac != "00:00:00:00:00:00") {
                    reachable = true
                }
            }
        }

        if (reachable) {
            val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
            val mac = arpTable[ip] ?: if (isCurrent) "Local Device" else "Unknown MAC"
            val hostName = try {
                val host = InetAddress.getByName(ip).canonicalHostName
                if (host != ip) host else if (isCurrent) "This Device" else "Network Host"
            } catch (_: Exception) {
                if (isCurrent) "This Device" else "Network Host"
            }

            val vendor = resolveVendorHint(mac)

            return DiscoveredDevice(
                ipAddress = ip,
                macAddress = mac,
                hostName = hostName,
                latencyMs = latency,
                isCurrentDevice = isCurrent,
                vendorHint = vendor
            )
        }
        return null
    }

    /**
     * Reads Linux kernel ARP table from /proc/net/arp to map IP addresses to MAC hardware addresses.
     */
    private fun readArpTable(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        try {
            BufferedReader(FileReader("/proc/net/arp")).use { reader ->
                var line: String?
                // Skip header line
                reader.readLine()
                while (reader.readLine().also { line = it } != null) {
                    val tokens = line?.trim()?.split("\\s+".toRegex()) ?: continue
                    if (tokens.size >= 4) {
                        val ip = tokens[0]
                        val mac = tokens[3].uppercase()
                        if (mac != "00:00:00:00:00:00") {
                            map[ip] = mac
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return map
    }

    private fun resolveVendorHint(mac: String): String {
        val prefix = mac.take(8).uppercase()
        return when {
            prefix.startsWith("00:1A:11") || prefix.startsWith("AC:DE:48") -> "Google"
            prefix.startsWith("F0:18:98") || prefix.startsWith("3C:22:FB") || prefix.startsWith("A4:83:E7") -> "Apple"
            prefix.startsWith("B8:27:EB") || prefix.startsWith("DC:A6:32") -> "Raspberry Pi"
            prefix.startsWith("BC:D0:74") || prefix.startsWith("D4:6A:91") -> "Samsung"
            prefix.startsWith("18:B4:30") || prefix.startsWith("64:16:66") -> "Nest / IoT"
            prefix.startsWith("00:50:56") || prefix.startsWith("00:0C:29") -> "VMware"
            prefix.startsWith("08:00:27") -> "VirtualBox"
            else -> "Ethernet/Wi-Fi Device"
        }
    }

    private fun ipToLong(ip: String): Long {
        val parts = ip.split('.').mapNotNull { it.toLongOrNull() }
        if (parts.size != 4) return 0L
        return (parts[0] shl 24) or (parts[1] shl 16) or (parts[2] shl 8) or parts[3]
    }
}
