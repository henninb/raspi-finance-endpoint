package finance.processors

import com.fasterxml.jackson.databind.ObjectMapper
import finance.domain.Transaction
import finance.services.MeterService
import io.micrometer.core.annotation.Timed
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.apache.logging.log4j.LogManager
import org.springframework.stereotype.Component

@Component
open class StringTransactionProcessor(private var meterService: MeterService) : Processor {

    @Throws(Exception::class)
    @Timed
    override fun process(exchange: Exchange) {
        val message = exchange.`in`
        val transaction = message.getBody(Transaction::class.java)
        logger.info("transaction.guid=${transaction.guid}")
        exchange.setProperty("guid", transaction.guid)
        message.body = transaction.toString()
        meterService.incrementCamelStringProcessor(transaction.accountNameOwner)
        logger.info("StringTransactionProcessor completed for ${transaction.guid}")
    }

    companion object {
        private val mapper = ObjectMapper()
        private val logger = LogManager.getLogger()
    }
}