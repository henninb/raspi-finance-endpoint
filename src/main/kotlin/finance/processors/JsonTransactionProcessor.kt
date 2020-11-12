package finance.processors

import com.fasterxml.jackson.databind.ObjectMapper
import finance.domain.Transaction
import finance.services.MeterService
import io.micrometer.core.annotation.Timed
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.apache.logging.log4j.LogManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.validation.ConstraintViolation
import javax.validation.Validator

@Component
open class JsonTransactionProcessor @Autowired constructor(private val validator: Validator,
                                                           private var meterService: MeterService) : Processor {

    @Throws(Exception::class)
    @Timed("json.transaction.processor.timer")
    override fun process(exchange: Exchange) {
        val message = exchange.`in`
        val payload = message.getBody(String::class.java)

        val transactions = mapper.readValue(payload, Array<Transaction>::class.java)
        logger.info(transactions.toString())
        for (transaction in transactions) {
            val constraintViolations: Set<ConstraintViolation<Transaction>> = validator.validate(transaction)
            if (constraintViolations.isNotEmpty()) {
                //TODO: handle the violation

                logger.error("the json <${transaction}>")
                logger.error("do not load into the database.")
                //meterService.incrementErrorCounter(transaction.accountNameOwner, MeterService.ErrorType.VALIDATION_ERROR)
                logger.error("METRIC_TRANSACTION_VALIDATOR_FAILED_COUNTER")
                throw RuntimeException("transaction object has validation errors.")
            }
        }
        logger.info("JsonTransactionProcessor size: ${transactions.size}")
        message.body = transactions
    }

    companion object {
        private val mapper = ObjectMapper()
        private val logger = LogManager.getLogger()
    }
}