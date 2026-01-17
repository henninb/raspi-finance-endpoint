package finance.services

import finance.domain.Totals
import finance.domain.Transaction
import finance.domain.TransactionState
import finance.repositories.TransactionRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Service for handling financial calculations and totals computation
 * Extracted from TransactionService for better separation of concerns and testability
 */
@Service
open class CalculationService(
    private val transactionRepository: TransactionRepository,
) : BaseService(),
    ICalculationService {
    companion object {
        private const val MAX_REASONABLE_AMOUNT = 999999999.99
        private val ZERO_SCALE_2 = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
    }

    override fun calculateActiveTotalsByAccountNameOwner(accountNameOwner: String): Totals {
        return try {
            logger.debug("Calculating active totals for account: $accountNameOwner")

            val resultSet =
                executeWithResilienceSync(
                    operation = {
                        transactionRepository.sumTotalsForActiveTransactionsByAccountNameOwner(accountNameOwner)
                    },
                    operationName = "calculateActiveTotalsByAccountNameOwner-$accountNameOwner",
                    timeoutSeconds = 45,
                )

            var totalsFuture = ZERO_SCALE_2
            var totalsCleared = ZERO_SCALE_2
            var totalsOutstanding = ZERO_SCALE_2

            resultSet.forEach { row ->
                try {
                    if (row !is Array<*>) {
                        logger.warn("Unexpected row type: ${row.javaClass.simpleName}, expected Array")
                        meterService.incrementExceptionCaughtCounter("UnexpectedRowType")
                        return@forEach
                    }

                    if (row.size < 3) {
                        logger.warn("Row has insufficient columns: ${row.size}, expected at least 3")
                        meterService.incrementExceptionCaughtCounter("InsufficientColumns")
                        return@forEach
                    }

                    val amount = row[0] as? BigDecimal
                    val transactionState = row[2] as? String

                    if (transactionState != null && amount != null) {
                        when (transactionState.lowercase()) {
                            "future" -> {
                                totalsFuture = amount.setScale(2, RoundingMode.HALF_UP)
                                logger.debug("Future totals: $totalsFuture")
                            }

                            "cleared" -> {
                                totalsCleared = amount.setScale(2, RoundingMode.HALF_UP)
                                logger.debug("Cleared totals: $totalsCleared")
                            }

                            "outstanding" -> {
                                totalsOutstanding = amount.setScale(2, RoundingMode.HALF_UP)
                                logger.debug("Outstanding totals: $totalsOutstanding")
                            }

                            else -> {
                                logger.debug("Unknown transaction state: $transactionState, amount: $amount")
                            }
                        }
                    } else {
                        logger.debug("Skipping row with null values: state=$transactionState, amount=$amount")
                    }
                } catch (ex: Exception) {
                    logger.error("Error processing row in totals calculation: ${ex.message}", ex)
                    meterService.incrementExceptionCaughtCounter("RowProcessingError")
                }
            }

            val result = createTotals(totalsFuture, totalsCleared, totalsOutstanding)
            logger.info("Calculated totals for $accountNameOwner: ${result.totals} (Future: ${result.totalsFuture}, Cleared: ${result.totalsCleared}, Outstanding: ${result.totalsOutstanding})")

            meterService.incrementExceptionThrownCounter("TotalsCalculated")
            return result
        } catch (ex: Exception) {
            logger.error("Error calculating totals for account $accountNameOwner: ${ex.message}", ex)
            meterService.incrementExceptionCaughtCounter("TotalsCalculationError")
            throw ex
        }
    }

    override fun calculateTotalsFromTransactions(transactions: List<Transaction>): Map<TransactionState, BigDecimal> {
        return try {
            logger.debug("Calculating totals from ${transactions.size} transactions")

            val totalsMap = mutableMapOf<TransactionState, BigDecimal>()

            transactions.forEach { transaction ->
                val state = transaction.transactionState
                val amount = transaction.amount

                val currentTotal = totalsMap[state] ?: BigDecimal.ZERO
                val newTotal = currentTotal.add(amount).setScale(2, RoundingMode.HALF_UP)
                totalsMap[state] = newTotal
            }

            logger.debug("Calculated in-memory totals: $totalsMap")
            return totalsMap.toMap()
        } catch (ex: Exception) {
            logger.error("Error calculating totals from transactions: ${ex.message}", ex)
            meterService.incrementExceptionCaughtCounter("InMemoryTotalsCalculationError")
            return emptyMap()
        }
    }

    override fun calculateGrandTotal(totalsMap: Map<TransactionState, BigDecimal>): BigDecimal {
        return try {
            logger.debug("Calculating grand total from totals map: $totalsMap")

            val grandTotal =
                totalsMap.values
                    .fold(BigDecimal.ZERO) { acc, amount -> acc.add(amount) }
                    .setScale(2, RoundingMode.HALF_UP)

            logger.debug("Calculated grand total: $grandTotal")
            return grandTotal
        } catch (ex: Exception) {
            logger.error("Error calculating grand total: ${ex.message}", ex)
            meterService.incrementExceptionCaughtCounter("GrandTotalCalculationError")
            return ZERO_SCALE_2
        }
    }

    override fun createTotals(
        totalsFuture: BigDecimal,
        totalsCleared: BigDecimal,
        totalsOutstanding: BigDecimal,
    ): Totals {
        return try {
            logger.debug("Creating Totals object: Future=$totalsFuture, Cleared=$totalsCleared, Outstanding=$totalsOutstanding")

            val grandTotal =
                calculateGrandTotal(
                    mapOf(
                        TransactionState.Future to totalsFuture,
                        TransactionState.Cleared to totalsCleared,
                        TransactionState.Outstanding to totalsOutstanding,
                    ),
                )

            val result =
                Totals(
                    totalsFuture = totalsFuture,
                    totalsCleared = totalsCleared,
                    totals = grandTotal,
                    totalsOutstanding = totalsOutstanding,
                )

            logger.debug("Created Totals object with grand total: $grandTotal")
            return result
        } catch (ex: Exception) {
            logger.error("Error creating Totals object: ${ex.message}", ex)
            meterService.incrementExceptionCaughtCounter("TotalsCreationError")

            // Return safe default
            return Totals(
                totalsFuture = ZERO_SCALE_2,
                totalsCleared = ZERO_SCALE_2,
                totals = ZERO_SCALE_2,
                totalsOutstanding = ZERO_SCALE_2,
            )
        }
    }

    override fun validateTotals(totals: Totals): Boolean {
        return try {
            logger.debug("Validating totals: $totals")

            // Validate individual amounts are within reasonable limits
            val amounts = listOf(totals.totalsFuture, totals.totalsCleared, totals.totalsOutstanding, totals.totals)
            val allAmountsReasonable =
                amounts.all { amount ->
                    amount.abs().compareTo(BigDecimal(MAX_REASONABLE_AMOUNT)) <= 0
                }

            if (!allAmountsReasonable) {
                logger.warn("Totals validation failed: amounts exceed reasonable limits")
                return false
            }

            // Validate that grand total equals sum of individual totals
            val expectedGrandTotal =
                calculateGrandTotal(
                    mapOf(
                        TransactionState.Future to totals.totalsFuture,
                        TransactionState.Cleared to totals.totalsCleared,
                        TransactionState.Outstanding to totals.totalsOutstanding,
                    ),
                )

            val grandTotalMatches = totals.totals.compareTo(expectedGrandTotal) == 0

            if (!grandTotalMatches) {
                logger.warn("Totals validation failed: grand total ${totals.totals} does not match expected $expectedGrandTotal")
                return false
            }

            logger.debug("Totals validation passed")
            return true
        } catch (ex: Exception) {
            logger.error("Error validating totals: ${ex.message}", ex)
            meterService.incrementExceptionCaughtCounter("TotalsValidationError")
            return false
        }
    }
}
