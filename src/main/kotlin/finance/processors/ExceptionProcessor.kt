package finance.processors

import io.micrometer.core.annotation.Timed
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.springframework.stereotype.Component

@Component
open class ExceptionProcessor : Processor, BaseProcessor() {

    @Timed
    override fun process(exchange: Exchange) {
        val message = exchange.`in`
        val payload = message.getBody(Exception::class.java)
        logger.warn(payload)
    }
}