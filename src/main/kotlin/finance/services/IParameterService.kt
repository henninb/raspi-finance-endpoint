package finance.services

import finance.domain.Parameter
import java.util.*

interface IParameterService {
    fun selectAll() : List<Parameter>
    fun insertParameter(parameter: Parameter): Parameter
    fun deleteByParameterName(parameterName: String): Boolean
    fun findByParameterName(parameterName: String): Optional<Parameter>
    fun updateParameter(parameter: Parameter): Parameter
    //abstract fun findByParameterName(parameterName: String): Any
}