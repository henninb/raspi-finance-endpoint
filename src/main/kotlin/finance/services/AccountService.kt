package finance.services

import finance.configurations.ResilienceComponents
import finance.domain.Account
import finance.domain.AccountType
import finance.domain.ServiceResult
import finance.domain.TransactionState
import finance.repositories.AccountRepository
import finance.repositories.ValidationAmountRepository
import finance.utils.TenantContext
import finance.utils.orThrowNotFound
import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import jakarta.validation.Validator
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.InvalidDataAccessResourceUsageException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Optional

@Service
class AccountService
    constructor(
        private val accountRepository: AccountRepository,
        private val validationAmountRepository: ValidationAmountRepository,
        private val transactionRepository: finance.repositories.TransactionRepository,
        meterService: MeterService,
        validator: Validator,
        resilienceComponents: ResilienceComponents,
    ) : CrudBaseService<Account, String>(meterService, validator, resilienceComponents) {
        override fun getEntityName(): String = "Account"

        // ===== New Standardized ServiceResult Methods =====

        override fun findAllActive(): ServiceResult<List<Account>> =
            handleServiceOperation("findAllActive", null) {
                val owner = TenantContext.getCurrentOwner()
                accountRepository.findByOwnerAndActiveStatusOrderByAccountNameOwner(owner, true)
            }

        override fun findById(id: String): ServiceResult<Account> =
            handleServiceOperation("findById", id) {
                val owner = TenantContext.getCurrentOwner()
                accountRepository.findByOwnerAndAccountNameOwner(owner, id).orThrowNotFound("Account", id)
            }

        override fun save(entity: Account): ServiceResult<Account> =
            handleServiceOperation("save", entity.accountNameOwner) {
                val owner = TenantContext.getCurrentOwner()
                entity.owner = owner

                // Check if account already exists
                val existingAccount = accountRepository.findByOwnerAndAccountNameOwner(owner, entity.accountNameOwner)
                if (existingAccount.isPresent) {
                    throw org.springframework.dao.DataIntegrityViolationException("Account already exists: ${entity.accountNameOwner}")
                }

                validateOrThrow(entity)

                val timestamp = nowTimestamp()
                entity.dateAdded = timestamp
                entity.dateUpdated = timestamp

                accountRepository.saveAndFlush(entity)
            }

        override fun update(entity: Account): ServiceResult<Account> =
            handleServiceOperation("update", entity.accountNameOwner) {
                val owner = TenantContext.getCurrentOwner()
                val accountToUpdate =
                    accountRepository
                        .findByOwnerAndAccountNameOwner(owner, entity.accountNameOwner)
                        .orThrowNotFound("Account", entity.accountNameOwner)
                accountToUpdate.accountNameOwner = entity.accountNameOwner
                accountToUpdate.accountType = entity.accountType
                accountToUpdate.activeStatus = entity.activeStatus
                accountToUpdate.moniker = entity.moniker
                accountToUpdate.future = entity.future
                accountToUpdate.outstanding = entity.outstanding
                accountToUpdate.cleared = entity.cleared
                accountToUpdate.dateClosed = entity.dateClosed
                accountToUpdate.dateUpdated = nowTimestamp()

                accountRepository.saveAndFlush(accountToUpdate)
            }

        override fun deleteById(id: String): ServiceResult<Account> =
            handleServiceOperation("deleteById", id) {
                val owner = TenantContext.getCurrentOwner()
                val account = accountRepository.findByOwnerAndAccountNameOwner(owner, id).orThrowNotFound("Account", id)
                val deleted = validationAmountRepository.deleteByOwnerAndAccountId(owner, account.accountId)
                if (deleted > 0) {
                    logger.info("Deleted $deleted ValidationAmount records for account: ${account.accountNameOwner}")
                }

                accountRepository.delete(account)
                account
            }

        // ===== Paginated ServiceResult Methods =====

        /**
         * Find all active accounts with pagination.
         * Sorted by accountNameOwner ascending.
         */
        fun findAllActive(pageable: Pageable): ServiceResult<Page<Account>> =
            handleServiceOperation("findAllActive-paginated", null) {
                val owner = TenantContext.getCurrentOwner()
                accountRepository.findAllByOwnerAndActiveStatusOrderByAccountNameOwner(owner, true, pageable)
            }

        // ===== Legacy Method Compatibility =====

        fun account(accountNameOwner: String): Optional<Account> {
            val owner = TenantContext.getCurrentOwner()
            return accountRepository.findByOwnerAndAccountNameOwner(owner, accountNameOwner)
        }

        fun accounts(): List<Account> {
            val result = findAllActive()
            return when (result) {
                is ServiceResult.Success -> result.data
                else -> emptyList()
            }
        }

        fun accountsByType(accountType: AccountType): List<Account> {
            val owner = TenantContext.getCurrentOwner()
            return accountRepository.findByOwnerAndActiveStatusAndAccountType(owner, true, accountType)
        }

        fun findAccountsThatRequirePayment(): List<Account> {
            val owner = TenantContext.getCurrentOwner()
            updateTotalsForAllAccounts()
            updateValidationDatesForAllAccounts()
            return accountRepository.findAccountsThatRequirePaymentByOwner(owner, true, AccountType.Credit)
        }

        fun sumOfAllTransactionsByTransactionState(transactionState: TransactionState): BigDecimal {
            val owner = TenantContext.getCurrentOwner()
            val totals: BigDecimal = accountRepository.sumOfAllTransactionsByTransactionStateAndOwner(transactionState.toString(), owner)
            return totals.setScale(2, RoundingMode.HALF_UP)
        }

        fun insertAccount(account: Account): Account =
            when (val result = save(account)) {
                is ServiceResult.Success -> {
                    result.data
                }

                is ServiceResult.ValidationError -> {
                    throw ValidationException("Validation failed: ${result.errors}")
                }

                is ServiceResult.BusinessError -> {
                    if (result.message.contains("already exists")) {
                        throw DataIntegrityViolationException("Account not inserted as the account already exists ${account.accountNameOwner}.")
                    } else {
                        throw RuntimeException("Failed to insert account: ${result.message}")
                    }
                }

                is ServiceResult.NotFound -> {
                    throw EntityNotFoundException("Account not found: ${account.accountNameOwner}")
                }

                is ServiceResult.SystemError -> {
                    throw RuntimeException("Failed to insert account", result.exception)
                }
            }

        fun updateTotalsForAllAccounts(): Boolean {
            val owner = TenantContext.getCurrentOwner()
            try {
                accountRepository.updateTotalsForAllAccountsByOwner(owner)
            } catch (invalidDataAccessResourceUsageException: InvalidDataAccessResourceUsageException) {
                meterService.incrementExceptionCaughtCounter("InvalidDataAccessResourceUsageException")
                logger.warn("InvalidDataAccessResourceUsageException: ${invalidDataAccessResourceUsageException.message}")
            }
            return true
        }

        fun updateValidationDatesForAllAccounts(): Boolean {
            val owner = TenantContext.getCurrentOwner()
            try {
                accountRepository.updateValidationDateForAllAccountsByOwner(owner)
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
            val owner = TenantContext.getCurrentOwner()
            logger.info("Renaming account from $oldAccountNameOwner to $newAccountNameOwner")

            val oldAccount =
                accountRepository
                    .findByOwnerAndAccountNameOwner(owner, oldAccountNameOwner)
                    .orElseThrow {
                        logger.error("Account not found: $oldAccountNameOwner")
                        EntityNotFoundException("Account not found: $oldAccountNameOwner")
                    }

            try {
                // First, update all transactions to use the new account name
                val transactionsUpdated =
                    transactionRepository.updateAccountNameOwnerForAllTransactionsByOwner(
                        owner,
                        oldAccountNameOwner,
                        newAccountNameOwner,
                    )
                logger.info("Updated $transactionsUpdated transactions from $oldAccountNameOwner to $newAccountNameOwner")

                // Then, rename the account itself
                oldAccount.accountNameOwner = newAccountNameOwner
                oldAccount.dateUpdated = nowTimestamp()
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
            val owner = TenantContext.getCurrentOwner()
            val account =
                accountRepository
                    .findByOwnerAndAccountNameOwner(owner, accountNameOwner)
                    .orElseThrow { EntityNotFoundException("Account not found: $accountNameOwner") }

            logger.info("Deactivating account: $accountNameOwner")

            // Deactivate all transactions for this account
            val transactionsUpdated = transactionRepository.deactivateAllTransactionsByOwnerAndAccountNameOwner(owner, accountNameOwner)
            logger.info("Deactivated $transactionsUpdated transactions for account: $accountNameOwner")

            // Deactivate the account
            account.activeStatus = false
            account.dateClosed = nowTimestamp()
            account.dateUpdated = nowTimestamp()
            val updatedAccount = accountRepository.saveAndFlush(account)
            logger.info("Successfully deactivated account: $accountNameOwner")
            return updatedAccount
        }

        fun activateAccount(accountNameOwner: String): Account {
            val owner = TenantContext.getCurrentOwner()
            val account =
                accountRepository
                    .findByOwnerAndAccountNameOwner(owner, accountNameOwner)
                    .orElseThrow { EntityNotFoundException("Account not found: $accountNameOwner") }

            logger.info("Activating account: $accountNameOwner")
            account.activeStatus = true
            account.dateUpdated = nowTimestamp()
            val updatedAccount = accountRepository.saveAndFlush(account)
            logger.info("Successfully activated account: $accountNameOwner")
            return updatedAccount
        }
    }
