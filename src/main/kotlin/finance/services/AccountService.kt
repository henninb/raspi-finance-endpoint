package finance.services

import finance.domain.Account
import finance.domain.AccountType
import finance.domain.ServiceResult
import finance.domain.TransactionState
import finance.repositories.AccountRepository
import finance.repositories.ValidationAmountRepository
import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.springframework.context.annotation.Primary
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.InvalidDataAccessResourceUsageException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.Timestamp
import java.util.Optional

/**
 * Standardized Account Service implementing ServiceResult pattern
 * Provides both new standardized methods and legacy compatibility
 */
@Service
@Primary
class AccountService(
    private val accountRepository: AccountRepository,
    private val validationAmountRepository: ValidationAmountRepository,
    private val transactionRepository: finance.repositories.TransactionRepository,
) : CrudBaseService<Account, String>() {
    override fun getEntityName(): String = "Account"

    // ===== New Standardized ServiceResult Methods =====

    override fun findAllActive(): ServiceResult<List<Account>> =
        handleServiceOperation("findAllActive", null) {
            accountRepository.findByActiveStatusOrderByAccountNameOwner(true)
        }

    override fun findById(id: String): ServiceResult<Account> =
        handleServiceOperation("findById", id) {
            val optionalAccount = accountRepository.findByAccountNameOwner(id)
            if (optionalAccount.isPresent) {
                optionalAccount.get()
            } else {
                throw EntityNotFoundException("Account not found: $id")
            }
        }

    override fun save(entity: Account): ServiceResult<Account> =
        handleServiceOperation("save", entity.accountNameOwner) {
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

    override fun update(entity: Account): ServiceResult<Account> =
        handleServiceOperation("update", entity.accountNameOwner) {
            val existingAccount = accountRepository.findByAccountNameOwner(entity.accountNameOwner)
            if (existingAccount.isEmpty) {
                throw EntityNotFoundException("Account not found: ${entity.accountNameOwner}")
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

    override fun deleteById(id: String): ServiceResult<Boolean> =
        handleServiceOperation("deleteById", id) {
            val optionalAccount = accountRepository.findByAccountNameOwner(id)
            if (optionalAccount.isEmpty) {
                throw EntityNotFoundException("Account not found: $id")
            }
            val account = optionalAccount.get()

            // Delete all associated ValidationAmount records first to avoid foreign key constraint violation
            // This is needed for production DB which lacks ON DELETE CASCADE
            val validationAmounts = validationAmountRepository.findByAccountId(account.accountId)
            if (validationAmounts.isNotEmpty()) {
                logger.info("Deleting ${validationAmounts.size} ValidationAmount records for account: ${account.accountNameOwner}")
                validationAmountRepository.deleteAll(validationAmounts)
            }

            accountRepository.delete(account)
            true
        }

    // ===== Paginated ServiceResult Methods =====

    /**
     * Find all active accounts with pagination.
     * Sorted by accountNameOwner ascending.
     */
    fun findAllActive(pageable: Pageable): ServiceResult<Page<Account>> =
        handleServiceOperation("findAllActive-paginated", null) {
            accountRepository.findAllByActiveStatusOrderByAccountNameOwner(true, pageable)
        }

    // ===== Legacy Method Compatibility =====

    fun account(accountNameOwner: String): Optional<Account> = accountRepository.findByAccountNameOwner(accountNameOwner)

    fun accounts(): List<Account> {
        val result = findAllActive()
        return when (result) {
            is ServiceResult.Success -> result.data
            else -> emptyList()
        }
    }

    fun accountsByType(accountType: AccountType): List<Account> = accountRepository.findByActiveStatusAndAccountType(true, accountType)

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
            is ServiceResult.Success -> {
                result.data
            }

            is ServiceResult.ValidationError -> {
                val violations =
                    result.errors
                        .map { (field, message) ->
                            object : jakarta.validation.ConstraintViolation<Account> {
                                override fun getMessage(): String = message

                                override fun getMessageTemplate(): String = message

                                override fun getRootBean(): Account = account

                                override fun getRootBeanClass(): Class<Account> = Account::class.java

                                override fun getLeafBean(): Any = account

                                override fun getExecutableParameters(): Array<Any> = emptyArray()

                                override fun getExecutableReturnValue(): Any? = null

                                override fun getPropertyPath(): jakarta.validation.Path =
                                    object : jakarta.validation.Path {
                                        override fun toString(): String = field

                                        override fun iterator(): MutableIterator<jakarta.validation.Path.Node> = mutableListOf<jakarta.validation.Path.Node>().iterator()
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
                    throw RuntimeException("Failed to insert account: $result")
                }
            }

            else -> {
                throw RuntimeException("Failed to insert account: $result")
            }
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

    @org.springframework.transaction.annotation.Transactional
    fun renameAccountNameOwner(
        oldAccountNameOwner: String,
        newAccountNameOwner: String,
    ): Account {
        logger.info("Renaming account from $oldAccountNameOwner to $newAccountNameOwner")

        val oldAccount =
            accountRepository
                .findByAccountNameOwner(oldAccountNameOwner)
                .orElseThrow {
                    logger.error("Account not found: $oldAccountNameOwner")
                    EntityNotFoundException("Account not found: $oldAccountNameOwner")
                }

        try {
            oldAccount.accountNameOwner = newAccountNameOwner
            oldAccount.dateUpdated = Timestamp(System.currentTimeMillis())
            val renamedAccount = accountRepository.saveAndFlush(oldAccount)
            logger.info("Successfully renamed account to: $newAccountNameOwner (accountId: ${renamedAccount.accountId})")
            return renamedAccount
        } catch (ex: org.springframework.dao.DataIntegrityViolationException) {
            logger.error("Cannot rename account: $newAccountNameOwner already exists or violates constraints", ex)
            throw ex
        } catch (ex: Exception) {
            logger.error("Failed to rename account from $oldAccountNameOwner to $newAccountNameOwner: ${ex.message}", ex)
            throw ex
        }
    }

    @org.springframework.transaction.annotation.Transactional
    fun deactivateAccount(accountNameOwner: String): Account {
        val account =
            accountRepository
                .findByAccountNameOwner(accountNameOwner)
                .orElseThrow { EntityNotFoundException("Account not found: $accountNameOwner") }

        logger.info("Deactivating account: $accountNameOwner")

        // Deactivate all transactions for this account
        val transactionsUpdated = transactionRepository.deactivateAllTransactionsByAccountNameOwner(accountNameOwner)
        logger.info("Deactivated $transactionsUpdated transactions for account: $accountNameOwner")

        // Deactivate the account
        account.activeStatus = false
        account.dateClosed = Timestamp(System.currentTimeMillis())
        account.dateUpdated = Timestamp(System.currentTimeMillis())
        val updatedAccount = accountRepository.saveAndFlush(account)
        logger.info("Successfully deactivated account: $accountNameOwner")
        return updatedAccount
    }

    fun activateAccount(accountNameOwner: String): Account {
        val account =
            accountRepository
                .findByAccountNameOwner(accountNameOwner)
                .orElseThrow { EntityNotFoundException("Account not found: $accountNameOwner") }

        logger.info("Activating account: $accountNameOwner")
        account.activeStatus = true
        account.dateUpdated = Timestamp(System.currentTimeMillis())
        val updatedAccount = accountRepository.saveAndFlush(account)
        logger.info("Successfully activated account: $accountNameOwner")
        return updatedAccount
    }
}
