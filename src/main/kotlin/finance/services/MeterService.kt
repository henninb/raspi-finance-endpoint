package finance.services

import finance.utils.Constants.ACCOUNT_NAME_TAG
import finance.utils.Constants.TRANSACTION_ALREADY_EXISTS_COUNTER
import finance.utils.Constants.TRANSACTION_RECEIVED_EVENT_COUNTER
import finance.utils.Constants.TRANSACTION_SUCCESSFULLY_INSERTED_COUNTER
import finance.utils.Constants.TRANSACTION_UPDATE_CLEARED_COUNTER
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Service

@Service
class MeterService(private var meterRegistry: MeterRegistry) {
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

//        Counter.builder(Constants.ERROR_COUNTER)
//                .tag(Constants.ACCOUNT_NAME_TAG, "")
//                .tag(Constants.ERROR_TYPE_TAG, "")
//                .register(meterRegistry)
    }
//
//    enum class ErrorType(val errorMessage: String) {
//        VALIDATION_ERROR("validation.error"),
//        DELAY("delay.error");
//    }
//

//
//    fun incrementErrorCounter(accountName: String, errorType: ErrorType) {
//        meterRegistry.counter(Constants.ERROR_COUNTER, Constants.ERROR_TYPE_TAG,
//                errorType.errorMessage, Constants.ACCOUNT_NAME_TAG, accountName).increment()
//    }

    fun incrementTransactionUpdateClearedCounter(accountName: String) {
        meterRegistry.counter(TRANSACTION_UPDATE_CLEARED_COUNTER, ACCOUNT_NAME_TAG, accountName).increment()
    }

    fun incrementTransactionSuccessfullyInsertedCounter(accountName: String) {
        meterRegistry.counter(TRANSACTION_SUCCESSFULLY_INSERTED_COUNTER, ACCOUNT_NAME_TAG, accountName).increment()
    }

    fun incrementTransactionAlreadyExistsCounter(accountName: String) {
        meterRegistry.counter(TRANSACTION_ALREADY_EXISTS_COUNTER, ACCOUNT_NAME_TAG, accountName).increment()
    }

    fun incrementTransactionReceivedCounter(accountName: String) {
        meterRegistry.counter(TRANSACTION_RECEIVED_EVENT_COUNTER, ACCOUNT_NAME_TAG, accountName).increment()
    }
}