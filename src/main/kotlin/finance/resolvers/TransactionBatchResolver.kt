package finance.resolvers

import finance.domain.ReceiptImage
import finance.domain.ServiceResult
import finance.domain.Transaction
import finance.services.StandardizedReceiptImageService
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.graphql.data.method.annotation.BatchMapping
import org.springframework.stereotype.Controller

/**
 * Batch resolver for Transaction associations used by GraphQL.
 * Demonstrates efficient batched resolution of ReceiptImage for Transactions.
 */
@Controller
class TransactionBatchResolver(
    private val receiptImageService: StandardizedReceiptImageService,
) {
    companion object {
        val logger: Logger = LogManager.getLogger()
    }

    /**
     * Batch maps Transactions to their ReceiptImage, returning null where missing.
     */
    @BatchMapping(typeName = "Transaction", field = "receiptImage")
    fun receiptImage(transactions: List<Transaction>): Map<Transaction, ReceiptImage?> {
        logger.debug("Batch resolving receiptImage for {} transactions", transactions.size)

        val resultMap = LinkedHashMap<Transaction, ReceiptImage?>(transactions.size)
        transactions.forEach { tx ->
            val txId = tx.transactionId
            val resolved =
                try {
                    when (val sr = receiptImageService.findByTransactionId(txId)) {
                        is ServiceResult.Success -> sr.data
                        else -> null
                    }
                } catch (ex: Exception) {
                    logger.warn("Failed resolving receipt image for transactionId={}: {}", txId, ex.message)
                    null
                }
            resultMap[tx] = resolved
        }
        return resultMap
    }
}
