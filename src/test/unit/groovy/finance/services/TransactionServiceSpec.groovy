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

import static org.junit.Assert.assertTrue

class TransactionServiceSpec extends Specification {
    TransactionRepository mockTransactionRepository = Mock(TransactionRepository)
    AccountRepository mockAccountRepository = Mock(AccountRepository)
    CategoryRepository mockCategoryRepository = Mock(CategoryRepository)

    TransactionService transactionService = new TransactionService(mockTransactionRepository, mockAccountRepository, mockCategoryRepository)

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
        Transaction transaction = new Transaction()
        Account account = new Account()
        Category category = new Category()
        Optional<Account> accountOptional = Optional.of(account)
        Optional<Category> categoryOptional = Optional.of(category)
        when:
        def isInserted = transactionService.insertTransaction(transaction)
        then:
        isInserted
        1 * mockCategoryRepository.findByCategory(categoryName) >> categoryOptional
        1 * mockAccountRepository.findByAccountNameOwner(accountName) >> accountOptional
        0 * _
    }



    @Test
    public void insertTransactionTest() {
        Transaction transaction = new Transaction();
        Account account = new Account();
        Category category = new Category();
        //Optional<Transaction> transactionOptional = Optional.of(transaction);
        Optional<Account> accountOptional = Optional.of(account);
        Optional<Category> categoryOptional = Optional.of(category);

        Mockito.when(categoryRepository.findByCategory(Mockito.anyString())).thenReturn(categoryOptional);
        Mockito.when(accountRepository.findByAccountNameOwner(Mockito.anyString())).thenReturn(accountOptional);
        Mockito.when(transactionRepository.saveAndFlush(Mockito.any())).thenReturn(transaction);
        boolean isInserted = transactionService.insertTransaction(transaction);
        assertTrue(isInserted);
    }




}
