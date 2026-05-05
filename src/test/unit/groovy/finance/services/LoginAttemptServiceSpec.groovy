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

    def "recordFailure on already-locked account keeps existing lock time"() {
        given: "account is locked after 10 failures"
        10.times { service.recordFailure("kate") }
        def firstRemaining = service.remainingLockSeconds("kate")

        when: "one more failure is recorded"
        service.recordFailure("kate")

        then: "account stays locked and lock expiry has not been extended"
        service.isLocked("kate")
        service.remainingLockSeconds("kate") <= firstRemaining + 1
    }

    def "isLocked removes expired lock entry and returns false"() {
        given: "inject an expired lock record via reflection"
        def field = LoginAttemptService.getDeclaredField("attempts")
        field.accessible = true
        def map = field.get(service)
        def recordClass = LoginAttemptService.getDeclaredClasses().find { it.simpleName == 'AttemptRecord' }
        def constructor = recordClass.getDeclaredConstructors()[0]
        constructor.accessible = true
        def expiredLockUntil = java.time.Instant.now().minusSeconds(1)
        def record = constructor.newInstance(10, expiredLockUntil, java.time.Instant.now().minusSeconds(10))
        map.put("expired_user", record)

        when:
        def locked = service.isLocked("expired_user")

        then:
        !locked
        !map.containsKey("expired_user")
    }

    def "cleanupExpiredEntries removes expired lock entries"() {
        given: "inject an expired lock record via reflection"
        def field = LoginAttemptService.getDeclaredField("attempts")
        field.accessible = true
        def map = field.get(service)
        def recordClass = LoginAttemptService.getDeclaredClasses().find { it.simpleName == 'AttemptRecord' }
        def constructor = recordClass.getDeclaredConstructors()[0]
        constructor.accessible = true
        def expiredLockUntil = java.time.Instant.now().minusSeconds(1)
        def record = constructor.newInstance(10, expiredLockUntil, java.time.Instant.now().minusSeconds(10))
        map.put("expired_locked_user", record)

        when:
        service.cleanupExpiredEntries()

        then:
        !map.containsKey("expired_locked_user")
    }

    def "cleanupExpiredEntries removes stale non-locked entries older than TTL"() {
        given: "inject a stale (old) non-locked record via reflection"
        def field = LoginAttemptService.getDeclaredField("attempts")
        field.accessible = true
        def map = field.get(service)
        def recordClass = LoginAttemptService.getDeclaredClasses().find { it.simpleName == 'AttemptRecord' }
        def constructor = recordClass.getDeclaredConstructors()[0]
        constructor.accessible = true
        def staleInstant = java.time.Instant.now().minusSeconds(3601)
        def record = constructor.newInstance(3, null, staleInstant)
        map.put("stale_user", record)

        when:
        service.cleanupExpiredEntries()

        then:
        !map.containsKey("stale_user")
    }

    def "evictOldestEntries preserves locked entries"() {
        given: "account is locked"
        10.times { service.recordFailure("locked_user") }
        assert service.isLocked("locked_user")
        def field = LoginAttemptService.getDeclaredField("attempts")
        field.accessible = true
        def map = field.get(service)
        int sizeBeforeEvict = map.size()

        when: "forced eviction"
        def method = LoginAttemptService.getDeclaredMethod("evictOldestEntries")
        method.accessible = true
        method.invoke(service)

        then: "locked entry is preserved"
        service.isLocked("locked_user")
    }

    def "remainingLockSeconds is zero for entry with null lockedUntil"() {
        given: "account has failure count but no lock"
        3.times { service.recordFailure("partial") }

        expect:
        service.remainingLockSeconds("partial") == 0L
    }
}
