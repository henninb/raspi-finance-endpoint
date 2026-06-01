package finance.utils

import jakarta.persistence.EntityNotFoundException
import spock.lang.Specification

class OptionalExtensionsSpec extends Specification {

    def 'orThrowNotFound returns value when Optional is present'() {
        given:
        def optional = Optional.of('found_value')

        when:
        def result = OptionalExtensionsKt.orThrowNotFound(optional, 'Entity', null)

        then:
        result == 'found_value'
        0 * _
    }

    def 'orThrowNotFound throws with entity name only when id is null and Optional is empty'() {
        given:
        def optional = Optional.empty()

        when:
        OptionalExtensionsKt.orThrowNotFound(optional, 'Account', null)

        then:
        def ex = thrown(EntityNotFoundException)
        ex.message == 'Account not found'
        0 * _
    }

    def 'orThrowNotFound throws with entity name and id when id is not null and Optional is empty'() {
        given:
        def optional = Optional.empty()

        when:
        OptionalExtensionsKt.orThrowNotFound(optional, 'Account', 42L)

        then:
        def ex = thrown(EntityNotFoundException)
        ex.message == 'Account not found: 42'
        0 * _
    }

    def 'orThrowNotFound throws with string id when Optional is empty'() {
        given:
        def optional = Optional.empty()

        when:
        OptionalExtensionsKt.orThrowNotFound(optional, 'Transaction', 'some-guid')

        then:
        def ex = thrown(EntityNotFoundException)
        ex.message == 'Transaction not found: some-guid'
        0 * _
    }

    def 'orThrowNotFound with present Optional and non-null id returns value'() {
        given:
        def optional = Optional.of(123L)

        when:
        def result = OptionalExtensionsKt.orThrowNotFound(optional, 'Account', 42L)

        then:
        result == 123L
        0 * _
    }
}
