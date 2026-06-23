package finance.services

import finance.domain.Reward
import finance.domain.Transaction
import finance.domain.TransactionType
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class RewardsCalculationService {
    private val excludedTypes = setOf(TransactionType.Income, TransactionType.Transfer)
    private val excludedCategories = setOf("payment", "returns", "bill_pay")

    fun calculateCashback(
        transaction: Transaction,
        tiers: List<Reward>,
    ): BigDecimal? {
        if (tiers.isEmpty()) return null
        if (transaction.transactionType in excludedTypes) return null
        if (transaction.category.lowercase() in excludedCategories) return null

        val category = transaction.category.lowercase()
        val matched =
            tiers
                .filter { tier -> category == tier.category || category.contains(tier.category) }
                .maxByOrNull { it.multiplier }
                ?: return transaction.amount
                    .abs()
                    .multiply(tiers.first().cpp)
                    .setScale(2, RoundingMode.HALF_UP)

        return transaction.amount
            .abs()
            .multiply(matched.multiplier)
            .multiply(matched.cpp)
            .setScale(2, RoundingMode.HALF_UP)
    }
}
