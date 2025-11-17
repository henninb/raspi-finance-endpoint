package finance.repositories

import finance.domain.Transaction
import finance.domain.TransactionState
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate
import java.util.Optional

interface TransactionRepository : JpaRepository<Transaction, Long> {
    fun findByGuid(guid: String): Optional<Transaction>

    fun findByAccountNameOwnerAndActiveStatusOrderByTransactionDateDesc(
        accountNameOwner: String,
        activeStatus: Boolean = true,
    ): List<Transaction>

    fun findByCategoryAndActiveStatusOrderByTransactionDateDesc(
        category: String,
        activeStatus: Boolean = true,
    ): List<Transaction>

    fun findByDescriptionAndActiveStatusOrderByTransactionDateDesc(
        description: String,
        activeStatus: Boolean = true,
    ): List<Transaction>

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.description = :descriptionName")
    fun countByDescriptionName(
        @Param("descriptionName") descriptionName: String,
    ): Long

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.category = :categoryName")
    fun countByCategoryName(
        @Param("categoryName") categoryName: String,
    ): Long

    // TODO: 6/27/20201 - add a default param for activeState
    // TODO: 6/27/20201 - what happens if the list is empty
    // TODO: 6/27/20201 - COALESCE, is it required?
    // TODO: 6/24/20201 - CREATE INDEX idx_transaction_account_name_owner ON t_transaction(account_name_owner, active_status);
    @Query(
        value = "SELECT SUM(amount), count(amount), transaction_state FROM t_transaction WHERE account_name_owner = :accountNameOwner AND active_status = true GROUP BY transaction_state",
        nativeQuery = true,
    )
    fun sumTotalsForActiveTransactionsByAccountNameOwner(
        @Param("accountNameOwner") accountNameOwner: String,
    ): List<Any>

//    @Query(
//        value = "SELECT * FROM t_transaction WHERE category = :category AND active_status = true",
//        nativeQuery = true
//    )
//    fun findTransactionsByCategory(
//        @Param("category") category: String
//    ): List<Transaction>

    fun findByAccountNameOwnerAndActiveStatusAndTransactionStateNotInOrderByTransactionDateDesc(
        accountNameOwner: String,
        activeStatus: Boolean = true,
        transactionStates: List<TransactionState>,
    ): List<Transaction>

    // Date range across all accounts with pagination
    fun findByTransactionDateBetween(
        startDate: LocalDate,
        endDate: LocalDate,
        pageable: Pageable,
    ): Page<Transaction>
}
