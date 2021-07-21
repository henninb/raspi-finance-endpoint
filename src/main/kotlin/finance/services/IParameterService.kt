package finance.services

import finance.domain.Parameter
import java.util.*

interface IParameterService {
    fun insertParameter(parameter: Parameter): Parameter
    fun deleteByParameterName(parameterName: String): Boolean
    fun findByParameter(parameterName: String): Optional<Parameter>
}