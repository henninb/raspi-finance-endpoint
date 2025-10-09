package finance.controllers.graphql

import finance.controllers.dto.CategoryInputDto
import finance.controllers.dto.DescriptionInputDto
import finance.controllers.dto.PaymentInputDto
import finance.controllers.dto.TransferInputDto
import finance.domain.Category
import finance.domain.Description
import finance.domain.Parameter
import finance.domain.Payment
import finance.domain.ServiceResult
import finance.domain.Transfer
import finance.services.StandardizedCategoryService
import finance.services.StandardizedDescriptionService
import finance.services.StandardizedParameterService
import finance.services.StandardizedPaymentService
import finance.services.StandardizedTransferService
import io.micrometer.core.instrument.MeterRegistry
import jakarta.validation.Valid
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import org.springframework.validation.annotation.Validated
import java.util.UUID

@Controller
@Validated
class GraphQLMutationController(
    private val categoryService: StandardizedCategoryService,
    private val descriptionService: StandardizedDescriptionService,
    private val parameterService: StandardizedParameterService,
    private val paymentService: StandardizedPaymentService,
    private val transferService: StandardizedTransferService,
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
    fun deletePayment(
        @Argument paymentId: Long,
    ): Boolean {
        logger.info("GraphQL - Deleting payment id={}", paymentId)
        return paymentService.deleteByPaymentId(paymentId)
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
    fun deleteTransfer(
        @Argument transferId: Long,
    ): Boolean {
        logger.info("GraphQL - Deleting transfer id={}", transferId)
        return transferService.deleteByTransferId(transferId)
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
                        is ServiceResult.Success -> findResult.data.categoryId
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
                        is ServiceResult.Success -> findResult.data.categoryId
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
                        is ServiceResult.Success -> findResult.data.descriptionId
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
                        is ServiceResult.Success -> findResult.data.descriptionId
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
}
