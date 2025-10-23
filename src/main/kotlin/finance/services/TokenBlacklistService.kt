package finance.services

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Service for managing blacklisted JWT tokens to enable proper logout functionality.
 * Tokens are stored in memory with their expiration times and automatically cleaned up
 * after they expire.
 */
@Service
class TokenBlacklistService {
    private val blacklistedTokens = ConcurrentHashMap<String, Long>()
    private val cleanup = Executors.newScheduledThreadPool(1)

    companion object {
        private val logger = LoggerFactory.getLogger(TokenBlacklistService::class.java)
    }

    init {
        // Schedule cleanup task to run every hour
        cleanup.scheduleWithFixedDelay(::cleanupExpiredTokens, 1, 1, TimeUnit.HOURS)
        logger.info("TokenBlacklistService initialized with hourly cleanup schedule")
    }

    /**
     * Adds a token to the blacklist with its expiration time.
     * @param token The JWT token to blacklist
     * @param expirationTime The expiration timestamp of the token in milliseconds
     */
    fun blacklistToken(
        token: String,
        expirationTime: Long,
    ) {
        blacklistedTokens[token] = expirationTime
        logger.info("Token blacklisted, expires at: ${Date(expirationTime)}, current blacklist size: ${blacklistedTokens.size}")
    }

    /**
     * Checks if a token is currently blacklisted.
     * @param token The JWT token to check
     * @return true if the token is blacklisted, false otherwise
     */
    fun isBlacklisted(token: String): Boolean {
        val isBlacklisted = blacklistedTokens.containsKey(token)
        if (isBlacklisted) {
            logger.debug("Token found in blacklist")
        }
        return isBlacklisted
    }

    /**
     * Removes expired tokens from the blacklist to prevent memory leaks.
     * This method is automatically called on a schedule.
     */
    private fun cleanupExpiredTokens() {
        val now = System.currentTimeMillis()
        val initialSize = blacklistedTokens.size
        blacklistedTokens.entries.removeIf { it.value < now }
        val removedCount = initialSize - blacklistedTokens.size
        if (removedCount > 0) {
            logger.info("Cleaned up $removedCount expired tokens from blacklist, remaining: ${blacklistedTokens.size}")
        }
    }

    /**
     * Gets the current size of the blacklist (for monitoring/testing purposes).
     * @return The number of blacklisted tokens
     */
    fun getBlacklistSize(): Int = blacklistedTokens.size
}
