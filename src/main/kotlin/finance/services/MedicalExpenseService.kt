package finance.services

import finance.domain.ClaimStatus
import finance.domain.MedicalExpense
import finance.domain.ServiceResult
import finance.exceptions.DuplicateMedicalExpenseException
import finance.repositories.MedicalExpenseRepository
import finance.utils.TenantContext
import jakarta.validation.ValidationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Standardized Medical Expense Service implementing ServiceResult pattern
 * Provides both new standardized methods and legacy compatibility
 */
@Service
@org.springframework.context.annotation.Primary
class MedicalExpenseService(
    private val medicalExpenseRepository: MedicalExpenseRepository,
) : CrudBaseService<MedicalExpense, Long>() {
    override fun getEntityName(): String = "MedicalExpense"

    // ===== New Standardized ServiceResult Methods =====

    override fun findAllActive(): ServiceResult<List<MedicalExpense>> =
        handleServiceOperation("findAllActive", null) {
            val owner = TenantContext.getCurrentOwner()
            medicalExpenseRepository.findByOwnerAndActiveStatusTrueOrderByServiceDateDesc(owner)
        }

    override fun findById(id: Long): ServiceResult<MedicalExpense> =
        handleServiceOperation("findById", id) {
            val owner = TenantContext.getCurrentOwner()
            val medicalExpense = medicalExpenseRepository.findByOwnerAndMedicalExpenseIdAndActiveStatusTrue(owner, id)
            medicalExpense ?: throw jakarta.persistence.EntityNotFoundException("MedicalExpense not found: $id")
        }

    override fun save(entity: MedicalExpense): ServiceResult<MedicalExpense> =
        handleServiceOperation("save", entity.medicalExpenseId) {
            val owner = TenantContext.getCurrentOwner()
            entity.owner = owner

            val violations = validator.validate(entity)
            if (violations.isNotEmpty()) {
                throw jakarta.validation.ConstraintViolationException("Validation failed", violations)
            }

            // Check for duplicates if transactionId is provided
            entity.transactionId?.let { transactionId ->
                if (transactionId > 0) {
                    val existingExpense = medicalExpenseRepository.findByOwnerAndTransactionId(owner, transactionId)
                    if (existingExpense != null) {
                        throw DataIntegrityViolationException("Medical expense already exists for transaction ID: $transactionId")
                    }
                }
            }

            medicalExpenseRepository.save(entity)
        }

    override fun update(entity: MedicalExpense): ServiceResult<MedicalExpense> =
        handleServiceOperation("update", entity.medicalExpenseId) {
            val owner = TenantContext.getCurrentOwner()
            medicalExpenseRepository.findByOwnerAndMedicalExpenseIdAndActiveStatusTrue(owner, entity.medicalExpenseId)
                ?: throw jakarta.persistence.EntityNotFoundException("MedicalExpense not found: ${entity.medicalExpenseId}")

            entity.owner = owner
            medicalExpenseRepository.save(entity)
        }

    override fun deleteById(id: Long): ServiceResult<Boolean> =
        handleServiceOperation("deleteById", id) {
            val owner = TenantContext.getCurrentOwner()
            val updatedRows = medicalExpenseRepository.softDeleteByOwnerAndMedicalExpenseId(owner, id)
            if (updatedRows == 0) {
                throw jakarta.persistence.EntityNotFoundException("MedicalExpense not found: $id")
            }
            true
        }

    // ===== Legacy Method Compatibility =====

    fun findAllMedicalExpenses(): List<MedicalExpense> {
        val result = findAllActive()
        return when (result) {
            is ServiceResult.Success -> {
                result.data
            }

            else -> {
                logger.error("Error retrieving all medical expenses: $result")
                emptyList()
            }
        }
    }

    fun insertMedicalExpense(medicalExpense: MedicalExpense): MedicalExpense {
        val owner = TenantContext.getCurrentOwner()
        medicalExpense.owner = owner
        logger.info("Inserting medical expense for transaction ID: ${medicalExpense.transactionId}")

        // Check for duplicates if transactionId is provided
        medicalExpense.transactionId?.let { transactionId ->
            if (transactionId > 0) {
                val existingExpense = medicalExpenseRepository.findByOwnerAndTransactionId(owner, transactionId)
                if (existingExpense != null) {
                    throw DuplicateMedicalExpenseException("Medical expense already exists for transaction ID: $transactionId")
                }
            }
        }

        val result = save(medicalExpense)
        return when (result) {
            is ServiceResult.Success -> {
                logger.info("Successfully inserted medical expense with ID: ${result.data.medicalExpenseId}")
                result.data
            }

            is ServiceResult.ValidationError -> {
                val message = "Validation failed: ${result.errors}"
                logger.error(message)
                throw ValidationException(message)
            }

            is ServiceResult.BusinessError -> {
                logger.error("Business error inserting medical expense: ${result.message}")
                throw DuplicateMedicalExpenseException(result.message)
            }

            else -> {
                val message = "Failed to insert medical expense: $result"
                logger.error(message)
                throw RuntimeException(message)
            }
        }
    }

    fun updateMedicalExpense(medicalExpense: MedicalExpense): MedicalExpense {
        logger.info("Updating medical expense with ID: ${medicalExpense.medicalExpenseId}")

        val result = update(medicalExpense)
        return when (result) {
            is ServiceResult.Success -> {
                logger.info("Successfully updated medical expense with ID: ${result.data.medicalExpenseId}")
                result.data
            }

            is ServiceResult.NotFound -> {
                val message = "Medical expense not found with ID: ${medicalExpense.medicalExpenseId}"
                logger.error(message)
                throw IllegalArgumentException(message)
            }

            is ServiceResult.BusinessError -> {
                logger.error("Business error updating medical expense: ${result.message}")
                throw org.springframework.dao.DataIntegrityViolationException(result.message)
            }

            else -> {
                val message = "Failed to update medical expense: $result"
                logger.error(message)
                throw RuntimeException(message)
            }
        }
    }

    fun findMedicalExpenseById(medicalExpenseId: Long): MedicalExpense? {
        val owner = TenantContext.getCurrentOwner()
        logger.debug("Finding medical expense by ID: $medicalExpenseId")
        return medicalExpenseRepository.findByOwnerAndMedicalExpenseIdAndActiveStatusTrue(owner, medicalExpenseId)
    }

    fun findMedicalExpenseByTransactionId(transactionId: Long): MedicalExpense? {
        val owner = TenantContext.getCurrentOwner()
        logger.debug("Finding medical expense by transaction ID: $transactionId")
        return medicalExpenseRepository.findByOwnerAndTransactionId(owner, transactionId)
    }

    fun findMedicalExpensesByAccountId(accountId: Long): List<MedicalExpense> {
        val owner = TenantContext.getCurrentOwner()
        logger.debug("Finding medical expenses by account ID: $accountId")
        return medicalExpenseRepository.findByOwnerAndAccountId(owner, accountId)
    }

    fun findMedicalExpensesByServiceDateRange(
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<MedicalExpense> {
        val owner = TenantContext.getCurrentOwner()
        logger.debug("Finding medical expenses by service date range: $startDate to $endDate")
        return medicalExpenseRepository.findByOwnerAndServiceDateBetweenAndActiveStatusTrue(owner, startDate, endDate)
    }

    fun findMedicalExpensesByAccountIdAndDateRange(
        accountId: Long,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<MedicalExpense> {
        val owner = TenantContext.getCurrentOwner()
        logger.debug("Finding medical expenses by account ID: $accountId and date range: $startDate to $endDate")
        return medicalExpenseRepository.findByOwnerAndAccountIdAndServiceDateBetween(owner, accountId, startDate, endDate)
    }

    fun findMedicalExpensesByProviderId(providerId: Long): List<MedicalExpense> {
        val owner = TenantContext.getCurrentOwner()
        logger.debug("Finding medical expenses by provider ID: $providerId")
        return medicalExpenseRepository.findByOwnerAndProviderIdAndActiveStatusTrue(owner, providerId)
    }

    fun findMedicalExpensesByFamilyMemberId(familyMemberId: Long): List<MedicalExpense> {
        val owner = TenantContext.getCurrentOwner()
        logger.debug("Finding medical expenses by family member ID: $familyMemberId")
        return medicalExpenseRepository.findByOwnerAndFamilyMemberIdAndActiveStatusTrue(owner, familyMemberId)
    }

    fun findMedicalExpensesByFamilyMemberAndDateRange(
        familyMemberId: Long,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<MedicalExpense> {
        val owner = TenantContext.getCurrentOwner()
        logger.debug("Finding medical expenses by family member ID: $familyMemberId and date range: $startDate to $endDate")
        return medicalExpenseRepository.findByOwnerAndFamilyMemberIdAndServiceDateBetween(owner, familyMemberId, startDate, endDate)
    }

    fun findMedicalExpensesByClaimStatus(claimStatus: ClaimStatus): List<MedicalExpense> {
        val owner = TenantContext.getCurrentOwner()
        logger.debug("Finding medical expenses by claim status: $claimStatus")
        return medicalExpenseRepository.findByOwnerAndClaimStatusAndActiveStatusTrue(owner, claimStatus)
    }

    fun findOutOfNetworkExpenses(): List<MedicalExpense> {
        val owner = TenantContext.getCurrentOwner()
        logger.debug("Finding out-of-network medical expenses")
        return medicalExpenseRepository.findByOwnerAndIsOutOfNetworkAndActiveStatusTrue(owner, true)
    }

    fun findOutstandingPatientBalances(): List<MedicalExpense> {
        val owner = TenantContext.getCurrentOwner()
        logger.debug("Finding outstanding patient balances")
        return medicalExpenseRepository.findOutstandingPatientBalancesByOwner(owner)
    }

    fun findActiveOpenClaims(): List<MedicalExpense> {
        val owner = TenantContext.getCurrentOwner()
        logger.debug("Finding active open claims")
        return medicalExpenseRepository.findActiveOpenClaimsByOwner(owner)
    }

    fun updateClaimStatus(
        medicalExpenseId: Long,
        claimStatus: ClaimStatus,
    ): Boolean {
        val owner = TenantContext.getCurrentOwner()
        logger.info("Updating claim status for medical expense ID: $medicalExpenseId to: $claimStatus")

        return try {
            val updatedRows = medicalExpenseRepository.updateClaimStatusByOwner(owner, medicalExpenseId, claimStatus)
            val success = updatedRows > 0
            if (success) {
                logger.info("Successfully updated claim status for medical expense ID: $medicalExpenseId")
            } else {
                logger.warn("No rows updated for medical expense ID: $medicalExpenseId")
            }
            success
        } catch (e: Exception) {
            logger.error("Error updating claim status for medical expense ID: $medicalExpenseId", e)
            throw e
        }
    }

    fun softDeleteMedicalExpense(medicalExpenseId: Long): Boolean {
        val owner = TenantContext.getCurrentOwner()
        logger.info("Soft deleting medical expense with ID: $medicalExpenseId")

        return try {
            val updatedRows = medicalExpenseRepository.softDeleteByOwnerAndMedicalExpenseId(owner, medicalExpenseId)
            val success = updatedRows > 0
            if (success) {
                logger.info("Successfully soft deleted medical expense with ID: $medicalExpenseId")
            } else {
                logger.warn("No rows updated for soft delete of medical expense ID: $medicalExpenseId")
            }
            success
        } catch (e: Exception) {
            logger.error("Error soft deleting medical expense with ID: $medicalExpenseId", e)
            throw e
        }
    }

    fun getTotalBilledAmountByYear(year: Int): BigDecimal {
        val owner = TenantContext.getCurrentOwner()
        logger.debug("Getting total billed amount for year: $year")
        return medicalExpenseRepository.getTotalBilledAmountByOwnerAndYear(owner, year) ?: BigDecimal.ZERO
    }

    fun getTotalPatientResponsibilityByYear(year: Int): BigDecimal {
        val owner = TenantContext.getCurrentOwner()
        logger.debug("Getting total patient responsibility for year: $year")
        return medicalExpenseRepository.getTotalPatientResponsibilityByOwnerAndYear(owner, year) ?: BigDecimal.ZERO
    }

    fun getTotalInsurancePaidByYear(year: Int): BigDecimal {
        val owner = TenantContext.getCurrentOwner()
        logger.debug("Getting total insurance paid for year: $year")
        return medicalExpenseRepository.getTotalInsurancePaidByOwnerAndYear(owner, year) ?: BigDecimal.ZERO
    }

    fun getClaimStatusCounts(): Map<ClaimStatus, Long> {
        val owner = TenantContext.getCurrentOwner()
        logger.debug("Getting claim status counts")

        return ClaimStatus.values().associateWith { status ->
            medicalExpenseRepository.countByOwnerAndClaimStatusAndActiveStatusTrue(owner, status)
        }
    }

    fun findMedicalExpensesByProcedureCode(procedureCode: String): List<MedicalExpense> {
        val owner = TenantContext.getCurrentOwner()
        logger.debug("Finding medical expenses by procedure code: $procedureCode")
        return medicalExpenseRepository.findByOwnerAndProcedureCodeAndActiveStatusTrue(owner, procedureCode)
    }

    fun findMedicalExpensesByDiagnosisCode(diagnosisCode: String): List<MedicalExpense> {
        val owner = TenantContext.getCurrentOwner()
        logger.debug("Finding medical expenses by diagnosis code: $diagnosisCode")
        return medicalExpenseRepository.findByOwnerAndDiagnosisCodeAndActiveStatusTrue(owner, diagnosisCode)
    }

    // New payment-related methods for Phase 2.5
    fun linkPaymentTransaction(
        medicalExpenseId: Long,
        transactionId: Long,
    ): MedicalExpense {
        val owner = TenantContext.getCurrentOwner()
        logger.info("Linking payment transaction $transactionId to medical expense $medicalExpenseId")

        val medicalExpense =
            medicalExpenseRepository.findByOwnerAndMedicalExpenseIdAndActiveStatusTrue(owner, medicalExpenseId)
                ?: throw IllegalArgumentException("Medical expense not found with ID: $medicalExpenseId")

        // Check if transaction already linked to another medical expense
        val existingExpense = medicalExpenseRepository.findByOwnerAndTransactionId(owner, transactionId)
        if (existingExpense != null && existingExpense.medicalExpenseId != medicalExpenseId) {
            throw DuplicateMedicalExpenseException("Transaction $transactionId is already linked to medical expense ${existingExpense.medicalExpenseId}")
        }

        try {
            medicalExpense.transactionId = transactionId
            val savedExpense = medicalExpenseRepository.save(medicalExpense)
            logger.info("Successfully linked transaction $transactionId to medical expense $medicalExpenseId")
            return savedExpense
        } catch (e: Exception) {
            logger.error("Error linking transaction $transactionId to medical expense $medicalExpenseId", e)
            throw e
        }
    }

    fun unlinkPaymentTransaction(medicalExpenseId: Long): MedicalExpense {
        val owner = TenantContext.getCurrentOwner()
        logger.info("Unlinking payment transaction from medical expense $medicalExpenseId")

        val medicalExpense =
            medicalExpenseRepository.findByOwnerAndMedicalExpenseIdAndActiveStatusTrue(owner, medicalExpenseId)
                ?: throw IllegalArgumentException("Medical expense not found with ID: $medicalExpenseId")

        try {
            medicalExpense.transactionId = null
            medicalExpense.paidAmount = BigDecimal.ZERO
            val savedExpense = medicalExpenseRepository.save(medicalExpense)
            logger.info("Successfully unlinked payment transaction from medical expense $medicalExpenseId")
            return savedExpense
        } catch (e: Exception) {
            logger.error("Error unlinking payment transaction from medical expense $medicalExpenseId", e)
            throw e
        }
    }

    fun updatePaidAmount(medicalExpenseId: Long): MedicalExpense {
        val owner = TenantContext.getCurrentOwner()
        logger.info("Updating paid amount for medical expense $medicalExpenseId")

        val medicalExpense =
            medicalExpenseRepository.findByOwnerAndMedicalExpenseIdAndActiveStatusTrue(owner, medicalExpenseId)
                ?: throw IllegalArgumentException("Medical expense not found with ID: $medicalExpenseId")

        try {
            medicalExpense.transactionId?.let { transactionId ->
                logger.debug("Transaction ID exists: $transactionId, paid amount will be synced externally")
            } ?: run {
                // If no transaction linked, ensure paid amount is zero
                medicalExpense.paidAmount = BigDecimal.ZERO
                logger.debug("No transaction linked, setting paid amount to zero")
            }

            val savedExpense = medicalExpenseRepository.save(medicalExpense)
            logger.info("Successfully updated paid amount for medical expense $medicalExpenseId")
            return savedExpense
        } catch (e: Exception) {
            logger.error("Error updating paid amount for medical expense $medicalExpenseId", e)
            throw e
        }
    }

    fun findUnpaidMedicalExpenses(): List<MedicalExpense> {
        val owner = TenantContext.getCurrentOwner()
        logger.debug("Finding unpaid medical expenses")
        return medicalExpenseRepository.findUnpaidMedicalExpensesByOwner(owner)
    }

    fun findPartiallyPaidMedicalExpenses(): List<MedicalExpense> {
        val owner = TenantContext.getCurrentOwner()
        logger.debug("Finding partially paid medical expenses")
        return medicalExpenseRepository.findPartiallyPaidMedicalExpensesByOwner(owner)
    }

    fun findFullyPaidMedicalExpenses(): List<MedicalExpense> {
        val owner = TenantContext.getCurrentOwner()
        logger.debug("Finding fully paid medical expenses")
        return medicalExpenseRepository.findFullyPaidMedicalExpensesByOwner(owner)
    }

    fun findMedicalExpensesWithoutTransaction(): List<MedicalExpense> {
        val owner = TenantContext.getCurrentOwner()
        logger.debug("Finding medical expenses without linked transactions")
        return medicalExpenseRepository.findMedicalExpensesWithoutTransactionByOwner(owner)
    }

    fun findOverpaidMedicalExpenses(): List<MedicalExpense> {
        val owner = TenantContext.getCurrentOwner()
        logger.debug("Finding overpaid medical expenses")
        return medicalExpenseRepository.findOverpaidMedicalExpensesByOwner(owner)
    }

    fun getTotalPaidAmountByYear(year: Int): BigDecimal {
        val owner = TenantContext.getCurrentOwner()
        logger.debug("Getting total paid amount for year: $year")
        return medicalExpenseRepository.getTotalPaidAmountByOwnerAndYear(owner, year) ?: BigDecimal.ZERO
    }

    fun getTotalUnpaidBalance(): BigDecimal {
        val owner = TenantContext.getCurrentOwner()
        logger.debug("Getting total unpaid balance")
        return medicalExpenseRepository.getTotalUnpaidBalanceByOwner(owner) ?: BigDecimal.ZERO
    }
}
