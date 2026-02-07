package finance.utils

import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory

/**
 * IP Address validation utility that safely extracts client IP addresses
 * while protecting against IP spoofing attacks via proxy headers.
 *
 * Only trusts X-Forwarded-For and X-Real-IP headers when the request
 * originates from known trusted proxy networks (private IP ranges).
 */
object IpAddressValidator {
    private val logger = LoggerFactory.getLogger(IpAddressValidator::class.java)

    /**
     * Safely extracts client IP address with proxy header validation.
     * Only trusts X-Forwarded-For/X-Real-IP from known private networks.
     *
     * @param request The HTTP servlet request
     * @return The validated client IP address
     */
    fun getClientIpAddress(request: HttpServletRequest): String {
        val clientIp = request.remoteAddr ?: "unknown"

        // Only trust proxy headers from known trusted networks
        return if (isFromTrustedProxy(clientIp)) {
            val xForwardedFor = request.getHeader("X-Forwarded-For")
            val xRealIp = request.getHeader("X-Real-IP")
            when {
                !xForwardedFor.isNullOrBlank() -> {
                    val forwardedIp = xForwardedFor.split(",")[0].trim()
                    if (isValidIpAddress(forwardedIp)) forwardedIp else clientIp
                }

                !xRealIp.isNullOrBlank() -> {
                    if (isValidIpAddress(xRealIp)) xRealIp else clientIp
                }

                else -> {
                    clientIp
                }
            }
        } else {
            // For untrusted sources, ignore proxy headers
            logger.debug("Ignoring proxy headers from untrusted IP: {}", clientIp)
            clientIp
        }
    }

    /**
     * Checks if the client IP is from a trusted proxy network.
     * Only private IP ranges and loopback are considered trusted.
     */
    private fun isFromTrustedProxy(clientIp: String): Boolean {
        if (clientIp == "unknown") return false

        // IPv6 loopback (::1 or full form)
        if (clientIp == "::1" || clientIp == "0:0:0:0:0:0:0:1") return true

        val trustedNetworks =
            listOf(
                "10.0.0.0/8", // Private Class A
                "172.16.0.0/12", // Private Class B
                "192.168.0.0/16", // Private Class C
                "127.0.0.0/8", // Loopback
            )

        return trustedNetworks.any { isIpInNetwork(clientIp, it) }
    }

    /**
     * Validates IP address format (IPv4 or IPv6).
     */
    private fun isValidIpAddress(ip: String): Boolean {
        // IPv4 validation
        val ipv4Regex = """^(\d{1,3}\.){3}\d{1,3}$""".toRegex()
        if (ipv4Regex.matches(ip)) {
            val parts = ip.split(".")
            return parts.all { it.toIntOrNull()?.let { num -> num in 0..255 } ?: false }
        }

        // IPv6 validation (simplified)
        val ipv6Regex = """^([0-9a-fA-F]{0,4}:){2,7}[0-9a-fA-F]{0,4}${'$'}""".toRegex()
        return ipv6Regex.matches(ip)
    }

    /**
     * Checks if an IP address is within a CIDR network range.
     */
    private fun isIpInNetwork(
        ip: String,
        cidr: String,
    ): Boolean {
        try {
            val parts = cidr.split("/")
            val networkAddress = parts[0]
            val prefixLength = parts[1].toInt()

            val ipBytes = ipToBytes(ip)
            val networkBytes = ipToBytes(networkAddress)

            if (ipBytes.size != networkBytes.size) return false

            val fullBytes = prefixLength / 8
            val remainingBits = prefixLength % 8

            // Check full bytes
            for (i in 0 until fullBytes) {
                if (ipBytes[i] != networkBytes[i]) return false
            }

            // Check remaining bits
            if (remainingBits > 0 && fullBytes < ipBytes.size) {
                val mask = (0xFF shl (8 - remainingBits)) and 0xFF
                if ((ipBytes[fullBytes].toInt() and mask) != (networkBytes[fullBytes].toInt() and mask)) {
                    return false
                }
            }

            return true
        } catch (e: Exception) {
            logger.warn("Failed to parse IP/CIDR: ip={}, cidr={}", ip, cidr, e)
            return false
        }
    }

    /**
     * Converts IPv4 address string to byte array.
     */
    private fun ipToBytes(ip: String): ByteArray = ip.split(".").map { it.toInt().toByte() }.toByteArray()
}
