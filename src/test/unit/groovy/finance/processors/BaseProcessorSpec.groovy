package finance.processors

import com.fasterxml.jackson.databind.ObjectMapper
import finance.repositories.AccountRepository
import finance.repositories.CategoryRepository
import finance.repositories.ReceiptImageRepository
import finance.repositories.TransactionRepository
import finance.services.*
import finance.utils.Constants
import io.micrometer.core.instrument.*
import org.apache.camel.CamelContext
import org.apache.camel.Exchange
import org.apache.camel.builder.ExchangeBuilder
import org.apache.camel.impl.DefaultCamelContext
import spock.lang.Specification

import javax.validation.Validation
import javax.validation.Validator

class BaseProcessorSpec extends Specification {
    protected TransactionRepository transactionRepositoryMock = GroovyMock(TransactionRepository)
    protected AccountRepository accountRepositoryMock = GroovyMock(AccountRepository)
    protected Validator validatorMock = GroovyMock(Validator)
    protected MeterRegistry meterRegistryMock = GroovyMock(MeterRegistry)
    protected MeterService meterService = new MeterService(meterRegistryMock)
    protected AccountService accountService = new AccountService(accountRepositoryMock, transactionRepositoryMock)
    protected CategoryRepository mockCategoryRepository = GroovyMock(CategoryRepository)
    protected CategoryService categoryService = new CategoryService(mockCategoryRepository)
    protected ReceiptImageRepository mockReceiptImageRepository = GroovyMock(ReceiptImageRepository)
    protected ReceiptImageService receiptImageService = new ReceiptImageService(mockReceiptImageRepository)
    protected ObjectMapper mapper = new ObjectMapper()
    protected TransactionService transactionService = new TransactionService(transactionRepositoryMock, accountService, categoryService, receiptImageService)
    protected TransactionService transactionServiceMock = GroovyMock(TransactionService)
    protected Validator validator = Validation.buildDefaultValidatorFactory().getValidator()
    protected Counter counter = GroovyMock(Counter)
    protected JsonTransactionProcessor jsonTransactionProcessor = new JsonTransactionProcessor()
    protected InsertTransactionProcessor insertTransactionProcessor = new InsertTransactionProcessor(transactionServiceMock)
    protected StringTransactionProcessor stringTransactionProcessor = new StringTransactionProcessor()

    protected Tag validationExceptionTag = Tag.of(Constants.EXCEPTION_NAME_TAG, 'ValidationException')
    protected Tag runtimeExceptionTag = Tag.of(Constants.EXCEPTION_NAME_TAG, 'RuntimeException')
    protected Tag serverNameTag = Tag.of(Constants.SERVER_NAME_TAG, 'server')
    protected Tags validationExceptionTags = Tags.of(validationExceptionTag, serverNameTag)
    protected Tags runtimeExceptionTags = Tags.of(runtimeExceptionTag, serverNameTag)
    protected Meter.Id validationExceptionThrownMeter = new Meter.Id(Constants.EXCEPTION_THROWN_COUNTER, validationExceptionTags, null, null, Meter.Type.COUNTER)
    //protected Meter.Id runtimeExceptionThrownMeter = new Meter.Id(Constants.EXCEPTION_THROWN_COUNTER, runtimeExceptionTags, null, null, Meter.Type.COUNTER)

    CamelContext camelContext = new DefaultCamelContext()
    Exchange exchange = ExchangeBuilder.anExchange(camelContext).build()

    static Meter.Id setMeterId(String counterName, String accountNameOwner) {
        Tag serverNameTag = Tag.of(Constants.SERVER_NAME_TAG, 'server')
        Tag accountNameOwnerTag = Tag.of(Constants.ACCOUNT_NAME_OWNER_TAG, accountNameOwner)
        Tags tags = Tags.of(accountNameOwnerTag, serverNameTag)
        return new Meter.Id(counterName, tags, null, null, Meter.Type.COUNTER)
    }
}
