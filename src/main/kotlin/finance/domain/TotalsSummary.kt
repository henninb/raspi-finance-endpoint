package finance.domain

data class TotalsSummary(
    val totalAmount: Double,
    val transactionCount: Long,
    val transactionState: TransactionState
)