package finance

import finance.domain.Account
import finance.domain.Category
import finance.domain.Transaction
import finance.helpers.AccountBuilder
import finance.helpers.TransactionBuilder
import finance.services.AccountService
import finance.services.ICategoryService
import finance.services.TransactionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import spock.lang.Specification

@ActiveProfiles("ora")
@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DatabaseInsertSpec extends Specification {

    @Autowired
    protected ICategoryService categoryService

    @Autowired
    protected AccountService accountService

    @Autowired
    protected TransactionService transactionService

    void 'test category - valid insert'() {
        given:
        Category category = new Category()
        category.categoryName = UUID.randomUUID().toString().substring(0, 8)
        category.activeStatus = false

        when:
        Boolean isInserted = categoryService.insertCategory(category)

        then:
        isInserted
        0 * _
    }

    void 'test account - valid insert'() {
        given:
        Account account = AccountBuilder.builder().build()

        when:
        Boolean isInserted = accountService.insertAccount(account)

        then:
        isInserted
        0 * _
    }

    void 'test transaction - valid insert'() {
        given:
        Transaction transaction = TransactionBuilder.builder().build()

        when:
        Boolean isInserted = transactionService.insertTransaction(transaction)

        then:
        isInserted
        0 * _
    }
}
