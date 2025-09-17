package finance.services

import finance.domain.Transaction
import finance.domain.TransactionState
import finance.helpers.TransactionBuilder
import spock.lang.Title

import java.sql.Date

@Title("TransactionService query/find helpers and state error cases")
class TransactionServiceQueryAndStateSpec extends BaseServiceSpec {

    void setup() {
        transactionService.validator = validatorMock
        transactionService.accountService = accountServiceMock
        transactionService.categoryService = categoryServiceMock
        transactionService.descriptionService = descriptionService
        transactionService.receiptImageService = receiptImageServiceMock
        transactionService.transactionRepository = transactionRepositoryMock
        transactionService.meterService = meterService
    }

    void "findTransactionsByCategory returns empty when none found"() {
        given:
        def category = 'utilities'

        when:
        def result = transactionService.findTransactionsByCategory(category)

        then:
        1 * transactionRepositoryMock.findByCategoryAndActiveStatusOrderByTransactionDateDesc(category, _ as Boolean) >> []
        result.isEmpty()
    }

    void "findTransactionsByCategory returns list when present"() {
        given:
        def category = 'groceries'
        def t1 = TransactionBuilder.builder().withCategory(category).build()
        def t2 = TransactionBuilder.builder().withCategory(category).build()

        when:
        def result = transactionService.findTransactionsByCategory(category)

        then:
        1 * transactionRepositoryMock.findByCategoryAndActiveStatusOrderByTransactionDateDesc(category, _ as Boolean) >> [t1, t2]
        result.size() == 2
        result*.category.every { it == category }
    }

    void "findTransactionsByDescription returns empty when none found"() {
        given:
        def desc = 'some merchant'

        when:
        def result = transactionService.findTransactionsByDescription(desc)

        then:
        1 * transactionRepositoryMock.findByDescriptionAndActiveStatusOrderByTransactionDateDesc(desc, _ as Boolean) >> []
        result.isEmpty()
    }

    void "findTransactionsByDescription returns list when present"() {
        given:
        def desc = 'coffee shop'
        def t1 = TransactionBuilder.builder().withDescription(desc).build()
        def t2 = TransactionBuilder.builder().withDescription(desc).build()

        when:
        def result = transactionService.findTransactionsByDescription(desc)

        then:
        1 * transactionRepositoryMock.findByDescriptionAndActiveStatusOrderByTransactionDateDesc(desc, _ as Boolean) >> [t1, t2]
        result.size() == 2
        result*.description.every { it == desc }
    }

    void "updateTransactionState throws when setting same state"() {
        given:
        def tx = TransactionBuilder.builder().withTransactionState(TransactionState.Cleared).build()

        when:
        transactionService.updateTransactionState(tx.guid, TransactionState.Cleared)

        then:
        1 * transactionRepositoryMock.findByGuid(tx.guid) >> Optional.of(tx)
        thrown(finance.domain.InvalidTransactionStateException)
    }

    void "updateTransactionState throws when clearing a future-dated transaction"() {
        given:
        def futureDate = Date.valueOf('2999-01-01')
        def tx = TransactionBuilder.builder()
                .withTransactionDate(futureDate)
                .withTransactionState(TransactionState.Outstanding)
                .build()

        when:
        transactionService.updateTransactionState(tx.guid, TransactionState.Cleared)

        then:
        1 * transactionRepositoryMock.findByGuid(tx.guid) >> Optional.of(tx)
        thrown(finance.domain.InvalidTransactionStateException)
    }

    void "updateTransactionState throws when transaction not found"() {
        when:
        transactionService.updateTransactionState('missing-guid', TransactionState.Cleared)

        then:
        1 * transactionRepositoryMock.findByGuid('missing-guid') >> Optional.empty()
        thrown(finance.domain.TransactionNotFoundException)
    }

    void "deleteReceiptImage returns false when id set but image missing"() {
        given:
        def tx = TransactionBuilder.builder().withReceiptImageId(42L).build()

        when:
        def result = transactionService.deleteReceiptImage(tx)

        then:
        1 * receiptImageServiceMock.findByReceiptImageId(42L) >> Optional.empty()
        0 * receiptImageServiceMock.deleteReceiptImage(*_)
        !result
    }
}

