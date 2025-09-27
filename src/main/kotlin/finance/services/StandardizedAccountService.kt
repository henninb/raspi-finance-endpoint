package finance.services

import finance.domain.Account
import finance.domain.AccountType
import finance.domain.ServiceResult
import finance.domain.TransactionState
import finance.repositories.AccountRepository
import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.InvalidDataAccessResourceUsageException
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.Timestamp
import java.util.*

/**
 * Standardized Account Service implementing ServiceResult pattern
 * Provides both new standardized methods and legacy compatibility
 */
@Service
@Primary
class StandardizedAccountService(
    private val accountRepository: AccountRepository
) : StandardizedBaseService<Account, String>() {

    override fun getEntityName(): String = "Account"

    // ===== New Standardized ServiceResult Methods =====

    override fun findAllActive(): ServiceResult<List<Account>> {
        return handleServiceOperation("findAllActive", null) {
            accountRepository.findByActiveStatusOrderByAccountNameOwner(true)
        }
    }

    override fun findById(id: String): ServiceResult<Account> {
        return handleServiceOperation("findById", id) {
            val optionalAccount = accountRepository.findByAccountNameOwner(id)
            if (optionalAccount.isPresent) {
                optionalAccount.get()
            } else {
                throw EntityNotFoundException("Account not found: $id")
            }
        }
    }

    override fun save(entity: Account): ServiceResult<Account> {
        return handleServiceOperation("save", entity.accountNameOwner) {
            // Check if account already exists
            val existingAccount = accountRepository.findByAccountNameOwner(entity.accountNameOwner)
            if (existingAccount.isPresent) {
                throw org.springframework.dao.DataIntegrityViolationException("Account already exists: ${entity.accountNameOwner}")
            }

            val violations = validator.validate(entity)
            if (violations.isNotEmpty()) {
                throw jakarta.validation.ConstraintViolationException("Validation failed", violations)
            }

            // Set timestamps
            val timestamp = Timestamp(System.currentTimeMillis())
            entity.dateAdded = timestamp
            entity.dateUpdated = timestamp

            accountRepository.saveAndFlush(entity)
        }
    }

    override fun update(entity: Account): ServiceResult<Account> {
        return handleServiceOperation("update", entity.accountId.toString()) {
            val existingAccount = accountRepository.findByAccountId(entity.accountId!!)
            if (existingAccount.isEmpty) {
                throw EntityNotFoundException("Account not found: ${entity.accountId}")
            }

            // Update fields from the provided entity
            val accountToUpdate = existingAccount.get()
            accountToUpdate.accountNameOwner = entity.accountNameOwner
            accountToUpdate.accountType = entity.accountType
            accountToUpdate.activeStatus = entity.activeStatus
            accountToUpdate.moniker = entity.moniker
            accountToUpdate.future = entity.future
            accountToUpdate.outstanding = entity.outstanding
            accountToUpdate.cleared = entity.cleared
            accountToUpdate.dateClosed = entity.dateClosed
            accountToUpdate.dateUpdated = Timestamp(System.currentTimeMillis())

            accountRepository.saveAndFlush(accountToUpdate)
        }
    }

    override fun deleteById(id: String): ServiceResult<Boolean> {
        return handleServiceOperation("deleteById", id) {
            val optionalAccount = accountRepository.findByAccountNameOwner(id)
            if (optionalAccount.isEmpty) {
                throw EntityNotFoundException("Account not found: $id")
            }
            accountRepository.delete(optionalAccount.get())
            true
        }
    }

    // ===== Legacy Method Compatibility =====

    fun account(accountNameOwner: String): Optional<Account> {
        return accountRepository.findByAccountNameOwner(accountNameOwner)
    }

    fun accounts(): List<Account> {
        val result = findAllActive()
        return when (result) {
            is ServiceResult.Success -> result.data
            else -> emptyList()
        }
    }

    fun accountsByType(accountType: AccountType): List<Account> {
        return accountRepository.findByActiveStatusAndAccountType(true, accountType)
    }

    fun findAccountsThatRequirePayment(): List<Account> {
        updateTotalsForAllAccounts()
        updateValidationDatesForAllAccounts()
        return accountRepository.findAccountsThatRequirePayment()
    }

    fun sumOfAllTransactionsByTransactionState(transactionState: TransactionState): BigDecimal {
        val totals: BigDecimal = accountRepository.sumOfAllTransactionsByTransactionState(transactionState.toString())
        return totals.setScale(2, RoundingMode.HALF_UP)
    }

    fun insertAccount(account: Account): Account {
        val result = save(account)
        return when (result) {
            is ServiceResult.Success -> result.data
            is ServiceResult.ValidationError -> {
                val violations = result.errors.map { (field, message) ->
                    object : jakarta.validation.ConstraintViolation<Account> {
                        override fun getMessage(): String = message
                        override fun getMessageTemplate(): String = message
                        override fun getRootBean(): Account = account
                        override fun getRootBeanClass(): Class<Account> = Account::class.java
                        override fun getLeafBean(): Any = account
                        override fun getExecutableParameters(): Array<Any> = emptyArray()
                        override fun getExecutableReturnValue(): Any? = null
                        override fun getPropertyPath(): jakarta.validation.Path {
                            return object : jakarta.validation.Path {
                                override fun toString(): String = field
                                override fun iterator(): MutableIterator<jakarta.validation.Path.Node> = mutableListOf<jakarta.validation.Path.Node>().iterator()
                            }
                        }
                        override fun getInvalidValue(): Any? = null
                        override fun getConstraintDescriptor(): jakarta.validation.metadata.ConstraintDescriptor<*>? = null
                        override fun <U : Any?> unwrap(type: Class<U>?): U = throw UnsupportedOperationException()
                    }
                }.toSet()
                throw ValidationException(jakarta.validation.ConstraintViolationException("Validation failed", violations))
            }
            is ServiceResult.BusinessError -> {
                if (result.message.contains("already exists")) {
                    throw DataIntegrityViolationException("Account not inserted as the account already exists ${account.accountNameOwner}.")
                } else {
                    throw RuntimeException("Failed to insert account: ${result}")
                }
            }
            else -> throw RuntimeException("Failed to insert account: ${result}")
        }
    }


    fun updateTotalsForAllAccounts(): Boolean {
        try {
            accountRepository.updateTotalsForAllAccounts()
        } catch (invalidDataAccessResourceUsageException: InvalidDataAccessResourceUsageException) {
            meterService.incrementExceptionCaughtCounter("InvalidDataAccessResourceUsageException")
            logger.warn("InvalidDataAccessResourceUsageException: ${invalidDataAccessResourceUsageException.message}")
        }
        return true
    }

    fun updateValidationDatesForAllAccounts(): Boolean {
        try {
            accountRepository.updateValidationDateForAllAccounts()
        } catch (invalidDataAccessResourceUsageException: InvalidDataAccessResourceUsageException) {
            meterService.incrementExceptionCaughtCounter("InvalidDataAccessResourceUsageException")
            logger.warn("InvalidDataAccessResourceUsageException: ${invalidDataAccessResourceUsageException.message}")
        }
        return true
    }


    fun renameAccountNameOwner(oldAccountNameOwner: String, newAccountNameOwner: String): Account {
        val oldAccount = accountRepository.findByAccountNameOwner(oldAccountNameOwner)
            .orElseThrow { EntityNotFoundException("Account not found") }

        logger.info("Renaming account from $oldAccountNameOwner to $newAccountNameOwner")
        oldAccount.accountNameOwner = newAccountNameOwner
        oldAccount.dateUpdated = Timestamp(System.currentTimeMillis())
        val renamedAccount = accountRepository.saveAndFlush(oldAccount)
        logger.info("Successfully renamed account to: $newAccountNameOwner")
        return renamedAccount
    }

    fun deactivateAccount(accountNameOwner: String): Account {
        val account = accountRepository.findByAccountNameOwner(accountNameOwner)
            .orElseThrow { EntityNotFoundException("Account not found: $accountNameOwner") }

        logger.info("Deactivating account: $accountNameOwner")
        account.activeStatus = false
        account.dateUpdated = Timestamp(System.currentTimeMillis())
        val updatedAccount = accountRepository.saveAndFlush(account)
        logger.info("Successfully deactivated account: $accountNameOwner")
        return updatedAccount
    }

    fun activateAccount(accountNameOwner: String): Account {
        val account = accountRepository.findByAccountNameOwner(accountNameOwner)
            .orElseThrow { EntityNotFoundException("Account not found: $accountNameOwner") }

        logger.info("Activating account: $accountNameOwner")
        account.activeStatus = true
        account.dateUpdated = Timestamp(System.currentTimeMillis())
        val updatedAccount = accountRepository.saveAndFlush(account)
        logger.info("Successfully activated account: $accountNameOwner")
        return updatedAccount
    }
}
