package finance.domain


data class DescriptionWithCount(
    val description: Description,
    val transactionCount: Long
)