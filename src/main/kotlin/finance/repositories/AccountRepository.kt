package finance.repositories

import finance.models.Account
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.*
import javax.transaction.Transactional

open interface AccountRepository<T : Account> : JpaRepository<T, Long> {
    fun findByAccountId(transactionId: Long?): Account
    fun findByAccountNameOwner(accountNameOwner: String): Optional<Account>
    fun findByActiveStatusOrderByAccountNameOwner(activeStatus: String): List<Account>

    @Modifying
    @Transactional
    @Query(value = "DELETE from t_account WHERE account_name_owner = ?1", nativeQuery = true)
    fun deleteByAccountNameOwner(accountNameOwner: String)
}
