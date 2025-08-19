package finance.processors

import com.fasterxml.jackson.databind.ObjectMapper
import finance.services.MeterService
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import jakarta.validation.Validator

open class BaseProcessor {

    @Autowired
    private lateinit var _meterService: MeterService
    open var meterService: MeterService
        get() = _meterService
        set(value) { _meterService = value }

    @Autowired
    private lateinit var _validator: Validator
    open var validator: Validator
        get() = _validator
        set(value) { _validator = value }

    companion object {
        val mapper = ObjectMapper()
        val logger: Logger = LogManager.getLogger()
    }
}