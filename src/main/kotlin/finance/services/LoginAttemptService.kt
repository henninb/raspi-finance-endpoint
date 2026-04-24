package finance.services

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Service
class LoginAttemptService {
    companion object {
        private val logger = LoggerFactory.getLogger(LoginAttemptService::class.java)
        private const val MAX_ATTEMPTS = 10
        private const val LOCKOUT_DURATION_SECONDS = 900L // 15 minutes
    }

    private data class AttemptRecord(
        val count: Int,
        val lockedUntil: Instant?,
    )

    private val attempts = ConcurrentHashMap<String, AttemptRecord>()

    fun isLocked(username: String): Boolean {
        val record = attempts[username.lowercase()] ?: return false
        val locked = record.lockedUntil != null && Instant.now().isBefore(record.lockedUntil)
        if (!locked && record.lockedUntil != null) {
            attempts.remove(username.lowercase())
        }
        return locked
    }

    fun recordFailure(username: String) {
        val key = username.lowercase()
        attempts.compute(key) { _, existing ->
            val record = existing ?: AttemptRecord(0, null)
            val newCount = record.count + 1
            val lockedUntil =
                when {
                    record.lockedUntil != null -> {
                        record.lockedUntil
                    }

                    newCount >= MAX_ATTEMPTS -> {
                        logger.warn("SECURITY: account locked after {} failed attempts: {}", newCount, key)
                        Instant.now().plusSeconds(LOCKOUT_DURATION_SECONDS)
                    }

                    else -> {
                        null
                    }
                }
            AttemptRecord(newCount, lockedUntil)
        }
    }

    fun recordSuccess(username: String) {
        attempts.remove(username.lowercase())
    }

    fun remainingLockSeconds(username: String): Long {
        val record = attempts[username.lowercase()] ?: return 0L
        val until = record.lockedUntil ?: return 0L
        return maxOf(0L, until.epochSecond - Instant.now().epochSecond)
    }
}
