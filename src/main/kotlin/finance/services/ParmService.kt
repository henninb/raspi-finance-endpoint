package finance.services

import finance.domain.Parm
import finance.repositories.ParmRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
class ParmService(private var parmRepository: ParmRepository) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun insertParm(parm: Parm): Boolean {
        parmRepository.save(parm)
        return true
    }

    fun findByParm(parmName: String): Optional<Parm> {
        val parmOptional: Optional<Parm> = parmRepository.findByParmName(parmName)
        if (parmOptional.isPresent) {
            return parmOptional
        }
        return Optional.empty()
    }
}