package finance.services

import finance.domain.TokenBlacklist
import finance.repositories.TokenBlacklistRepository
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.time.Instant
import java.util.Date
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

@Service
class TokenBlacklistService(
    private val tokenBlacklistRepository: TokenBlacklistRepository,
) {
    private val cleanup: ScheduledExecutorService = Executors.newScheduledThreadPool(1)

    companion object {
        private val logger = LoggerFactory.getLogger(TokenBlacklistService::class.java)
    }

    init {
        cleanup.scheduleWithFixedDelay(::cleanupExpiredTokens, 1, 1, TimeUnit.HOURS)
        logger.info("TokenBlacklistService initialized with hourly cleanup schedule")
    }

    @PreDestroy
    fun shutdown() {
        logger.info("Shutting down TokenBlacklistService cleanup executor")
        try {
            cleanup.shutdown()
            if (!cleanup.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.warn("Cleanup executor did not terminate in 5 seconds, forcing shutdown")
                cleanup.shutdownNow()
                if (!cleanup.awaitTermination(5, TimeUnit.SECONDS)) {
                    logger.error("Cleanup executor did not terminate after forced shutdown")
                }
            }
            logger.info("TokenBlacklistService cleanup executor shut down successfully")
        } catch (ex: InterruptedException) {
            logger.error("Interrupted while shutting down cleanup executor", ex)
            cleanup.shutdownNow()
            Thread.currentThread().interrupt()
        }
    }

    fun blacklistToken(
        token: String,
        expirationTime: Long,
    ) {
        val hash = hashToken(token)
        val expiresAt = Instant.ofEpochMilli(expirationTime)
        val entry = TokenBlacklist(tokenHash = hash, expiresAt = expiresAt)
        tokenBlacklistRepository.save(entry)
        logger.info("Token blacklisted (hash={}), expires at: {}", hash.take(8) + "...", Date(expirationTime))
    }

    fun isBlacklisted(token: String): Boolean {
        val hash = hashToken(token)
        val blacklisted = tokenBlacklistRepository.existsByTokenHash(hash)
        if (blacklisted) {
            logger.debug("Token (hash={}...) found in blacklist", hash.take(8))
        }
        return blacklisted
    }

    fun getBlacklistSize(): Int = tokenBlacklistRepository.count().toInt()

    private fun cleanupExpiredTokens() {
        val removed = tokenBlacklistRepository.deleteAllExpiredBefore(Instant.now())
        if (removed > 0) {
            logger.info("Cleaned up {} expired tokens from blacklist", removed)
        }
    }

    private fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(token.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
