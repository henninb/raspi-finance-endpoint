package finance.controllers.graphql

import finance.controllers.dto.AccountInputDto
import finance.controllers.dto.CategoryInputDto
import finance.controllers.dto.DescriptionInputDto
import finance.controllers.dto.MedicalExpenseInputDto
import finance.controllers.dto.ParameterInputDto
import finance.controllers.dto.PaymentInputDto
import finance.controllers.dto.TransactionInputDto
import finance.controllers.dto.TransferInputDto
import finance.controllers.dto.ValidationAmountInputDto
import finance.domain.Account
import finance.domain.Category
import finance.domain.Description
import finance.domain.MedicalExpense
import finance.domain.Parameter
import finance.domain.Payment
import finance.domain.ReoccurringType
import finance.domain.ServiceResult
import finance.domain.Transaction
import finance.domain.Transfer
import finance.domain.ValidationAmount
import finance.domain.getOrThrow
import finance.services.AccountService
import finance.services.CategoryService
import finance.services.DescriptionService
import finance.services.MedicalExpenseService
import finance.services.ParameterService
import finance.services.PaymentService
import finance.services.TransactionService
import finance.services.TransferService
import finance.services.ValidationAmountService
import io.micrometer.core.instrument.MeterRegistry
import jakarta.validation.Valid
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import org.springframework.validation.annotation.Validated
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.UUID

