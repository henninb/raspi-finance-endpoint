package finance.processors

import finance.domain.Transaction
import io.micrometer.core.annotation.Timed
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.springframework.stereotype.Component
import javax.validation.ConstraintViolation
import javax.validation.ValidationException

@Component
open class JsonTransactionProcessor : Processor, BaseProcessor() {

    @Throws(Exception::class)
    @Timed
    override fun process(exchange: Exchange) {
        val message = exchange.`in`
        val payload = message.getBody(String::class.java)

        val transactions = mapper.readValue(payload, Array<Transaction>::class.java)

        for (transaction in transactions) {
            val constraintViolations: Set<ConstraintViolation<Transaction>> = validator.validate(transaction)
            if (constraintViolations.isNotEmpty()) {
                logger.error("payload: $transaction")
                constraintViolations.forEach { constraintViolation -> logger.error(constraintViolation.message) }
                logger.error("Cannot insert transaction as there is a constraint violation on the data.")
                throw ValidationException("Cannot insert transaction as there is a constraint violation on the data.")
            }
        }
        logger.info("JsonTransactionProcessor size = ${transactions.size}")
        message.body = transactions
    }
}