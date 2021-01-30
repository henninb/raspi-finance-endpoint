package finance.services

import finance.utils.Constants
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import spock.lang.Specification

@SuppressWarnings("GroovyAccessibility")
class MeterServiceSpec extends Specification {

    protected MeterRegistry meterRegistryMock = GroovyMock(MeterRegistry)
    protected MeterService meterService = new MeterService(meterRegistryMock)
    protected Counter counter = Mock(Counter)

    static Meter.Id setMeterId(String counterName, String accountNameOwner) {
        Tag serverNameTag = Tag.of(Constants.SERVER_NAME_TAG, 'server')
        Tag accountNameOwnerTag = Tag.of(Constants.ACCOUNT_NAME_OWNER_TAG, accountNameOwner)
        Tags tags = Tags.of(accountNameOwnerTag, serverNameTag)
        return new Meter.Id(counterName, tags, null, null, Meter.Type.COUNTER)
    }

    void 'test increment account list is empty'() {
        when:
        meterService.incrementAccountListIsEmpty('test')

        then:
        1 * meterRegistryMock.counter(setMeterId(Constants.TRANSACTION_ACCOUNT_LIST_NONE_FOUND_COUNTER, 'test')) >> counter
        1 * counter.increment()
        0 * _
    }

    void 'IncrementTransactionUpdateClearedCounter'() {
        when:
        meterService.incrementTransactionUpdateClearedCounter('test')

        then:
        1 * meterRegistryMock.counter(setMeterId(Constants.TRANSACTION_TRANSACTION_STATE_UPDATED_CLEARED_COUNTER, 'test')) >> counter
        1 * counter.increment()
        0 * _
    }

    void "IncrementTransactionSuccessfullyInsertedCounter"() {
        when:
        meterService.incrementTransactionUpdateClearedCounter('test')

        then:
        1 * meterRegistryMock.counter(setMeterId(Constants.TRANSACTION_TRANSACTION_STATE_UPDATED_CLEARED_COUNTER, 'test')) >> counter
        1 * counter.increment()
        0 * _
    }

    void "IncrementTransactionAlreadyExistsCounter"() {
        when:
        meterService.incrementTransactionAlreadyExistsCounter('test')

        then:
        1 * meterRegistryMock.counter(setMeterId(Constants.TRANSACTION_ALREADY_EXISTS_COUNTER, 'test')) >> counter
        1 * counter.increment()
        0 * _
    }

    void "IncrementTransactionRestSelectNoneFoundCounter"() {
        when:
        meterService.incrementTransactionRestSelectNoneFoundCounter('test')

        then:
        1 * meterRegistryMock.counter(setMeterId(Constants.TRANSACTION_REST_SELECT_NONE_FOUND_COUNTER, 'test')) >> counter
        1 * counter.increment()
        0 * _
    }

    void "IncrementTransactionRestTransactionStateUpdateFailureCounter"() {
        when:
        meterService.incrementTransactionRestTransactionStateUpdateFailureCounter('test')

        then:
        1 * meterRegistryMock.counter(setMeterId(Constants.TRANSACTION_REST_TRANSACTION_STATE_UPDATE_FAILURE_COUNTER, 'test')) >> counter
        1 * counter.increment()
        0 * _
    }

    void "IncrementTransactionRestReoccurringStateUpdateFailureCounter"() {
        when:
        meterService.incrementTransactionRestReoccurringStateUpdateFailureCounter('test')

        then:
        1 * meterRegistryMock.counter(setMeterId(Constants.TRANSACTION_REST_REOCCURRING_STATE_UPDATE_FAILURE_COUNTER, 'test')) >> counter
        1 * counter.increment()
        0 * _
    }

    void "IncrementTransactionReceiptImageInserted"() {
        when:
        meterService.incrementTransactionReceiptImageInserted('test')

        then:
        1 * meterRegistryMock.counter(setMeterId(Constants.TRANSACTION_RECEIPT_IMAGE_INSERTED_COUNTER, 'test')) >> counter
        1 * counter.increment()
        0 * _
    }

    void "IncrementCamelStringProcessor"() {
        when:
        meterService.incrementCamelStringProcessor('test')

        then:
        1 * meterRegistryMock.counter(setMeterId(Constants.CAMEL_STRING_PROCESSOR_COUNTER, 'test')) >> counter
        1 * counter.increment()
        0 * _
    }

    void "IncrementCamelTransactionSuccessfullyInsertedCounter"() {
        when:
        meterService.incrementCamelTransactionSuccessfullyInsertedCounter('test')

        then:
        1 * meterRegistryMock.counter(setMeterId(Constants.CAMEL_TRANSACTION_SUCCESSFULLY_INSERTED_COUNTER, 'test')) >> counter
        1 * counter.increment()
        0 * _
    }
}