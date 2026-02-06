package finance.repositories

import finance.domain.ClaimStatus
import finance.domain.MedicalExpense
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

@Repository
interface MedicalExpenseRepository : JpaRepository<MedicalExpense, Long> {
    fun findByActiveStatusTrueOrderByServiceDateDesc(): List<MedicalExpense>

    fun findByTransactionId(transactionId: Long): MedicalExpense?

    fun findByMedicalExpenseIdAndActiveStatusTrue(medicalExpenseId: Long): MedicalExpense?

    fun findByServiceDateBetween(
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<MedicalExpense>

    fun findByServiceDateBetweenAndActiveStatusTrue(
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<MedicalExpense>

    fun findByProviderId(providerId: Long?): List<MedicalExpense>

    fun findByProviderIdAndActiveStatusTrue(providerId: Long?): List<MedicalExpense>

    fun findByFamilyMemberId(familyMemberId: Long?): List<MedicalExpense>

    fun findByFamilyMemberIdAndActiveStatusTrue(familyMemberId: Long?): List<MedicalExpense>

    fun findByClaimStatus(claimStatus: ClaimStatus): List<MedicalExpense>

    fun findByClaimStatusAndActiveStatusTrue(claimStatus: ClaimStatus): List<MedicalExpense>

    fun findByIsOutOfNetwork(isOutOfNetwork: Boolean): List<MedicalExpense>

    fun findByIsOutOfNetworkAndActiveStatusTrue(isOutOfNetwork: Boolean): List<MedicalExpense>

    fun findByClaimNumber(claimNumber: String): MedicalExpense?

    fun findByClaimNumberAndActiveStatusTrue(claimNumber: String): MedicalExpense?

    @Query(
        """
        SELECT me FROM MedicalExpense me
        WHERE me.transactionId IN (
            SELECT t.transactionId FROM Transaction t WHERE t.accountId = :accountId
        )
        AND me.activeStatus = true
        ORDER BY me.serviceDate DESC
        """,
    )
    fun findByAccountId(
        @Param("accountId") accountId: Long,
    ): List<MedicalExpense>

    @Query(
        """
        SELECT me FROM MedicalExpense me
        WHERE me.transactionId IN (
            SELECT t.transactionId FROM Transaction t WHERE t.accountId = :accountId
        )
        AND me.serviceDate BETWEEN :startDate AND :endDate
        AND me.activeStatus = true
        ORDER BY me.serviceDate DESC
        """,
    )
    fun findByAccountIdAndServiceDateBetween(
        @Param("accountId") accountId: Long,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate,
    ): List<MedicalExpense>

    @Query(
        """
        SELECT SUM(me.billedAmount) FROM MedicalExpense me
        WHERE EXTRACT(YEAR FROM me.serviceDate) = :year
        AND me.activeStatus = true
        """,
    )
    fun getTotalBilledAmountByYear(
        @Param("year") year: Int,
    ): BigDecimal?

    @Query(
        """
        SELECT SUM(me.patientResponsibility) FROM MedicalExpense me
        WHERE EXTRACT(YEAR FROM me.serviceDate) = :year
        AND me.activeStatus = true
        """,
    )
    fun getTotalPatientResponsibilityByYear(
        @Param("year") year: Int,
    ): BigDecimal?

    @Query(
        """
        SELECT SUM(me.insurancePaid) FROM MedicalExpense me
        WHERE EXTRACT(YEAR FROM me.serviceDate) = :year
        AND me.activeStatus = true
        """,
    )
    fun getTotalInsurancePaidByYear(
        @Param("year") year: Int,
    ): BigDecimal?

    @Query(
        """
        SELECT me FROM MedicalExpense me
        WHERE me.familyMemberId = :familyMemberId
        AND me.serviceDate BETWEEN :startDate AND :endDate
        AND me.activeStatus = true
        ORDER BY me.serviceDate DESC
        """,
    )
    fun findByFamilyMemberIdAndServiceDateBetween(
        @Param("familyMemberId") familyMemberId: Long?,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate,
    ): List<MedicalExpense>

    @Query(
        """
        SELECT COUNT(*) FROM MedicalExpense me
        WHERE me.claimStatus = :claimStatus
        AND me.activeStatus = true
        """,
    )
    fun countByClaimStatusAndActiveStatusTrue(
        @Param("claimStatus") claimStatus: ClaimStatus,
    ): Long

    @Query(
        """
        SELECT me FROM MedicalExpense me
        WHERE me.patientResponsibility > 0
        AND me.paidDate IS NULL
        AND me.activeStatus = true
        ORDER BY me.serviceDate DESC
        """,
    )
    fun findOutstandingPatientBalances(): List<MedicalExpense>

    @Query(
        """
        SELECT me FROM MedicalExpense me
        WHERE me.claimStatus NOT IN ('paid', 'closed', 'denied')
        AND me.activeStatus = true
        ORDER BY me.serviceDate DESC
        """,
    )
    fun findActiveOpenClaims(): List<MedicalExpense>

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(
        """
        UPDATE MedicalExpense me
        SET me.activeStatus = false, me.dateUpdated = CURRENT_TIMESTAMP
        WHERE me.medicalExpenseId = :medicalExpenseId
        """,
    )
    fun softDeleteByMedicalExpenseId(
        @Param("medicalExpenseId") medicalExpenseId: Long,
    ): Int

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(
        """
        UPDATE MedicalExpense me
        SET me.claimStatus = :claimStatus, me.dateUpdated = CURRENT_TIMESTAMP
        WHERE me.medicalExpenseId = :medicalExpenseId
        """,
    )
    fun updateClaimStatus(
        @Param("medicalExpenseId") medicalExpenseId: Long,
        @Param("claimStatus") claimStatus: ClaimStatus,
    ): Int

    @Query(
        """
        SELECT me FROM MedicalExpense me
        WHERE me.procedureCode = :procedureCode
        AND me.activeStatus = true
        ORDER BY me.serviceDate DESC
        """,
    )
    fun findByProcedureCodeAndActiveStatusTrue(
        @Param("procedureCode") procedureCode: String,
    ): List<MedicalExpense>

    @Query(
        """
        SELECT me FROM MedicalExpense me
        WHERE me.diagnosisCode = :diagnosisCode
        AND me.activeStatus = true
        ORDER BY me.serviceDate DESC
        """,
    )
    fun findByDiagnosisCodeAndActiveStatusTrue(
        @Param("diagnosisCode") diagnosisCode: String,
    ): List<MedicalExpense>

    // New payment-related queries for Phase 2.5
    @Query(
        """
        SELECT me FROM MedicalExpense me
        WHERE me.paidAmount = 0
        AND me.activeStatus = true
        ORDER BY me.serviceDate DESC
        """,
    )
    fun findUnpaidMedicalExpenses(): List<MedicalExpense>

    @Query(
        """
        SELECT me FROM MedicalExpense me
        WHERE me.paidAmount > 0
        AND me.paidAmount < me.patientResponsibility
        AND me.activeStatus = true
        ORDER BY me.serviceDate DESC
        """,
    )
    fun findPartiallyPaidMedicalExpenses(): List<MedicalExpense>

    @Query(
        """
        SELECT me FROM MedicalExpense me
        WHERE me.paidAmount >= me.patientResponsibility
        AND me.activeStatus = true
        ORDER BY me.serviceDate DESC
        """,
    )
    fun findFullyPaidMedicalExpenses(): List<MedicalExpense>

    @Query(
        """
        SELECT me FROM MedicalExpense me
        WHERE me.transactionId IS NULL
        AND me.activeStatus = true
        ORDER BY me.serviceDate DESC
        """,
    )
    fun findMedicalExpensesWithoutTransaction(): List<MedicalExpense>

    @Query(
        """
        SELECT me FROM MedicalExpense me
        WHERE me.paidAmount > me.patientResponsibility
        AND me.activeStatus = true
        ORDER BY me.serviceDate DESC
        """,
    )
    fun findOverpaidMedicalExpenses(): List<MedicalExpense>

    @Query(
        """
        SELECT SUM(me.paidAmount) FROM MedicalExpense me
        WHERE EXTRACT(YEAR FROM me.serviceDate) = :year
        AND me.activeStatus = true
        """,
    )
    fun getTotalPaidAmountByYear(
        @Param("year") year: Int,
    ): BigDecimal?

    @Query(
        """
        SELECT SUM(me.patientResponsibility - me.paidAmount) FROM MedicalExpense me
        WHERE me.paidAmount < me.patientResponsibility
        AND me.activeStatus = true
        """,
    )
    fun getTotalUnpaidBalance(): BigDecimal?

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(
        """
        UPDATE MedicalExpense me
        SET me.paidAmount = :paidAmount, me.dateUpdated = CURRENT_TIMESTAMP
        WHERE me.medicalExpenseId = :medicalExpenseId
        """,
    )
    fun updatePaidAmount(
        @Param("medicalExpenseId") medicalExpenseId: Long,
        @Param("paidAmount") paidAmount: BigDecimal,
    ): Int

    // --- Owner-scoped methods for multi-tenancy (Phase 4) ---

    fun findByOwnerAndActiveStatusTrueOrderByServiceDateDesc(
        owner: String,
    ): List<MedicalExpense>

    fun findByOwnerAndTransactionId(
        owner: String,
        transactionId: Long,
    ): MedicalExpense?

    fun findByOwnerAndMedicalExpenseIdAndActiveStatusTrue(
        owner: String,
        medicalExpenseId: Long,
    ): MedicalExpense?

    fun findByOwnerAndServiceDateBetweenAndActiveStatusTrue(
        owner: String,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<MedicalExpense>

    fun findByOwnerAndProviderIdAndActiveStatusTrue(
        owner: String,
        providerId: Long?,
    ): List<MedicalExpense>

    fun findByOwnerAndFamilyMemberIdAndActiveStatusTrue(
        owner: String,
        familyMemberId: Long?,
    ): List<MedicalExpense>

    fun findByOwnerAndClaimStatusAndActiveStatusTrue(
        owner: String,
        claimStatus: ClaimStatus,
    ): List<MedicalExpense>

    fun findByOwnerAndIsOutOfNetworkAndActiveStatusTrue(
        owner: String,
        isOutOfNetwork: Boolean,
    ): List<MedicalExpense>

    fun findByOwnerAndClaimNumberAndActiveStatusTrue(
        owner: String,
        claimNumber: String,
    ): MedicalExpense?

    @Query(
        """
        SELECT me FROM MedicalExpense me
        WHERE me.owner = :owner
        AND me.transactionId IN (
            SELECT t.transactionId FROM Transaction t WHERE t.accountId = :accountId AND t.owner = :owner
        )
        AND me.activeStatus = true
        ORDER BY me.serviceDate DESC
        """,
    )
    fun findByOwnerAndAccountId(
        @Param("owner") owner: String,
        @Param("accountId") accountId: Long,
    ): List<MedicalExpense>

    @Query(
        """
        SELECT me FROM MedicalExpense me
        WHERE me.owner = :owner
        AND me.transactionId IN (
            SELECT t.transactionId FROM Transaction t WHERE t.accountId = :accountId AND t.owner = :owner
        )
        AND me.serviceDate BETWEEN :startDate AND :endDate
        AND me.activeStatus = true
        ORDER BY me.serviceDate DESC
        """,
    )
    fun findByOwnerAndAccountIdAndServiceDateBetween(
        @Param("owner") owner: String,
        @Param("accountId") accountId: Long,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate,
    ): List<MedicalExpense>

    @Query(
        """
        SELECT SUM(me.billedAmount) FROM MedicalExpense me
        WHERE me.owner = :owner
        AND EXTRACT(YEAR FROM me.serviceDate) = :year
        AND me.activeStatus = true
        """,
    )
    fun getTotalBilledAmountByOwnerAndYear(
        @Param("owner") owner: String,
        @Param("year") year: Int,
    ): BigDecimal?

    @Query(
        """
        SELECT SUM(me.patientResponsibility) FROM MedicalExpense me
        WHERE me.owner = :owner
        AND EXTRACT(YEAR FROM me.serviceDate) = :year
        AND me.activeStatus = true
        """,
    )
    fun getTotalPatientResponsibilityByOwnerAndYear(
        @Param("owner") owner: String,
        @Param("year") year: Int,
    ): BigDecimal?

    @Query(
        """
        SELECT SUM(me.insurancePaid) FROM MedicalExpense me
        WHERE me.owner = :owner
        AND EXTRACT(YEAR FROM me.serviceDate) = :year
        AND me.activeStatus = true
        """,
    )
    fun getTotalInsurancePaidByOwnerAndYear(
        @Param("owner") owner: String,
        @Param("year") year: Int,
    ): BigDecimal?

    @Query(
        """
        SELECT me FROM MedicalExpense me
        WHERE me.owner = :owner
        AND me.familyMemberId = :familyMemberId
        AND me.serviceDate BETWEEN :startDate AND :endDate
        AND me.activeStatus = true
        ORDER BY me.serviceDate DESC
        """,
    )
    fun findByOwnerAndFamilyMemberIdAndServiceDateBetween(
        @Param("owner") owner: String,
        @Param("familyMemberId") familyMemberId: Long?,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate,
    ): List<MedicalExpense>

    @Query(
        """
        SELECT COUNT(*) FROM MedicalExpense me
        WHERE me.owner = :owner
        AND me.claimStatus = :claimStatus
        AND me.activeStatus = true
        """,
    )
    fun countByOwnerAndClaimStatusAndActiveStatusTrue(
        @Param("owner") owner: String,
        @Param("claimStatus") claimStatus: ClaimStatus,
    ): Long

    @Query(
        """
        SELECT me FROM MedicalExpense me
        WHERE me.owner = :owner
        AND me.patientResponsibility > 0
        AND me.paidDate IS NULL
        AND me.activeStatus = true
        ORDER BY me.serviceDate DESC
        """,
    )
    fun findOutstandingPatientBalancesByOwner(
        @Param("owner") owner: String,
    ): List<MedicalExpense>

    @Query(
        """
        SELECT me FROM MedicalExpense me
        WHERE me.owner = :owner
        AND me.claimStatus NOT IN ('paid', 'closed', 'denied')
        AND me.activeStatus = true
        ORDER BY me.serviceDate DESC
        """,
    )
    fun findActiveOpenClaimsByOwner(
        @Param("owner") owner: String,
    ): List<MedicalExpense>

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(
        """
        UPDATE MedicalExpense me
        SET me.activeStatus = false, me.dateUpdated = CURRENT_TIMESTAMP
        WHERE me.medicalExpenseId = :medicalExpenseId
        AND me.owner = :owner
        """,
    )
    fun softDeleteByOwnerAndMedicalExpenseId(
        @Param("owner") owner: String,
        @Param("medicalExpenseId") medicalExpenseId: Long,
    ): Int

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(
        """
        UPDATE MedicalExpense me
        SET me.claimStatus = :claimStatus, me.dateUpdated = CURRENT_TIMESTAMP
        WHERE me.medicalExpenseId = :medicalExpenseId
        AND me.owner = :owner
        """,
    )
    fun updateClaimStatusByOwner(
        @Param("owner") owner: String,
        @Param("medicalExpenseId") medicalExpenseId: Long,
        @Param("claimStatus") claimStatus: ClaimStatus,
    ): Int

    @Query(
        """
        SELECT me FROM MedicalExpense me
        WHERE me.owner = :owner
        AND me.procedureCode = :procedureCode
        AND me.activeStatus = true
        ORDER BY me.serviceDate DESC
        """,
    )
    fun findByOwnerAndProcedureCodeAndActiveStatusTrue(
        @Param("owner") owner: String,
        @Param("procedureCode") procedureCode: String,
    ): List<MedicalExpense>

    @Query(
        """
        SELECT me FROM MedicalExpense me
        WHERE me.owner = :owner
        AND me.diagnosisCode = :diagnosisCode
        AND me.activeStatus = true
        ORDER BY me.serviceDate DESC
        """,
    )
    fun findByOwnerAndDiagnosisCodeAndActiveStatusTrue(
        @Param("owner") owner: String,
        @Param("diagnosisCode") diagnosisCode: String,
    ): List<MedicalExpense>

    @Query(
        """
        SELECT me FROM MedicalExpense me
        WHERE me.owner = :owner
        AND me.paidAmount = 0
        AND me.activeStatus = true
        ORDER BY me.serviceDate DESC
        """,
    )
    fun findUnpaidMedicalExpensesByOwner(
        @Param("owner") owner: String,
    ): List<MedicalExpense>

    @Query(
        """
        SELECT me FROM MedicalExpense me
        WHERE me.owner = :owner
        AND me.paidAmount > 0
        AND me.paidAmount < me.patientResponsibility
        AND me.activeStatus = true
        ORDER BY me.serviceDate DESC
        """,
    )
    fun findPartiallyPaidMedicalExpensesByOwner(
        @Param("owner") owner: String,
    ): List<MedicalExpense>

    @Query(
        """
        SELECT me FROM MedicalExpense me
        WHERE me.owner = :owner
        AND me.paidAmount >= me.patientResponsibility
        AND me.activeStatus = true
        ORDER BY me.serviceDate DESC
        """,
    )
    fun findFullyPaidMedicalExpensesByOwner(
        @Param("owner") owner: String,
    ): List<MedicalExpense>

    @Query(
        """
        SELECT me FROM MedicalExpense me
        WHERE me.owner = :owner
        AND me.transactionId IS NULL
        AND me.activeStatus = true
        ORDER BY me.serviceDate DESC
        """,
    )
    fun findMedicalExpensesWithoutTransactionByOwner(
        @Param("owner") owner: String,
    ): List<MedicalExpense>

    @Query(
        """
        SELECT me FROM MedicalExpense me
        WHERE me.owner = :owner
        AND me.paidAmount > me.patientResponsibility
        AND me.activeStatus = true
        ORDER BY me.serviceDate DESC
        """,
    )
    fun findOverpaidMedicalExpensesByOwner(
        @Param("owner") owner: String,
    ): List<MedicalExpense>

    @Query(
        """
        SELECT SUM(me.paidAmount) FROM MedicalExpense me
        WHERE me.owner = :owner
        AND EXTRACT(YEAR FROM me.serviceDate) = :year
        AND me.activeStatus = true
        """,
    )
    fun getTotalPaidAmountByOwnerAndYear(
        @Param("owner") owner: String,
        @Param("year") year: Int,
    ): BigDecimal?

    @Query(
        """
        SELECT SUM(me.patientResponsibility - me.paidAmount) FROM MedicalExpense me
        WHERE me.owner = :owner
        AND me.paidAmount < me.patientResponsibility
        AND me.activeStatus = true
        """,
    )
    fun getTotalUnpaidBalanceByOwner(
        @Param("owner") owner: String,
    ): BigDecimal?

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(
        """
        UPDATE MedicalExpense me
        SET me.paidAmount = :paidAmount, me.dateUpdated = CURRENT_TIMESTAMP
        WHERE me.medicalExpenseId = :medicalExpenseId
        AND me.owner = :owner
        """,
    )
    fun updatePaidAmountByOwner(
        @Param("owner") owner: String,
        @Param("medicalExpenseId") medicalExpenseId: Long,
        @Param("paidAmount") paidAmount: BigDecimal,
    ): Int
}
