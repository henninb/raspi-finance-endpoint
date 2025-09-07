package finance.resolvers

import com.fasterxml.jackson.databind.ObjectMapper
import finance.domain.Payment
import finance.services.IPaymentService
import graphql.schema.DataFetcher
import io.micrometer.core.instrument.MeterRegistry
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import java.sql.Date
import java.util.*

@Component
open class PaymentGraphQLResolver(
    private val paymentService: IPaymentService,
    private val meterRegistry: MeterRegistry
) {

    val payments: DataFetcher<List<Payment>>
        get() = DataFetcher { environment ->
            try {
                logger.info("GraphQL: Fetching all payments")
                val result = paymentService.findAllPayments()
                meterRegistry.counter("graphql.payments.fetch.success").increment()
                logger.info("GraphQL: Successfully fetched ${result.size} payments")
                result
            } catch (e: Exception) {
                logger.error("GraphQL: Error fetching payments", e)
                meterRegistry.counter("graphql.payments.fetch.error").increment()
                throw e
            }
        }

    fun payment(): DataFetcher<Payment?> {
        return DataFetcher { environment ->
            try {
                val paymentId: Long = requireNotNull(environment.getArgument<Long>("paymentId")) { "paymentId is required" }
                logger.info("GraphQL: Fetching payment with ID: $paymentId")

                val paymentOptional = paymentService.findByPaymentId(paymentId)
                val result = if (paymentOptional.isPresent) {
                    meterRegistry.counter("graphql.payment.fetch.success").increment()
                    logger.info("GraphQL: Successfully fetched payment with ID: $paymentId")
                    paymentOptional.get()
                } else {
                    meterRegistry.counter("graphql.payment.fetch.notfound").increment()
                    logger.warn("GraphQL: Payment not found with ID: $paymentId")
                    null
                }
                result
            } catch (e: Exception) {
                logger.error("GraphQL: Error fetching payment", e)
                meterRegistry.counter("graphql.payment.fetch.error").increment()
                throw e
            }
        }
    }

    @PreAuthorize("hasAuthority('USER')")
    fun createPayment(): DataFetcher<Payment> {
        return DataFetcher { environment ->
            try {
                logger.info("GraphQL: Creating new payment")
                val paymentInput = requireNotNull(environment.getArgument<Map<String, Any>>("payment")) { "payment input is required" }

                // Convert input to Payment object
                val payment = mapper.convertValue(paymentInput, Payment::class.java)

                // Set required fields
                payment.guidSource = UUID.randomUUID().toString()
                payment.guidDestination = UUID.randomUUID().toString()
                payment.activeStatus = true

                // Parse transaction date if provided as string
                paymentInput["transactionDate"]?.let { dateInput ->
                    when (dateInput) {
                        is String -> payment.transactionDate = Date.valueOf(dateInput)
                        is Date -> payment.transactionDate = dateInput
                    }
                }

                logger.debug("GraphQL: Payment to create: $payment")

                val result = paymentService.insertPaymentNew(payment)
                meterRegistry.counter("graphql.payment.create.success").increment()
                logger.info("GraphQL: Successfully created payment with ID: ${result.paymentId}")
                result
            } catch (e: Exception) {
                logger.error("GraphQL: Error creating payment", e)
                meterRegistry.counter("graphql.payment.create.error").increment()
                throw e
            }
        }
    }

    @PreAuthorize("hasAuthority('USER')")
    fun deletePayment(): DataFetcher<Boolean> {
        return DataFetcher { environment ->
            try {
                val paymentId: Long = requireNotNull(environment.getArgument<Long>("paymentId")) { "paymentId is required" }
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
    }

    companion object {
        val mapper = ObjectMapper()
        val logger: Logger = LogManager.getLogger()
    }
}
