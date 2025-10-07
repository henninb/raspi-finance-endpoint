package finance.controllers.graphql

import finance.controllers.dto.PaymentInputDto
import finance.controllers.dto.TransferInputDto
import finance.domain.Category
import finance.domain.Parameter
import finance.domain.Payment
import finance.domain.ServiceResult
import finance.domain.Transfer
import finance.services.StandardizedCategoryService
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
        @Argument("category") @Valid category: Category,
    ): Category {
        logger.info("GraphQL - Creating category via @MutationMapping")
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
        @Argument("category") @Valid category: Category,
    ): Category {
        logger.info("GraphQL - Updating category: {}", category.categoryName)
        return when (val result = categoryService.update(category)) {
            is ServiceResult.Success -> {
                meterRegistry.counter("graphql.category.update.success").increment()
                logger.info("GraphQL - Updated category: {}", result.data.categoryName)
                result.data
            }
            is ServiceResult.NotFound -> {
                logger.warn("GraphQL - Category not found: {}", category.categoryName)
                throw IllegalArgumentException("Category not found: ${category.categoryName}")
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
}
