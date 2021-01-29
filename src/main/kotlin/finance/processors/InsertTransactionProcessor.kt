package finance.processors

import com.fasterxml.jackson.databind.ObjectMapper
import finance.domain.Transaction
import finance.services.MeterService
import finance.services.TransactionService
import io.micrometer.core.annotation.Timed
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.apache.logging.log4j.LogManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
open class InsertTransactionProcessor(
    private var transactionService: TransactionService,
    private var meterService: MeterService
) : Processor {
    //private val logger = LoggerFactory.getLogger(this.javaClass)
    //private val logger = LoggerFactory.getLogger(javaClass)

    @Throws(Exception::class)
    @Timed
    override fun process(exchange: Exchange) {
        val message = exchange.`in`
        val payload = message.getBody(String::class.java)
        logger.debug("payload = $payload")
        val transaction = mapper.readValue(payload, Transaction::class.java)
        logger.debug("will call to insertTransaction(), guid=${transaction.guid} description=${transaction.description}")

        transactionService.insertTransaction(transaction)
        meterService.incrementCamelTransactionSuccessfullyInsertedCounter(transaction.accountNameOwner)
        message.body = transaction.toString()
        logger.info("InsertTransactionProcessor completed for guid=${transaction.guid}.")
    }

    companion object {
        private val mapper = ObjectMapper()
        private val logger = LogManager.getLogger()
    }
}
