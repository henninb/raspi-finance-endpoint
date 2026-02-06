package finance.repositories

import finance.domain.Transaction
import finance.domain.TransactionState
import jakarta.transaction.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
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

    @Query("SELECT t.description, COUNT(t) FROM Transaction t WHERE t.description IN :descriptionNames GROUP BY t.description")
    fun countByDescriptionNameIn(
        @Param("descriptionNames") descriptionNames: List<String>,
    ): List<Array<Any>>

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.category = :categoryName")
    fun countByCategoryName(
        @Param("categoryName") categoryName: String,
    ): Long

    @Query("SELECT t.category, COUNT(t) FROM Transaction t WHERE t.category IN :categoryNames GROUP BY t.category")
    fun countByCategoryNameIn(
        @Param("categoryNames") categoryNames: List<String>,
    ): List<Array<Any>>

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

    // Paginated queries for all active transactions
    fun findByActiveStatus(
        activeStatus: Boolean = true,
        pageable: Pageable,
    ): Page<Transaction>

    // Paginated queries for account-specific transactions
    fun findByAccountNameOwnerAndActiveStatus(
        accountNameOwner: String,
        activeStatus: Boolean = true,
        pageable: Pageable,
    ): Page<Transaction>

    // Paginated queries for category-specific transactions
    fun findByCategoryAndActiveStatus(
        category: String,
        activeStatus: Boolean = true,
        pageable: Pageable,
    ): Page<Transaction>

    // Paginated queries for description-specific transactions
    fun findByDescriptionAndActiveStatus(
        description: String,
        activeStatus: Boolean = true,
        pageable: Pageable,
    ): Page<Transaction>

    // Bulk deactivate all transactions for an account
    @Modifying
    @Transactional
    @Query(
        value = "UPDATE {h-schema}t_transaction SET active_status = false, date_updated = now() WHERE account_name_owner = :accountNameOwner",
        nativeQuery = true,
    )
    fun deactivateAllTransactionsByAccountNameOwner(
        @Param("accountNameOwner") accountNameOwner: String,
    ): Int

    // Bulk update accountNameOwner for all transactions (for account renaming)
    @Modifying
    @Transactional
    @Query(
        value = "UPDATE {h-schema}t_transaction SET account_name_owner = :newAccountNameOwner, date_updated = now() WHERE account_name_owner = :oldAccountNameOwner",
        nativeQuery = true,
    )
    fun updateAccountNameOwnerForAllTransactions(
        @Param("oldAccountNameOwner") oldAccountNameOwner: String,
        @Param("newAccountNameOwner") newAccountNameOwner: String,
    ): Int

    // --- Owner-scoped methods for multi-tenancy (Phase 4) ---

    fun findByOwnerAndGuid(
        owner: String,
        guid: String,
    ): Optional<Transaction>

    fun findByOwnerAndAccountNameOwnerAndActiveStatusOrderByTransactionDateDesc(
        owner: String,
        accountNameOwner: String,
        activeStatus: Boolean = true,
    ): List<Transaction>

    fun findByOwnerAndCategoryAndActiveStatusOrderByTransactionDateDesc(
        owner: String,
        category: String,
        activeStatus: Boolean = true,
    ): List<Transaction>

    fun findByOwnerAndDescriptionAndActiveStatusOrderByTransactionDateDesc(
        owner: String,
        description: String,
        activeStatus: Boolean = true,
    ): List<Transaction>

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.owner = :owner AND t.description = :descriptionName")
    fun countByOwnerAndDescriptionName(
        @Param("owner") owner: String,
        @Param("descriptionName") descriptionName: String,
    ): Long

    @Query("SELECT t.description, COUNT(t) FROM Transaction t WHERE t.owner = :owner AND t.description IN :descriptionNames GROUP BY t.description")
    fun countByOwnerAndDescriptionNameIn(
        @Param("owner") owner: String,
        @Param("descriptionNames") descriptionNames: List<String>,
    ): List<Array<Any>>

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.owner = :owner AND t.category = :categoryName")
    fun countByOwnerAndCategoryName(
        @Param("owner") owner: String,
        @Param("categoryName") categoryName: String,
    ): Long

    @Query("SELECT t.category, COUNT(t) FROM Transaction t WHERE t.owner = :owner AND t.category IN :categoryNames GROUP BY t.category")
    fun countByOwnerAndCategoryNameIn(
        @Param("owner") owner: String,
        @Param("categoryNames") categoryNames: List<String>,
    ): List<Array<Any>>

    @Query(
        value = "SELECT SUM(amount), count(amount), transaction_state FROM t_transaction WHERE owner = :owner AND account_name_owner = :accountNameOwner AND active_status = true GROUP BY transaction_state",
        nativeQuery = true,
    )
    fun sumTotalsForActiveTransactionsByOwnerAndAccountNameOwner(
        @Param("owner") owner: String,
        @Param("accountNameOwner") accountNameOwner: String,
    ): List<Any>

    fun findByOwnerAndAccountNameOwnerAndActiveStatusAndTransactionStateNotInOrderByTransactionDateDesc(
        owner: String,
        accountNameOwner: String,
        activeStatus: Boolean = true,
        transactionStates: List<TransactionState>,
    ): List<Transaction>

    fun findByOwnerAndTransactionDateBetween(
        owner: String,
        startDate: LocalDate,
        endDate: LocalDate,
        pageable: Pageable,
    ): Page<Transaction>

    fun findByOwnerAndActiveStatus(
        owner: String,
        activeStatus: Boolean = true,
        pageable: Pageable,
    ): Page<Transaction>

    fun findByOwnerAndAccountNameOwnerAndActiveStatus(
        owner: String,
        accountNameOwner: String,
        activeStatus: Boolean = true,
        pageable: Pageable,
    ): Page<Transaction>

    fun findByOwnerAndCategoryAndActiveStatus(
        owner: String,
        category: String,
        activeStatus: Boolean = true,
        pageable: Pageable,
    ): Page<Transaction>

    fun findByOwnerAndDescriptionAndActiveStatus(
        owner: String,
        description: String,
        activeStatus: Boolean = true,
        pageable: Pageable,
    ): Page<Transaction>

    @Modifying
    @Transactional
    @Query(
        value = "UPDATE {h-schema}t_transaction SET active_status = false, date_updated = now() WHERE account_name_owner = :accountNameOwner AND owner = :owner",
        nativeQuery = true,
    )
    fun deactivateAllTransactionsByOwnerAndAccountNameOwner(
        @Param("owner") owner: String,
        @Param("accountNameOwner") accountNameOwner: String,
    ): Int

    @Modifying
    @Transactional
    @Query(
        value = "UPDATE {h-schema}t_transaction SET account_name_owner = :newAccountNameOwner, date_updated = now() WHERE account_name_owner = :oldAccountNameOwner AND owner = :owner",
        nativeQuery = true,
    )
    fun updateAccountNameOwnerForAllTransactionsByOwner(
        @Param("owner") owner: String,
        @Param("oldAccountNameOwner") oldAccountNameOwner: String,
        @Param("newAccountNameOwner") newAccountNameOwner: String,
    ): Int
}
