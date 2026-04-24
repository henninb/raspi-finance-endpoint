package finance.services

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Service
class LoginAttemptService {
    companion object {
        private val logger = LoggerFactory.getLogger(LoginAttemptService::class.java)
        private const val MAX_ATTEMPTS = 10
        private const val LOCKOUT_DURATION_SECONDS = 900L // 15 minutes
        private const val MAX_TRACKED_USERNAMES = 10_000
        private const val ENTRY_TTL_SECONDS = 1800L // evict stale non-locked entries after 30 min
    }

    private data class AttemptRecord(
        val count: Int,
        val lockedUntil: Instant?,
        val lastAttemptAt: Instant,
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
        if (attempts.size >= MAX_TRACKED_USERNAMES) {
            logger.warn("SECURITY: LoginAttemptService at capacity ({}) — evicting oldest entries", MAX_TRACKED_USERNAMES)
            evictOldestEntries()
        }
        val key = username.lowercase()
        attempts.compute(key) { _, existing ->
            val record = existing ?: AttemptRecord(0, null, Instant.now())
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
            AttemptRecord(newCount, lockedUntil, Instant.now())
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

    @Scheduled(fixedDelay = 600_000) // every 10 minutes
    fun cleanupExpiredEntries() {
        val cutoff = Instant.now().minusSeconds(ENTRY_TTL_SECONDS)
        val removed =
            attempts.entries.removeIf { (_, record) ->
                val lockExpired = record.lockedUntil != null && Instant.now().isAfter(record.lockedUntil)
                val stale = record.lockedUntil == null && record.lastAttemptAt.isBefore(cutoff)
                lockExpired || stale
            }
        if (removed) {
            logger.debug("LoginAttemptService cleanup: map size now {}", attempts.size)
        }
    }

    private fun evictOldestEntries() {
        val toRemove =
            attempts.entries
                .filter { (_, v) -> v.lockedUntil == null }
                .sortedBy { (_, v) -> v.lastAttemptAt }
                .take(MAX_TRACKED_USERNAMES / 4)
                .map { it.key }
        toRemove.forEach { attempts.remove(it) }
    }
}
