package finance.services

import com.fasterxml.jackson.databind.ObjectMapper
import finance.domain.Parm
import finance.repositories.ParmRepository
import org.apache.logging.log4j.LogManager
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
open class ParmService(private var parmRepository: ParmRepository) {

    fun insertParm(parm: Parm): Boolean {
        parmRepository.saveAndFlush(parm)
        return true
    }

    fun deleteByParmName(parmName: String) {
        logger.info("deleteByCategory")

        parmRepository.deleteByParmName(parmName)
    }

    fun findByParm(parmName: String): Optional<Parm> {
        val parmOptional: Optional<Parm> = parmRepository.findByParmName(parmName)
        if (parmOptional.isPresent) {
            return parmOptional
        }
        return Optional.empty()
    }

    companion object {
        private val mapper = ObjectMapper()
        private val logger = LogManager.getLogger()
    }
}