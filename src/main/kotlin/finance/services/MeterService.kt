package finance.services

import finance.utils.Constants.ACCOUNT_NAME_OWNER_TAG
import finance.utils.Constants.EXCEPTION_CAUGHT_COUNTER
import finance.utils.Constants.EXCEPTION_NAME_TAG
import finance.utils.Constants.EXCEPTION_THROWN_COUNTER
import finance.utils.Constants.SERVER_NAME_TAG
import finance.utils.Constants.TRANSACTION_ACCOUNT_LIST_NONE_FOUND_COUNTER
import finance.utils.Constants.TRANSACTION_ALREADY_EXISTS_COUNTER
import finance.utils.Constants.TRANSACTION_RECEIPT_IMAGE_INSERTED_COUNTER
import finance.utils.Constants.TRANSACTION_REST_REOCCURRING_TYPE_UPDATE_FAILURE_COUNTER
import finance.utils.Constants.TRANSACTION_REST_SELECT_NONE_FOUND_COUNTER
import finance.utils.Constants.TRANSACTION_REST_TRANSACTION_STATE_UPDATE_FAILURE_COUNTER
import finance.utils.Constants.TRANSACTION_SUCCESSFULLY_INSERTED_COUNTER
import finance.utils.Constants.TRANSACTION_TRANSACTION_STATE_UPDATED_CLEARED_COUNTER
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class MeterService(
    private var meterRegistry: MeterRegistry,
    @param:Value("\${metrics.server-name:}") private val configuredServerName: String? = null,
) {
    constructor(meterRegistry: MeterRegistry) : this(meterRegistry, null)
    constructor() : this(SimpleMeterRegistry(), null)

    private val hostName: String =
        when {
            !configuredServerName.isNullOrBlank() -> configuredServerName
            else -> {
                val envHost = setHostName()
                if (envHost.isBlank() || envHost == "Unknown") "server" else envHost
            }
        }

    fun setHostName(): String {
        val env = System.getenv()
        return if (env.containsKey("COMPUTERNAME")) {
            env["COMPUTERNAME"] ?: "Unknown"
        } else if (env.containsKey("HOSTNAME")) {
            env["HOSTNAME"] ?: "Unknown"
        } else {
            "Unknown"
        }
    }

    fun incrementExceptionThrownCounter(exceptionName: String): Unit =
        Counter
            .builder(EXCEPTION_THROWN_COUNTER)
            .description("Increments the counter for every exception thrown.")
            .tags(
                listOfNotNull(
                    Tag.of(EXCEPTION_NAME_TAG, exceptionName),
                    Tag.of(SERVER_NAME_TAG, hostName),
                ),
            ).register(meterRegistry)
            .increment()

    fun incrementExceptionCaughtCounter(exceptionName: String): Unit =
        Counter
            .builder(EXCEPTION_CAUGHT_COUNTER)
            .description("Increments the counter for every exception caught.")
            .tags(
                listOfNotNull(
                    Tag.of(EXCEPTION_NAME_TAG, exceptionName),
                    Tag.of(SERVER_NAME_TAG, hostName),
                ),
            ).register(meterRegistry)
            .increment()

    fun incrementTransactionUpdateClearedCounter(accountNameOwner: String): Unit =
        Counter
            .builder(TRANSACTION_TRANSACTION_STATE_UPDATED_CLEARED_COUNTER)
            .description("Increments the counter for each transaction state toggled to cleared.")
            .tags(
                listOfNotNull(
                    Tag.of(ACCOUNT_NAME_OWNER_TAG, accountNameOwner),
                    Tag.of(SERVER_NAME_TAG, hostName),
                ),
            ).register(meterRegistry)
            .increment()

    fun incrementTransactionSuccessfullyInsertedCounter(accountNameOwner: String): Unit =
        Counter
            .builder(TRANSACTION_SUCCESSFULLY_INSERTED_COUNTER)
            .tags(
                listOfNotNull(
                    Tag.of(ACCOUNT_NAME_OWNER_TAG, accountNameOwner),
                    Tag.of(SERVER_NAME_TAG, hostName),
                ),
            ).register(meterRegistry)
            .increment()

    fun incrementTransactionAlreadyExistsCounter(accountNameOwner: String): Unit =
        Counter
            .builder(TRANSACTION_ALREADY_EXISTS_COUNTER)
            .tags(
                listOfNotNull(
                    Tag.of(ACCOUNT_NAME_OWNER_TAG, accountNameOwner),
                    Tag.of(SERVER_NAME_TAG, hostName),
                ),
            ).register(meterRegistry)
            .increment()

    fun incrementTransactionRestSelectNoneFoundCounter(accountNameOwner: String): Unit =
        Counter
            .builder(TRANSACTION_REST_SELECT_NONE_FOUND_COUNTER)
            .tags(
                listOfNotNull(
                    Tag.of(ACCOUNT_NAME_OWNER_TAG, accountNameOwner),
                    Tag.of(SERVER_NAME_TAG, hostName),
                ),
            ).register(meterRegistry)
            .increment()

    fun incrementTransactionRestTransactionStateUpdateFailureCounter(accountNameOwner: String): Unit =
        Counter
            .builder(TRANSACTION_REST_TRANSACTION_STATE_UPDATE_FAILURE_COUNTER)
            .tags(
                listOfNotNull(
                    Tag.of(ACCOUNT_NAME_OWNER_TAG, accountNameOwner),
                    Tag.of(SERVER_NAME_TAG, hostName),
                ),
            ).register(meterRegistry)
            .increment()

    fun incrementTransactionRestReoccurringStateUpdateFailureCounter(accountNameOwner: String): Unit =
        Counter
            .builder(TRANSACTION_REST_REOCCURRING_TYPE_UPDATE_FAILURE_COUNTER)
            .tags(
                listOfNotNull(
                    Tag.of(ACCOUNT_NAME_OWNER_TAG, accountNameOwner),
                    Tag.of(SERVER_NAME_TAG, hostName),
                ),
            ).register(meterRegistry)
            .increment()

    fun incrementAccountListIsEmpty(accountNameOwner: String): Unit =
        Counter
            .builder(TRANSACTION_ACCOUNT_LIST_NONE_FOUND_COUNTER)
            .tags(
                listOfNotNull(
                    Tag.of(ACCOUNT_NAME_OWNER_TAG, accountNameOwner),
                    Tag.of(SERVER_NAME_TAG, hostName),
                ),
            ).register(meterRegistry)
            .increment()

    fun incrementTransactionReceiptImageInserted(accountNameOwner: String): Unit =
        Counter
            .builder(TRANSACTION_RECEIPT_IMAGE_INSERTED_COUNTER)
            .tags(
                listOfNotNull(
                    Tag.of(ACCOUNT_NAME_OWNER_TAG, accountNameOwner),
                    Tag.of(SERVER_NAME_TAG, hostName),
                ),
            ).register(meterRegistry)
            .increment()
}
