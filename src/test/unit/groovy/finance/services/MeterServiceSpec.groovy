package finance.services

import finance.utils.Constants
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import spock.lang.Specification

class MeterServiceSpec extends Specification {

    def "uses configured server name and tags counters accordingly"() {
        given:
        def registry = new SimpleMeterRegistry()
        def service = new MeterService(registry, "test-server")

        when:
        service.incrementExceptionThrownCounter("TestException")

        then:
        def c = registry.get(Constants.EXCEPTION_THROWN_COUNTER)
                .tags(Constants.EXCEPTION_NAME_TAG, "TestException",
                      Constants.SERVER_NAME_TAG, "test-server")
                .counter()
        c.count() == 1d
    }

    def "setHostName returns a non-null value for environment-derived host"() {
        given:
        def registry = new SimpleMeterRegistry()
        def service = new MeterService(registry) // no configured server name

        when:
        def host = service.setHostName()

        then:
        host != null
        host instanceof String
        host.length() >= 0
    }

    def "increments transaction state updated cleared counter with tags"() {
        given:
        def registry = new SimpleMeterRegistry()
        def service = new MeterService(registry, "acct-srv")
        def owner = "acct_owner"

        when:
        service.incrementTransactionUpdateClearedCounter(owner)

        then:
        def counter = registry.get(Constants.TRANSACTION_TRANSACTION_STATE_UPDATED_CLEARED_COUNTER)
                .tags(Constants.ACCOUNT_NAME_OWNER_TAG, owner,
                      Constants.SERVER_NAME_TAG, "acct-srv")
                .counter()
        counter.count() == 1d
    }
}
