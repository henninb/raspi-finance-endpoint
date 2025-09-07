package finance.resolvers

import com.fasterxml.jackson.databind.ObjectMapper
import finance.domain.Transfer
import finance.services.ITransferService
import graphql.schema.DataFetcher
import io.micrometer.core.instrument.MeterRegistry
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import java.sql.Date
import java.util.*

@Component
open class TransferGraphQLResolver(
    private val transferService: ITransferService,
    private val meterRegistry: MeterRegistry
) {

    val transfers: DataFetcher<List<Transfer>>
        get() = DataFetcher { environment ->
            try {
                logger.info("GraphQL: Fetching all transfers")
                val result = transferService.findAllTransfers()
                meterRegistry.counter("graphql.transfers.fetch.success").increment()
                logger.info("GraphQL: Successfully fetched ${result.size} transfers")

                // Log sample transfer data for debugging serialization
                if (result.isNotEmpty()) {
                    val firstTransfer = result.first()
                    logger.debug("GraphQL: Sample transfer data - ID: ${firstTransfer.transferId}, Source: ${firstTransfer.sourceAccount}, Dest: ${firstTransfer.destinationAccount}")
                    logger.debug("GraphQL: Sample transfer timestamps - dateAdded: ${firstTransfer.dateAdded}, dateUpdated: ${firstTransfer.dateUpdated}")

                    // Test serialization
                    try {
                        val serialized = firstTransfer.toString()
                        logger.debug("GraphQL: Transfer serialization test successful, length: ${serialized.length}")
                    } catch (serializationException: Exception) {
                        logger.error("GraphQL: Transfer serialization failed", serializationException)
                    }
                }

                result
            } catch (e: Exception) {
                logger.error("GraphQL: Error fetching transfers", e)
                meterRegistry.counter("graphql.transfers.fetch.error").increment()
                throw e
            }
        }

    fun transfer(): DataFetcher<Transfer?> {
        return DataFetcher { environment ->
            try {
                val transferId: Long = requireNotNull(environment.getArgument<Long>("transferId")) { "transferId is required" }
                logger.info("GraphQL: Fetching transfer with ID: $transferId")

                val transferOptional = transferService.findByTransferId(transferId)
                val result = if (transferOptional.isPresent) {
                    meterRegistry.counter("graphql.transfer.fetch.success").increment()
                    logger.info("GraphQL: Successfully fetched transfer with ID: $transferId")
                    transferOptional.get()
                } else {
                    meterRegistry.counter("graphql.transfer.fetch.notfound").increment()
                    logger.warn("GraphQL: Transfer not found with ID: $transferId")
                    null
                }
                result
            } catch (e: Exception) {
                logger.error("GraphQL: Error fetching transfer", e)
                meterRegistry.counter("graphql.transfer.fetch.error").increment()
                throw e
            }
        }
    }

    @PreAuthorize("hasAuthority('USER')")
    fun createTransfer(): DataFetcher<Transfer> {
        return DataFetcher { environment ->
            try {
                logger.info("GraphQL: Creating new transfer")
                val transferInput = requireNotNull(environment.getArgument<Map<String, Any>>("transfer")) { "transfer input is required" }

                // Convert input to Transfer object
                val transfer = mapper.convertValue(transferInput, Transfer::class.java)

                // Set required fields
                transfer.guidSource = UUID.randomUUID().toString()
                transfer.guidDestination = UUID.randomUUID().toString()
                transfer.activeStatus = true

                // Parse transaction date if provided as string
                transferInput["transactionDate"]?.let { dateInput ->
                    when (dateInput) {
                        is String -> transfer.transactionDate = Date.valueOf(dateInput)
                        is Date -> transfer.transactionDate = dateInput
                    }
                }

                logger.debug("GraphQL: Transfer to create: $transfer")

                val result = transferService.insertTransfer(transfer)
                meterRegistry.counter("graphql.transfer.create.success").increment()
                logger.info("GraphQL: Successfully created transfer with ID: ${result.transferId}")
                result
            } catch (e: Exception) {
                logger.error("GraphQL: Error creating transfer", e)
                meterRegistry.counter("graphql.transfer.create.error").increment()
                throw e
            }
        }
    }

    @PreAuthorize("hasAuthority('USER')")
    fun deleteTransfer(): DataFetcher<Boolean> {
        return DataFetcher { environment ->
            try {
                val transferId: Long = requireNotNull(environment.getArgument<Long>("transferId")) { "transferId is required" }
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
    }

    companion object {
        val mapper = ObjectMapper()
        val logger: Logger = LogManager.getLogger()
    }
}
