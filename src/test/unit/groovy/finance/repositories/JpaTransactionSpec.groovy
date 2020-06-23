package finance.repositories

import finance.domain.Account
import finance.domain.Transaction
import finance.helpers.AccountBuilder
import finance.helpers.TransactionBuilder

//import finance.repositories.TransactionRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import spock.lang.Specification

@DataJpaTest
class JpaTransactionSpec extends Specification {

    @Autowired
    TransactionRepository transactionRepository

    @Autowired
    AccountRepository accountRepository

    @Autowired
    TestEntityManager entityManager

    def "jpa test fort transaction repository"() {
        given:
        Transaction transaction = new TransactionBuilder().build()
        transaction.accountId = 1005
        Account account = new AccountBuilder().build()
        account.accountId = 1005
        println "transaction = $transaction"
        println "account = $account"
        //entityManager.persist(transaction)
        entityManager.merge(account)
        //entityManager.merge(transaction)
        //entityManager.persist(new Book("Testing Spring with Spock"))

        expect: "the correct count is inside the repository"
        transactionRepository.count() == 0L
        accountRepository.count() == 1L
    }
}








