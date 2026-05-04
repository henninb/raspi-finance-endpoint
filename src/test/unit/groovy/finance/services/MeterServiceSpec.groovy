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

    def "no-arg constructor creates a service with SimpleMeterRegistry"() {
        when:
        def service = new MeterService()

        then:
        service != null
        service.setHostName() != null
    }

    def "incrementExceptionCaughtCounter registers and increments counter"() {
        given:
        def registry = new SimpleMeterRegistry()
        def service = new MeterService(registry, "test-server")

        when:
        service.incrementExceptionCaughtCounter("SomeException")

        then:
        def c = registry.get(Constants.EXCEPTION_CAUGHT_COUNTER)
                .tags(Constants.EXCEPTION_NAME_TAG, "SomeException",
                      Constants.SERVER_NAME_TAG, "test-server")
                .counter()
        c.count() == 1d
    }

    def "incrementTransactionSuccessfullyInsertedCounter registers and increments counter"() {
        given:
        def registry = new SimpleMeterRegistry()
        def service = new MeterService(registry, "s1")
        def account = "checking_primary"

        when:
        service.incrementTransactionSuccessfullyInsertedCounter(account)

        then:
        registry.get(Constants.TRANSACTION_SUCCESSFULLY_INSERTED_COUNTER)
                .tags(Constants.ACCOUNT_NAME_OWNER_TAG, account, Constants.SERVER_NAME_TAG, "s1")
                .counter().count() == 1d
    }

    def "incrementTransactionAlreadyExistsCounter registers and increments counter"() {
        given:
        def registry = new SimpleMeterRegistry()
        def service = new MeterService(registry, "s1")
        def account = "savings_primary"

        when:
        service.incrementTransactionAlreadyExistsCounter(account)

        then:
        registry.get(Constants.TRANSACTION_ALREADY_EXISTS_COUNTER)
                .tags(Constants.ACCOUNT_NAME_OWNER_TAG, account, Constants.SERVER_NAME_TAG, "s1")
                .counter().count() == 1d
    }

    def "incrementTransactionRestSelectNoneFoundCounter registers and increments counter"() {
        given:
        def registry = new SimpleMeterRegistry()
        def service = new MeterService(registry, "s1")
        def account = "checking_primary"

        when:
        service.incrementTransactionRestSelectNoneFoundCounter(account)

        then:
        registry.get(Constants.TRANSACTION_REST_SELECT_NONE_FOUND_COUNTER)
                .tags(Constants.ACCOUNT_NAME_OWNER_TAG, account, Constants.SERVER_NAME_TAG, "s1")
                .counter().count() == 1d
    }

    def "incrementTransactionRestTransactionStateUpdateFailureCounter registers and increments counter"() {
        given:
        def registry = new SimpleMeterRegistry()
        def service = new MeterService(registry, "s1")
        def account = "checking_primary"

        when:
        service.incrementTransactionRestTransactionStateUpdateFailureCounter(account)

        then:
        registry.get(Constants.TRANSACTION_REST_TRANSACTION_STATE_UPDATE_FAILURE_COUNTER)
                .tags(Constants.ACCOUNT_NAME_OWNER_TAG, account, Constants.SERVER_NAME_TAG, "s1")
                .counter().count() == 1d
    }

    def "incrementTransactionRestReoccurringStateUpdateFailureCounter registers and increments counter"() {
        given:
        def registry = new SimpleMeterRegistry()
        def service = new MeterService(registry, "s1")
        def account = "checking_primary"

        when:
        service.incrementTransactionRestReoccurringStateUpdateFailureCounter(account)

        then:
        registry.get(Constants.TRANSACTION_REST_REOCCURRING_TYPE_UPDATE_FAILURE_COUNTER)
                .tags(Constants.ACCOUNT_NAME_OWNER_TAG, account, Constants.SERVER_NAME_TAG, "s1")
                .counter().count() == 1d
    }

    def "incrementAccountListIsEmpty registers and increments counter"() {
        given:
        def registry = new SimpleMeterRegistry()
        def service = new MeterService(registry, "s1")
        def account = "empty_account"

        when:
        service.incrementAccountListIsEmpty(account)

        then:
        registry.get(Constants.TRANSACTION_ACCOUNT_LIST_NONE_FOUND_COUNTER)
                .tags(Constants.ACCOUNT_NAME_OWNER_TAG, account, Constants.SERVER_NAME_TAG, "s1")
                .counter().count() == 1d
    }

    def "incrementTransactionReceiptImageInserted registers and increments counter"() {
        given:
        def registry = new SimpleMeterRegistry()
        def service = new MeterService(registry, "s1")
        def account = "checking_primary"

        when:
        service.incrementTransactionReceiptImageInserted(account)

        then:
        registry.get(Constants.TRANSACTION_RECEIPT_IMAGE_INSERTED_COUNTER)
                .tags(Constants.ACCOUNT_NAME_OWNER_TAG, account, Constants.SERVER_NAME_TAG, "s1")
                .counter().count() == 1d
    }

    def "constructor with single registry uses hostname fallback when name is blank"() {
        given:
        def registry = new SimpleMeterRegistry()

        when:
        def service = new MeterService(registry)
        service.incrementExceptionCaughtCounter("TestEx")

        then:
        registry.get(Constants.EXCEPTION_CAUGHT_COUNTER)
                .tags(Constants.EXCEPTION_NAME_TAG, "TestEx")
                .counter().count() == 1d
    }
}