@Controller
@Validated
@PreAuthorize("hasAuthority('USER')")
class GraphQLMutationController(
    private val accountService: AccountService,
    private val categoryService: CategoryService,
    private val descriptionService: DescriptionService,
    private val medicalExpenseService: MedicalExpenseService,
    private val parameterService: ParameterService,
    private val paymentService: PaymentService,
    private val transactionService: TransactionService,
    private val transferService: TransferService,
    private val validationAmountService: ValidationAmountService,
    private val meterRegistry: MeterRegistry,
) {
    companion object {
        private val logger: Logger = LogManager.getLogger()
    }

    @MutationMapping
    fun createPayment(
        @Argument("payment") @Valid payment: PaymentInputDto,
    ): Payment {
        logger.info("GraphQL - Creating payment via @MutationMapping")

        val domain =
            Payment().apply {
                this.sourceAccount = payment.sourceAccount
                this.destinationAccount = payment.destinationAccount
                this.transactionDate = payment.transactionDate
                this.amount = payment.amount
                this.activeStatus = payment.activeStatus ?: true
            }

        val data = paymentService.save(domain).getOrThrow()
        meterRegistry.counter("graphql.payment.create.success").increment()
        logger.info("GraphQL - Created payment id={}", data.paymentId)
        return data
    }

    @MutationMapping
    fun updatePayment(
        @Argument id: Long,
        @Argument("payment") @Valid payment: PaymentInputDto,
    ): Payment {
        logger.info("GraphQL - Updating payment id={}", id)

        // Find existing payment to preserve GUIDs
        val existingPayment = paymentService.findByPaymentId(id)
        if (existingPayment.isEmpty) {
            logger.warn("GraphQL - Payment not found: {}", id)
            throw IllegalArgumentException("Payment not found: $id")
        }

        val domain =
            Payment().apply {
                this.sourceAccount = payment.sourceAccount
                this.destinationAccount = payment.destinationAccount
                this.transactionDate = payment.transactionDate
                this.amount = payment.amount
                this.guidSource = existingPayment.get().guidSource
                this.guidDestination = existingPayment.get().guidDestination
                this.activeStatus = payment.activeStatus ?: true
            }

        val updated = paymentService.updatePayment(id, domain)
        meterRegistry.counter("graphql.payment.update.success").increment()
        logger.info("GraphQL - Updated payment id={}", updated.paymentId)
        return updated
    }

    @MutationMapping
    fun deletePayment(
        @Argument id: Long,
    ): Boolean {
        logger.info("GraphQL - Deleting payment id={}", id)
        return when (val result = paymentService.deleteById(id)) {
            is ServiceResult.Success -> true
            is ServiceResult.SystemError -> throw result.exception
            else -> false
        }
    }

    @MutationMapping
    fun createTransfer(
        @Argument("transfer") @Valid transfer: TransferInputDto,
    ): Transfer {
        logger.info("GraphQL - Creating transfer via @MutationMapping")
        val domain =
            Transfer().apply {
                this.sourceAccount = transfer.sourceAccount
                this.destinationAccount = transfer.destinationAccount
                this.transactionDate = transfer.transactionDate
                this.amount = transfer.amount
                this.guidSource = UUID.randomUUID().toString()
                this.guidDestination = UUID.randomUUID().toString()
                this.activeStatus = transfer.activeStatus ?: true
            }
        val saved = transferService.insertTransfer(domain)
        meterRegistry.counter("graphql.transfer.create.success").increment()
        logger.info("GraphQL - Created transfer id={}", saved.transferId)
        return saved
    }

    @MutationMapping
    fun updateTransfer(
        @Argument id: Long,
        @Argument("transfer") @Valid transfer: TransferInputDto,
    ): Transfer {
        logger.info("GraphQL - Updating transfer id={}", id)

        // Find existing transfer to preserve GUIDs
        val existingTransfer = transferService.findByTransferId(id)
        if (existingTransfer.isEmpty) {
            logger.warn("GraphQL - Transfer not found: {}", id)
            throw IllegalArgumentException("Transfer not found: $id")
        }

        val domain =
            Transfer().apply {
                this.transferId = id
                this.sourceAccount = transfer.sourceAccount
                this.destinationAccount = transfer.destinationAccount
                this.transactionDate = transfer.transactionDate
                this.amount = transfer.amount
                this.guidSource = existingTransfer.get().guidSource
                this.guidDestination = existingTransfer.get().guidDestination
                this.activeStatus = transfer.activeStatus ?: true
            }

        val updated = transferService.updateTransfer(domain)
        meterRegistry.counter("graphql.transfer.update.success").increment()
        logger.info("GraphQL - Updated transfer id={}", updated.transferId)
        return updated
    }

    @MutationMapping
    fun deleteTransfer(
        @Argument id: Long,
    ): Boolean {
        logger.info("GraphQL - Deleting transfer id={}", id)
        return transferService.deleteByTransferId(id)
    }

    @MutationMapping
    fun createParameter(
        @Argument("parameter") @Valid parameter: ParameterInputDto,
    ): Parameter {
        logger.info("GraphQL - Creating parameter via @MutationMapping")
        val domain =
            Parameter().apply {
                this.parameterName = parameter.parameterName
                this.parameterValue = parameter.parameterValue
                this.activeStatus = parameter.activeStatus ?: true
            }
        val data = parameterService.save(domain).getOrThrow()
        meterRegistry.counter("graphql.parameter.create.success").increment()
        logger.info("GraphQL - Created parameter id={}", data.parameterId)
        return data
    }

    @MutationMapping
    fun updateParameter(
        @Argument("parameter") @Valid parameter: ParameterInputDto,
    ): Parameter {
        logger.info("GraphQL - Updating parameter id={}", parameter.parameterId)
        val domain =
            Parameter().apply {
                this.parameterId = parameter.parameterId ?: 0L
                this.parameterName = parameter.parameterName
                this.parameterValue = parameter.parameterValue
                this.activeStatus = parameter.activeStatus ?: true
            }
        val data = parameterService.update(domain).getOrThrow()
        meterRegistry.counter("graphql.parameter.update.success").increment()
        logger.info("GraphQL - Updated parameter id={}", data.parameterId)
        return data
    }

    @MutationMapping
    fun deleteParameter(
        @Argument parameterId: Long,
    ): Boolean {
        logger.info("GraphQL - Deleting parameter id={}", parameterId)
        return handleDelete(
            parameterService.deleteById(parameterId),
            "graphql.parameter.delete.success",
            "GraphQL - Deleted parameter id={}",
            parameterId,
        )
    }

    @MutationMapping
    fun createCategory(
        @Argument("category") @Valid categoryInput: CategoryInputDto,
    ): Category {
        logger.info("GraphQL - Creating category via @MutationMapping")

        val category =
            Category().apply {
                this.categoryId = categoryInput.categoryId ?: 0L
                this.categoryName = categoryInput.categoryName
                this.activeStatus = categoryInput.activeStatus ?: true
            }

        val data = categoryService.save(category).getOrThrow()
        meterRegistry.counter("graphql.category.create.success").increment()
        logger.info("GraphQL - Created category: {}", data.categoryName)
        return data
    }

    @MutationMapping
    fun updateCategory(
        @Argument("category") @Valid categoryInput: CategoryInputDto,
        @Argument("oldCategoryName") oldCategoryName: String?,
    ): Category {
        logger.info(
            "GraphQL - Updating category with input: categoryId={}, categoryName={}, oldCategoryName={}",
            categoryInput.categoryId,
            categoryInput.categoryName,
            oldCategoryName,
        )

        // Determine which category to update based on what information is provided:
        // 1. If categoryId is provided, use it directly
        // 2. If oldCategoryName is provided, look up the category by that name
        // 3. If neither is provided, look up by the new categoryName (for simple updates without renaming)
        val existingCategoryId =
            when {
                categoryInput.categoryId != null -> {
                    logger.debug("Using provided categoryId: {}", categoryInput.categoryId)
                    categoryInput.categoryId
                }

                oldCategoryName != null -> {
                    logger.debug("Looking up category by oldCategoryName: {}", oldCategoryName)
                    when (val findResult = categoryService.findByCategoryNameStandardized(oldCategoryName)) {
                        is ServiceResult.Success -> {
                            findResult.data.categoryId
                        }

                        is ServiceResult.NotFound -> {
                            logger.warn("GraphQL - Old category not found: {}", oldCategoryName)
                            throw IllegalArgumentException("Category not found: $oldCategoryName")
                        }

                        else -> {
                            throw RuntimeException("Failed to find category: $oldCategoryName")
                        }
                    }
                }

                else -> {
                    logger.debug("Looking up category by categoryName: {}", categoryInput.categoryName)
                    when (val findResult = categoryService.findByCategoryNameStandardized(categoryInput.categoryName)) {
                        is ServiceResult.Success -> {
                            findResult.data.categoryId
                        }

                        is ServiceResult.NotFound -> {
                            logger.warn("GraphQL - Category not found: {}", categoryInput.categoryName)
                            throw IllegalArgumentException(
                                "Category not found: ${categoryInput.categoryName}. " +
                                    "If renaming, provide oldCategoryName parameter. " +
                                    "Example: updateCategory(category: { categoryName: \"newname\" }, oldCategoryName: \"oldname\")",
                            )
                        }

                        else -> {
                            throw RuntimeException("Failed to find category: ${categoryInput.categoryName}")
                        }
                    }
                }
            }

        val category =
            Category().apply {
                this.categoryId = existingCategoryId
                this.categoryName = categoryInput.categoryName
                this.activeStatus = categoryInput.activeStatus ?: true
            }

        val data = categoryService.update(category).getOrThrow()
        meterRegistry.counter("graphql.category.update.success").increment()
        logger.info("GraphQL - Updated category: {}", data.categoryName)
        return data
    }

    @MutationMapping
    fun deleteCategory(
        @Argument categoryName: String,
    ): Boolean {
        logger.info("GraphQL - Deleting category: {}", categoryName)
        return handleDelete(
            categoryService.deleteByCategoryNameStandardized(categoryName),
            "graphql.category.delete.success",
            "GraphQL - Deleted category: {}",
            categoryName,
        )
    }

    @MutationMapping
    fun createDescription(
        @Argument("description") @Valid descriptionInput: DescriptionInputDto,
    ): Description {
        logger.info("GraphQL - Creating description via @MutationMapping")

        val description =
            Description().apply {
                this.descriptionId = descriptionInput.descriptionId ?: 0L
                this.descriptionName = descriptionInput.descriptionName
                this.activeStatus = descriptionInput.activeStatus ?: true
            }

        val data = descriptionService.save(description).getOrThrow()
        meterRegistry.counter("graphql.description.create.success").increment()
        logger.info("GraphQL - Created description: {}", data.descriptionName)
        return data
    }

    @MutationMapping
    fun updateDescription(
        @Argument("description") @Valid descriptionInput: DescriptionInputDto,
        @Argument("oldDescriptionName") oldDescriptionName: String?,
    ): Description {
        logger.info(
            "GraphQL - Updating description with input: descriptionId={}, descriptionName={}, oldDescriptionName={}",
            descriptionInput.descriptionId,
            descriptionInput.descriptionName,
            oldDescriptionName,
        )

        // Determine which description to update based on what information is provided:
        // 1. If descriptionId is provided, use it directly
        // 2. If oldDescriptionName is provided, look up the description by that name
        // 3. If neither is provided, look up by the new descriptionName (for simple updates without renaming)
        val existingDescriptionId =
            when {
                descriptionInput.descriptionId != null -> {
                    logger.debug("Using provided descriptionId: {}", descriptionInput.descriptionId)
                    descriptionInput.descriptionId
                }

                oldDescriptionName != null -> {
                    logger.debug("Looking up description by oldDescriptionName: {}", oldDescriptionName)
                    when (val findResult = descriptionService.findByDescriptionNameStandardized(oldDescriptionName)) {
                        is ServiceResult.Success -> {
                            findResult.data.descriptionId
                        }

                        is ServiceResult.NotFound -> {
                            logger.warn("GraphQL - Old description not found: {}", oldDescriptionName)
                            throw IllegalArgumentException("Description not found: $oldDescriptionName")
                        }

                        else -> {
                            throw RuntimeException("Failed to find description: $oldDescriptionName")
                        }
                    }
                }

                else -> {
                    logger.debug("Looking up description by descriptionName: {}", descriptionInput.descriptionName)
                    when (val findResult = descriptionService.findByDescriptionNameStandardized(descriptionInput.descriptionName)) {
                        is ServiceResult.Success -> {
                            findResult.data.descriptionId
                        }

                        is ServiceResult.NotFound -> {
                            logger.warn("GraphQL - Description not found: {}", descriptionInput.descriptionName)
                            throw IllegalArgumentException(
                                "Description not found: ${descriptionInput.descriptionName}. " +
                                    "If renaming, provide oldDescriptionName parameter. " +
                                    "Example: updateDescription(description: { descriptionName: \"newname\" }, oldDescriptionName: \"oldname\")",
                            )
                        }

                        else -> {
                            throw RuntimeException("Failed to find description: ${descriptionInput.descriptionName}")
                        }
                    }
                }
            }

        val description =
            Description().apply {
                this.descriptionId = existingDescriptionId
                this.descriptionName = descriptionInput.descriptionName
                this.activeStatus = descriptionInput.activeStatus ?: true
            }

        val data = descriptionService.update(description).getOrThrow()
        meterRegistry.counter("graphql.description.update.success").increment()
        logger.info("GraphQL - Updated description: {}", data.descriptionName)
        return data
    }

    @MutationMapping
    fun deleteDescription(
        @Argument descriptionName: String,
    ): Boolean {
        logger.info("GraphQL - Deleting description: {}", descriptionName)
        return handleDelete(
            descriptionService.deleteByDescriptionNameStandardized(descriptionName),
            "graphql.description.delete.success",
            "GraphQL - Deleted description: {}",
            descriptionName,
        )
    }

    @MutationMapping
    fun createMedicalExpense(
        @Argument("medicalExpense") @Valid medicalExpenseInput: MedicalExpenseInputDto,
    ): MedicalExpense {
        logger.info("GraphQL - Creating medical expense via @MutationMapping")
        requireMedicalExpenseFields(medicalExpenseInput)

        val medicalExpense =
            MedicalExpense().apply {
                this.medicalExpenseId = medicalExpenseInput.medicalExpenseId ?: 0L
                this.transactionId = medicalExpenseInput.transactionId
                this.providerId = medicalExpenseInput.providerId
                this.familyMemberId = medicalExpenseInput.familyMemberId
                this.serviceDate = medicalExpenseInput.serviceDate!!
                this.serviceDescription = medicalExpenseInput.serviceDescription
                this.procedureCode = medicalExpenseInput.procedureCode
                this.diagnosisCode = medicalExpenseInput.diagnosisCode
                this.billedAmount = medicalExpenseInput.billedAmount!!
                this.insuranceDiscount = medicalExpenseInput.insuranceDiscount!!
                this.insurancePaid = medicalExpenseInput.insurancePaid!!
                this.patientResponsibility = medicalExpenseInput.patientResponsibility!!
                this.paidDate = medicalExpenseInput.paidDate
                this.isOutOfNetwork = medicalExpenseInput.isOutOfNetwork!!
                this.claimNumber = medicalExpenseInput.claimNumber!!
                this.claimStatus = medicalExpenseInput.claimStatus!!
                this.activeStatus = medicalExpenseInput.activeStatus ?: true
                this.paidAmount = medicalExpenseInput.paidAmount!!
            }

        val data = medicalExpenseService.save(medicalExpense).getOrThrow()
        meterRegistry.counter("graphql.medicalExpense.create.success").increment()
        logger.info("GraphQL - Created medical expense: {}", data.medicalExpenseId)
        return data
    }

    @MutationMapping
    fun updateMedicalExpense(
        @Argument("medicalExpense") @Valid medicalExpenseInput: MedicalExpenseInputDto,
    ): MedicalExpense {
        logger.info("GraphQL - Updating medical expense id={}", medicalExpenseInput.medicalExpenseId)
        require(medicalExpenseInput.medicalExpenseId != null) { "medicalExpenseId is required for update" }
        requireMedicalExpenseFields(medicalExpenseInput)

        val medicalExpense =
            MedicalExpense().apply {
                this.medicalExpenseId = medicalExpenseInput.medicalExpenseId
                this.transactionId = medicalExpenseInput.transactionId
                this.providerId = medicalExpenseInput.providerId
                this.familyMemberId = medicalExpenseInput.familyMemberId
                this.serviceDate = medicalExpenseInput.serviceDate!!
                this.serviceDescription = medicalExpenseInput.serviceDescription
                this.procedureCode = medicalExpenseInput.procedureCode
                this.diagnosisCode = medicalExpenseInput.diagnosisCode
                this.billedAmount = medicalExpenseInput.billedAmount!!
                this.insuranceDiscount = medicalExpenseInput.insuranceDiscount!!
                this.insurancePaid = medicalExpenseInput.insurancePaid!!
                this.patientResponsibility = medicalExpenseInput.patientResponsibility!!
                this.paidDate = medicalExpenseInput.paidDate
                this.isOutOfNetwork = medicalExpenseInput.isOutOfNetwork!!
                this.claimNumber = medicalExpenseInput.claimNumber!!
                this.claimStatus = medicalExpenseInput.claimStatus!!
                this.activeStatus = medicalExpenseInput.activeStatus ?: true
                this.paidAmount = medicalExpenseInput.paidAmount!!
            }

        val data = medicalExpenseService.update(medicalExpense).getOrThrow()
        meterRegistry.counter("graphql.medicalExpense.update.success").increment()
        logger.info("GraphQL - Updated medical expense: {}", data.medicalExpenseId)
        return data
    }

    @MutationMapping
    fun deleteMedicalExpense(
        @Argument medicalExpenseId: Long,
    ): Boolean {
        logger.info("GraphQL - Deleting medical expense id={}", medicalExpenseId)
        return handleDelete(
            medicalExpenseService.deleteById(medicalExpenseId),
            "graphql.medicalExpense.delete.success",
            "GraphQL - Deleted medical expense id={}",
            medicalExpenseId,
        )
    }

    @MutationMapping
    fun createValidationAmount(
        @Argument("validationAmount") @Valid validationAmountInput: ValidationAmountInputDto,
    ): ValidationAmount {
        logger.info("GraphQL - Creating validation amount via @MutationMapping")

        val validationAmount =
            ValidationAmount().apply {
                this.validationId = validationAmountInput.validationId ?: 0L
                this.accountId = validationAmountInput.accountId
                this.validationDate = validationAmountInput.validationDate
                this.activeStatus = validationAmountInput.activeStatus ?: true
                this.transactionState = validationAmountInput.transactionState
                this.amount = validationAmountInput.amount
            }

        val data = validationAmountService.save(validationAmount).getOrThrow()
        meterRegistry.counter("graphql.validationAmount.create.success").increment()
        logger.info("GraphQL - Created validation amount: {}", data.validationId)
        return data
    }

    @MutationMapping
    fun updateValidationAmount(
        @Argument("validationAmount") @Valid validationAmountInput: ValidationAmountInputDto,
    ): ValidationAmount {
        logger.info("GraphQL - Updating validation amount id={}", validationAmountInput.validationId)

        val validationAmount =
            ValidationAmount().apply {
                this.validationId = validationAmountInput.validationId!!
                this.accountId = validationAmountInput.accountId
                this.validationDate = validationAmountInput.validationDate
                this.activeStatus = validationAmountInput.activeStatus ?: true
                this.transactionState = validationAmountInput.transactionState
                this.amount = validationAmountInput.amount
            }

        val data = validationAmountService.update(validationAmount).getOrThrow()
        meterRegistry.counter("graphql.validationAmount.update.success").increment()
        logger.info("GraphQL - Updated validation amount: {}", data.validationId)
        return data
    }

    @MutationMapping
    fun deleteValidationAmount(
        @Argument validationId: Long,
    ): Boolean {
        logger.info("GraphQL - Deleting validation amount id={}", validationId)
        return handleDelete(
            validationAmountService.deleteById(validationId),
            "graphql.validationAmount.delete.success",
            "GraphQL - Deleted validation amount id={}",
            validationId,
        )
    }

    @MutationMapping
    fun createAccount(
        @Argument("account") @Valid accountInput: AccountInputDto,
    ): Account {
        logger.info("GraphQL - Creating account via @MutationMapping")

        val account =
            Account().apply {
                this.accountId = accountInput.accountId ?: 0L
                this.accountNameOwner = accountInput.accountNameOwner
                this.accountType = accountInput.accountType
                this.activeStatus = accountInput.activeStatus ?: true
                this.moniker = accountInput.moniker ?: "0000"
                this.outstanding = accountInput.outstanding ?: BigDecimal.ZERO
                this.cleared = accountInput.cleared ?: BigDecimal.ZERO
                this.future = accountInput.future ?: BigDecimal.ZERO
                this.dateClosed = accountInput.dateClosed ?: Timestamp(0)
                this.validationDate = accountInput.validationDate ?: Timestamp(System.currentTimeMillis())
                this.taxBucket = accountInput.taxBucket
            }

        val data = accountService.save(account).getOrThrow()
        meterRegistry.counter("graphql.account.create.success").increment()
        logger.info("GraphQL - Created account: {}", data.accountNameOwner)
        return data
    }

    @MutationMapping
    fun updateAccount(
        @Argument("account") @Valid accountInput: AccountInputDto,
        @Argument("oldAccountNameOwner") oldAccountNameOwner: String?,
    ): Account {
        logger.info(
            "GraphQL - Updating account with input: accountId={}, accountNameOwner={}, oldAccountNameOwner={}",
            accountInput.accountId,
            accountInput.accountNameOwner,
            oldAccountNameOwner,
        )

        // Determine which account to update based on what information is provided:
        // 1. If accountId is provided, use it directly
        // 2. If oldAccountNameOwner is provided, look up the account by that name
        // 3. If neither is provided, look up by the new accountNameOwner (for simple updates without renaming)
        val existingAccountId =
            when {
                accountInput.accountId != null -> {
                    logger.debug("Using provided accountId: {}", accountInput.accountId)
                    accountInput.accountId
                }

                oldAccountNameOwner != null -> {
                    logger.debug("Looking up account by oldAccountNameOwner: {}", oldAccountNameOwner)
                    when (val findResult = accountService.findById(oldAccountNameOwner)) {
                        is ServiceResult.Success -> {
                            findResult.data.accountId
                        }

                        is ServiceResult.NotFound -> {
                            logger.warn("GraphQL - Old account not found: {}", oldAccountNameOwner)
                            throw IllegalArgumentException("Account not found: $oldAccountNameOwner")
                        }

                        else -> {
                            throw RuntimeException("Failed to find account: $oldAccountNameOwner")
                        }
                    }
                }

                else -> {
                    logger.debug("Looking up account by accountNameOwner: {}", accountInput.accountNameOwner)
                    when (val findResult = accountService.findById(accountInput.accountNameOwner)) {
                        is ServiceResult.Success -> {
                            findResult.data.accountId
                        }

                        is ServiceResult.NotFound -> {
                            logger.warn("GraphQL - Account not found: {}", accountInput.accountNameOwner)
                            throw IllegalArgumentException(
                                "Account not found: ${accountInput.accountNameOwner}. " +
                                    "If renaming, provide oldAccountNameOwner parameter. " +
                                    "Example: updateAccount(account: { accountNameOwner: \"newname\" }, oldAccountNameOwner: \"oldname\")",
                            )
                        }

                        else -> {
                            throw RuntimeException("Failed to find account: ${accountInput.accountNameOwner}")
                        }
                    }
                }
            }

        val account =
            Account().apply {
                this.accountId = existingAccountId
                this.accountNameOwner = accountInput.accountNameOwner
                this.accountType = accountInput.accountType
                this.activeStatus = accountInput.activeStatus ?: true
                this.moniker = accountInput.moniker ?: "0000"
                this.outstanding = accountInput.outstanding ?: BigDecimal.ZERO
                this.cleared = accountInput.cleared ?: BigDecimal.ZERO
                this.future = accountInput.future ?: BigDecimal.ZERO
                this.dateClosed = accountInput.dateClosed ?: Timestamp(0)
                this.validationDate = accountInput.validationDate ?: Timestamp(System.currentTimeMillis())
                this.taxBucket = accountInput.taxBucket
            }

        val data = accountService.update(account).getOrThrow()
        meterRegistry.counter("graphql.account.update.success").increment()
        logger.info("GraphQL - Updated account: {}", data.accountNameOwner)
        return data
    }

    @MutationMapping
    fun deleteAccount(
        @Argument accountNameOwner: String,
    ): Boolean {
        logger.info("GraphQL - Deleting account: {}", accountNameOwner)
        return handleDelete(
            accountService.deleteById(accountNameOwner),
            "graphql.account.delete.success",
            "GraphQL - Deleted account: {}",
            accountNameOwner,
        )
    }

    @MutationMapping
    fun createTransaction(
        @Argument("transaction") @Valid transactionInput: TransactionInputDto,
    ): Transaction {
        logger.info("GraphQL - Creating transaction via @MutationMapping")

        val transaction =
            Transaction().apply {
                this.transactionId = transactionInput.transactionId ?: 0L
                this.guid = transactionInput.guid ?: UUID.randomUUID().toString()
                this.accountId = transactionInput.accountId ?: 0L
                this.accountType = transactionInput.accountType
                this.transactionType = transactionInput.transactionType
                this.accountNameOwner = transactionInput.accountNameOwner
                this.transactionDate = transactionInput.transactionDate
                this.description = transactionInput.description
                this.category = transactionInput.category
                this.amount = transactionInput.amount
                this.transactionState = transactionInput.transactionState
                this.activeStatus = transactionInput.activeStatus ?: true
                this.reoccurringType = transactionInput.reoccurringType ?: ReoccurringType.Undefined
                this.notes = transactionInput.notes ?: ""
                this.dueDate = transactionInput.dueDate
                this.receiptImageId = transactionInput.receiptImageId
                this.dateAdded = Timestamp(System.currentTimeMillis())
                this.dateUpdated = Timestamp(System.currentTimeMillis())
            }

        val data = transactionService.save(transaction).getOrThrow()
        meterRegistry.counter("graphql.transaction.create.success").increment()
        logger.info("GraphQL - Created transaction: {}", data.guid)
        return data
    }

    @MutationMapping
    fun updateTransaction(
        @Argument("transaction") @Valid transactionInput: TransactionInputDto,
    ): Transaction {
        logger.info("GraphQL - Updating transaction guid={}", transactionInput.guid)

        val transaction =
            Transaction().apply {
                this.transactionId = transactionInput.transactionId ?: 0L
                this.guid = transactionInput.guid ?: throw IllegalArgumentException("GUID is required for update")
                this.accountId = transactionInput.accountId ?: 0L
                this.accountType = transactionInput.accountType
                this.transactionType = transactionInput.transactionType
                this.accountNameOwner = transactionInput.accountNameOwner
                this.transactionDate = transactionInput.transactionDate
                this.description = transactionInput.description
                this.category = transactionInput.category
                this.amount = transactionInput.amount
                this.transactionState = transactionInput.transactionState
                this.activeStatus = transactionInput.activeStatus ?: true
                this.reoccurringType = transactionInput.reoccurringType ?: ReoccurringType.Undefined
                this.notes = transactionInput.notes ?: ""
                this.dueDate = transactionInput.dueDate
                this.receiptImageId = transactionInput.receiptImageId
            }

        val data = transactionService.update(transaction).getOrThrow()
        meterRegistry.counter("graphql.transaction.update.success").increment()
        logger.info("GraphQL - Updated transaction: {}", data.guid)
        return data
    }

    @MutationMapping
    fun deleteTransaction(
        @Argument guid: String,
    ): Boolean {
        logger.info("GraphQL - Deleting transaction guid={}", guid)
        return handleDelete(
            transactionService.deleteById(guid),
            "graphql.transaction.delete.success",
            "GraphQL - Deleted transaction: {}",
            guid,
        )
    }

    private fun requireMedicalExpenseFields(input: MedicalExpenseInputDto) {
        val checks =
            listOf(
                input.serviceDate to "serviceDate",
                input.billedAmount to "billedAmount",
                input.insuranceDiscount to "insuranceDiscount",
                input.insurancePaid to "insurancePaid",
                input.patientResponsibility to "patientResponsibility",
                input.isOutOfNetwork to "isOutOfNetwork",
                input.claimNumber to "claimNumber",
                input.claimStatus to "claimStatus",
                input.paidAmount to "paidAmount",
            )
        checks.forEach { (value, name) ->
            if (value == null) throw IllegalArgumentException("$name is required")
        }
    }

    private fun handleDelete(
        result: ServiceResult<*>,
        metricName: String,
        logMessage: String,
        vararg logArgs: Any,
    ): Boolean {
        val deleted = result is ServiceResult.Success
        if (deleted) {
            meterRegistry.counter(metricName).increment()
            logger.info(logMessage, *logArgs)
        }
        return deleted
    }
}
