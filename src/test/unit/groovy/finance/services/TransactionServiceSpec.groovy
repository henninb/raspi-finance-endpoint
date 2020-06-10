package finance.services

import finance.domain.Account
import finance.domain.Category
import finance.domain.Transaction
import finance.repositories.AccountRepository
import finance.repositories.CategoryRepository
import finance.repositories.TransactionRepository
import org.junit.Test
import org.mockito.Mockito
import spock.lang.Specification

import javax.validation.Validator

import static org.junit.Assert.assertTrue

class TransactionServiceSpec extends Specification {
    TransactionRepository mockTransactionRepository = Mock(TransactionRepository)
    AccountRepository mockAccountRepository = Mock(AccountRepository)
    AccountService accountService = new AccountService(mockAccountRepository)
    CategoryRepository mockCategoryRepository = Mock(CategoryRepository)
    CategoryService categoryService = new CategoryService(mockCategoryRepository)
    TransactionService transactionService = new TransactionService(mockTransactionRepository, accountService, categoryService)

    def "test Delete By GUID"() {
        given:
        def guid = "123"
        Transaction transaction = new Transaction()
        Optional<Transaction> transactionOptional = Optional.of(transaction)
        when:
        def isDeleted = transactionService.deleteByGuid(guid)
        then:
        isDeleted
        1 * mockTransactionRepository.deleteByGuid(guid)
        1 * mockTransactionRepository.findByGuid(guid) >> transactionOptional
        0 * _
    }

    def "test Delete By GUID - no record returned"() {
        given:
        def guid = "123"
        Optional<Transaction> transactionOptional = Optional.empty()
        when:
        def isDeleted = transactionService.deleteByGuid(guid)
        then:
        !isDeleted
        1 * mockTransactionRepository.findByGuid(guid) >> transactionOptional
        0 * _
    }

    def "test Find By GUID"() {
        given:
        def guid = "123"
        Transaction transaction = new Transaction()
        Optional<Transaction> transactionOptional = Optional.of(transaction)
        when:
        transactionService.findByGuid(guid)
        then:
        1 * mockTransactionRepository.findByGuid(guid) >> transactionOptional
        0 * _
    }

    def "test insert valid transaction"() {
        given:
        def categoryName = "my-category"
        def accountName = "my-account-name"
        def guid = "123"
        Transaction transaction = new Transaction()
        Account account = new Account()
        Category category = new Category()
        Optional<Account> accountOptional = Optional.of(account)
        Optional<Category> categoryOptional = Optional.of(category)
        when:
        transaction.guid = guid
        transaction.accountNameOwner = accountName
        transaction.category = categoryName
        def isInserted = transactionService.insertTransaction(transaction)
        then:
        isInserted
        1 * mockTransactionRepository.findByGuid(guid) >> Optional.empty()
        1 * mockAccountRepository.findByAccountNameOwner(accountName) >> accountOptional
        1 * mockCategoryRepository.findByCategory(categoryName) >> categoryOptional
        1 * mockTransactionRepository.saveAndFlush(transaction) >> true
        0 * _
    }

    def "test insert duplicate transaction attempt"() {
        given:
        def categoryName = "my-category"
        def accountName = "my-account-name"
        def guid = "123"
        Transaction transaction = new Transaction()
        Optional<Transaction> transactionOptional = Optional.of(transaction)
        when:
        transaction.guid = guid
        transaction.accountNameOwner = accountName
        transaction.category = categoryName
        def isInserted = transactionService.insertTransaction(transaction)
        then:
        !isInserted
        1 * mockTransactionRepository.findByGuid(guid) >> transactionOptional
        0 * _
    }

    def "test insert valid transaction - account name does not exist"() {
        given:
        def categoryName = "my-category"
        def accountName = "my-account-name"
        def guid = "123"
        Transaction transaction = new Transaction()
        Account account = new Account()
        Category category = new Category()
        Optional<Account> accountOptional = Optional.of(account)
        Optional<Category> categoryOptional = Optional.of(category)
        when:
        transaction.guid = guid
        transaction.accountNameOwner = accountName
        transaction.category = categoryName
        def isInserted = transactionService.insertTransaction(transaction)
        then:
        isInserted
        1 * mockTransactionRepository.findByGuid(guid) >> Optional.empty()
        1 * mockAccountRepository.findByAccountNameOwner(accountName) >> Optional.empty()
        1 * mockAccountRepository.saveAndFlush(_) >> true
        1 * mockAccountRepository.findByAccountNameOwner(accountName) >> accountOptional
        1 * mockCategoryRepository.findByCategory(categoryName) >> categoryOptional
        1 * mockTransactionRepository.saveAndFlush(transaction) >> true
        0 * _
    }

    def "test insert valid transaction - category name does not exist"() {
        given:
        def categoryName = "my-category"
        def accountName = "my-account-name"
        def guid = "123"
        Transaction transaction = new Transaction()
        Account account = new Account()
        Category category = new Category()
        Optional<Account> accountOptional = Optional.of(account)
        Optional<Category> categoryOptional = Optional.of(category)
        when:
        transaction.guid = guid
        transaction.accountNameOwner = accountName
        transaction.category = categoryName
        def isInserted = transactionService.insertTransaction(transaction)
        then:
        isInserted
        1 * mockTransactionRepository.findByGuid(guid) >> Optional.empty()
        1 * mockAccountRepository.findByAccountNameOwner(accountName) >> accountOptional
        1 * mockCategoryRepository.findByCategory(categoryName) >> Optional.empty()
        1 * mockCategoryRepository.save(_)
        1 * mockTransactionRepository.saveAndFlush(transaction) >> true
        0 * _
    }
}
