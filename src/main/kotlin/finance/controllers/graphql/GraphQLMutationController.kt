package finance.controllers.graphql

import finance.controllers.dto.AccountInputDto
import finance.controllers.dto.CategoryInputDto
import finance.controllers.dto.DescriptionInputDto
import finance.controllers.dto.MedicalExpenseInputDto
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
        val logger: Logger = LogManager.getLogger()
    }

    @PreAuthorize("hasAuthority('USER')")
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
                this.guidSource = UUID.randomUUID().toString()
                this.guidDestination = UUID.randomUUID().toString()
                this.activeStatus = payment.activeStatus ?: true
            }

        val saved = paymentService.insertPayment(domain)
        meterRegistry.counter("graphql.payment.create.success").increment()
        logger.info("GraphQL - Created payment id={}", saved.paymentId)
        return saved
    }

    @PreAuthorize("hasAuthority('USER')")
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

    @PreAuthorize("hasAuthority('USER')")
    @MutationMapping
    fun deletePayment(
        @Argument id: Long,
    ): Boolean {
        logger.info("GraphQL - Deleting payment id={}", id)
        return paymentService.deleteByPaymentId(id)
    }

    @PreAuthorize("hasAuthority('USER')")
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

    @PreAuthorize("hasAuthority('USER')")
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

    @PreAuthorize("hasAuthority('USER')")
    @MutationMapping
    fun deleteTransfer(
        @Argument id: Long,
    ): Boolean {
        logger.info("GraphQL - Deleting transfer id={}", id)
        return transferService.deleteByTransferId(id)
    }

    @PreAuthorize("hasAuthority('USER')")
    @MutationMapping
    fun createParameter(
        @Argument("parameter") @Valid parameter: Parameter,
    ): Parameter {
        logger.info("GraphQL - Creating parameter via @MutationMapping")
        return when (val result = parameterService.save(parameter)) {
            is ServiceResult.Success -> {
                meterRegistry.counter("graphql.parameter.create.success").increment()
                logger.info("GraphQL - Created parameter id={}", result.data.parameterId)
                result.data
            }

            is ServiceResult.ValidationError -> {
                logger.warn("GraphQL - Validation error creating parameter: {}", result.errors)
                throw IllegalArgumentException("Validation failed: ${result.errors}")
            }

            is ServiceResult.BusinessError -> {
                logger.warn("GraphQL - Business error creating parameter: {}", result.message)
                throw IllegalStateException(result.message)
            }

            else -> {
                logger.error("GraphQL - Unexpected error creating parameter")
                throw RuntimeException("Failed to create parameter")
            }
        }
    }

    @PreAuthorize("hasAuthority('USER')")
    @MutationMapping
    fun updateParameter(
        @Argument("parameter") @Valid parameter: Parameter,
    ): Parameter {
        logger.info("GraphQL - Updating parameter id={}", parameter.parameterId)
        return when (val result = parameterService.update(parameter)) {
            is ServiceResult.Success -> {
                meterRegistry.counter("graphql.parameter.update.success").increment()
                logger.info("GraphQL - Updated parameter id={}", result.data.parameterId)
                result.data
            }

            is ServiceResult.NotFound -> {
                logger.warn("GraphQL - Parameter not found: {}", parameter.parameterId)
                throw IllegalArgumentException("Parameter not found: ${parameter.parameterId}")
            }

            is ServiceResult.ValidationError -> {
                logger.warn("GraphQL - Validation error updating parameter: {}", result.errors)
                throw IllegalArgumentException("Validation failed: ${result.errors}")
            }

            is ServiceResult.BusinessError -> {
                logger.warn("GraphQL - Business error updating parameter: {}", result.message)
                throw IllegalStateException(result.message)
            }

            else -> {
                logger.error("GraphQL - Unexpected error updating parameter")
                throw RuntimeException("Failed to update parameter")
            }
        }
    }

    @PreAuthorize("hasAuthority('USER')")
    @MutationMapping
    fun deleteParameter(
        @Argument parameterId: Long,
    ): Boolean {
        logger.info("GraphQL - Deleting parameter id={}", parameterId)
        return when (val result = parameterService.deleteById(parameterId)) {
            is ServiceResult.Success -> {
                meterRegistry.counter("graphql.parameter.delete.success").increment()
                logger.info("GraphQL - Deleted parameter id={}", parameterId)
                result.data
            }

            is ServiceResult.NotFound -> {
                logger.warn("GraphQL - Parameter not found for deletion: {}", parameterId)
                false
            }

            else -> {
                logger.error("GraphQL - Error deleting parameter id={}", parameterId)
                false
            }
        }
    }

    @PreAuthorize("hasAuthority('USER')")
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

        return when (val result = categoryService.save(category)) {
            is ServiceResult.Success -> {
                meterRegistry.counter("graphql.category.create.success").increment()
                logger.info("GraphQL - Created category: {}", result.data.categoryName)
                result.data
            }

            is ServiceResult.ValidationError -> {
                logger.warn("GraphQL - Validation error creating category: {}", result.errors)
                throw IllegalArgumentException("Validation failed: ${result.errors}")
            }

            is ServiceResult.BusinessError -> {
                logger.warn("GraphQL - Business error creating category: {}", result.message)
                throw IllegalStateException(result.message)
            }

            else -> {
                logger.error("GraphQL - Unexpected error creating category")
                throw RuntimeException("Failed to create category")
            }
        }
    }

    @PreAuthorize("hasAuthority('USER')")
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
                            logger.error("GraphQL - Error finding category: {}", oldCategoryName)
                            throw RuntimeException("Failed to find category: $oldCategoryName")
                        }
                    }
                }

                else -> {
                    // Try to look up by the new name (for updates that don't involve renaming)
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
                            logger.error("GraphQL - Error finding category: {}", categoryInput.categoryName)
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

        return when (val result = categoryService.update(category)) {
            is ServiceResult.Success -> {
                meterRegistry.counter("graphql.category.update.success").increment()
                logger.info("GraphQL - Updated category: {}", result.data.categoryName)
                result.data
            }

            is ServiceResult.NotFound -> {
                logger.warn("GraphQL - Category not found with ID: {}", existingCategoryId)
                throw IllegalArgumentException("Category not found with ID: $existingCategoryId")
            }

            is ServiceResult.ValidationError -> {
                logger.warn("GraphQL - Validation error updating category: {}", result.errors)
                throw IllegalArgumentException("Validation failed: ${result.errors}")
            }

            is ServiceResult.BusinessError -> {
                logger.warn("GraphQL - Business error updating category: {}", result.message)
                throw IllegalStateException(result.message)
            }

            else -> {
                logger.error("GraphQL - Unexpected error updating category")
                throw RuntimeException("Failed to update category")
            }
        }
    }

    @PreAuthorize("hasAuthority('USER')")
    @MutationMapping
    fun deleteCategory(
        @Argument categoryName: String,
    ): Boolean {
        logger.info("GraphQL - Deleting category: {}", categoryName)
        return when (val result = categoryService.deleteByCategoryNameStandardized(categoryName)) {
            is ServiceResult.Success -> {
                meterRegistry.counter("graphql.category.delete.success").increment()
                logger.info("GraphQL - Deleted category: {}", categoryName)
                result.data
            }

            is ServiceResult.NotFound -> {
                logger.warn("GraphQL - Category not found for deletion: {}", categoryName)
                false
            }

            else -> {
                logger.error("GraphQL - Error deleting category: {}", categoryName)
                false
            }
        }
    }

    @PreAuthorize("hasAuthority('USER')")
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

        return when (val result = descriptionService.save(description)) {
            is ServiceResult.Success -> {
                meterRegistry.counter("graphql.description.create.success").increment()
                logger.info("GraphQL - Created description: {}", result.data.descriptionName)
                result.data
            }

            is ServiceResult.ValidationError -> {
                logger.warn("GraphQL - Validation error creating description: {}", result.errors)
                throw IllegalArgumentException("Validation failed: ${result.errors}")
            }

            is ServiceResult.BusinessError -> {
                logger.warn("GraphQL - Business error creating description: {}", result.message)
                throw IllegalStateException(result.message)
            }

            else -> {
                logger.error("GraphQL - Unexpected error creating description")
                throw RuntimeException("Failed to create description")
            }
        }
    }

    @PreAuthorize("hasAuthority('USER')")
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
                            logger.error("GraphQL - Error finding description: {}", oldDescriptionName)
                            throw RuntimeException("Failed to find description: $oldDescriptionName")
                        }
                    }
                }

                else -> {
                    // Try to look up by the new name (for updates that don't involve renaming)
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
                            logger.error("GraphQL - Error finding description: {}", descriptionInput.descriptionName)
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

        return when (val result = descriptionService.update(description)) {
            is ServiceResult.Success -> {
                meterRegistry.counter("graphql.description.update.success").increment()
                logger.info("GraphQL - Updated description: {}", result.data.descriptionName)
                result.data
            }

            is ServiceResult.NotFound -> {
                logger.warn("GraphQL - Description not found with ID: {}", existingDescriptionId)
                throw IllegalArgumentException("Description not found with ID: $existingDescriptionId")
            }

            is ServiceResult.ValidationError -> {
                logger.warn("GraphQL - Validation error updating description: {}", result.errors)
                throw IllegalArgumentException("Validation failed: ${result.errors}")
            }

            is ServiceResult.BusinessError -> {
                logger.warn("GraphQL - Business error updating description: {}", result.message)
                throw IllegalStateException(result.message)
            }

            else -> {
                logger.error("GraphQL - Unexpected error updating description")
                throw RuntimeException("Failed to update description")
            }
        }
    }

    @PreAuthorize("hasAuthority('USER')")
    @MutationMapping
    fun deleteDescription(
        @Argument descriptionName: String,
    ): Boolean {
        logger.info("GraphQL - Deleting description: {}", descriptionName)
        return when (val result = descriptionService.deleteByDescriptionNameStandardized(descriptionName)) {
            is ServiceResult.Success -> {
                meterRegistry.counter("graphql.description.delete.success").increment()
                logger.info("GraphQL - Deleted description: {}", descriptionName)
                result.data
            }

            is ServiceResult.NotFound -> {
                logger.warn("GraphQL - Description not found for deletion: {}", descriptionName)
                false
            }

            else -> {
                logger.error("GraphQL - Error deleting description: {}", descriptionName)
                false
            }
        }
    }

    @PreAuthorize("hasAuthority('USER')")
    @MutationMapping
    fun createMedicalExpense(
        @Argument("medicalExpense") @Valid medicalExpenseInput: MedicalExpenseInputDto,
    ): MedicalExpense {
        logger.info("GraphQL - Creating medical expense via @MutationMapping")

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

        return when (val result = medicalExpenseService.save(medicalExpense)) {
            is ServiceResult.Success -> {
                meterRegistry.counter("graphql.medicalExpense.create.success").increment()
                logger.info("GraphQL - Created medical expense: {}", result.data.medicalExpenseId)
                result.data
            }

            is ServiceResult.ValidationError -> {
                logger.warn("GraphQL - Validation error creating medical expense: {}", result.errors)
                throw IllegalArgumentException("Validation failed: ${result.errors}")
            }

            is ServiceResult.BusinessError -> {
                logger.warn("GraphQL - Business error creating medical expense: {}", result.message)
                throw IllegalStateException(result.message)
            }

            else -> {
                logger.error("GraphQL - Unexpected error creating medical expense")
                throw RuntimeException("Failed to create medical expense")
            }
        }
    }

    @PreAuthorize("hasAuthority('USER')")
    @MutationMapping
    fun updateMedicalExpense(
        @Argument("medicalExpense") @Valid medicalExpenseInput: MedicalExpenseInputDto,
    ): MedicalExpense {
        logger.info("GraphQL - Updating medical expense id={}", medicalExpenseInput.medicalExpenseId)

        val medicalExpense =
            MedicalExpense().apply {
                this.medicalExpenseId = medicalExpenseInput.medicalExpenseId!!
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

        return when (val result = medicalExpenseService.update(medicalExpense)) {
            is ServiceResult.Success -> {
                meterRegistry.counter("graphql.medicalExpense.update.success").increment()
                logger.info("GraphQL - Updated medical expense: {}", result.data.medicalExpenseId)
                result.data
            }

            is ServiceResult.NotFound -> {
                logger.warn("GraphQL - Medical expense not found: {}", medicalExpenseInput.medicalExpenseId)
                throw IllegalArgumentException("Medical expense not found: ${medicalExpenseInput.medicalExpenseId}")
            }

            is ServiceResult.ValidationError -> {
                logger.warn("GraphQL - Validation error updating medical expense: {}", result.errors)
                throw IllegalArgumentException("Validation failed: ${result.errors}")
            }

            is ServiceResult.BusinessError -> {
                logger.warn("GraphQL - Business error updating medical expense: {}", result.message)
                throw IllegalStateException(result.message)
            }

            else -> {
                logger.error("GraphQL - Unexpected error updating medical expense")
                throw RuntimeException("Failed to update medical expense")
            }
        }
    }

    @PreAuthorize("hasAuthority('USER')")
    @MutationMapping
    fun deleteMedicalExpense(
        @Argument medicalExpenseId: Long,
    ): Boolean {
        logger.info("GraphQL - Deleting medical expense id={}", medicalExpenseId)
        return when (val result = medicalExpenseService.deleteById(medicalExpenseId)) {
            is ServiceResult.Success -> {
                meterRegistry.counter("graphql.medicalExpense.delete.success").increment()
                logger.info("GraphQL - Deleted medical expense id={}", medicalExpenseId)
                result.data
            }

            is ServiceResult.NotFound -> {
                logger.warn("GraphQL - Medical expense not found for deletion: {}", medicalExpenseId)
                false
            }

            else -> {
                logger.error("GraphQL - Error deleting medical expense id={}", medicalExpenseId)
                false
            }
        }
    }

    @PreAuthorize("hasAuthority('USER')")
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

        return when (val result = validationAmountService.save(validationAmount)) {
            is ServiceResult.Success -> {
                meterRegistry.counter("graphql.validationAmount.create.success").increment()
                logger.info("GraphQL - Created validation amount: {}", result.data.validationId)
                result.data
            }

            is ServiceResult.ValidationError -> {
                logger.warn("GraphQL - Validation error creating validation amount: {}", result.errors)
                throw IllegalArgumentException("Validation failed: ${result.errors}")
            }

            is ServiceResult.BusinessError -> {
                logger.warn("GraphQL - Business error creating validation amount: {}", result.message)
                throw IllegalStateException(result.message)
            }

            else -> {
                logger.error("GraphQL - Unexpected error creating validation amount")
                throw RuntimeException("Failed to create validation amount")
            }
        }
    }

    @PreAuthorize("hasAuthority('USER')")
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

        return when (val result = validationAmountService.update(validationAmount)) {
            is ServiceResult.Success -> {
                meterRegistry.counter("graphql.validationAmount.update.success").increment()
                logger.info("GraphQL - Updated validation amount: {}", result.data.validationId)
                result.data
            }

            is ServiceResult.NotFound -> {
                logger.warn("GraphQL - Validation amount not found: {}", validationAmountInput.validationId)
                throw IllegalArgumentException("Validation amount not found: ${validationAmountInput.validationId}")
            }

            is ServiceResult.ValidationError -> {
                logger.warn("GraphQL - Validation error updating validation amount: {}", result.errors)
                throw IllegalArgumentException("Validation failed: ${result.errors}")
            }

            is ServiceResult.BusinessError -> {
                logger.warn("GraphQL - Business error updating validation amount: {}", result.message)
                throw IllegalStateException(result.message)
            }

            else -> {
                logger.error("GraphQL - Unexpected error updating validation amount")
                throw RuntimeException("Failed to update validation amount")
            }
        }
    }

    @PreAuthorize("hasAuthority('USER')")
    @MutationMapping
    fun deleteValidationAmount(
        @Argument validationId: Long,
    ): Boolean {
        logger.info("GraphQL - Deleting validation amount id={}", validationId)
        return when (val result = validationAmountService.deleteById(validationId)) {
            is ServiceResult.Success -> {
                meterRegistry.counter("graphql.validationAmount.delete.success").increment()
                logger.info("GraphQL - Deleted validation amount id={}", validationId)
                result.data
            }

            is ServiceResult.NotFound -> {
                logger.warn("GraphQL - Validation amount not found for deletion: {}", validationId)
                false
            }

            else -> {
                logger.error("GraphQL - Error deleting validation amount id={}", validationId)
                false
            }
        }
    }

    @PreAuthorize("hasAuthority('USER')")
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
            }

        return when (val result = accountService.save(account)) {
            is ServiceResult.Success -> {
                meterRegistry.counter("graphql.account.create.success").increment()
                logger.info("GraphQL - Created account: {}", result.data.accountNameOwner)
                result.data
            }

            is ServiceResult.ValidationError -> {
                logger.warn("GraphQL - Validation error creating account: {}", result.errors)
                throw IllegalArgumentException("Validation failed: ${result.errors}")
            }

            is ServiceResult.BusinessError -> {
                logger.warn("GraphQL - Business error creating account: {}", result.message)
                throw IllegalStateException(result.message)
            }

            else -> {
                logger.error("GraphQL - Unexpected error creating account")
                throw RuntimeException("Failed to create account")
            }
        }
    }

    @PreAuthorize("hasAuthority('USER')")
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
                            logger.error("GraphQL - Error finding account: {}", oldAccountNameOwner)
                            throw RuntimeException("Failed to find account: $oldAccountNameOwner")
                        }
                    }
                }

                else -> {
                    // Try to look up by the new name (for updates that don't involve renaming)
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
                            logger.error("GraphQL - Error finding account: {}", accountInput.accountNameOwner)
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
            }

        return when (val result = accountService.update(account)) {
            is ServiceResult.Success -> {
                meterRegistry.counter("graphql.account.update.success").increment()
                logger.info("GraphQL - Updated account: {}", result.data.accountNameOwner)
                result.data
            }

            is ServiceResult.NotFound -> {
                logger.warn("GraphQL - Account not found with ID: {}", existingAccountId)
                throw IllegalArgumentException("Account not found with ID: $existingAccountId")
            }

            is ServiceResult.ValidationError -> {
                logger.warn("GraphQL - Validation error updating account: {}", result.errors)
                throw IllegalArgumentException("Validation failed: ${result.errors}")
            }

            is ServiceResult.BusinessError -> {
                logger.warn("GraphQL - Business error updating account: {}", result.message)
                throw IllegalStateException(result.message)
            }

            else -> {
                logger.error("GraphQL - Unexpected error updating account")
                throw RuntimeException("Failed to update account")
            }
        }
    }

    @PreAuthorize("hasAuthority('USER')")
    @MutationMapping
    fun deleteAccount(
        @Argument accountNameOwner: String,
    ): Boolean {
        logger.info("GraphQL - Deleting account: {}", accountNameOwner)
        return when (val result = accountService.deleteById(accountNameOwner)) {
            is ServiceResult.Success -> {
                meterRegistry.counter("graphql.account.delete.success").increment()
                logger.info("GraphQL - Deleted account: {}", accountNameOwner)
                result.data
            }

            is ServiceResult.NotFound -> {
                logger.warn("GraphQL - Account not found for deletion: {}", accountNameOwner)
                false
            }

            else -> {
                logger.error("GraphQL - Error deleting account: {}", accountNameOwner)
                false
            }
        }
    }

    @PreAuthorize("hasAuthority('USER')")
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

        return when (val result = transactionService.save(transaction)) {
            is ServiceResult.Success -> {
                meterRegistry.counter("graphql.transaction.create.success").increment()
                logger.info("GraphQL - Created transaction: {}", result.data.guid)
                result.data
            }

            is ServiceResult.ValidationError -> {
                logger.warn("GraphQL - Validation error creating transaction: {}", result.errors)
                throw IllegalArgumentException("Validation failed: ${result.errors}")
            }

            is ServiceResult.BusinessError -> {
                logger.warn("GraphQL - Business error creating transaction: {}", result.message)
                throw IllegalStateException(result.message)
            }

            else -> {
                logger.error("GraphQL - Unexpected error creating transaction")
                throw RuntimeException("Failed to create transaction")
            }
        }
    }

    @PreAuthorize("hasAuthority('USER')")
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

        return when (val result = transactionService.update(transaction)) {
            is ServiceResult.Success -> {
                meterRegistry.counter("graphql.transaction.update.success").increment()
                logger.info("GraphQL - Updated transaction: {}", result.data.guid)
                result.data
            }

            is ServiceResult.NotFound -> {
                logger.warn("GraphQL - Transaction not found: {}", transaction.guid)
                throw IllegalArgumentException("Transaction not found: ${transaction.guid}")
            }

            is ServiceResult.ValidationError -> {
                logger.warn("GraphQL - Validation error updating transaction: {}", result.errors)
                throw IllegalArgumentException("Validation failed: ${result.errors}")
            }

            is ServiceResult.BusinessError -> {
                logger.warn("GraphQL - Business error updating transaction: {}", result.message)
                throw IllegalStateException(result.message)
            }

            else -> {
                logger.error("GraphQL - Unexpected error updating transaction")
                throw RuntimeException("Failed to update transaction")
            }
        }
    }

    @PreAuthorize("hasAuthority('USER')")
    @MutationMapping
    fun deleteTransaction(
        @Argument guid: String,
    ): Boolean {
        logger.info("GraphQL - Deleting transaction guid={}", guid)
        return when (val result = transactionService.deleteById(guid)) {
            is ServiceResult.Success -> {
                meterRegistry.counter("graphql.transaction.delete.success").increment()
                logger.info("GraphQL - Deleted transaction: {}", guid)
                result.data
            }

            is ServiceResult.NotFound -> {
                logger.warn("GraphQL - Transaction not found for deletion: {}", guid)
                false
            }

            else -> {
                logger.error("GraphQL - Error deleting transaction: {}", guid)
                false
            }
        }
    }
}
