package finance.services

import finance.domain.*
import finance.repositories.TransferRepository
import io.micrometer.core.annotation.Timed
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.*
import jakarta.validation.ConstraintViolation

@Service
open class TransferService(
    private var transferRepository: TransferRepository
) : ITransferService, BaseService() {

    @Timed
    override fun findAllTransfers(): List<Transfer> {
        return transferRepository.findAll().sortedByDescending { transfer -> transfer.transactionDate }
    }

    @Timed
    override fun deleteByTransferId(transferId: Long): Boolean {
        val transfer = transferRepository.findByTransferId(transferId).get()
        transferRepository.delete(transfer)
        return true
    }

    @Timed
    override fun findByTransferId(transferId: Long): Optional<Transfer> {
        logger.info("service - findByTransferId = $transferId")
        val transferOptional: Optional<Transfer> = transferRepository.findByTransferId(transferId)
        if (transferOptional.isPresent) {
            return transferOptional
        }
        return Optional.empty()
    }
}
