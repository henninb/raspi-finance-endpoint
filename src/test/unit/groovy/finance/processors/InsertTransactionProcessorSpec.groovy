package finance.processors

import com.fasterxml.jackson.databind.ObjectMapper
import finance.domain.Account
import finance.domain.Category
import finance.domain.Transaction
import finance.helpers.AccountBuilder
import finance.helpers.CategoryBuilder
import finance.repositories.AccountRepository
import finance.repositories.CategoryRepository
import finance.repositories.TransactionRepository
import finance.services.AccountService
import finance.services.CategoryService
import finance.services.TransactionService
import org.apache.camel.Exchange
import org.apache.camel.Message
import spock.lang.Specification

import javax.validation.Validator

class InsertTransactionProcessorSpec extends Specification {
    Message mockMessage = Mock(Message)
    Exchange mockExchange = Mock(Exchange)
    TransactionRepository mockTransactionRepository = Mock(TransactionRepository)
    AccountRepository mockAccountRepository = Mock(AccountRepository)
    Validator mockValidator = Mock(Validator)
    AccountService accountService = new AccountService(mockAccountRepository, mockValidator)
    CategoryRepository mockCategoryRepository = GroovyMock(CategoryRepository)
    CategoryService categoryService = new CategoryService(mockCategoryRepository, mockValidator)
    ObjectMapper mapper = new ObjectMapper()
    TransactionService transactionService = new TransactionService(mockTransactionRepository, accountService, categoryService, mockValidator)
    InsertTransactionProcessor processor = new InsertTransactionProcessor(transactionService)
    def jsonPayload = '''
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

    def "test -- InsertTransactionProcessor - empty transaction"() {
        given:
        Transaction transaction = mapper.readValue(jsonPayload, Transaction.class)

        when:
        processor.process(mockExchange)

        then:
        1 * mockExchange.getIn() >> mockMessage
        1 * mockMessage.getBody(String.class) >> jsonPayload
        1 * mockTransactionRepository.findByGuid(transaction.guid) >> Optional.of(transaction)
        1 * mockValidator.validate(_) >> new HashSet()
        1 * mockMessage.setBody(transaction.toString())
        0 * _
    }

    def "test -- InsertTransactionProcessor - happy path"() {
        given:
        Transaction transaction = mapper.readValue(jsonPayload, Transaction.class)
        Account account = AccountBuilder.builder().build()
        Category category = CategoryBuilder.builder().build()

        account.accountNameOwner = transaction.accountNameOwner
        category.category = transaction.category

        when:
        processor.process(mockExchange)

        then:
        1 * mockExchange.getIn() >> mockMessage
        1 * mockMessage.getBody(String.class) >> jsonPayload
        1 * mockTransactionRepository.findByGuid(transaction.guid) >> Optional.empty()
        1 * mockAccountRepository.findByAccountNameOwner(transaction.accountNameOwner) >> Optional.empty()
        2 * mockAccountRepository.findByAccountNameOwner(transaction.accountNameOwner) >> Optional.of(account)
        1 * mockCategoryRepository.findByCategory(transaction.category) >> Optional.empty()
        1 * mockCategoryRepository.saveAndFlush(category)
        3 * mockValidator.validate(_) >> new HashSet()
        //1 * mockValidator.validate(account as Object) >> new HashSet()
        1 * mockTransactionRepository.saveAndFlush(_)
        1 * mockMessage.setBody(transaction.toString())
        0 * _
    }
}