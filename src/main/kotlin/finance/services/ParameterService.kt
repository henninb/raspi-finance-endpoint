package finance.services

import com.fasterxml.jackson.databind.ObjectMapper
import finance.domain.Parameter
import finance.repositories.ParameterRepository
import org.apache.logging.log4j.LogManager
import org.springframework.stereotype.Service
import java.util.*

@Service
open class ParameterService(private var parameterRepository: ParameterRepository,
                            private var meterService: MeterService) {

    fun insertParm(parameter: Parameter): Boolean {
        parameterRepository.saveAndFlush(parameter)
        return true
    }

    fun deleteByParmName(parmName: String) {
        logger.info("deleteByCategory")

        parameterRepository.deleteByParameterName(parmName)
    }

    fun findByParm(parmName: String): Optional<Parameter> {
        val parameterOptional: Optional<Parameter> = parameterRepository.findByParameterName(parmName)
        if (parameterOptional.isPresent) {
            return parameterOptional
        }
        return Optional.empty()
    }

    companion object {
        private val mapper = ObjectMapper()
        private val logger = LogManager.getLogger()
    }
}