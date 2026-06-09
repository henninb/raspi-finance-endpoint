package finance.repositories

import finance.domain.Parameter
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface ParameterRepository : JpaRepository<Parameter, Long> {
    fun findByOwnerAndParameterName(
        owner: String,
        parameterName: String,
    ): Optional<Parameter>

    fun findByOwnerAndParameterId(
        owner: String,
        parameterId: Long,
    ): Optional<Parameter>

    fun findByOwnerAndActiveStatusIsTrue(
        owner: String,
    ): List<Parameter>
}
