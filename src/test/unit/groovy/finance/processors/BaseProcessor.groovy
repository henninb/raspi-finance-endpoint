package finance.processors

import com.fasterxml.jackson.databind.ObjectMapper
import finance.repositories.AccountRepository
import finance.repositories.CategoryRepository
import finance.repositories.ReceiptImageRepository
import finance.repositories.TransactionRepository
import finance.services.AccountService
import finance.services.CategoryService
import finance.services.MeterService
import finance.services.ReceiptImageService
import finance.services.TransactionService
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.apache.camel.Exchange
import org.apache.camel.Message
import spock.lang.Specification

import javax.validation.Validation
import javax.validation.Validator

class BaseProcessor extends Specification {
    protected Message mockMessage = GroovyMock(Message)
    protected Exchange mockExchange = GroovyMock(Exchange)
    protected TransactionRepository mockTransactionRepository = GroovyMock(TransactionRepository)
    protected AccountRepository mockAccountRepository = GroovyMock(AccountRepository)
    protected Validator validatorMock = GroovyMock(Validator)
    protected MeterRegistry meterRegistryMock = GroovyMock(MeterRegistry)
    protected MeterService meterService = new MeterService(meterRegistryMock)
    protected AccountService accountService = new AccountService(mockAccountRepository, validatorMock, meterService)
    protected CategoryRepository mockCategoryRepository = GroovyMock(CategoryRepository)
    protected CategoryService categoryService = new CategoryService(mockCategoryRepository, validatorMock, meterService)
    protected ReceiptImageRepository mockReceiptImageRepository = GroovyMock(ReceiptImageRepository)
    protected ReceiptImageService receiptImageService = new ReceiptImageService(mockReceiptImageRepository, validatorMock, meterService)
    protected ObjectMapper mapper = new ObjectMapper()
    protected TransactionService transactionService = new TransactionService(mockTransactionRepository, accountService, categoryService, receiptImageService, validatorMock, meterService)
    protected Validator validator = Validation.buildDefaultValidatorFactory().getValidator()
    protected Counter counter = GroovyMock(Counter)
    protected Validator mockValidator = GroovyMock(Validator)
    protected JsonTransactionProcessor jsonTransactionProcessor = new JsonTransactionProcessor(mockValidator, meterService)
    protected InsertTransactionProcessor insertTransactionProcessor = new InsertTransactionProcessor(transactionService, meterService)
    protected StringTransactionProcessor stringTransactionProcessor = new StringTransactionProcessor(meterService)

}
