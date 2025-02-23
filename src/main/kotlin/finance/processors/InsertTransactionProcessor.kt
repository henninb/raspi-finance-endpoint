package finance.processors

import com.fasterxml.jackson.databind.ObjectMapper
import finance.domain.Transaction
import finance.services.MeterService
import finance.services.TransactionService
import io.micrometer.core.annotation.Timed
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

//@Component
//open class InsertTransactionProcessor(
//    private var transactionService: TransactionService
//) : Processor, BaseProcessor() {
//    //private val logger = LoggerFactory.getLogger(this.javaClass)
//    //private val logger = LoggerFactory.getLogger(javaClass)
//
//    @Throws(Exception::class)
//    @Timed
//    override fun process(exchange: Exchange) {
//        val message = exchange.`in`
//        val payload = message.getBody(String::class.java)
//        logger.debug("payload = $payload")
//        val transaction = mapper.readValue(payload, Transaction::class.java)
//        logger.debug("will call to insertTransaction(), guid=${transaction.guid} description=${transaction.description}")
//
//        transactionService.insertTransaction(transaction)
//        meterService.incrementCamelTransactionSuccessfullyInsertedCounter(transaction.accountNameOwner)
//        message.body = transaction.toString()
//        logger.info("InsertTransactionProcessor completed for guid=${transaction.guid}.")
//    }
//}

@Component
open class InsertTransactionProcessor(
    private var transactionService: TransactionService,
    private val mapper: ObjectMapper, // Assuming you are using Jackson's ObjectMapper
    // val meterService: MeterService // Add this as an injected dependency
) : Processor, BaseProcessor() {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    @Throws(Exception::class)
    @Timed
    override fun process(exchange: Exchange) {
        val message = exchange.`in`
        val payload = message.getBody(String::class.java)
        logger.debug("payload = $payload")

        val transaction = try {
            mapper.readValue(payload, Transaction::class.java)
        } catch (e: Exception) {
            logger.error("Error parsing payload", e)
            throw e // Optionally, return a failed response
        }

        logger.debug("will call to insertTransaction(), guid=${transaction.guid} description=${transaction.description}")

        try {
            transactionService.insertTransaction(transaction)
        } catch (e: Exception) {
            logger.error("Error inserting transaction", e)
            throw e // Or handle appropriately
        }

        meterService.incrementCamelTransactionSuccessfullyInsertedCounter(transaction.accountNameOwner)
        message.body = transaction.toString() // Ensure this is the desired format
        logger.info("InsertTransactionProcessor completed for guid=${transaction.guid}.")
    }
}

