package finance.repositories

import finance.BaseIntegrationSpec
import finance.domain.TokenBlacklist
import org.springframework.beans.factory.annotation.Autowired

import java.time.Instant

class TokenBlacklistRepositoryIntSpec extends BaseIntegrationSpec {

    @Autowired
    TokenBlacklistRepository tokenBlacklistRepository

    void 'should save and find a blacklisted token by hash'() {
        given:
        String hash = "sha256-" + UUID.randomUUID().toString().replace("-", "")
        TokenBlacklist token = new TokenBlacklist(
            tokenHash: hash,
            expiresAt: Instant.now().plusSeconds(3600)
        )

        when:
        TokenBlacklist saved = tokenBlacklistRepository.save(token)

        then:
        saved.tokenBlacklistId > 0
        saved.tokenHash == hash
        saved.expiresAt != null

        when:
        boolean exists = tokenBlacklistRepository.existsByTokenHash(hash)

        then:
        exists == true
    }

    void 'existsByTokenHash returns false for unknown hash'() {
        when:
        boolean exists = tokenBlacklistRepository.existsByTokenHash("nonexistent-hash-" + UUID.randomUUID())

        then:
        exists == false
    }

    void 'deleteAllExpiredBefore removes only expired tokens'() {
        given:
        String expiredHash = "expired-" + UUID.randomUUID().toString().replace("-", "")
        String activeHash = "active-" + UUID.randomUUID().toString().replace("-", "")

        TokenBlacklist expiredToken = new TokenBlacklist(
            tokenHash: expiredHash,
            expiresAt: Instant.now().minusSeconds(7200)
        )
        TokenBlacklist activeToken = new TokenBlacklist(
            tokenHash: activeHash,
            expiresAt: Instant.now().plusSeconds(3600)
        )

        tokenBlacklistRepository.save(expiredToken)
        tokenBlacklistRepository.save(activeToken)

        when:
        int deletedCount = tokenBlacklistRepository.deleteAllExpiredBefore(Instant.now())

        then:
        deletedCount >= 1
        !tokenBlacklistRepository.existsByTokenHash(expiredHash)
        tokenBlacklistRepository.existsByTokenHash(activeHash)
    }

    void 'deleteAllExpiredBefore with future threshold deletes nothing'() {
        given:
        String hash = "future-" + UUID.randomUUID().toString().replace("-", "")
        TokenBlacklist token = new TokenBlacklist(
            tokenHash: hash,
            expiresAt: Instant.now().plusSeconds(3600)
        )
        tokenBlacklistRepository.save(token)

        when:
        int deletedCount = tokenBlacklistRepository.deleteAllExpiredBefore(Instant.now().minusSeconds(7200))

        then:
        deletedCount == 0
        tokenBlacklistRepository.existsByTokenHash(hash)
    }

    void 'basic JPA CRUD operations work on token blacklist'() {
        given:
        String hash = "crud-" + UUID.randomUUID().toString().replace("-", "")
        TokenBlacklist token = new TokenBlacklist(
            tokenHash: hash,
            expiresAt: Instant.now().plusSeconds(1800)
        )

        when:
        TokenBlacklist saved = tokenBlacklistRepository.save(token)
        long id = saved.tokenBlacklistId

        then:
        tokenBlacklistRepository.findById(id).isPresent()
        tokenBlacklistRepository.existsByTokenHash(hash)

        when:
        tokenBlacklistRepository.deleteById(id)

        then:
        !tokenBlacklistRepository.findById(id).isPresent()
        !tokenBlacklistRepository.existsByTokenHash(hash)
    }
}
