package finance.controllers

import finance.domain.AccountType
import finance.domain.ReceiptImage
import finance.domain.ServiceResult
import finance.domain.Transaction
import finance.domain.TransactionState
import finance.domain.TransactionType
import finance.services.StandardizedReceiptImageService
import finance.resolvers.TransactionBatchResolver
import spock.lang.Specification

class TransactionBatchMappingSpec extends Specification {

    def receiptImageService = Mock(StandardizedReceiptImageService)
    def controller = new TransactionBatchResolver(receiptImageService)

    private static Transaction txWithId(Long id) {
        def tx = new Transaction()
        tx.transactionId = id
        tx.accountType = AccountType.Debit
        tx.transactionType = TransactionType.Expense
        tx.transactionState = TransactionState.Cleared
        return tx
    }

    private static ReceiptImage ri(Long receiptId, Long txId) {
        def ri = new ReceiptImage(receiptId, txId, true)
        ri.imageFormatType = finance.domain.ImageFormatType.Jpeg
        ri.image = new byte[1]
        ri.thumbnail = new byte[1]
        return ri
    }

    def "receiptImage @BatchMapping maps transactions to images with nulls for missing"() {
        given:
        def t1 = txWithId(1L)
        def t2 = txWithId(2L)

        and:
        def img = ri(10L, 1L)

        when:
        def result = controller.receiptImage([t1, t2])

        then:
        1 * receiptImageService.findByTransactionId(1L) >> ServiceResult.Success.of(img)
        1 * receiptImageService.findByTransactionId(2L) >> ServiceResult.NotFound.of("not found")

        and:
        result.size() == 2
        result.get(t1).receiptImageId == 10L
        result.get(t2) == null
    }
}
