package finance.controllers

import finance.domain.Transfer
import finance.services.ITransferService
import io.micrometer.core.instrument.MeterRegistry
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import java.math.BigDecimal
import java.sql.Date
import java.util.*

/**
 * Modern Spring Boot 4.0 GraphQL Transfer Controller
 * Uses annotation-based approach with automatic discovery and wiring
 */
@Controller
open class TransferGraphQLController(
    private val transferService: ITransferService,
    private val meterRegistry: MeterRegistry
) {

    @QueryMapping
    fun transfers(): List<Transfer> {
        return try {
            logger.info("GraphQL: Fetching all transfers")
            val result = transferService.findAllTransfers()
            meterRegistry.counter("graphql.transfers.fetch.success").increment()
            logger.info("GraphQL: Successfully fetched ${result.size} transfers")
            result
        } catch (e: Exception) {
            logger.error("GraphQL: Error fetching transfers", e)
            meterRegistry.counter("graphql.transfers.fetch.error").increment()
            throw e
        }
    }

    @QueryMapping
    fun transfer(@Argument transferId: Long): Transfer? {
        return try {
            logger.info("GraphQL: Fetching transfer with ID: $transferId")
            val transferOptional = transferService.findByTransferId(transferId)

            if (transferOptional.isPresent) {
                meterRegistry.counter("graphql.transfer.fetch.success").increment()
                logger.info("GraphQL: Successfully fetched transfer with ID: $transferId")
                transferOptional.get()
            } else {
                meterRegistry.counter("graphql.transfer.fetch.notfound").increment()
                logger.warn("GraphQL: Transfer not found with ID: $transferId")
                null
            }
        } catch (e: Exception) {
            logger.error("GraphQL: Error fetching transfer", e)
            meterRegistry.counter("graphql.transfer.fetch.error").increment()
            throw e
        }
    }

    @MutationMapping
    @PreAuthorize("hasAuthority('USER')")
    fun createTransfer(@Argument transfer: TransferInput): Transfer {
        return try {
            logger.info("GraphQL: Creating new transfer")

            // Convert input to Transfer entity
            val transferEntity = Transfer().apply {
                sourceAccount = transfer.sourceAccount
                destinationAccount = transfer.destinationAccount
                transactionDate = Date.valueOf(transfer.transactionDate)
                amount = transfer.amount
                guidSource = UUID.randomUUID().toString()
                guidDestination = UUID.randomUUID().toString()
                activeStatus = transfer.activeStatus ?: true
            }

            logger.debug("GraphQL: Transfer to create: $transferEntity")

            val result = transferService.insertTransfer(transferEntity)
            meterRegistry.counter("graphql.transfer.create.success").increment()
            logger.info("GraphQL: Successfully created transfer with ID: ${result.transferId}")
            result
        } catch (e: Exception) {
            logger.error("GraphQL: Error creating transfer", e)
            meterRegistry.counter("graphql.transfer.create.error").increment()
            throw e
        }
    }

    @MutationMapping
    @PreAuthorize("hasAuthority('USER')")
    fun deleteTransfer(@Argument transferId: Long): Boolean {
        return try {
            logger.info("GraphQL: Deleting transfer with ID: $transferId")

            val result = transferService.deleteByTransferId(transferId)
            if (result) {
                meterRegistry.counter("graphql.transfer.delete.success").increment()
                logger.info("GraphQL: Successfully deleted transfer with ID: $transferId")
            } else {
                meterRegistry.counter("graphql.transfer.delete.notfound").increment()
                logger.warn("GraphQL: Transfer not found for deletion with ID: $transferId")
            }
            result
        } catch (e: Exception) {
            logger.error("GraphQL: Error deleting transfer", e)
            meterRegistry.counter("graphql.transfer.delete.error").increment()
            throw e
        }
    }

    /**
     * Transfer input data class for GraphQL mutations
     */
    data class TransferInput(
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