package finance.processors

import com.fasterxml.jackson.databind.ObjectMapper
import io.micrometer.core.annotation.Timed
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.apache.logging.log4j.LogManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
open class ExceptionProcessor() : Processor {

    @Timed
    override fun process(exchange: Exchange) {
        val message = exchange.`in`
        val payload = message.getBody(Exception::class.java)
        logger.warn(payload)
    }

    companion object {
        private val mapper = ObjectMapper()
        private val logger = LogManager.getLogger()
    }
}