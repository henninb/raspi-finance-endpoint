package finance.services

import finance.domain.Account
import finance.domain.AccountType
import finance.domain.Category
import finance.domain.Transaction
import finance.repositories.TransactionRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.Timestamp
import java.util.*
//import javax.transaction.Transactional
import javax.validation.ConstraintViolation
import javax.validation.Validator

@Service
open class TransactionService @Autowired constructor(private var transactionRepository: TransactionRepository,
                                                     private var accountService: AccountService,
                                                     private var categoryService: CategoryService,
                                                     private val validator: Validator) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

//    fun findAllTransactions(): List<Transaction> {
//        return transactionRepository.findAll()
//    }

    //TODO: fix the delete
    @Transactional
    open fun deleteByGuid(guid: String): Boolean {
        val transactionOptional: Optional<Transaction> = transactionRepository.findByGuid(guid)
        if (transactionOptional.isPresent) {
            val transaction = transactionOptional.get()
            println("transaction.categories = ${transaction.categories}")
//            for( category in transaction.categories) {
//                println("remove category")
//                transaction.categories.remove(category)
//            }


//            transaction.categories.forEach {
//                transaction.categories.remove(it)
//            }

            if (transaction.categories.size > 0) {
                val categoryOptional = categoryService.findByCategory(transaction.category)
                transaction.categories.remove(categoryOptional.get())
            }
//            try {
                transactionRepository.deleteByGuid(guid)
//            } catch (e: EmptyTransactionException) {
//                return false
//            }
            return true
        }
        return false
    }

    @Transactional
    open fun insertTransaction(transaction: Transaction): Boolean {

        val constraintViolations: Set<ConstraintViolation<Transaction>> = validator.validate(transaction)
        if (constraintViolations.isNotEmpty()) {
            logger.info("insertTransaction() ConstraintViolation")
        }
        logger.info("*** insert transaction ***")
        val transactionOptional = findByGuid(transaction.guid)

        if (transactionOptional.isPresent) {
            val transactionDb = transactionOptional.get()
            logger.info("*** update transaction ***")
            return updateTransaction(transactionDb, transaction)
        }

        processAccount(transaction)
        processCategory(transaction)
        logger.info("transaction = $transaction")
        transactionRepository.saveAndFlush(transaction)
        logger.info("*** inserted transaction ***")
        return true
    }

    open fun updateTransactionCleared(guid: String) : Boolean {
        val transactionOptional = findByGuid(guid)
        if (transactionOptional.isPresent) {
            val transaction = transactionOptional.get()
            transaction.cleared = 1
            transactionRepository.saveAndFlush(transaction)
            logger.info("*** update transaction $guid ***")
        } else {
            logger.info("*** updateTransactionCleared transaction not found $guid ***")
            return false
        }
        return true
    }

    private fun processAccount(transaction: Transaction) {
        var accountOptional = accountService.findByAccountNameOwner(transaction.accountNameOwner)
        if (accountOptional.isPresent) {
            logger.info("METRIC_ACCOUNT_ALREADY_EXISTS_COUNTER")
            transaction.accountId = accountOptional.get().accountId
            transaction.accountType = accountOptional.get().accountType
        } else {
            logger.info("METRIC_ACCOUNT_NOT_FOUND_COUNTER")
            val account = createDefaultAccount(transaction.accountNameOwner, transaction.accountType)
            logger.debug("will insertAccount")
            accountService.insertAccount(account)
            logger.debug("called insertAccount")
            accountOptional = accountService.findByAccountNameOwner(transaction.accountNameOwner)
            transaction.accountId = accountOptional.get().accountId
            transaction.accountType = accountOptional.get().accountType
            //meterRegistry.counter(METRIC_ACCOUNT_NOT_FOUND_COUNTER).increment()
        }
    }

    private fun processCategory(transaction: Transaction) {
        when {
            transaction.category != "" -> {
                val optionalCategory = categoryService.findByCategory(transaction.category)
                if (optionalCategory.isPresent) {
                    transaction.categories.add(optionalCategory.get())
                } else {
                    val category = createDefaultCategory(transaction.category)
                    categoryService.insertCategory(category)
                    transaction.categories.add(category)
                }
            }
        }
    }

    private fun updateTransaction(transactionDb: Transaction, transaction: Transaction): Boolean {
        if (transactionDb.accountNameOwner.trim() == transaction.accountNameOwner) {

            if (transactionDb.amount != transaction.amount) {
                logger.info("discrepancy in the amount for <${transactionDb.guid}>")
                //TODO: metric for this
                transactionRepository.setAmountByGuid(transaction.amount, transaction.guid)
                return true
            }

            if (transactionDb.cleared != transaction.cleared) {
                logger.info("discrepancy in the cleared value for <${transactionDb.guid}>")
                //TODO: metric for this
                transactionRepository.setClearedByGuid(transaction.cleared, transaction.guid)
                return true
            }
        }
        
        logger.info("transaction already exists, no transaction data inserted.")
        return false
    }

    private fun createDefaultCategory(categoryName: String): Category {
        val category = Category()

        category.category = categoryName
        return category
    }

    private fun createDefaultAccount(accountNameOwner: String, accountType: AccountType): Account {
        val account = Account()

        account.accountNameOwner = accountNameOwner
        account.moniker = "0000"
        account.accountType = accountType
        account.activeStatus = true
        account.dateAdded = Timestamp(System.currentTimeMillis())
        account.dateUpdated = Timestamp(System.currentTimeMillis())
        return account
    }

    @Transactional
    open fun findByGuid(guid: String): Optional<Transaction> {
        logger.info("call findByGuid")
        val transactionOptional: Optional<Transaction> = transactionRepository.findByGuid(guid)
        if (transactionOptional.isPresent) {
            return transactionOptional
        }
        return Optional.empty()
    }

    @Transactional
    open fun fetchTotalsByAccountNameOwner(accountNameOwner: String): Map<String, BigDecimal> {

        val result: MutableMap<String, BigDecimal> = HashMap()
        var totalsCleared= 0.00
        var totals = 0.00
        try {
            totalsCleared = transactionRepository.getTotalsByAccountNameOwnerCleared(accountNameOwner)
            totals = transactionRepository.getTotalsByAccountNameOwner(accountNameOwner)
        } catch( e: EmptyResultDataAccessException) {
            logger.warn("empty getTotalsByAccountNameOwnerCleared and getTotalsByAccountNameOwner.")
        }

        result["totals"] = BigDecimal(totals).setScale(2, RoundingMode.HALF_UP)
        result["totalsCleared"] = BigDecimal(totalsCleared).setScale(2, RoundingMode.HALF_UP)
        return result
    }

    @Transactional
    open fun findByAccountNameOwnerIgnoreCaseOrderByTransactionDate(accountNameOwner: String): List<Transaction> {
        val transactions: List<Transaction> = transactionRepository.findByAccountNameOwnerIgnoreCaseOrderByTransactionDateDesc(accountNameOwner)
        val sortedTransactions = transactions.sortedWith(compareBy({ it.cleared }, { it.transactionDate }))
        if (transactions.isEmpty()) {
            logger.error("an empty list of AccountNameOwner.")
            //return something
        }
        return sortedTransactions
    }

    @Transactional
    open fun patchTransaction(transaction: Transaction): Boolean {
        val constraintViolations: Set<ConstraintViolation<Transaction>> = validator.validate(transaction)
        if (constraintViolations.isNotEmpty()) {
            logger.info("patchTransaction() ConstraintViolation.")
        }
        val optionalTransaction = transactionRepository.findByGuid(transaction.guid)
        //TODO: add logic for patch
        if (optionalTransaction.isPresent) {
            val fromDb = optionalTransaction.get()
            if (fromDb.guid == transaction.guid) {
                logger.info("successful patch $transaction")
                transactionRepository.saveAndFlush(transaction)
            } else {
                logger.warn("GUID did not match any database records.")
                return false
            }
        } else {
            logger.warn("WARN: cannot patch a transaction without a valid GUID.")
            return false
        }
        return true
    }
}
