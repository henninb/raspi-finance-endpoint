package finance.processors

import finance.domain.Transaction
import io.micrometer.core.annotation.Timed
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.springframework.stereotype.Component

@Component
open class StringTransactionProcessor : Processor, BaseProcessor() {

    @Throws(Exception::class)
    @Timed
    override fun process(exchange: Exchange) {
        val message = exchange.`in`
        val transaction =
            message.getBody(Transaction::class.java) ?: throw RuntimeException("Invalid object exchanged.")
        logger.debug("transaction.guid=${transaction.guid}")
        logger.debug("exchangeId=${exchange.exchangeId}")
        exchange.setProperty("guid", transaction.guid)
        message.body = transaction.toString()
        meterService.incrementCamelStringProcessor(transaction.accountNameOwner)
        logger.info("StringTransactionProcessor completed for ${transaction.guid}")
    }
}