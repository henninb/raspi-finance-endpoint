package finance.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(
    name = "t_token_blacklist",
    indexes = [
        Index(name = "idx_token_blacklist_token_hash", columnList = "token_hash", unique = true),
        Index(name = "idx_token_blacklist_expires_at", columnList = "expires_at"),
    ],
)
class TokenBlacklist(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @SequenceGenerator(name = "t_token_blacklist_token_blacklist_id_seq")
    @Column(name = "token_blacklist_id", nullable = false)
    var tokenBlacklistId: Long = 0,
    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    var tokenHash: String = "",
    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant = Instant.EPOCH,
)
