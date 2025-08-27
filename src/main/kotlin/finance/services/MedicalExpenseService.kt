package finance.services

import finance.domain.ClaimStatus
import finance.domain.MedicalExpense
import finance.exceptions.DuplicateMedicalExpenseException
import finance.repositories.MedicalExpenseRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.sql.Date

@Service
open class MedicalExpenseService(
    private val medicalExpenseRepository: MedicalExpenseRepository
) : IMedicalExpenseService, BaseService() {

    private val logger = LoggerFactory.getLogger(MedicalExpenseService::class.java)
    
    init {
        logger.info("★★★ MedicalExpenseService constructor called! Repository: $medicalExpenseRepository")
    }

    override fun insertMedicalExpense(medicalExpense: MedicalExpense): MedicalExpense {
        logger.info("Inserting medical expense for transaction ID: ${medicalExpense.transactionId}")

        // Reinstate duplicate check now that repository is stable
        if (medicalExpense.transactionId > 0) {
            val existingExpense = medicalExpenseRepository.findByTransactionId(medicalExpense.transactionId)
            if (existingExpense != null) {
                throw DuplicateMedicalExpenseException("Medical expense already exists for transaction ID: ${medicalExpense.transactionId}")
            }
        }

        try {
            val savedExpense = medicalExpenseRepository.save(medicalExpense)
            logger.info("Successfully inserted medical expense with ID: ${savedExpense.medicalExpenseId}")
            return savedExpense
        } catch (e: Exception) {
            logger.error("Error inserting medical expense for transaction ID: ${medicalExpense.transactionId}", e)
            throw e
        }
    }

    override fun updateMedicalExpense(medicalExpense: MedicalExpense): MedicalExpense {
        logger.info("Updating medical expense with ID: ${medicalExpense.medicalExpenseId}")
        
        val existingExpense = medicalExpenseRepository.findByMedicalExpenseIdAndActiveStatusTrue(medicalExpense.medicalExpenseId)
            ?: throw IllegalArgumentException("Medical expense not found with ID: ${medicalExpense.medicalExpenseId}")

        try {
            val updatedExpense = medicalExpenseRepository.save(medicalExpense)
            logger.info("Successfully updated medical expense with ID: ${updatedExpense.medicalExpenseId}")
            return updatedExpense
        } catch (e: Exception) {
            logger.error("Error updating medical expense with ID: ${medicalExpense.medicalExpenseId}", e)
            throw e
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
        
        try {
            val updatedRows = medicalExpenseRepository.updateClaimStatus(medicalExpenseId, claimStatus)
            val success = updatedRows > 0
            if (success) {
                logger.info("Successfully updated claim status for medical expense ID: $medicalExpenseId")
            } else {
                logger.warn("No rows updated for medical expense ID: $medicalExpenseId")
            }
            return success
        } catch (e: Exception) {
            logger.error("Error updating claim status for medical expense ID: $medicalExpenseId", e)
            throw e
        }
    }

    override fun softDeleteMedicalExpense(medicalExpenseId: Long): Boolean {
        logger.info("Soft deleting medical expense with ID: $medicalExpenseId")
        
        try {
            val updatedRows = medicalExpenseRepository.softDeleteByMedicalExpenseId(medicalExpenseId)
            val success = updatedRows > 0
            if (success) {
                logger.info("Successfully soft deleted medical expense with ID: $medicalExpenseId")
            } else {
                logger.warn("No rows updated for soft delete of medical expense ID: $medicalExpenseId")
            }
            return success
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
}
