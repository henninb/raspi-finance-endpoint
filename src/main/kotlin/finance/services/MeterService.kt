package finance.services

import finance.utils.Constants.ACCOUNT_NAME_OWNER_TAG
import finance.utils.Constants.EXCEPTION_CAUGHT_COUNTER
import finance.utils.Constants.EXCEPTION_THROWN_COUNTER
import finance.utils.Constants.EXCEPTION_NAME_TAG
import finance.utils.Constants.TRANSACTION_ALREADY_EXISTS_COUNTER
import finance.utils.Constants.TRANSACTION_ACCOUNT_LIST_NONE_FOUND_COUNTER
import finance.utils.Constants.TRANSACTION_RECEIPT_IMAGE_INSERTED_COUNTER
import finance.utils.Constants.TRANSACTION_REST_REOCCURRING_STATE_UPDATE_FAILURE_COUNTER
import finance.utils.Constants.TRANSACTION_REST_SELECT_NONE_FOUND_COUNTER
import finance.utils.Constants.TRANSACTION_REST_TRANSACTION_STATE_UPDATE_FAILURE_COUNTER
import finance.utils.Constants.TRANSACTION_SUCCESSFULLY_INSERTED_COUNTER
import finance.utils.Constants.TRANSACTION_TRANSACTION_STATE_UPDATED_CLEARED_COUNTER
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
open class MeterService(private var meterRegistry: MeterRegistry) {
    init {
        Counter.builder(TRANSACTION_ALREADY_EXISTS_COUNTER)
            .tag(ACCOUNT_NAME_OWNER_TAG, "")
            .register(meterRegistry)

        Counter.builder(TRANSACTION_SUCCESSFULLY_INSERTED_COUNTER)
            .tag(ACCOUNT_NAME_OWNER_TAG, "")
            .register(meterRegistry)

        Counter.builder(TRANSACTION_TRANSACTION_STATE_UPDATED_CLEARED_COUNTER)
            .tag(ACCOUNT_NAME_OWNER_TAG, "")
            .register(meterRegistry)

        Counter.builder(EXCEPTION_THROWN_COUNTER)
            .tag(EXCEPTION_NAME_TAG, "")
            .register(meterRegistry)

        Counter.builder(EXCEPTION_CAUGHT_COUNTER)
            .tag(EXCEPTION_NAME_TAG, "")
            .register(meterRegistry)

        Counter.builder(TRANSACTION_RECEIPT_IMAGE_INSERTED_COUNTER)
            .tag(ACCOUNT_NAME_OWNER_TAG, "")
            .register(meterRegistry)

        Counter.builder(TRANSACTION_ACCOUNT_LIST_NONE_FOUND_COUNTER)
            .tag(ACCOUNT_NAME_OWNER_TAG, "")
            .register(meterRegistry)

        Counter.builder(TRANSACTION_REST_SELECT_NONE_FOUND_COUNTER)
            .tag(ACCOUNT_NAME_OWNER_TAG, "")
            .register(meterRegistry)

        Counter.builder(TRANSACTION_REST_TRANSACTION_STATE_UPDATE_FAILURE_COUNTER)
            .tag(ACCOUNT_NAME_OWNER_TAG, "")
            .register(meterRegistry)

        Counter.builder(TRANSACTION_REST_REOCCURRING_STATE_UPDATE_FAILURE_COUNTER)
            .tag(ACCOUNT_NAME_OWNER_TAG, "")
            .register(meterRegistry)
    }

    @Transactional
    open fun incrementExceptionThrownCounter(exceptionName: String) {
        meterRegistry.counter(EXCEPTION_THROWN_COUNTER, EXCEPTION_NAME_TAG, exceptionName).increment()
    }

    @Transactional
    open fun incrementExceptionCaughtCounter(exceptionName: String) {
        meterRegistry.counter(EXCEPTION_THROWN_COUNTER, EXCEPTION_NAME_TAG, exceptionName).increment()
    }

    @Transactional
    open fun incrementTransactionUpdateClearedCounter(accountName: String) {
        meterRegistry.counter(TRANSACTION_TRANSACTION_STATE_UPDATED_CLEARED_COUNTER, ACCOUNT_NAME_OWNER_TAG, accountName).increment()
    }

    @Transactional
    open fun incrementTransactionSuccessfullyInsertedCounter(accountNameOwner: String) {
        meterRegistry.counter(TRANSACTION_SUCCESSFULLY_INSERTED_COUNTER, ACCOUNT_NAME_OWNER_TAG, accountNameOwner).increment()
    }

    @Transactional
    open fun incrementTransactionAlreadyExistsCounter(accountNameOwner: String) {
        meterRegistry.counter(TRANSACTION_ALREADY_EXISTS_COUNTER, ACCOUNT_NAME_OWNER_TAG, accountNameOwner).increment()
    }

    @Transactional
    open fun incrementTransactionRestSelectNoneFoundCounter(accountNameOwner: String) {
        meterRegistry.counter(TRANSACTION_REST_SELECT_NONE_FOUND_COUNTER, ACCOUNT_NAME_OWNER_TAG, accountNameOwner).increment()
    }

    @Transactional
    open fun incrementTransactionRestTransactionStateUpdateFailureCounter(accountNameOwner: String) {
        meterRegistry.counter(TRANSACTION_REST_TRANSACTION_STATE_UPDATE_FAILURE_COUNTER, ACCOUNT_NAME_OWNER_TAG, accountNameOwner).increment()
    }

    @Transactional
    open fun incrementTransactionRestReoccurringStateUpdateFailureCounter(accountNameOwner: String) {
        meterRegistry.counter(TRANSACTION_REST_REOCCURRING_STATE_UPDATE_FAILURE_COUNTER, ACCOUNT_NAME_OWNER_TAG, accountNameOwner).increment()
    }

    @Transactional
    open fun incrementAccountListIsEmpty(accountNameOwner: String) {
        meterRegistry.counter(TRANSACTION_ACCOUNT_LIST_NONE_FOUND_COUNTER, ACCOUNT_NAME_OWNER_TAG, accountNameOwner).increment()
    }

    @Transactional
    open fun incrementTransactionReceiptImageInserted(accountNameOwner: String) {
        meterRegistry.counter(TRANSACTION_RECEIPT_IMAGE_INSERTED_COUNTER, ACCOUNT_NAME_OWNER_TAG, accountNameOwner).increment()
    }
}