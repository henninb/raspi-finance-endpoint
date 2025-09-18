package finance.services

import finance.domain.ClaimStatus
import finance.domain.MedicalExpense
import finance.domain.ServiceResult
import finance.exceptions.DuplicateMedicalExpenseException
import finance.repositories.MedicalExpenseRepository
import jakarta.validation.ValidationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.sql.Date

/**
 * Standardized Medical Expense Service implementing ServiceResult pattern
 * Provides both new standardized methods and legacy compatibility
 */
@Service
@org.springframework.context.annotation.Primary
class StandardizedMedicalExpenseService(
    private val medicalExpenseRepository: MedicalExpenseRepository
) : StandardizedBaseService<MedicalExpense, Long>(), IMedicalExpenseService {

    override fun getEntityName(): String = "MedicalExpense"

    // ===== New Standardized ServiceResult Methods =====

    override fun findAllActive(): ServiceResult<List<MedicalExpense>> {
        return handleServiceOperation("findAllActive", null) {
            medicalExpenseRepository.findByActiveStatusTrueOrderByServiceDateDesc()
        }
    }

    override fun findById(id: Long): ServiceResult<MedicalExpense> {
        return handleServiceOperation("findById", id) {
            val medicalExpense = medicalExpenseRepository.findByMedicalExpenseIdAndActiveStatusTrue(id)
            medicalExpense ?: throw jakarta.persistence.EntityNotFoundException("MedicalExpense not found: $id")
        }
    }

    override fun save(entity: MedicalExpense): ServiceResult<MedicalExpense> {
        return handleServiceOperation("save", entity.medicalExpenseId) {
            val violations = validator.validate(entity)
            if (violations.isNotEmpty()) {
                throw jakarta.validation.ConstraintViolationException("Validation failed", violations)
            }

            // Check for duplicates if transactionId is provided
            entity.transactionId?.let { transactionId ->
                if (transactionId > 0) {
                    val existingExpense = medicalExpenseRepository.findByTransactionId(transactionId)
                    if (existingExpense != null) {
                        throw DataIntegrityViolationException("Medical expense already exists for transaction ID: $transactionId")
                    }
                }
            }

            medicalExpenseRepository.save(entity)
        }
    }

    override fun update(entity: MedicalExpense): ServiceResult<MedicalExpense> {
        return handleServiceOperation("update", entity.medicalExpenseId) {
            val existingExpense = medicalExpenseRepository.findByMedicalExpenseIdAndActiveStatusTrue(entity.medicalExpenseId!!)
                ?: throw jakarta.persistence.EntityNotFoundException("MedicalExpense not found: ${entity.medicalExpenseId}")

            medicalExpenseRepository.save(entity)
        }
    }

    override fun deleteById(id: Long): ServiceResult<Boolean> {
        return handleServiceOperation("deleteById", id) {
            val updatedRows = medicalExpenseRepository.softDeleteByMedicalExpenseId(id)
            if (updatedRows == 0) {
                throw jakarta.persistence.EntityNotFoundException("MedicalExpense not found: $id")
            }
            true
        }
    }

    // ===== Legacy Method Compatibility =====

    override fun findAllMedicalExpenses(): List<MedicalExpense> {
        val result = findAllActive()
        return when (result) {
            is ServiceResult.Success -> result.data
            else -> {
                logger.error("Error retrieving all medical expenses: $result")
                emptyList()
            }
        }
    }

    override fun insertMedicalExpense(medicalExpense: MedicalExpense): MedicalExpense {
        logger.info("Inserting medical expense for transaction ID: ${medicalExpense.transactionId}")

        // Check for duplicates if transactionId is provided
        medicalExpense.transactionId?.let { transactionId ->
            if (transactionId > 0) {
                val existingExpense = medicalExpenseRepository.findByTransactionId(transactionId)
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

    override fun updateMedicalExpense(medicalExpense: MedicalExpense): MedicalExpense {
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
            else -> {
                val message = "Failed to update medical expense: $result"
                logger.error(message)
                throw RuntimeException(message)
            }
        }
    }

    override fun findMedicalExpenseById(medicalExpenseId: Long): MedicalExpense? {
        logger.debug("Finding medical expense by ID: $medicalExpenseId")
        return medicalExpenseRepository.findByMedicalExpenseIdAndActiveStatusTrue(medicalExpenseId)
    }

    override fun findMedicalExpenseByTransactionId(transactionId: Long): MedicalExpense? {
        logger.debug("Finding medical expense by transaction ID: $transactionId")
        return medicalExpenseRepository.findByTransactionId(transactionId)
    }

    override fun findMedicalExpensesByAccountId(accountId: Long): List<MedicalExpense> {
        logger.debug("Finding medical expenses by account ID: $accountId")
        return medicalExpenseRepository.findByAccountId(accountId)
    }

    override fun findMedicalExpensesByServiceDateRange(startDate: Date, endDate: Date): List<MedicalExpense> {
        logger.debug("Finding medical expenses by service date range: $startDate to $endDate")
        return medicalExpenseRepository.findByServiceDateBetweenAndActiveStatusTrue(startDate, endDate)
    }

    override fun findMedicalExpensesByAccountIdAndDateRange(
        accountId: Long,
        startDate: Date,
        endDate: Date
    ): List<MedicalExpense> {
        logger.debug("Finding medical expenses by account ID: $accountId and date range: $startDate to $endDate")
        return medicalExpenseRepository.findByAccountIdAndServiceDateBetween(accountId, startDate, endDate)
    }

    override fun findMedicalExpensesByProviderId(providerId: Long): List<MedicalExpense> {
        logger.debug("Finding medical expenses by provider ID: $providerId")
        return medicalExpenseRepository.findByProviderIdAndActiveStatusTrue(providerId)
    }

    override fun findMedicalExpensesByFamilyMemberId(familyMemberId: Long): List<MedicalExpense> {
        logger.debug("Finding medical expenses by family member ID: $familyMemberId")
        return medicalExpenseRepository.findByFamilyMemberIdAndActiveStatusTrue(familyMemberId)
    }

    override fun findMedicalExpensesByFamilyMemberAndDateRange(
        familyMemberId: Long,
        startDate: Date,
        endDate: Date
    ): List<MedicalExpense> {
        logger.debug("Finding medical expenses by family member ID: $familyMemberId and date range: $startDate to $endDate")
        return medicalExpenseRepository.findByFamilyMemberIdAndServiceDateBetween(familyMemberId, startDate, endDate)
    }

    override fun findMedicalExpensesByClaimStatus(claimStatus: ClaimStatus): List<MedicalExpense> {
        logger.debug("Finding medical expenses by claim status: $claimStatus")
        return medicalExpenseRepository.findByClaimStatusAndActiveStatusTrue(claimStatus)
    }

    override fun findOutOfNetworkExpenses(): List<MedicalExpense> {
        logger.debug("Finding out-of-network medical expenses")
        return medicalExpenseRepository.findByIsOutOfNetworkAndActiveStatusTrue(true)
    }

    override fun findOutstandingPatientBalances(): List<MedicalExpense> {
        logger.debug("Finding outstanding patient balances")
        return medicalExpenseRepository.findOutstandingPatientBalances()
    }

    override fun findActiveOpenClaims(): List<MedicalExpense> {
        logger.debug("Finding active open claims")
        return medicalExpenseRepository.findActiveOpenClaims()
    }

    override fun updateClaimStatus(medicalExpenseId: Long, claimStatus: ClaimStatus): Boolean {
        logger.info("Updating claim status for medical expense ID: $medicalExpenseId to: $claimStatus")

        return try {
            val updatedRows = medicalExpenseRepository.updateClaimStatus(medicalExpenseId, claimStatus)
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

    override fun softDeleteMedicalExpense(medicalExpenseId: Long): Boolean {
        logger.info("Soft deleting medical expense with ID: $medicalExpenseId")

        return try {
            val updatedRows = medicalExpenseRepository.softDeleteByMedicalExpenseId(medicalExpenseId)
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

    override fun getTotalBilledAmountByYear(year: Int): BigDecimal {
        logger.debug("Getting total billed amount for year: $year")
        return medicalExpenseRepository.getTotalBilledAmountByYear(year) ?: BigDecimal.ZERO
    }

    override fun getTotalPatientResponsibilityByYear(year: Int): BigDecimal {
        logger.debug("Getting total patient responsibility for year: $year")
        return medicalExpenseRepository.getTotalPatientResponsibilityByYear(year) ?: BigDecimal.ZERO
    }

    override fun getTotalInsurancePaidByYear(year: Int): BigDecimal {
        logger.debug("Getting total insurance paid for year: $year")
        return medicalExpenseRepository.getTotalInsurancePaidByYear(year) ?: BigDecimal.ZERO
    }

    override fun getClaimStatusCounts(): Map<ClaimStatus, Long> {
        logger.debug("Getting claim status counts")

        return ClaimStatus.values().associateWith { status ->
            medicalExpenseRepository.countByClaimStatusAndActiveStatusTrue(status)
        }
    }

    override fun findMedicalExpensesByProcedureCode(procedureCode: String): List<MedicalExpense> {
        logger.debug("Finding medical expenses by procedure code: $procedureCode")
        return medicalExpenseRepository.findByProcedureCodeAndActiveStatusTrue(procedureCode)
    }

    override fun findMedicalExpensesByDiagnosisCode(diagnosisCode: String): List<MedicalExpense> {
        logger.debug("Finding medical expenses by diagnosis code: $diagnosisCode")
        return medicalExpenseRepository.findByDiagnosisCodeAndActiveStatusTrue(diagnosisCode)
    }

    // New payment-related methods for Phase 2.5
    override fun linkPaymentTransaction(medicalExpenseId: Long, transactionId: Long): MedicalExpense {
        logger.info("Linking payment transaction $transactionId to medical expense $medicalExpenseId")

        val medicalExpense = medicalExpenseRepository.findByMedicalExpenseIdAndActiveStatusTrue(medicalExpenseId)
            ?: throw IllegalArgumentException("Medical expense not found with ID: $medicalExpenseId")

        // Check if transaction already linked to another medical expense
        val existingExpense = medicalExpenseRepository.findByTransactionId(transactionId)
        if (existingExpense != null && existingExpense.medicalExpenseId != medicalExpenseId) {
            throw DuplicateMedicalExpenseException("Transaction $transactionId is already linked to medical expense ${existingExpense.medicalExpenseId}")
        }

        try {
            medicalExpense.transactionId = transactionId
            // Note: In a full implementation, we would fetch the transaction amount and set paidAmount
            // For now, we'll rely on the updatePaidAmount method to sync the amounts
            val savedExpense = medicalExpenseRepository.save(medicalExpense)
            logger.info("Successfully linked transaction $transactionId to medical expense $medicalExpenseId")
            return savedExpense
        } catch (e: Exception) {
            logger.error("Error linking transaction $transactionId to medical expense $medicalExpenseId", e)
            throw e
        }
    }

    override fun unlinkPaymentTransaction(medicalExpenseId: Long): MedicalExpense {
        logger.info("Unlinking payment transaction from medical expense $medicalExpenseId")

        val medicalExpense = medicalExpenseRepository.findByMedicalExpenseIdAndActiveStatusTrue(medicalExpenseId)
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

    override fun updatePaidAmount(medicalExpenseId: Long): MedicalExpense {
        logger.info("Updating paid amount for medical expense $medicalExpenseId")

        val medicalExpense = medicalExpenseRepository.findByMedicalExpenseIdAndActiveStatusTrue(medicalExpenseId)
            ?: throw IllegalArgumentException("Medical expense not found with ID: $medicalExpenseId")

        try {
            medicalExpense.transactionId?.let { transactionId ->
                // In a full implementation, we would fetch the transaction and sync the amount
                // For now, we'll rely on the service layer or controller to provide the amount
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

    override fun findUnpaidMedicalExpenses(): List<MedicalExpense> {
        logger.debug("Finding unpaid medical expenses")
        return medicalExpenseRepository.findUnpaidMedicalExpenses()
    }

    override fun findPartiallyPaidMedicalExpenses(): List<MedicalExpense> {
        logger.debug("Finding partially paid medical expenses")
        return medicalExpenseRepository.findPartiallyPaidMedicalExpenses()
    }

    override fun findFullyPaidMedicalExpenses(): List<MedicalExpense> {
        logger.debug("Finding fully paid medical expenses")
        return medicalExpenseRepository.findFullyPaidMedicalExpenses()
    }

    override fun findMedicalExpensesWithoutTransaction(): List<MedicalExpense> {
        logger.debug("Finding medical expenses without linked transactions")
        return medicalExpenseRepository.findMedicalExpensesWithoutTransaction()
    }

    override fun findOverpaidMedicalExpenses(): List<MedicalExpense> {
        logger.debug("Finding overpaid medical expenses")
        return medicalExpenseRepository.findOverpaidMedicalExpenses()
    }

    override fun getTotalPaidAmountByYear(year: Int): BigDecimal {
        logger.debug("Getting total paid amount for year: $year")
        return medicalExpenseRepository.getTotalPaidAmountByYear(year) ?: BigDecimal.ZERO
    }

    override fun getTotalUnpaidBalance(): BigDecimal {
        logger.debug("Getting total unpaid balance")
        return medicalExpenseRepository.getTotalUnpaidBalance() ?: BigDecimal.ZERO
    }
}