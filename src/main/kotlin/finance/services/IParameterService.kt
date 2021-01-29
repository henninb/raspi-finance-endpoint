package finance.services

import finance.domain.Parameter
import java.util.*

interface IParameterService {
    fun insertParameter(parameter: Parameter): Boolean
    fun deleteByParameterName(parameterName: String)
    fun findByParameter(parameterName: String): Optional<Parameter>
}