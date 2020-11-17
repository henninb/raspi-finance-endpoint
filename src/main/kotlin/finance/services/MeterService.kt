package finance.services

import finance.utils.Constants.ACCOUNT_NAME_TAG
import finance.utils.Constants.EXCEPTION_COUNTER
import finance.utils.Constants.EXCEPTION_NAME_TYPE_TAG
import finance.utils.Constants.TRANSACTION_ALREADY_EXISTS_COUNTER
import finance.utils.Constants.TRANSACTION_LIST_IS_EMPTY
import finance.utils.Constants.TRANSACTION_RECEIPT_IMAGE
import finance.utils.Constants.TRANSACTION_RECEIVED_EVENT_COUNTER
import finance.utils.Constants.TRANSACTION_SUCCESSFULLY_INSERTED_COUNTER
import finance.utils.Constants.TRANSACTION_UPDATE_CLEARED_COUNTER
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
open class MeterService(private var meterRegistry: MeterRegistry) {
    init {
        Counter.builder(TRANSACTION_RECEIVED_EVENT_COUNTER)
                .tag(ACCOUNT_NAME_TAG, "")
                .register(meterRegistry)

        Counter.builder(TRANSACTION_ALREADY_EXISTS_COUNTER)
                .tag(ACCOUNT_NAME_TAG, "")
                .register(meterRegistry)

        Counter.builder(TRANSACTION_SUCCESSFULLY_INSERTED_COUNTER)
                .tag(ACCOUNT_NAME_TAG, "")
                .register(meterRegistry)

        Counter.builder(TRANSACTION_UPDATE_CLEARED_COUNTER)
                .tag(ACCOUNT_NAME_TAG, "")
                .register(meterRegistry)

        Counter.builder(EXCEPTION_COUNTER)
                .tag(EXCEPTION_NAME_TYPE_TAG, "")
                .register(meterRegistry)

        Counter.builder(TRANSACTION_RECEIPT_IMAGE)
                .tag(ACCOUNT_NAME_TAG, "")
                .register(meterRegistry)

        Counter.builder(TRANSACTION_LIST_IS_EMPTY)
                .tag(ACCOUNT_NAME_TAG, "")
                .register(meterRegistry)

    }

    fun incrementExceptionCounter(exceptionName: String) {
        meterRegistry.counter(EXCEPTION_COUNTER, EXCEPTION_NAME_TYPE_TAG, exceptionName).increment()
    }

    @Transactional
    open fun incrementTransactionUpdateClearedCounter(accountName: String) {
        meterRegistry.counter(TRANSACTION_UPDATE_CLEARED_COUNTER, ACCOUNT_NAME_TAG, accountName).increment()
    }

    @Transactional
    open fun incrementTransactionSuccessfullyInsertedCounter(accountNameOwner: String) {
        meterRegistry.counter(TRANSACTION_SUCCESSFULLY_INSERTED_COUNTER, ACCOUNT_NAME_TAG, accountNameOwner).increment()
    }

    @Transactional
    open fun incrementTransactionAlreadyExistsCounter(accountNameOwner: String) {
        meterRegistry.counter(TRANSACTION_ALREADY_EXISTS_COUNTER, ACCOUNT_NAME_TAG, accountNameOwner).increment()
    }

    @Transactional
    open fun incrementTransactionReceivedCounter(accountNameOwner: String) {
        meterRegistry.counter(TRANSACTION_RECEIVED_EVENT_COUNTER, ACCOUNT_NAME_TAG, accountNameOwner).increment()
    }

    fun incrementAccountListIsEmpty(accountNameOwner: String) {
        meterRegistry.counter(TRANSACTION_LIST_IS_EMPTY, ACCOUNT_NAME_TAG, accountNameOwner).increment()
    }

    @Transactional
    open fun incrementTransactionReceiptImage(accountNameOwner: String) {
        meterRegistry.counter(TRANSACTION_RECEIPT_IMAGE, ACCOUNT_NAME_TAG, accountNameOwner).increment()
    }
}