package finance.repositories

import finance.domain.Parameter
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface ParameterRepository : JpaRepository<Parameter, Long> {
    fun findByParameterName(parameterName: String): Optional<Parameter>

    fun findByParameterId(parameterId: Long): Optional<Parameter>

    fun findByActiveStatusIsTrue(): List<Parameter>

    // --- Owner-scoped methods for multi-tenancy (Phase 4) ---

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
