package finance.services

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import javax.validation.Validator

open class BaseService {

//    @Autowired
//    lateinit var meterService: MeterService
//
//    @Autowired
//    lateinit var  validator: Validator

    companion object {
        val mapper = ObjectMapper()
        val logger: Logger = LogManager.getLogger()
    }
}