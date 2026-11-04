package com.omnishield.utility.core.tokens

import android.util.Base64
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class JwtParsedReport(
    val headerJson: String,
    val payloadJson: String,
    val algorithm: String,
    val tokenType: String,
    val subject: String?,
    val issuer: String?,
    val expirationDateFormatted: String?,
    val isExpired: Boolean,
    val expirationRelativeStatus: String,
    val issuedAtFormatted: String?,
    val securityWarnings: List<String>
)

data class X509ParsedReport(
    val subject: String,
    val issuer: String,
    val serialNumberHex: String,
    val validFromFormatted: String,
    val validToFormatted: String,
    val isExpired: Boolean,
    val publicKeyAlgorithm: String,
    val signatureAlgorithm: String,
    val sha256Fingerprint: String,
    val sha1Fingerprint: String
)

object TokenInspectorUtils {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault())

    /**
     * Decodes and parses a standard JSON Web Token (JWT) completely offline.
     */
    fun parseJwt(jwtToken: String): Result<JwtParsedReport> = runCatching {
        val cleanToken = jwtToken.trim()
            .removePrefix("Bearer ")
            .removePrefix("bearer ")
            .trim()

        val parts = cleanToken.split(".")
        if (parts.size < 2) {
            throw IllegalArgumentException("Invalid JWT format. Expected at least 2 segments separated by dots.")
        }

        fun decodeSegment(segment: String): String {
            val normalized = when (segment.length % 4) {
                2 -> "$segment=="
                3 -> "$segment="
                else -> segment
            }
            val bytes = Base64.decode(normalized, Base64.URL_SAFE)
            return String(bytes, Charsets.UTF_8)
        }

        val headerRaw = decodeSegment(parts[0])
        val payloadRaw = decodeSegment(parts[1])

        val headerObj = JSONObject(headerRaw)
        val payloadObj = JSONObject(payloadRaw)

        val alg = headerObj.optString("alg", "UNKNOWN")
        val typ = headerObj.optString("typ", "JWT")
        val sub = payloadObj.optString("sub").takeIf { it.isNotEmpty() }
        val iss = payloadObj.optString("iss").takeIf { it.isNotEmpty() }

        val warnings = mutableListOf<String>()
        if (alg.equals("none", ignoreCase = true)) {
            warnings.add("CRITICAL VULNERABILITY: Token uses unsigned 'none' algorithm!")
        }
        if (parts.size == 2 || parts[2].isBlank()) {
            warnings.add("WARNING: Token is missing cryptographic signature.")
        }

        var expFormatted: String? = null
        var isExpired = false
        var expStatus = "No expiration claim (nbf/exp missing)"

        if (payloadObj.has("exp")) {
            val expSeconds = payloadObj.getLong("exp")
            val expMillis = expSeconds * 1000L
            val expDate = Date(expMillis)
            expFormatted = dateFormat.format(expDate)

            val now = System.currentTimeMillis()
            if (now > expMillis) {
                isExpired = true
                val diffHours = (now - expMillis) / (1000 * 60 * 60)
                expStatus = if (diffHours > 24) "EXPIRED (${diffHours / 24} days ago)" else "EXPIRED ($diffHours hours ago)"
                warnings.add("Token has expired.")
            } else {
                val remainingHours = (expMillis - now) / (1000 * 60 * 60)
                expStatus = if (remainingHours > 24) "Valid (${remainingHours / 24} days remaining)" else "Valid ($remainingHours hours remaining)"
            }
        }

        var iatFormatted: String? = null
        if (payloadObj.has("iat")) {
            val iatMillis = payloadObj.getLong("iat") * 1000L
            iatFormatted = dateFormat.format(Date(iatMillis))
        }

        JwtParsedReport(
            headerJson = headerObj.toString(2),
            payloadJson = payloadObj.toString(2),
            algorithm = alg,
            tokenType = typ,
            subject = sub,
            issuer = iss,
            expirationDateFormatted = expFormatted,
            isExpired = isExpired,
            expirationRelativeStatus = expStatus,
            issuedAtFormatted = iatFormatted,
            securityWarnings = warnings
        )
    }

    /**
     * Parses an X.509 Certificate from PEM string or DER binary.
     */
    fun parseX509Certificate(pemOrDerText: String): Result<X509ParsedReport> = runCatching {
        var cleanPem = pemOrDerText.trim()
        if (!cleanPem.contains("-----BEGIN CERTIFICATE-----")) {
            cleanPem = "-----BEGIN CERTIFICATE-----\n$cleanPem\n-----END CERTIFICATE-----"
        }

        val cf = CertificateFactory.getInstance("X.509")
        val cert = cf.generateCertificate(ByteArrayInputStream(cleanPem.toByteArray(Charsets.UTF_8))) as X509Certificate

        val now = Date()
        val isExpired = now.after(cert.notAfter) || now.before(cert.notBefore)

        val sha256 = getFingerprint(cert.encoded, "SHA-256")
        val sha1 = getFingerprint(cert.encoded, "SHA-1")

        X509ParsedReport(
            subject = cert.subjectDN?.name ?: "Unknown Subject",
            issuer = cert.issuerDN?.name ?: "Unknown Issuer",
            serialNumberHex = cert.serialNumber.toString(16).uppercase(),
            validFromFormatted = dateFormat.format(cert.notBefore),
            validToFormatted = dateFormat.format(cert.notAfter),
            isExpired = isExpired,
            publicKeyAlgorithm = "${cert.publicKey.algorithm} (${cert.publicKey.format ?: "Standard"})",
            signatureAlgorithm = cert.sigAlgName ?: "Unknown",
            sha256Fingerprint = sha256,
            sha1Fingerprint = sha1
        )
    }

    private fun getFingerprint(bytes: ByteArray, algorithm: String): String {
        val md = MessageDigest.getInstance(algorithm)
        val digest = md.digest(bytes)
        return digest.joinToString(":") { "%02X".format(it) }
    }
}
