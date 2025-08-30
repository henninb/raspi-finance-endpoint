package finance.services

import finance.domain.ClaimStatus
import finance.domain.MedicalExpense
import java.math.BigDecimal
import java.sql.Date

interface IMedicalExpenseService {

    fun findAllMedicalExpenses(): List<MedicalExpense>

    fun insertMedicalExpense(medicalExpense: MedicalExpense): MedicalExpense

    fun updateMedicalExpense(medicalExpense: MedicalExpense): MedicalExpense

    fun findMedicalExpenseById(medicalExpenseId: Long): MedicalExpense?

    fun findMedicalExpenseByTransactionId(transactionId: Long): MedicalExpense?

    fun findMedicalExpensesByAccountId(accountId: Long): List<MedicalExpense>

    fun findMedicalExpensesByServiceDateRange(startDate: Date, endDate: Date): List<MedicalExpense>

    fun findMedicalExpensesByAccountIdAndDateRange(
        accountId: Long,
        startDate: Date,
        endDate: Date
    ): List<MedicalExpense>

    fun findMedicalExpensesByProviderId(providerId: Long): List<MedicalExpense>

    fun findMedicalExpensesByFamilyMemberId(familyMemberId: Long): List<MedicalExpense>

    fun findMedicalExpensesByFamilyMemberAndDateRange(
        familyMemberId: Long,
        startDate: Date,
        endDate: Date
    ): List<MedicalExpense>

    fun findMedicalExpensesByClaimStatus(claimStatus: ClaimStatus): List<MedicalExpense>

    fun findOutOfNetworkExpenses(): List<MedicalExpense>

    fun findOutstandingPatientBalances(): List<MedicalExpense>

    fun findActiveOpenClaims(): List<MedicalExpense>

    fun updateClaimStatus(medicalExpenseId: Long, claimStatus: ClaimStatus): Boolean

    fun softDeleteMedicalExpense(medicalExpenseId: Long): Boolean

    fun getTotalBilledAmountByYear(year: Int): BigDecimal

    fun getTotalPatientResponsibilityByYear(year: Int): BigDecimal

    fun getTotalInsurancePaidByYear(year: Int): BigDecimal

    fun getClaimStatusCounts(): Map<ClaimStatus, Long>

    fun findMedicalExpensesByProcedureCode(procedureCode: String): List<MedicalExpense>

    fun findMedicalExpensesByDiagnosisCode(diagnosisCode: String): List<MedicalExpense>
}