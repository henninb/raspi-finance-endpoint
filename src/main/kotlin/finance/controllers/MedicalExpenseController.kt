package finance.controllers

import finance.domain.ClaimStatus
import finance.domain.MedicalExpense
import finance.exceptions.DuplicateMedicalExpenseException
import finance.services.IMedicalExpenseService
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.orm.jpa.JpaSystemException
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.sql.Date

@CrossOrigin
@RestController
@RequestMapping("/api/medical-expenses")
open class MedicalExpenseController(private val medicalExpenseService: IMedicalExpenseService) : BaseController() {

    init {
        logger.info("★★★ MedicalExpenseController constructor called! Service: $medicalExpenseService")
    }

    @GetMapping
    fun getAllMedicalExpenses(): ResponseEntity<List<MedicalExpense>> {
        logger.info("GET /medical-expenses - Retrieving all medical expenses")

        return try {
            val medicalExpenses = medicalExpenseService.findAllMedicalExpenses()
            logger.info("Successfully retrieved ${medicalExpenses.size} medical expenses")
            ResponseEntity.ok(medicalExpenses)
        } catch (e: Exception) {
            logger.error("Error retrieving all medical expenses", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @PostMapping
    fun insertMedicalExpense(@Valid @RequestBody medicalExpense: MedicalExpense): ResponseEntity<MedicalExpense> {
        logger.info("POST /medical-expenses - Creating medical expense for transaction ID: ${medicalExpense.transactionId}")
        logger.info("Service instance: ${medicalExpenseService}")

        return try {
            val createdExpense = medicalExpenseService.insertMedicalExpense(medicalExpense)
            logger.info("Successfully created medical expense with ID: ${createdExpense.medicalExpenseId}")
            ResponseEntity.status(HttpStatus.CREATED).body(createdExpense)
        } catch (e: DuplicateMedicalExpenseException) {
            logger.warn("Duplicate medical expense attempted: ${e.message}")
            throw ResponseStatusException(HttpStatus.CONFLICT, "Duplicate medical expense found.")
        } catch (e: JpaSystemException) {
            logger.warn("JPA system exception for medical expense creation: ${e.message}")
            throw ResponseStatusException(HttpStatus.CONFLICT, "Duplicate medical expense found.")
        } catch (e: DataIntegrityViolationException) {
            logger.warn("Data integrity violation for medical expense creation: ${e.message}")
            throw ResponseStatusException(HttpStatus.CONFLICT, "Duplicate medical expense found.")
        } catch (e: ConstraintViolationException) {
            logger.warn("Constraint violation for medical expense creation: ${e.message}")
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid medical expense data: ${e.message}")
        } catch (e: IllegalArgumentException) {
            logger.warn("Bad request for medical expense creation: ${e.message}")
            ResponseEntity.badRequest().build()
        } catch (e: IllegalStateException) {
            logger.warn("Medical expense validation failed: ${e.message}")
            ResponseEntity.badRequest().build()
        } catch (e: Exception) {
            logger.error("Error creating medical expense - Exception type: ${e::class.qualifiedName}, Message: ${e.message}")
            // Log constraint violations specifically for debugging
            if (e is ConstraintViolationException) {
                logger.error("Constraint violations: ${e.constraintViolations.map { "${it.propertyPath}: ${it.message}" }}")
            }
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @PostMapping("/insert", consumes = ["application/json"], produces = ["application/json"])
    fun insertMedicalExpenseWithInsertEndpoint(@Valid @RequestBody medicalExpense: MedicalExpense): ResponseEntity<MedicalExpense> {
        logger.info("POST /medical-expenses/insert - Creating medical expense for transaction ID: ${medicalExpense.transactionId}")

        return try {
            val createdExpense = medicalExpenseService.insertMedicalExpense(medicalExpense)
            logger.info("Successfully created medical expense with ID: ${createdExpense.medicalExpenseId}")
            ResponseEntity.status(HttpStatus.CREATED).body(createdExpense)
        } catch (e: DuplicateMedicalExpenseException) {
            logger.warn("Duplicate medical expense attempted: ${e.message}")
            throw ResponseStatusException(HttpStatus.CONFLICT, "Duplicate medical expense found.")
        } catch (e: JpaSystemException) {
            logger.warn("JPA system exception for medical expense creation: ${e.message}")
            throw ResponseStatusException(HttpStatus.CONFLICT, "Duplicate medical expense found.")
        } catch (e: DataIntegrityViolationException) {
            logger.warn("Data integrity violation for medical expense creation: ${e.message}")
            throw ResponseStatusException(HttpStatus.CONFLICT, "Duplicate medical expense found.")
        } catch (e: ConstraintViolationException) {
            logger.warn("Constraint violation for medical expense creation: ${e.message}")
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid medical expense data: ${e.message}")
        } catch (e: IllegalArgumentException) {
            logger.warn("Bad request for medical expense creation: ${e.message}")
            ResponseEntity.badRequest().build()
        } catch (e: IllegalStateException) {
            logger.warn("Medical expense validation failed: ${e.message}")
            ResponseEntity.badRequest().build()
        } catch (e: Exception) {
            logger.error("Error creating medical expense - Exception type: ${e::class.qualifiedName}, Message: ${e.message}")
            // Log constraint violations specifically for debugging
            if (e is ConstraintViolationException) {
                logger.error("Constraint violations: ${e.constraintViolations.map { "${it.propertyPath}: ${it.message}" }}")
            }
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @PutMapping("/{medicalExpenseId}")
    fun updateMedicalExpense(
        @PathVariable @Min(1, message = "Medical expense ID must be positive") medicalExpenseId: Long,
        @Valid @RequestBody medicalExpense: MedicalExpense
    ): ResponseEntity<MedicalExpense> {
        logger.info("PUT /medical-expenses/$medicalExpenseId - Updating medical expense")

        return try {
            medicalExpense.medicalExpenseId = medicalExpenseId
            val updatedExpense = medicalExpenseService.updateMedicalExpense(medicalExpense)
            logger.info("Successfully updated medical expense with ID: $medicalExpenseId")
            ResponseEntity.ok(updatedExpense)
        } catch (e: IllegalArgumentException) {
            logger.warn("Bad request for medical expense update: ${e.message}")
            ResponseEntity.badRequest().build()
        } catch (e: Exception) {
            logger.error("Error updating medical expense with ID: $medicalExpenseId", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/{medicalExpenseId}")
    fun getMedicalExpenseById(
        @PathVariable @Min(1, message = "Medical expense ID must be positive") medicalExpenseId: Long
    ): ResponseEntity<MedicalExpense> {
        logger.info("GET /medical-expenses/$medicalExpenseId - Retrieving medical expense")

        return try {
            val medicalExpense = medicalExpenseService.findMedicalExpenseById(medicalExpenseId)
            if (medicalExpense != null) {
                ResponseEntity.ok(medicalExpense)
            } else {
                logger.info("Medical expense not found with ID: $medicalExpenseId")
                ResponseEntity.notFound().build()
            }
        } catch (e: Exception) {
            logger.error("Error retrieving medical expense with ID: $medicalExpenseId", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/transaction/{transactionId}")
    fun getMedicalExpenseByTransactionId(
        @PathVariable @Min(1, message = "Transaction ID must be positive") transactionId: Long
    ): ResponseEntity<MedicalExpense> {
        logger.info("GET /medical-expenses/transaction/$transactionId - Retrieving medical expense by transaction ID")

        return try {
            val medicalExpense = medicalExpenseService.findMedicalExpenseByTransactionId(transactionId)
            if (medicalExpense != null) {
                ResponseEntity.ok(medicalExpense)
            } else {
                logger.info("Medical expense not found for transaction ID: $transactionId")
                ResponseEntity.notFound().build()
            }
        } catch (e: Exception) {
            logger.error("Error retrieving medical expense for transaction ID: $transactionId", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/account/{accountId}")
    fun getMedicalExpensesByAccountId(
        @PathVariable @Min(1, message = "Account ID must be positive") accountId: Long
    ): ResponseEntity<List<MedicalExpense>> {
        logger.info("GET /medical-expenses/account/$accountId - Retrieving medical expenses by account ID")

        return try {
            val medicalExpenses = medicalExpenseService.findMedicalExpensesByAccountId(accountId)
            ResponseEntity.ok(medicalExpenses)
        } catch (e: Exception) {
            logger.error("Error retrieving medical expenses for account ID: $accountId", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/account/{accountId}/date-range")
    fun getMedicalExpensesByAccountIdAndDateRange(
        @PathVariable @Min(1, message = "Account ID must be positive") accountId: Long,
        @RequestParam @NotNull @DateTimeFormat(pattern = "yyyy-MM-dd") startDate: Date,
        @RequestParam @NotNull @DateTimeFormat(pattern = "yyyy-MM-dd") endDate: Date
    ): ResponseEntity<List<MedicalExpense>> {
        logger.info("GET /medical-expenses/account/$accountId/date-range - Retrieving medical expenses by account and date range")

        return try {
            val medicalExpenses = medicalExpenseService.findMedicalExpensesByAccountIdAndDateRange(accountId, startDate, endDate)
            ResponseEntity.ok(medicalExpenses)
        } catch (e: Exception) {
            logger.error("Error retrieving medical expenses for account ID: $accountId and date range", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/provider/{providerId}")
    fun getMedicalExpensesByProviderId(
        @PathVariable @Min(1, message = "Provider ID must be positive") providerId: Long
    ): ResponseEntity<List<MedicalExpense>> {
        logger.info("GET /medical-expenses/provider/$providerId - Retrieving medical expenses by provider ID")

        return try {
            val medicalExpenses = medicalExpenseService.findMedicalExpensesByProviderId(providerId)
            ResponseEntity.ok(medicalExpenses)
        } catch (e: Exception) {
            logger.error("Error retrieving medical expenses for provider ID: $providerId", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/family-member/{familyMemberId}")
    fun getMedicalExpensesByFamilyMemberId(
        @PathVariable @Min(1, message = "Family member ID must be positive") familyMemberId: Long
    ): ResponseEntity<List<MedicalExpense>> {
        logger.info("GET /medical-expenses/family-member/$familyMemberId - Retrieving medical expenses by family member ID")

        return try {
            val medicalExpenses = medicalExpenseService.findMedicalExpensesByFamilyMemberId(familyMemberId)
            ResponseEntity.ok(medicalExpenses)
        } catch (e: Exception) {
            logger.error("Error retrieving medical expenses for family member ID: $familyMemberId", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/family-member/{familyMemberId}/date-range")
    fun getMedicalExpensesByFamilyMemberAndDateRange(
        @PathVariable @Min(1, message = "Family member ID must be positive") familyMemberId: Long,
        @RequestParam @NotNull @DateTimeFormat(pattern = "yyyy-MM-dd") startDate: Date,
        @RequestParam @NotNull @DateTimeFormat(pattern = "yyyy-MM-dd") endDate: Date
    ): ResponseEntity<List<MedicalExpense>> {
        logger.info("GET /medical-expenses/family-member/$familyMemberId/date-range - Retrieving medical expenses by family member and date range")

        return try {
            val medicalExpenses = medicalExpenseService.findMedicalExpensesByFamilyMemberAndDateRange(familyMemberId, startDate, endDate)
            ResponseEntity.ok(medicalExpenses)
        } catch (e: Exception) {
            logger.error("Error retrieving medical expenses for family member ID: $familyMemberId and date range", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/claim-status/{claimStatus}")
    fun getMedicalExpensesByClaimStatus(
        @PathVariable claimStatus: ClaimStatus
    ): ResponseEntity<List<MedicalExpense>> {
        logger.info("GET /medical-expenses/claim-status/$claimStatus - Retrieving medical expenses by claim status")

        return try {
            val medicalExpenses = medicalExpenseService.findMedicalExpensesByClaimStatus(claimStatus)
            ResponseEntity.ok(medicalExpenses)
        } catch (e: Exception) {
            logger.error("Error retrieving medical expenses for claim status: $claimStatus", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/out-of-network")
    fun getOutOfNetworkExpenses(): ResponseEntity<List<MedicalExpense>> {
        logger.info("GET /medical-expenses/out-of-network - Retrieving out-of-network medical expenses")

        return try {
            val medicalExpenses = medicalExpenseService.findOutOfNetworkExpenses()
            ResponseEntity.ok(medicalExpenses)
        } catch (e: Exception) {
            logger.error("Error retrieving out-of-network medical expenses", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/outstanding-balances")
    fun getOutstandingPatientBalances(): ResponseEntity<List<MedicalExpense>> {
        logger.info("GET /medical-expenses/outstanding-balances - Retrieving outstanding patient balances")

        return try {
            val medicalExpenses = medicalExpenseService.findOutstandingPatientBalances()
            ResponseEntity.ok(medicalExpenses)
        } catch (e: Exception) {
            logger.error("Error retrieving outstanding patient balances", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/open-claims")
    fun getActiveOpenClaims(): ResponseEntity<List<MedicalExpense>> {
        logger.info("GET /medical-expenses/open-claims - Retrieving active open claims")

        return try {
            val medicalExpenses = medicalExpenseService.findActiveOpenClaims()
            ResponseEntity.ok(medicalExpenses)
        } catch (e: Exception) {
            logger.error("Error retrieving active open claims", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @PatchMapping("/{medicalExpenseId}/claim-status")
    fun updateClaimStatusPatch(
        @PathVariable @Min(1, message = "Medical expense ID must be positive") medicalExpenseId: Long,
        @RequestParam claimStatus: ClaimStatus
    ): ResponseEntity<Map<String, String>> {
        logger.info("PATCH /medical-expenses/$medicalExpenseId/claim-status - Updating claim status to: $claimStatus")
        return updateClaimStatusInternal(medicalExpenseId, claimStatus)
    }

    @PutMapping("/{medicalExpenseId}/claim-status")
    fun updateClaimStatusPut(
        @PathVariable @Min(1, message = "Medical expense ID must be positive") medicalExpenseId: Long,
        @RequestParam claimStatus: ClaimStatus
    ): ResponseEntity<Map<String, String>> {
        logger.info("PUT /medical-expenses/$medicalExpenseId/claim-status - Updating claim status to: $claimStatus")
        return updateClaimStatusInternal(medicalExpenseId, claimStatus)
    }

    private fun updateClaimStatusInternal(medicalExpenseId: Long, claimStatus: ClaimStatus): ResponseEntity<Map<String, String>> {
        return try {
            val success = medicalExpenseService.updateClaimStatus(medicalExpenseId, claimStatus)
            if (success) {
                ResponseEntity.ok(mapOf("message" to "Claim status updated successfully"))
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (e: Exception) {
            logger.error("Error updating claim status for medical expense ID: $medicalExpenseId", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @DeleteMapping("/{medicalExpenseId}")
    fun softDeleteMedicalExpense(
        @PathVariable @Min(1, message = "Medical expense ID must be positive") medicalExpenseId: Long
    ): ResponseEntity<Map<String, String>> {
        logger.info("DELETE /medical-expenses/$medicalExpenseId - Soft deleting medical expense")

        return try {
            val success = medicalExpenseService.softDeleteMedicalExpense(medicalExpenseId)
            if (success) {
                ResponseEntity.ok(mapOf("message" to "Medical expense deleted successfully"))
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (e: Exception) {
            logger.error("Error deleting medical expense with ID: $medicalExpenseId", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/totals/year/{year}")
    fun getMedicalTotalsByYear(
        @PathVariable @Min(2000, message = "Year must be 2000 or later") year: Int
    ): ResponseEntity<Map<String, BigDecimal>> {
        logger.info("GET /medical-expenses/totals/year/$year - Retrieving medical totals by year")

        return try {
            val totalBilled = medicalExpenseService.getTotalBilledAmountByYear(year)
            val totalPatientResponsibility = medicalExpenseService.getTotalPatientResponsibilityByYear(year)
            val totalInsurancePaid = medicalExpenseService.getTotalInsurancePaidByYear(year)

            val totals = mapOf(
                "totalBilled" to totalBilled,
                "totalPatientResponsibility" to totalPatientResponsibility,
                "totalInsurancePaid" to totalInsurancePaid,
                "totalCovered" to (totalInsurancePaid + (totalBilled - totalPatientResponsibility - totalInsurancePaid))
            )
            ResponseEntity.ok(totals)
        } catch (e: Exception) {
            logger.error("Error retrieving medical totals for year: $year", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/claim-status-counts")
    fun getClaimStatusCounts(): ResponseEntity<Map<ClaimStatus, Long>> {
        logger.info("GET /medical-expenses/claim-status-counts - Retrieving claim status counts")

        return try {
            val statusCounts = medicalExpenseService.getClaimStatusCounts()
            ResponseEntity.ok(statusCounts)
        } catch (e: Exception) {
            logger.error("Error retrieving claim status counts", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/procedure-code/{procedureCode}")
    fun getMedicalExpensesByProcedureCode(
        @PathVariable procedureCode: String
    ): ResponseEntity<List<MedicalExpense>> {
        logger.info("GET /medical-expenses/procedure-code/$procedureCode - Retrieving medical expenses by procedure code")

        return try {
            val medicalExpenses = medicalExpenseService.findMedicalExpensesByProcedureCode(procedureCode)
            ResponseEntity.ok(medicalExpenses)
        } catch (e: Exception) {
            logger.error("Error retrieving medical expenses for procedure code: $procedureCode", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/diagnosis-code/{diagnosisCode}")
    fun getMedicalExpensesByDiagnosisCode(
        @PathVariable diagnosisCode: String
    ): ResponseEntity<List<MedicalExpense>> {
        logger.info("GET /medical-expenses/diagnosis-code/$diagnosisCode - Retrieving medical expenses by diagnosis code")

        return try {
            val medicalExpenses = medicalExpenseService.findMedicalExpensesByDiagnosisCode(diagnosisCode)
            ResponseEntity.ok(medicalExpenses)
        } catch (e: Exception) {
            logger.error("Error retrieving medical expenses for diagnosis code: $diagnosisCode", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/date-range")
    fun getMedicalExpensesByDateRange(
        @RequestParam @NotNull @DateTimeFormat(pattern = "yyyy-MM-dd") startDate: Date,
        @RequestParam @NotNull @DateTimeFormat(pattern = "yyyy-MM-dd") endDate: Date
    ): ResponseEntity<List<MedicalExpense>> {
        logger.info("GET /medical-expenses/date-range - Retrieving medical expenses by date range")

        return try {
            val medicalExpenses = medicalExpenseService.findMedicalExpensesByServiceDateRange(startDate, endDate)
            ResponseEntity.ok(medicalExpenses)
        } catch (e: Exception) {
            logger.error("Error retrieving medical expenses for date range", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    // New payment-related endpoints for Phase 2.5
    @PostMapping("/{medicalExpenseId}/payments/{transactionId}")
    fun linkPaymentTransaction(
        @PathVariable @Min(1, message = "Medical expense ID must be positive") medicalExpenseId: Long,
        @PathVariable @Min(1, message = "Transaction ID must be positive") transactionId: Long
    ): ResponseEntity<MedicalExpense> {
        logger.info("POST /medical-expenses/$medicalExpenseId/payments/$transactionId - Linking payment transaction")

        return try {
            val updatedExpense = medicalExpenseService.linkPaymentTransaction(medicalExpenseId, transactionId)
            logger.info("Successfully linked transaction $transactionId to medical expense $medicalExpenseId")
            ResponseEntity.ok(updatedExpense)
        } catch (e: DuplicateMedicalExpenseException) {
            logger.warn("Duplicate transaction linkage attempted: ${e.message}")
            throw ResponseStatusException(HttpStatus.CONFLICT, e.message)
        } catch (e: IllegalArgumentException) {
            logger.warn("Bad request for transaction linkage: ${e.message}")
            ResponseEntity.badRequest().build()
        } catch (e: Exception) {
            logger.error("Error linking transaction $transactionId to medical expense $medicalExpenseId", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @DeleteMapping("/{medicalExpenseId}/payments")
    fun unlinkPaymentTransaction(
        @PathVariable @Min(1, message = "Medical expense ID must be positive") medicalExpenseId: Long
    ): ResponseEntity<MedicalExpense> {
        logger.info("DELETE /medical-expenses/$medicalExpenseId/payments - Unlinking payment transaction")

        return try {
            val updatedExpense = medicalExpenseService.unlinkPaymentTransaction(medicalExpenseId)
            logger.info("Successfully unlinked payment transaction from medical expense $medicalExpenseId")
            ResponseEntity.ok(updatedExpense)
        } catch (e: IllegalArgumentException) {
            logger.warn("Bad request for transaction unlinking: ${e.message}")
            ResponseEntity.badRequest().build()
        } catch (e: Exception) {
            logger.error("Error unlinking payment transaction from medical expense $medicalExpenseId", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @PutMapping("/{medicalExpenseId}/sync-payment")
    fun syncPaymentAmount(
        @PathVariable @Min(1, message = "Medical expense ID must be positive") medicalExpenseId: Long
    ): ResponseEntity<MedicalExpense> {
        logger.info("PUT /medical-expenses/$medicalExpenseId/sync-payment - Syncing payment amount")

        return try {
            val updatedExpense = medicalExpenseService.updatePaidAmount(medicalExpenseId)
            logger.info("Successfully synced payment amount for medical expense $medicalExpenseId")
            ResponseEntity.ok(updatedExpense)
        } catch (e: IllegalArgumentException) {
            logger.warn("Bad request for payment sync: ${e.message}")
            ResponseEntity.badRequest().build()
        } catch (e: Exception) {
            logger.error("Error syncing payment amount for medical expense $medicalExpenseId", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/unpaid")
    fun getUnpaidMedicalExpenses(): ResponseEntity<List<MedicalExpense>> {
        logger.info("GET /medical-expenses/unpaid - Retrieving unpaid medical expenses")

        return try {
            val medicalExpenses = medicalExpenseService.findUnpaidMedicalExpenses()
            logger.info("Successfully retrieved ${medicalExpenses.size} unpaid medical expenses")
            ResponseEntity.ok(medicalExpenses)
        } catch (e: Exception) {
            logger.error("Error retrieving unpaid medical expenses", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/partially-paid")
    fun getPartiallyPaidMedicalExpenses(): ResponseEntity<List<MedicalExpense>> {
        logger.info("GET /medical-expenses/partially-paid - Retrieving partially paid medical expenses")

        return try {
            val medicalExpenses = medicalExpenseService.findPartiallyPaidMedicalExpenses()
            logger.info("Successfully retrieved ${medicalExpenses.size} partially paid medical expenses")
            ResponseEntity.ok(medicalExpenses)
        } catch (e: Exception) {
            logger.error("Error retrieving partially paid medical expenses", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/fully-paid")
    fun getFullyPaidMedicalExpenses(): ResponseEntity<List<MedicalExpense>> {
        logger.info("GET /medical-expenses/fully-paid - Retrieving fully paid medical expenses")

        return try {
            val medicalExpenses = medicalExpenseService.findFullyPaidMedicalExpenses()
            logger.info("Successfully retrieved ${medicalExpenses.size} fully paid medical expenses")
            ResponseEntity.ok(medicalExpenses)
        } catch (e: Exception) {
            logger.error("Error retrieving fully paid medical expenses", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/without-transaction")
    fun getMedicalExpensesWithoutTransaction(): ResponseEntity<List<MedicalExpense>> {
        logger.info("GET /medical-expenses/without-transaction - Retrieving medical expenses without linked transactions")

        return try {
            val medicalExpenses = medicalExpenseService.findMedicalExpensesWithoutTransaction()
            logger.info("Successfully retrieved ${medicalExpenses.size} medical expenses without transactions")
            ResponseEntity.ok(medicalExpenses)
        } catch (e: Exception) {
            logger.error("Error retrieving medical expenses without transactions", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/overpaid")
    fun getOverpaidMedicalExpenses(): ResponseEntity<List<MedicalExpense>> {
        logger.info("GET /medical-expenses/overpaid - Retrieving overpaid medical expenses")

        return try {
            val medicalExpenses = medicalExpenseService.findOverpaidMedicalExpenses()
            logger.info("Successfully retrieved ${medicalExpenses.size} overpaid medical expenses")
            ResponseEntity.ok(medicalExpenses)
        } catch (e: Exception) {
            logger.error("Error retrieving overpaid medical expenses", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/totals/year/{year}/paid")
    fun getTotalPaidAmountByYear(
        @PathVariable @Min(2000, message = "Year must be 2000 or later") year: Int
    ): ResponseEntity<Map<String, BigDecimal>> {
        logger.info("GET /medical-expenses/totals/year/$year/paid - Retrieving total paid amount by year")

        return try {
            val totalPaid = medicalExpenseService.getTotalPaidAmountByYear(year)
            val response = mapOf("totalPaid" to totalPaid, "year" to BigDecimal(year))
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            logger.error("Error retrieving total paid amount for year: $year", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/totals/unpaid-balance")
    fun getTotalUnpaidBalance(): ResponseEntity<Map<String, BigDecimal>> {
        logger.info("GET /medical-expenses/totals/unpaid-balance - Retrieving total unpaid balance")

        return try {
            val totalUnpaid = medicalExpenseService.getTotalUnpaidBalance()
            val response = mapOf("totalUnpaidBalance" to totalUnpaid)
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            logger.error("Error retrieving total unpaid balance", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }
}
