package finance.services

import com.fasterxml.jackson.databind.ObjectMapper
import finance.domain.ValidationAmount
import finance.repositories.ValidationAmountRepository
import org.apache.logging.log4j.LogManager
import org.springframework.stereotype.Service
import javax.validation.Validator

@Service
open class ValidationAmountService(
    private var validationAmountRepository: ValidationAmountRepository,
    private val validator: Validator,
    private var meterService: MeterService
) : IValidationAmountService {

    companion object {
        private val mapper = ObjectMapper()
        private val logger = LogManager.getLogger()
    }

    override fun insertValidationAmount(validationAmount: ValidationAmount) {
        TODO("Not yet implemented")
    }
}
