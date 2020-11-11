package finance.processors

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.apache.logging.log4j.LogManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
open class ExceptionProcessor @Autowired constructor() : Processor {
    override fun process(exchange: Exchange) {
        val message = exchange.`in`
        val payload = message.getBody(Exception::class.java)
        println(payload)
    }

    companion object {
        private val mapper = ObjectMapper()
        private val logger = LogManager.getLogger()
    }
}