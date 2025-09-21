package finance.controllers

import finance.domain.Payment
import finance.services.IPaymentService
import io.micrometer.core.instrument.MeterRegistry
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import java.math.BigDecimal
import java.sql.Date
import java.util.*

/**
 * Modern Spring Boot 4.0 GraphQL Payment Controller
 * Uses annotation-based approach with automatic discovery and wiring
 */
@Controller
open class PaymentGraphQLController(
    private val paymentService: IPaymentService,
    private val meterRegistry: MeterRegistry
) {

    @QueryMapping
    fun payments(): List<Payment> {
        return try {
            logger.info("GraphQL: Fetching all payments")
            val result = paymentService.findAllPayments() ?: emptyList()
            meterRegistry.counter("graphql.payments.fetch.success").increment()
            logger.info("GraphQL: Successfully fetched ${result.size} payments")
            result
        } catch (e: Exception) {
            logger.error("GraphQL: Error fetching payments", e)
            meterRegistry.counter("graphql.payments.fetch.error").increment()
            throw e
        }
    }

    @QueryMapping
    fun payment(@Argument paymentId: Long): Payment? {
        return try {
            logger.info("GraphQL: Fetching payment with ID: $paymentId")
            val paymentOptional = paymentService.findByPaymentId(paymentId)

            if (paymentOptional.isPresent) {
                meterRegistry.counter("graphql.payment.fetch.success").increment()
                logger.info("GraphQL: Successfully fetched payment with ID: $paymentId")
                paymentOptional.get()
            } else {
                meterRegistry.counter("graphql.payment.fetch.notfound").increment()
                logger.warn("GraphQL: Payment not found with ID: $paymentId")
                null
            }
        } catch (e: Exception) {
            logger.error("GraphQL: Error fetching payment", e)
            meterRegistry.counter("graphql.payment.fetch.error").increment()
            throw e
        }
    }

    @MutationMapping
    @PreAuthorize("hasAuthority('USER')")
    fun createPayment(@Argument payment: PaymentInput): Payment {
        return try {
            logger.info("GraphQL: Creating new payment")

            // Convert input to Payment entity with proper date handling
            val paymentEntity = Payment().apply {
                sourceAccount = payment.sourceAccount
                destinationAccount = payment.destinationAccount
                transactionDate = try {
                    Date.valueOf(payment.transactionDate)
                } catch (e: IllegalArgumentException) {
                    logger.warn("GraphQL: Invalid date format ${payment.transactionDate}, using current date")
                    Date(System.currentTimeMillis())
                }
                amount = payment.amount
                guidSource = UUID.randomUUID().toString()
                guidDestination = UUID.randomUUID().toString()
                activeStatus = payment.activeStatus ?: true
            }

            logger.debug("GraphQL: Payment to create: $paymentEntity")

            val result = paymentService.insertPayment(paymentEntity)
            meterRegistry.counter("graphql.payment.create.success").increment()
            logger.info("GraphQL: Successfully created payment with ID: ${result.paymentId}")
            result
        } catch (e: Exception) {
            logger.error("GraphQL: Error creating payment", e)
            meterRegistry.counter("graphql.payment.create.error").increment()
            throw e
        }
    }

    @MutationMapping
    @PreAuthorize("hasAuthority('USER')")
    fun deletePayment(@Argument paymentId: Long): Boolean {
        return try {
            logger.info("GraphQL: Deleting payment with ID: $paymentId")

            val result = paymentService.deleteByPaymentId(paymentId)
            if (result) {
                meterRegistry.counter("graphql.payment.delete.success").increment()
                logger.info("GraphQL: Successfully deleted payment with ID: $paymentId")
            } else {
                meterRegistry.counter("graphql.payment.delete.notfound").increment()
                logger.warn("GraphQL: Payment not found for deletion with ID: $paymentId")
            }
            result
        } catch (e: Exception) {
            logger.error("GraphQL: Error deleting payment", e)
            meterRegistry.counter("graphql.payment.delete.error").increment()
            throw e
        }
    }

    /**
     * Payment input data class for GraphQL mutations
     */
    data class PaymentInput(
        val paymentId: Long? = null,
        val sourceAccount: String,
        val destinationAccount: String,
        val transactionDate: String, // Will be converted to Date
        val amount: BigDecimal,
        val activeStatus: Boolean? = null
    )

    companion object {
        val logger: Logger = LogManager.getLogger()
    }
}