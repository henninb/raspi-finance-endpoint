package finance.repositories

import finance.domain.TokenBlacklist
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Repository
interface TokenBlacklistRepository : JpaRepository<TokenBlacklist, Long> {
    fun existsByTokenHash(tokenHash: String): Boolean

    @Modifying
    @Transactional
    @Query("DELETE FROM TokenBlacklist t WHERE t.expiresAt < :now")
    fun deleteAllExpiredBefore(now: Instant): Int
}
