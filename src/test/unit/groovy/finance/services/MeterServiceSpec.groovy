package finance.services

import io.micrometer.core.instrument.*
import spock.lang.Specification

import static finance.utils.Constants.*

@SuppressWarnings("GroovyAccessibility")
class MeterServiceSpec extends Specification {

    protected MeterRegistry meterRegistryMock = GroovyMock(MeterRegistry)
    protected MeterService meterService = new MeterService(meterRegistryMock)
    protected Counter counter = Mock(Counter)

    static Meter.Id setMeterId(String counterName, String accountNameOwner) {
        Tag serverNameTag = Tag.of(SERVER_NAME_TAG, 'server')
        Tag accountNameOwnerTag = Tag.of(ACCOUNT_NAME_OWNER_TAG, accountNameOwner)
        Tags tags = Tags.of(accountNameOwnerTag, serverNameTag)
        return new Meter.Id(counterName, tags, null, null, Meter.Type.COUNTER)
    }

    void 'test increment account list is empty'() {
        when:
        meterService.incrementAccountListIsEmpty('test')

        then:
        1 * meterRegistryMock.counter(setMeterId(TRANSACTION_ACCOUNT_LIST_NONE_FOUND_COUNTER, 'test')) >> counter
        1 * counter.increment()
        0 * _
    }

    void 'IncrementTransactionUpdateClearedCounter'() {
        when:
        meterService.incrementTransactionUpdateClearedCounter('test')

        then:
        1 * meterRegistryMock.counter(setMeterId(TRANSACTION_TRANSACTION_STATE_UPDATED_CLEARED_COUNTER, 'test')) >> counter
        1 * counter.increment()
        0 * _
    }

    void "IncrementTransactionSuccessfullyInsertedCounter"() {
        when:
        meterService.incrementTransactionUpdateClearedCounter('test')

        then:
        1 * meterRegistryMock.counter(setMeterId(TRANSACTION_TRANSACTION_STATE_UPDATED_CLEARED_COUNTER, 'test')) >> counter
        1 * counter.increment()
        0 * _
    }

    void "IncrementTransactionAlreadyExistsCounter"() {
        when:
        meterService.incrementTransactionAlreadyExistsCounter('test')

        then:
        1 * meterRegistryMock.counter(setMeterId(TRANSACTION_ALREADY_EXISTS_COUNTER, 'test')) >> counter
        1 * counter.increment()
        0 * _
    }

    void "IncrementTransactionRestSelectNoneFoundCounter"() {
        when:
        meterService.incrementTransactionRestSelectNoneFoundCounter('test')

        then:
        1 * meterRegistryMock.counter(setMeterId(TRANSACTION_REST_SELECT_NONE_FOUND_COUNTER, 'test')) >> counter
        1 * counter.increment()
        0 * _
    }

    void "IncrementTransactionRestTransactionStateUpdateFailureCounter"() {
        when:
        meterService.incrementTransactionRestTransactionStateUpdateFailureCounter('test')

        then:
        1 * meterRegistryMock.counter(setMeterId(TRANSACTION_REST_TRANSACTION_STATE_UPDATE_FAILURE_COUNTER, 'test')) >> counter
        1 * counter.increment()
        0 * _
    }

    void "IncrementTransactionRestReoccurringStateUpdateFailureCounter"() {
        when:
        meterService.incrementTransactionRestReoccurringStateUpdateFailureCounter('test')

        then:
        1 * meterRegistryMock.counter(setMeterId(TRANSACTION_REST_REOCCURRING_TYPE_UPDATE_FAILURE_COUNTER, 'test')) >> counter
        1 * counter.increment()
        0 * _
    }

    void "IncrementTransactionReceiptImageInserted"() {
        when:
        meterService.incrementTransactionReceiptImageInserted('test')

        then:
        1 * meterRegistryMock.counter(setMeterId(TRANSACTION_RECEIPT_IMAGE_INSERTED_COUNTER, 'test')) >> counter
        1 * counter.increment()
        0 * _
    }

}