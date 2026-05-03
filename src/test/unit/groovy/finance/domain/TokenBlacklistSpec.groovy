package finance.domain

import spock.lang.Specification
import java.time.Instant

class TokenBlacklistSpec extends Specification {

    def "should create TokenBlacklist with default values"() {
        when:
        def blacklist = new TokenBlacklist()

        then:
        blacklist.tokenBlacklistId == 0L
        blacklist.tokenHash == ""
        blacklist.expiresAt == Instant.EPOCH
    }

    def "should create TokenBlacklist with provided values"() {
        given:
        def id = 1L
        def hash = "somehash"
        def expiry = Instant.now()

        when:
        def blacklist = new TokenBlacklist(id, hash, expiry)

        then:
        blacklist.tokenBlacklistId == id
        blacklist.tokenHash == hash
        blacklist.expiresAt == expiry
    }

    def "should allow updating values"() {
        given:
        def blacklist = new TokenBlacklist()
        def newExpiry = Instant.now().plusSeconds(3600)

        when:
        blacklist.tokenBlacklistId = 5L
        blacklist.tokenHash = "updatedhash"
        blacklist.expiresAt = newExpiry

        then:
        blacklist.tokenBlacklistId == 5L
        blacklist.tokenHash == "updatedhash"
        blacklist.expiresAt == newExpiry
    }
}
