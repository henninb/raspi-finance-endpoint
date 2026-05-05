package finance.services

import finance.repositories.TokenBlacklistRepository
import java.time.Instant
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class TokenBlacklistServiceSpec extends spock.lang.Specification {

    def tokenBlacklistRepository = Mock(TokenBlacklistRepository)
    TokenBlacklistService service

    def cleanup() {
        if (service != null) {
            try {
                service.shutdown()
            } catch (Throwable ignored) {
            }
        }
        Thread.interrupted()
    }

    def "blacklistToken should persist hashed token with expiration"() {
        given:
        service = new TokenBlacklistService(tokenBlacklistRepository)
        def token = "jwt-token-123"
        def expiration = Instant.parse("2026-05-03T18:30:00Z").toEpochMilli()

        when:
        service.blacklistToken(token, expiration)

        then:
        1 * tokenBlacklistRepository.save(_) >> { saved ->
            assert saved[0].tokenHash != token
            assert saved[0].tokenHash.size() == 64
            assert saved[0].expiresAt == Instant.ofEpochMilli(expiration)
            saved[0]
        }
        0 * _
    }

    def "isBlacklisted should hash token before repository lookup"() {
        given:
        service = new TokenBlacklistService(tokenBlacklistRepository)

        when:
        def blacklisted = service.isBlacklisted("jwt-token-123")

        then:
        1 * tokenBlacklistRepository.existsByTokenHash({ it.size() == 64 && it != "jwt-token-123" }) >> true
        blacklisted
        0 * _
    }

    def "getBlacklistSize should delegate to repository count"() {
        given:
        service = new TokenBlacklistService(tokenBlacklistRepository)

        when:
        def size = service.getBlacklistSize()

        then:
        1 * tokenBlacklistRepository.count() >> 12L
        size == 12
        0 * _
    }

    def "cleanupExpiredTokens should delete expired entries using current instant"() {
        given:
        service = new TokenBlacklistService(tokenBlacklistRepository)

        when:
        def method = TokenBlacklistService.getDeclaredMethod("cleanupExpiredTokens")
        method.accessible = true
        method.invoke(service)

        then:
        1 * tokenBlacklistRepository.deleteAllExpiredBefore(_ as Instant) >> { Instant now ->
            assert now.isBefore(Instant.now().plusSeconds(1))
            assert now.isAfter(Instant.now().minusSeconds(5))
            3
        }
        0 * _
    }

    def "shutdown should stop the internal executor"() {
        given:
        service = new TokenBlacklistService(tokenBlacklistRepository)
        def field = TokenBlacklistService.getDeclaredField("cleanup")
        field.accessible = true
        def cleanupExecutor = field.get(service) as ScheduledExecutorService

        when:
        service.shutdown()
        def isShutdown = cleanupExecutor.isShutdown()
        service = null

        then:
        isShutdown
    }

    def "shutdown should be safe to call twice"() {
        given:
        service = new TokenBlacklistService(tokenBlacklistRepository)

        when:
        service.shutdown()
        service.shutdown()
        service = null

        then:
        0 * _
    }

    def "isBlacklisted returns false for non-blacklisted token"() {
        given:
        service = new TokenBlacklistService(tokenBlacklistRepository)

        when:
        def blacklisted = service.isBlacklisted("not-blacklisted-token")

        then:
        1 * tokenBlacklistRepository.existsByTokenHash({ it.size() == 64 }) >> false
        !blacklisted
    }

    def "cleanupExpiredTokens does not log when zero tokens removed"() {
        given:
        service = new TokenBlacklistService(tokenBlacklistRepository)

        when:
        def method = TokenBlacklistService.getDeclaredMethod("cleanupExpiredTokens")
        method.accessible = true
        method.invoke(service)

        then:
        1 * tokenBlacklistRepository.deleteAllExpiredBefore(_ as java.time.Instant) >> 0
        0 * _
    }

    def "hashToken produces consistent SHA-256 hex for same input"() {
        given:
        service = new TokenBlacklistService(tokenBlacklistRepository)
        def method = TokenBlacklistService.getDeclaredMethod("hashToken", String)
        method.accessible = true

        when:
        def hash1 = method.invoke(service, "same-token")
        def hash2 = method.invoke(service, "same-token")

        then:
        hash1 == hash2
        hash1.length() == 64
        hash1 ==~ /[0-9a-f]{64}/
    }

    def "hashToken produces different values for different inputs"() {
        given:
        service = new TokenBlacklistService(tokenBlacklistRepository)
        def method = TokenBlacklistService.getDeclaredMethod("hashToken", String)
        method.accessible = true

        when:
        def hash1 = method.invoke(service, "token-a")
        def hash2 = method.invoke(service, "token-b")

        then:
        hash1 != hash2
    }

    def "blacklistToken stores hash not raw token"() {
        given:
        service = new TokenBlacklistService(tokenBlacklistRepository)
        def rawToken = "my-secret-jwt"
        def expiration = System.currentTimeMillis() + 3600_000L

        when:
        service.blacklistToken(rawToken, expiration)

        then:
        1 * tokenBlacklistRepository.save({
            it.tokenHash != rawToken && it.tokenHash.length() == 64
        })
    }
}
