package finance.repositories

import finance.domain.Parameter
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
// import javax.transaction.Transactional

interface ParameterRepository : JpaRepository<Parameter, Long> {
    fun findByParameterName(parameterName: String): Optional<Parameter>

    fun findByParameterId(parameterId: Long): Optional<Parameter>

    fun findByActiveStatusIsTrue(): List<Parameter>
}
