package finance.services

import com.fasterxml.jackson.databind.ObjectMapper
import finance.domain.Parameter
import finance.repositories.ParameterRepository
import org.apache.logging.log4j.LogManager
import org.springframework.stereotype.Service
import java.sql.Timestamp
import java.util.*

@Service
open class ParameterService(private var parameterRepository: ParameterRepository,
                            private var meterService: MeterService) {

    fun insertParameter(parameter: Parameter): Boolean {
        parameter.dateAdded = Timestamp(Calendar.getInstance().time.time)
        parameter.dateUpdated = Timestamp(Calendar.getInstance().time.time)
        parameterRepository.saveAndFlush(parameter)
        return true
    }

    fun deleteByParameterName(parameterName: String) {
        parameterRepository.deleteByParameterName(parameterName)
    }

    fun findByParameter(parameterName: String): Optional<Parameter> {
        val parameterOptional: Optional<Parameter> = parameterRepository.findByParameterName(parameterName)
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