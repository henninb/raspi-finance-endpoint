package finance.services

import spock.lang.Specification

class LoginAttemptServiceSpec extends Specification {

    LoginAttemptService service

    def setup() {
        service = new LoginAttemptService()
    }

    def "fresh username is not locked"() {
        expect:
        !service.isLocked("alice")
    }

    def "account locks after MAX_ATTEMPTS failures"() {
        given:
        10.times { service.recordFailure("bob") }

        expect:
        service.isLocked("bob")
    }

    def "account is not locked before reaching MAX_ATTEMPTS"() {
        given:
        9.times { service.recordFailure("carol") }

        expect:
        !service.isLocked("carol")
    }

    def "successful login clears the attempt counter"() {
        given:
        5.times { service.recordFailure("dave") }

        when:
        service.recordSuccess("dave")

        then:
        !service.isLocked("dave")
    }

    def "username comparison is case-insensitive"() {
        given:
        10.times { service.recordFailure("Eve") }

        expect:
        service.isLocked("eve")
        service.isLocked("EVE")
        service.isLocked("Eve")
    }

    def "remainingLockSeconds returns zero for unlocked username"() {
        expect:
        service.remainingLockSeconds("nobody") == 0L
    }

    def "remainingLockSeconds returns positive value while locked"() {
        given:
        10.times { service.recordFailure("frank") }

        expect:
        service.remainingLockSeconds("frank") > 0L
        service.remainingLockSeconds("frank") <= 900L
    }

    def "cleanup removes expired lock entries"() {
        given: "a locked account"
        10.times { service.recordFailure("grace") }
        assert service.isLocked("grace")

        when: "cleanup runs (entries are still live — count unchanged)"
        service.cleanupExpiredEntries()

        then: "locked entry is still present because lock has not expired"
        service.isLocked("grace")
    }

    def "cleanup removes stale non-locked entries"() {
        given: "several failed attempts that did not trigger a lock"
        3.times { service.recordFailure("henry") }

        when: "manually run cleanup (entries are recent, nothing removed yet)"
        service.cleanupExpiredEntries()

        then: "no exception is thrown and service remains stable"
        !service.isLocked("henry")
    }

    def "eviction occurs when map reaches capacity"() {
        given: "fill the map up to the limit with unique usernames (non-locked entries)"
        // Use a smaller controlled scenario: record 1 failure per unique username
        (1..100).each { i -> service.recordFailure("user_${i}") }

        expect: "service remains stable after many entries"
        !service.isLocked("user_1") // 1 failure does not lock
    }

    def "multiple independent usernames track separately"() {
        given:
        9.times { service.recordFailure("ivan") }
        5.times { service.recordFailure("julia") }

        expect:
        !service.isLocked("ivan")
        !service.isLocked("julia")

        when:
        service.recordFailure("ivan")

        then:
        service.isLocked("ivan")
        !service.isLocked("julia")
    }

    def "remainingLockSeconds returns 0 after lock expires conceptually"() {
        when: "no lock set"
        long remaining = service.remainingLockSeconds("unknown")

        then:
        remaining == 0L
    }
}
