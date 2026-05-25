package finance.routes

import com.fasterxml.jackson.databind.ObjectMapper
import finance.configurations.CamelProperties
import finance.processors.ExceptionProcessor
import finance.processors.InsertTransactionProcessor
import finance.processors.JsonTransactionProcessor
import finance.processors.StringTransactionProcessor
import finance.repositories.AccountRepository
import finance.repositories.CategoryRepository
import finance.repositories.ReceiptImageRepository
import finance.repositories.TransactionRepository
import finance.services.*
import finance.utils.Constants
import io.micrometer.core.instrument.*
import org.apache.camel.model.ModelCamelContext
import spock.lang.Specification

import jakarta.validation.Validation
import jakarta.validation.Validator

class BaseRouteBuilderSpec extends Specification {

    protected ModelCamelContext camelContext
    protected Validator validatorMock = GroovyMock(Validator)
    protected Counter counter = Mock(Counter)
    protected MeterRegistry meterRegistryMock = GroovyMock(MeterRegistry)
    protected MeterService meterService = new MeterService(meterRegistryMock)
    protected Validator validator = Validation.buildDefaultValidatorFactory().getValidator()

    protected TransactionService transactionServiceMock = GroovyMock(TransactionService)

    protected JsonTransactionProcessor jsonTransactionProcessor = new JsonTransactionProcessor()
    protected InsertTransactionProcessor insertTransactionProcessor = new InsertTransactionProcessor(transactionServiceMock)
    protected StringTransactionProcessor stringTransactionProcessor = new StringTransactionProcessor()
    protected ExceptionProcessor exceptionProcessorMock = GroovyMock(ExceptionProcessor)

    protected ObjectMapper objectMapper = new ObjectMapper()

    static Meter.Id setMeterId(String counterName, String accountNameOwner) {
        Tag serverNameTag = Tag.of(Constants.SERVER_NAME_TAG, 'server')
        Tag accountNameOwnerTag = Tag.of(Constants.ACCOUNT_NAME_OWNER_TAG, accountNameOwner)
        Tags tags = Tags.of(accountNameOwnerTag, serverNameTag)
        return new Meter.Id(counterName, tags, null, null, Meter.Type.COUNTER)
    }

    void cleanup() {
        camelContext.stop()
    }

    protected CamelProperties camelProperties = new CamelProperties(
            "true",
            "jsonFileWriterRoute",
            "transactionToDatabaseRoute",
            "mock:toTransactionToDatabaseRoute",
            "mock:toSavedFileEndpoint",
            "mock:toFailedJsonFileEndpoint",
            "mock:toFailedJsonParserEndpoint")
}
