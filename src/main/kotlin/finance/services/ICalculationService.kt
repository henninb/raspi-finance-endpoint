package finance.services

import finance.domain.Totals
import finance.domain.Transaction
import finance.domain.TransactionState
import java.math.BigDecimal

/**
 * Interface for calculation operations extracted from TransactionService
 * Handles financial calculations and totals computation
 */
interface ICalculationService {
    /**
     * Calculates active totals by account name owner using repository aggregation
     * @param accountNameOwner The account name to calculate totals for
     * @return Totals object containing totals for Future, Cleared, Outstanding, and Grand Total
     */
    fun calculateActiveTotalsByAccountNameOwner(accountNameOwner: String): Totals

    /**
     * Calculates totals from a collection of transactions (in-memory calculation)
     * Useful for testing and when transactions are already loaded
     * @param transactions List of transactions to calculate totals for
     * @return Map of TransactionState to BigDecimal totals
     */
    fun calculateTotalsFromTransactions(transactions: List<Transaction>): Map<TransactionState, BigDecimal>

    /**
     * Calculates grand total from individual state totals
     * @param totalsMap Map of TransactionState to BigDecimal amounts
     * @return Grand total as BigDecimal with proper rounding
     */
    fun calculateGrandTotal(totalsMap: Map<TransactionState, BigDecimal>): BigDecimal

    /**
     * Creates a Totals object from calculated amounts
     * @param totalsFuture Future transaction total
     * @param totalsCleared Cleared transaction total
     * @param totalsOutstanding Outstanding transaction total
     * @return Totals object with calculated grand total
     */
    fun createTotals(
        totalsFuture: BigDecimal,
        totalsCleared: BigDecimal,
        totalsOutstanding: BigDecimal,
    ): Totals

    /**
     * Validates that calculated totals are within expected ranges
     * @param totals The Totals object to validate
     * @return true if totals are valid, false otherwise
     */
    fun validateTotals(totals: Totals): Boolean
}
