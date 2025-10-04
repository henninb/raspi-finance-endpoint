package finance.controllers.graphql

import finance.controllers.dto.PaymentInputDto
import finance.controllers.dto.TransferInputDto
import finance.domain.Payment
import finance.domain.Transfer
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
}
