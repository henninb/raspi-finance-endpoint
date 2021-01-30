package finance.routes

import finance.configurations.CamelProperties
import finance.helpers.TransactionBuilder
import finance.processors.ExceptionProcessor
import finance.processors.InsertTransactionProcessor
import finance.processors.JsonTransactionProcessor
import finance.processors.StringTransactionProcessor
import finance.repositories.AccountRepository
import finance.repositories.CategoryRepository
import finance.repositories.DescriptionRepository
import finance.repositories.ParameterRepository
import finance.repositories.PaymentRepository
import finance.repositories.ReceiptImageRepository
import finance.repositories.TransactionRepository
import finance.services.AccountService
import finance.services.CategoryService
import finance.services.MeterService
import finance.services.ReceiptImageService
import finance.services.TransactionService
import io.micrometer.core.instrument.MeterRegistry
import org.apache.camel.model.ModelCamelContext
import spock.lang.Specification
import javax.validation.Validator

class BaseRouteBuilderSpec extends Specification {

    protected ModelCamelContext camelContext
    //protected Validator validator = Validation.buildDefaultValidatorFactory().getValidator()
    protected Validator validatorMock = GroovyMock(Validator)
    protected MeterRegistry meterRegistryMock = GroovyMock(MeterRegistry)
    protected MeterService meterService = new MeterService(meterRegistryMock)

    protected ExceptionProcessor mockExceptionProcessor = new ExceptionProcessor()
    protected ReceiptImageRepository receiptImageRepositoryMock = GroovyMock(ReceiptImageRepository)
    protected CategoryRepository categoryRepositoryMock = GroovyMock(CategoryRepository)
    protected AccountRepository accountRepositoryMock = GroovyMock(AccountRepository)
    protected TransactionRepository transactionRepositoryMock = GroovyMock(TransactionRepository)
    protected AccountService accountService = new AccountService(accountRepositoryMock, validatorMock, meterService)
    protected ReceiptImageService receiptImageService = new ReceiptImageService(receiptImageRepositoryMock, validatorMock, meterService)
    protected CategoryService categoryService = new CategoryService(categoryRepositoryMock, validatorMock, meterService)
    protected TransactionService transactionService = new TransactionService(transactionRepositoryMock, accountService, categoryService, receiptImageService, validatorMock, meterService)

    protected InsertTransactionProcessor insertTransactionProcessorMock = new InsertTransactionProcessor(transactionService, meterService)
    protected JsonTransactionProcessor mockJsonTransactionProcessor = new JsonTransactionProcessor(validatorMock, meterService)
    protected StringTransactionProcessor stringTransactionProcessorMock = new StringTransactionProcessor(meterService)


//    protected CamelProperties camelProperties = new CamelProperties(
//            "true",
//            'jsonFileReaderRoute',
//            'direct:routeFromLocal',
//            'fileWriterRoute',
//            'direct:routeFromLocal',
//            'transactionToDatabaseRoute',
//            'mock:toTransactionToDatabaseRoute',
//            'mock:toSavedFileEndpoint',
//            'mock:toFailedJsonFileEndpoint',
//            'mock:toFailedJsonParserEndpoint')
}
