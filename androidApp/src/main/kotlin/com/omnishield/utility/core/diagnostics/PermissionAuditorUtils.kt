package com.omnishield.utility.core.diagnostics

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build

data class AppPrivacyRisk(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean,
    val riskScore: Int, // 0 to 100
    val riskLevel: String, // Low, Moderate, Critical
    val sensitivePermissions: List<String>
)

object PermissionAuditorUtils {

    fun auditInstalledApps(context: Context): List<AppPrivacyRisk> {
        val pm = context.packageManager
        val flags = PackageManager.GET_PERMISSIONS
        val packages: List<PackageInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledPackages(flags)
        }

        val results = mutableListOf<AppPrivacyRisk>()

        for (pkg in packages) {
            val appInfo = pkg.applicationInfo ?: continue
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

            val perms = pkg.requestedPermissions ?: continue
            val sensitiveList = mutableListOf<String>()
            var score = 0

            for (p in perms) {
                when (p) {
                    "android.permission.CAMERA" -> {
                        sensitiveList.add("Camera")
                        score += 25
                    }
                    "android.permission.RECORD_AUDIO" -> {
                        sensitiveList.add("Microphone")
                        score += 25
                    }
                    "android.permission.ACCESS_FINE_LOCATION" -> {
                        sensitiveList.add("Precise GPS")
                        score += 20
                    }
                    "android.permission.ACCESS_BACKGROUND_LOCATION" -> {
                        sensitiveList.add("Background Location")
                        score += 30
                    }
                    "android.permission.READ_CONTACTS" -> {
                        sensitiveList.add("Contacts")
                        score += 15
                    }
                    "android.permission.READ_SMS" -> {
                        sensitiveList.add("SMS Messages")
                        score += 25
                    }
                    "android.permission.READ_CALL_LOG" -> {
                        sensitiveList.add("Call History")
                        score += 20
                    }
                }
            }

            if (sensitiveList.isNotEmpty()) {
                val clamped = score.coerceAtMost(100)
                val level = when {
                    clamped >= 60 -> "High Risk"
                    clamped >= 35 -> "Moderate"
                    else -> "Low Exposure"
                }

                val appLabel = runCatching { pm.getApplicationLabel(appInfo).toString() }.getOrDefault(pkg.packageName)
                results.add(
                    AppPrivacyRisk(
                        packageName = pkg.packageName,
                        appName = appLabel,
                        isSystemApp = isSystem,
                        riskScore = clamped,
                        riskLevel = level,
                        sensitivePermissions = sensitiveList
                    )
                )
            }
        }

        return results.sortedByDescending { it.riskScore }
    }
}
