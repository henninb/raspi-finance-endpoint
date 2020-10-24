package finance.repositories

import finance.domain.Category
import finance.domain.Parm
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*
import javax.transaction.Transactional

interface ParmRepository : JpaRepository<Parm, Long> {
    fun findByParmName(parmName: String): Optional<Parm>
}