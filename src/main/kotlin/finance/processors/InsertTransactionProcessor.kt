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
open class InsertTransactionProcessor @Autowired constructor(
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
        logger.info("payload=$payload")
        val transaction = mapper.readValue(payload, Transaction::class.java)
        logger.info("will call to insertTransaction(), guid=${transaction.guid} description=${transaction.description}")

//        meterRegistry.timer(METRIC_INSERT_TRANSACTION_TIMER).record {
//            transactionService.insertTransaction(transaction)
//        }
        transactionService.insertTransaction(transaction)

        logger.info("called to insertTransaction(), guid=${transaction.guid} description=${transaction.description}")
        message.body = transaction.toString()
        logger.debug("InsertTransactionProcessor completed")
    }

    companion object {
        private val mapper = ObjectMapper()
        private val logger = LogManager.getLogger()
    }
}
