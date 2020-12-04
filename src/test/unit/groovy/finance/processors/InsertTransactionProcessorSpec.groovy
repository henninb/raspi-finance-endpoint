package finance.processors

import com.fasterxml.jackson.databind.ObjectMapper
import finance.domain.Account
import finance.domain.Category
import finance.domain.Transaction
import finance.helpers.AccountBuilder
import finance.helpers.CategoryBuilder
import finance.repositories.AccountRepository
import finance.repositories.CategoryRepository
import finance.repositories.ReceiptImageRepository
import finance.repositories.TransactionRepository
import finance.services.AccountService
import finance.services.CategoryService
import finance.services.MeterService
import finance.services.ReceiptImageService
import finance.services.TransactionService
import org.apache.camel.Exchange
import org.apache.camel.Message
import spock.lang.Specification

import javax.validation.Validator

class InsertTransactionProcessorSpec extends Specification {
    protected Message mockMessage = GroovyMock(Message)
    protected Exchange mockExchange = GroovyMock(Exchange)
    protected TransactionRepository mockTransactionRepository = GroovyMock(TransactionRepository)
    protected AccountRepository mockAccountRepository = GroovyMock(AccountRepository)
    protected Validator mockValidator = GroovyMock(Validator)
    protected MeterService mockMeterService = GroovyMock()
    protected AccountService accountService = new AccountService(mockAccountRepository, mockValidator, mockMeterService)
    protected CategoryRepository mockCategoryRepository = GroovyMock(CategoryRepository)
    protected CategoryService categoryService = new CategoryService(mockCategoryRepository, mockValidator, mockMeterService)
    protected ReceiptImageRepository mockReceiptImageRepository = GroovyMock(ReceiptImageRepository)
    protected ReceiptImageService receiptImageService = new ReceiptImageService(mockReceiptImageRepository)
    protected ObjectMapper mapper = new ObjectMapper()
    protected TransactionService transactionService = new TransactionService(mockTransactionRepository, accountService, categoryService, receiptImageService, mockValidator, mockMeterService)
    protected InsertTransactionProcessor processor = new InsertTransactionProcessor(transactionService, mockMeterService)

    String jsonPayload = '''
        {"accountId":0,
        "accountType":"credit",
        "transactionDate":1553645394,
        "dateUpdated":1593981072000,
        "dateAdded":1593981072000,
        "guid":"4ea3be58-3993-46de-88a2-4ffc7f1d73bb",
        "accountNameOwner":"chase_brian",
        "description":"aliexpress.com",
        "category":"online",
        "amount":3.14,
        "transactionState":"cleared",
        "reoccurring":false,
        "notes":"my note to you"}
        '''

    void 'test -- InsertTransactionProcessor - empty transaction'() {
        given:
        Transaction transaction = mapper.readValue(jsonPayload, Transaction)

        when:
        processor.process(mockExchange)

        then:
        1 * mockExchange.in >> mockMessage
        1 * mockMessage.body(String) >> jsonPayload
        1 * mockTransactionRepository.findByGuid(transaction.guid) >> Optional.of(transaction)
        1 * mockValidator.validate(_) >> new HashSet()
        1 * mockMessage.setBody(transaction.toString())
        0 * _
    }

    def "test -- InsertTransactionProcessor - happy path"() {
        given:
        Transaction transaction = mapper.readValue(jsonPayload, Transaction)
        Account account = AccountBuilder.builder().build()
        Category category = CategoryBuilder.builder().build()

        account.accountNameOwner = transaction.accountNameOwner
        category.category = transaction.category

        when:
        processor.process(mockExchange)

        then:
        1 * mockExchange.in >> mockMessage
        1 * mockMessage.body(String) >> jsonPayload
        1 * mockTransactionRepository.findByGuid(transaction.guid) >> Optional.empty()
        1 * mockAccountRepository.findByAccountNameOwner(transaction.accountNameOwner) >> Optional.empty()
        2 * mockAccountRepository.findByAccountNameOwner(transaction.accountNameOwner) >> Optional.of(account)
        1 * mockCategoryRepository.findByCategory(transaction.category) >> Optional.empty()
        1 * mockCategoryRepository.saveAndFlush(category)
        3 * mockValidator.validate(_) >> new HashSet()
        //1 * mockValidator.validate(account as Object) >> new HashSet()
        1 * mockTransactionRepository.saveAndFlush(_)
        1 * mockMeterService.incrementTransactionSuccessfullyInsertedCounter(transaction.accountNameOwner)
        1 * mockMessage.setBody(transaction.toString())
        0 * _
    }
}
