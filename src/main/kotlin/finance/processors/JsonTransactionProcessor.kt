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
import javax.validation.ConstraintViolation
import javax.validation.ValidationException
import javax.validation.Validator

@Component
open class JsonTransactionProcessor @Autowired constructor(
    private val validator: Validator,
    private var meterService: MeterService
) : Processor {

    @Throws(Exception::class)
    @Timed("json.transaction.processor.timer")
    override fun process(exchange: Exchange) {
        val message = exchange.`in`
        val payload = message.getBody(String::class.java)

        val transactions = mapper.readValue(payload, Array<Transaction>::class.java)

        for (transaction in transactions) {
            val constraintViolations: Set<ConstraintViolation<Transaction>> = validator.validate(transaction)
            if (constraintViolations.isNotEmpty()) {
                logger.error("payload: $transaction")
                logger.error("METRIC_TRANSACTION_VALIDATOR_FAILED_COUNTER")
                constraintViolations.forEach { constraintViolation -> logger.error(constraintViolation.message) }
                logger.error("Cannot insert transaction as there is a constraint violation on the data.")
                throw ValidationException("Cannot insert transaction as there is a constraint violation on the data.")
            }
        }
        logger.info("JsonTransactionProcessor size = ${transactions.size}")
        message.body = transactions
    }

    companion object {
        private val mapper = ObjectMapper()
        private val logger = LogManager.getLogger()
    }
}