package finance.repositories

import finance.domain.Parameter
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*
import javax.transaction.Transactional

interface ParameterRepository : JpaRepository<Parameter, Long> {
    fun findByParameterName(parameterName: String): Optional<Parameter>

    @Transactional
    fun deleteByParameterName(parameterName: String)
}