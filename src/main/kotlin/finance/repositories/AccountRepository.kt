package finance.repositories

import finance.domain.Account
import finance.domain.AccountType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.math.BigDecimal
import java.util.*
import javax.transaction.Transactional

interface AccountRepository : JpaRepository<Account, Long> {
    fun findByAccountNameOwner(accountNameOwner: String): Optional<Account>
    fun findByActiveStatusOrderByAccountNameOwner(activeStatus: Boolean = true): List<Account>
    fun findByActiveStatusAndAccountTypeAndTotalsIsGreaterThanOrderByAccountNameOwner(
        activeStatus: Boolean = true,
        accountType: AccountType = AccountType.Credit,
        totals: BigDecimal = BigDecimal(
            0.0
        )
    ): List<Account>

    @Transactional
    fun deleteByAccountNameOwner(accountNameOwner: String)

    //TODO: need to deprecate this method 6/24/2021
    @Modifying
    @Transactional
    @Query(
        value = "UPDATE t_account SET totals = x.totals FROM (SELECT account_name_owner, SUM(amount) AS totals FROM t_transaction WHERE active_status = true GROUP BY account_name_owner) x WHERE t_account.account_name_owner = x.account_name_owner",
        nativeQuery = true
    )
    //SpEL
//    UPDATE Persons
//    SET  Persons.PersonCityName=(SELECT AddressList.PostCode
//    FROM AddressList
//    WHERE AddressList.PersonId = Persons.PersonId)
    //@Query(value = "UPDATE #{entityName} SET totals=(SELECT SUM(amount) AS totals FROM Transaction WHERE active_status = true and Account.account_name_owner = x.account_name_owner")
    fun updateTheGrandTotalForAllTransactions()

    //TODO: need to deprecate this method 6/24/2021
    @Modifying
    @Transactional
    @Query(
        value = "UPDATE t_account SET totals_balanced = x.totals_balanced FROM (SELECT account_name_owner, SUM(amount) AS totals_balanced FROM t_transaction WHERE transaction_state = 'cleared' AND active_status = true GROUP BY account_name_owner) x WHERE t_account.account_name_owner = x.account_name_owner",
        nativeQuery = true
    )
    fun updateTheGrandTotalForAllClearedTransactions()

    @Modifying
    @Transactional
    @Query(
        value = "UPDATE t_account SET cleared = x.summation, date_updated = now() FROM (SELECT account_name_owner, SUM(amount) AS summation FROM t_transaction WHERE transaction_state = 'cleared' AND active_status = true GROUP BY account_name_owner) x WHERE t_account.account_name_owner = x.account_name_owner",
        nativeQuery = true
    )
    fun updateTotalsForClearedTransactionType()

    @Modifying
    @Transactional
    @Query(
        value = "UPDATE t_account SET future = x.summation, date_updated = now() FROM (SELECT account_name_owner, SUM(amount) AS summation FROM t_transaction WHERE transaction_state = 'future' AND active_status = true GROUP BY account_name_owner) x WHERE t_account.account_name_owner = x.account_name_owner",
        nativeQuery = true
    )
    fun updateTotalsForFutureTransactionType()

    @Modifying
    @Transactional
    @Query(
        value = "UPDATE t_account SET outstanding = x.summation, date_updated = now() FROM (SELECT account_name_owner, SUM(amount) AS summation FROM t_transaction WHERE transaction_state = 'outstanding' AND active_status = true GROUP BY account_name_owner) x WHERE t_account.account_name_owner = x.account_name_owner",
        nativeQuery = true
    )
    fun updateTotalsForOutstandingTransactionType()

    @Query(
        value = "SELECT COALESCE((A.debits - B.credits), 0.0) FROM ( SELECT SUM(amount) AS debits FROM t_transaction WHERE account_type = 'debit' AND active_status = true) A,( SELECT SUM(amount) AS credits FROM t_transaction WHERE account_type = 'credit' AND active_status = true) B",
        nativeQuery = true
    )
    fun computeTheGrandTotalForAllTransactions(): BigDecimal

    @Query(
        value = "SELECT COALESCE((A.debits - B.credits), 0.0) FROM ( SELECT SUM(amount) AS debits FROM t_transaction WHERE account_type = 'debit' AND transaction_state = 'cleared' AND active_status = true) A,( SELECT SUM(amount) AS credits FROM t_transaction WHERE account_type = 'credit' and transaction_state = 'cleared' AND active_status = true) B",
        nativeQuery = true
    )
    fun computeTheGrandTotalForAllClearedTransactions(): BigDecimal

    @Query(
        value = "SELECT account_name_owner FROM t_transaction WHERE transaction_state = 'cleared' and account_name_owner in (select account_name_owner from t_account where account_type = 'credit' and active_status = true) or (transaction_state = 'outstanding' and account_type = 'credit' and description ='payment') group by account_name_owner having sum(amount) > 0 order by account_name_owner",
        nativeQuery = true
    )
    fun findAccountsThatRequirePayment(): List<String>
}
