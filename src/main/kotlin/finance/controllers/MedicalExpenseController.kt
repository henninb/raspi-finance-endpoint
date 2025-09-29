package finance.controllers

import finance.domain.ClaimStatus
import finance.domain.MedicalExpense
import finance.domain.ServiceResult
import finance.exceptions.DuplicateMedicalExpenseException
import finance.services.StandardizedMedicalExpenseService
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.orm.jpa.JpaSystemException
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.sql.Date

@CrossOrigin
@RestController
@RequestMapping("/api/medical-expenses")
class MedicalExpenseController(private val standardizedMedicalExpenseService: StandardizedMedicalExpenseService) :
    StandardizedBaseController(), StandardRestController<MedicalExpense, Long> {
    init {
        logger.info("★★★ MedicalExpenseController constructor called! Service: $standardizedMedicalExpenseService")
    }

    // ===== STANDARDIZED ENDPOINTS (NEW) =====

    /**
     * Standardized collection retrieval - GET /api/medical-expenses/active
     * Returns active medical expenses using standardized patterns
     */
    @GetMapping("/active", produces = ["application/json"])
    override fun findAllActive(): ResponseEntity<List<MedicalExpense>> {
        return when (val result = standardizedMedicalExpenseService.findAllActive()) {
            is ServiceResult.Success -> {
                logger.info("Retrieved ${result.data.size} active medical expenses")
                ResponseEntity.ok(result.data)
            }
            is ServiceResult.NotFound -> {
                logger.warn("No medical expenses found")
                ResponseEntity.notFound().build()
            }
            is ServiceResult.SystemError -> {
                logger.error("System error retrieving medical expenses: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }
    }

    /**
     * Standardized single entity retrieval - GET /api/medical-expenses/{medicalExpenseId}
     * Uses camelCase parameter without @PathVariable annotation
     */
    @GetMapping("/{medicalExpenseId}", produces = ["application/json"])
    override fun findById(
        @PathVariable medicalExpenseId: Long,
    ): ResponseEntity<MedicalExpense> {
        return when (val result = standardizedMedicalExpenseService.findById(medicalExpenseId)) {
            is ServiceResult.Success -> {
                logger.info("Retrieved medical expense: $medicalExpenseId")
                ResponseEntity.ok(result.data)
            }
            is ServiceResult.NotFound -> {
                logger.warn("Medical expense not found: $medicalExpenseId")
                ResponseEntity.notFound().build()
            }
            is ServiceResult.SystemError -> {
                logger.error("System error retrieving medical expense $medicalExpenseId: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }
    }

    /**
     * Standardized entity creation - POST /api/medical-expenses
     * Returns 201 CREATED
     */
    @PostMapping(consumes = ["application/json"], produces = ["application/json"])
    override fun save(
        @Valid @RequestBody medicalExpense: MedicalExpense,
    ): ResponseEntity<MedicalExpense> {
        return when (val result = standardizedMedicalExpenseService.save(medicalExpense)) {
            is ServiceResult.Success -> {
                logger.info("Medical expense created successfully: ${result.data.medicalExpenseId}")
                ResponseEntity.status(HttpStatus.CREATED).body(result.data)
            }
            is ServiceResult.ValidationError -> {
                logger.warn("Validation error creating medical expense: ${result.errors}")
                ResponseEntity.badRequest().build<MedicalExpense>()
            }
            is ServiceResult.BusinessError -> {
                logger.warn("Business error creating medical expense: ${result.message}")
                ResponseEntity.status(HttpStatus.CONFLICT).build<MedicalExpense>()
            }
            is ServiceResult.SystemError -> {
                logger.error("System error creating medical expense: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build<MedicalExpense>()
            }
            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }
    }

    /**
     * Standardized entity update - PUT /api/medical-expenses/{medicalExpenseId}
     * Uses camelCase parameter without @PathVariable annotation
     */
    @PutMapping("/{medicalExpenseId}", consumes = ["application/json"], produces = ["application/json"])
    override fun update(
        @PathVariable medicalExpenseId: Long,
        @Valid @RequestBody medicalExpense: MedicalExpense,
    ): ResponseEntity<MedicalExpense> {
        // Ensure the ID matches the path parameter
        medicalExpense.medicalExpenseId = medicalExpenseId

        return when (val result = standardizedMedicalExpenseService.update(medicalExpense)) {
            is ServiceResult.Success -> {
                logger.info("Medical expense updated successfully: $medicalExpenseId")
                ResponseEntity.ok(result.data)
            }
            is ServiceResult.NotFound -> {
                logger.warn("Medical expense not found for update: $medicalExpenseId")
                ResponseEntity.notFound().build()
            }
            is ServiceResult.ValidationError -> {
                logger.warn("Validation error updating medical expense: ${result.errors}")
                ResponseEntity.badRequest().build<MedicalExpense>()
            }
            is ServiceResult.BusinessError -> {
                logger.warn("Business error updating medical expense: ${result.message}")
                ResponseEntity.status(HttpStatus.CONFLICT).build<MedicalExpense>()
            }
            is ServiceResult.SystemError -> {
                logger.error("System error updating medical expense $medicalExpenseId: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build<MedicalExpense>()
            }
            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }
    }

    /**
     * Standardized entity deletion - DELETE /api/medical-expenses/{medicalExpenseId}
     * Returns 200 OK with deleted entity (standardized behavior)
     */
    @DeleteMapping("/{medicalExpenseId}", produces = ["application/json"])
    override fun deleteById(
        @PathVariable medicalExpenseId: Long,
    ): ResponseEntity<MedicalExpense> {
        // First find the entity to return it after deletion
        val entityResult = standardizedMedicalExpenseService.findById(medicalExpenseId)
        if (entityResult !is ServiceResult.Success) {
            logger.warn("Medical expense not found for deletion: $medicalExpenseId")
            return ResponseEntity.notFound().build()
        }

        return when (val deleteResult = standardizedMedicalExpenseService.deleteById(medicalExpenseId)) {
            is ServiceResult.Success -> {
                logger.info("Medical expense deleted successfully: $medicalExpenseId")
                ResponseEntity.ok(entityResult.data)
            }
            is ServiceResult.NotFound -> {
                logger.warn("Medical expense not found for deletion: $medicalExpenseId")
                ResponseEntity.notFound().build()
            }
            is ServiceResult.SystemError -> {
                logger.error("System error deleting medical expense $medicalExpenseId: ${deleteResult.exception.message}", deleteResult.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
            else -> {
                logger.error("Unexpected result type: $deleteResult")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }
    }

    // ===== LEGACY ENDPOINTS (BACKWARD COMPATIBILITY) =====

    /**
     * Legacy collection endpoint - GET /api/medical-expenses/all
     * Original method name preserved for backward compatibility
     */
    @GetMapping("/all")
    fun getAllMedicalExpenses(): ResponseEntity<List<MedicalExpense>> {
        logger.info("GET /medical-expenses - Retrieving all medical expenses")

        return try {
            val medicalExpenses = standardizedMedicalExpenseService.findAllMedicalExpenses()
            logger.info("Successfully retrieved ${medicalExpenses.size} medical expenses")
            ResponseEntity.ok(medicalExpenses)
        } catch (e: Exception) {
            logger.error("Error retrieving all medical expenses", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    /**
     * Legacy CRUD endpoint - POST /api/medical-expenses/legacy
     * Original method name preserved for backward compatibility
     */
    @PostMapping("/legacy", consumes = ["application/json"], produces = ["application/json"])
    fun insertMedicalExpense(
        @Valid @RequestBody medicalExpense: MedicalExpense,
    ): ResponseEntity<MedicalExpense> {
        logger.info("POST /medical-expenses - Creating medical expense for transaction ID: ${medicalExpense.transactionId}")
        logger.info("Service instance: $standardizedMedicalExpenseService")

        return try {
            val createdExpense = standardizedMedicalExpenseService.insertMedicalExpense(medicalExpense)
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
    fun insertMedicalExpenseWithInsertEndpoint(
        @Valid @RequestBody medicalExpense: MedicalExpense,
    ): ResponseEntity<MedicalExpense> {
        logger.info("POST /medical-expenses/insert - Creating medical expense for transaction ID: ${medicalExpense.transactionId}")

        return try {
            val createdExpense = standardizedMedicalExpenseService.insertMedicalExpense(medicalExpense)
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

    /**
     * Legacy CRUD endpoint - PUT /api/medical-expenses/update/{medicalExpenseId}
     * Original method name preserved for backward compatibility
     */
    @PutMapping("/update/{medicalExpenseId}", consumes = ["application/json"], produces = ["application/json"])
    fun updateMedicalExpense(
        @PathVariable medicalExpenseId: Long,
        @Valid @RequestBody medicalExpense: MedicalExpense,
    ): ResponseEntity<MedicalExpense> {
        logger.info("PUT /medical-expenses/$medicalExpenseId - Updating medical expense")

        return try {
            medicalExpense.medicalExpenseId = medicalExpenseId
            val updatedExpense = standardizedMedicalExpenseService.updateMedicalExpense(medicalExpense)
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

    /**
     * Legacy CRUD endpoint - GET /api/medical-expenses/select/{medicalExpenseId}
     * Original method name preserved for backward compatibility
     */
    @GetMapping("/select/{medicalExpenseId}", produces = ["application/json"])
    fun getMedicalExpenseById(
        @PathVariable medicalExpenseId: Long,
    ): ResponseEntity<MedicalExpense> {
        logger.info("GET /medical-expenses/$medicalExpenseId - Retrieving medical expense")

        return try {
            val medicalExpense = standardizedMedicalExpenseService.findMedicalExpenseById(medicalExpenseId)
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
        @PathVariable transactionId: Long,
    ): ResponseEntity<MedicalExpense> {
        logger.info("GET /medical-expenses/transaction/$transactionId - Retrieving medical expense by transaction ID")

        return try {
            val medicalExpense = standardizedMedicalExpenseService.findMedicalExpenseByTransactionId(transactionId)
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
        @PathVariable accountId: Long,
    ): ResponseEntity<List<MedicalExpense>> {
        logger.info("GET /medical-expenses/account/$accountId - Retrieving medical expenses by account ID")

        return try {
            val medicalExpenses = standardizedMedicalExpenseService.findMedicalExpensesByAccountId(accountId)
            ResponseEntity.ok(medicalExpenses)
        } catch (e: Exception) {
            logger.error("Error retrieving medical expenses for account ID: $accountId", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/account/{accountId}/date-range")
    fun getMedicalExpensesByAccountIdAndDateRange(
        @PathVariable accountId: Long,
        @RequestParam @NotNull @DateTimeFormat(pattern = "yyyy-MM-dd") startDate: Date,
        @RequestParam @NotNull @DateTimeFormat(pattern = "yyyy-MM-dd") endDate: Date,
    ): ResponseEntity<List<MedicalExpense>> {
        logger.info("GET /medical-expenses/account/$accountId/date-range - Retrieving medical expenses by account and date range")

        return try {
            val medicalExpenses = standardizedMedicalExpenseService.findMedicalExpensesByAccountIdAndDateRange(accountId, startDate, endDate)
            ResponseEntity.ok(medicalExpenses)
        } catch (e: Exception) {
            logger.error("Error retrieving medical expenses for account ID: $accountId and date range", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/provider/{providerId}")
    fun getMedicalExpensesByProviderId(
        @PathVariable providerId: Long,
    ): ResponseEntity<List<MedicalExpense>> {
        logger.info("GET /medical-expenses/provider/$providerId - Retrieving medical expenses by provider ID")

        return try {
            val medicalExpenses = standardizedMedicalExpenseService.findMedicalExpensesByProviderId(providerId)
            ResponseEntity.ok(medicalExpenses)
        } catch (e: Exception) {
            logger.error("Error retrieving medical expenses for provider ID: $providerId", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/family-member/{familyMemberId}")
    fun getMedicalExpensesByFamilyMemberId(
        @PathVariable familyMemberId: Long,
    ): ResponseEntity<List<MedicalExpense>> {
        logger.info("GET /medical-expenses/family-member/$familyMemberId - Retrieving medical expenses by family member ID")

        return try {
            val medicalExpenses = standardizedMedicalExpenseService.findMedicalExpensesByFamilyMemberId(familyMemberId)
            ResponseEntity.ok(medicalExpenses)
        } catch (e: Exception) {
            logger.error("Error retrieving medical expenses for family member ID: $familyMemberId", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/family-member/{familyMemberId}/date-range")
    fun getMedicalExpensesByFamilyMemberAndDateRange(
        @PathVariable familyMemberId: Long,
        @RequestParam @NotNull @DateTimeFormat(pattern = "yyyy-MM-dd") startDate: Date,
        @RequestParam @NotNull @DateTimeFormat(pattern = "yyyy-MM-dd") endDate: Date,
    ): ResponseEntity<List<MedicalExpense>> {
        logger.info("GET /medical-expenses/family-member/$familyMemberId/date-range - Retrieving medical expenses by family member and date range")

        return try {
            val medicalExpenses = standardizedMedicalExpenseService.findMedicalExpensesByFamilyMemberAndDateRange(familyMemberId, startDate, endDate)
            ResponseEntity.ok(medicalExpenses)
        } catch (e: Exception) {
            logger.error("Error retrieving medical expenses for family member ID: $familyMemberId and date range", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/claim-status/{claimStatus}")
    fun getMedicalExpensesByClaimStatus(
        @PathVariable claimStatus: ClaimStatus,
    ): ResponseEntity<List<MedicalExpense>> {
        logger.info("GET /medical-expenses/claim-status/$claimStatus - Retrieving medical expenses by claim status")

        return try {
            val medicalExpenses = standardizedMedicalExpenseService.findMedicalExpensesByClaimStatus(claimStatus)
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
            val medicalExpenses = standardizedMedicalExpenseService.findOutOfNetworkExpenses()
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
            val medicalExpenses = standardizedMedicalExpenseService.findOutstandingPatientBalances()
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
            val medicalExpenses = standardizedMedicalExpenseService.findActiveOpenClaims()
            ResponseEntity.ok(medicalExpenses)
        } catch (e: Exception) {
            logger.error("Error retrieving active open claims", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @PatchMapping("/{medicalExpenseId}/claim-status")
    fun updateClaimStatusPatch(
        @PathVariable medicalExpenseId: Long,
        @RequestParam claimStatus: ClaimStatus,
    ): ResponseEntity<Map<String, String>> {
        logger.info("PATCH /medical-expenses/$medicalExpenseId/claim-status - Updating claim status to: $claimStatus")
        return updateClaimStatusInternal(medicalExpenseId, claimStatus)
    }

    @PutMapping("/{medicalExpenseId}/claim-status")
    fun updateClaimStatusPut(
        @PathVariable medicalExpenseId: Long,
        @RequestParam claimStatus: ClaimStatus,
    ): ResponseEntity<Map<String, String>> {
        logger.info("PUT /medical-expenses/$medicalExpenseId/claim-status - Updating claim status to: $claimStatus")
        return updateClaimStatusInternal(medicalExpenseId, claimStatus)
    }

    private fun updateClaimStatusInternal(
        medicalExpenseId: Long,
        claimStatus: ClaimStatus,
    ): ResponseEntity<Map<String, String>> {
        return try {
            val success = standardizedMedicalExpenseService.updateClaimStatus(medicalExpenseId, claimStatus)
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

    /**
     * Legacy CRUD endpoint - DELETE /api/medical-expenses/delete/{medicalExpenseId}
     * Original method name preserved for backward compatibility
     */
    @DeleteMapping("/delete/{medicalExpenseId}", produces = ["application/json"])
    fun softDeleteMedicalExpense(
        @PathVariable medicalExpenseId: Long,
    ): ResponseEntity<Map<String, String>> {
        logger.info("DELETE /medical-expenses/$medicalExpenseId - Soft deleting medical expense")

        return try {
            val success = standardizedMedicalExpenseService.softDeleteMedicalExpense(medicalExpenseId)
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
        @PathVariable year: Int,
    ): ResponseEntity<Map<String, BigDecimal>> {
        logger.info("GET /medical-expenses/totals/year/$year - Retrieving medical totals by year")

        return try {
            val totalBilled = standardizedMedicalExpenseService.getTotalBilledAmountByYear(year)
            val totalPatientResponsibility = standardizedMedicalExpenseService.getTotalPatientResponsibilityByYear(year)
            val totalInsurancePaid = standardizedMedicalExpenseService.getTotalInsurancePaidByYear(year)

            val totals =
                mapOf(
                    "totalBilled" to totalBilled,
                    "totalPatientResponsibility" to totalPatientResponsibility,
                    "totalInsurancePaid" to totalInsurancePaid,
                    "totalCovered" to (totalInsurancePaid + (totalBilled - totalPatientResponsibility - totalInsurancePaid)),
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
            val statusCounts = standardizedMedicalExpenseService.getClaimStatusCounts()
            ResponseEntity.ok(statusCounts)
        } catch (e: Exception) {
            logger.error("Error retrieving claim status counts", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/procedure-code/{procedureCode}")
    fun getMedicalExpensesByProcedureCode(
        @PathVariable procedureCode: String,
    ): ResponseEntity<List<MedicalExpense>> {
        logger.info("GET /medical-expenses/procedure-code/$procedureCode - Retrieving medical expenses by procedure code")

        return try {
            val medicalExpenses = standardizedMedicalExpenseService.findMedicalExpensesByProcedureCode(procedureCode)
            ResponseEntity.ok(medicalExpenses)
        } catch (e: Exception) {
            logger.error("Error retrieving medical expenses for procedure code: $procedureCode", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/diagnosis-code/{diagnosisCode}")
    fun getMedicalExpensesByDiagnosisCode(
        @PathVariable diagnosisCode: String,
    ): ResponseEntity<List<MedicalExpense>> {
        logger.info("GET /medical-expenses/diagnosis-code/$diagnosisCode - Retrieving medical expenses by diagnosis code")

        return try {
            val medicalExpenses = standardizedMedicalExpenseService.findMedicalExpensesByDiagnosisCode(diagnosisCode)
            ResponseEntity.ok(medicalExpenses)
        } catch (e: Exception) {
            logger.error("Error retrieving medical expenses for diagnosis code: $diagnosisCode", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/date-range")
    fun getMedicalExpensesByDateRange(
        @RequestParam @NotNull @DateTimeFormat(pattern = "yyyy-MM-dd") startDate: Date,
        @RequestParam @NotNull @DateTimeFormat(pattern = "yyyy-MM-dd") endDate: Date,
    ): ResponseEntity<List<MedicalExpense>> {
        logger.info("GET /medical-expenses/date-range - Retrieving medical expenses by date range")

        return try {
            val medicalExpenses = standardizedMedicalExpenseService.findMedicalExpensesByServiceDateRange(startDate, endDate)
            ResponseEntity.ok(medicalExpenses)
        } catch (e: Exception) {
            logger.error("Error retrieving medical expenses for date range", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    // New payment-related endpoints for Phase 2.5
    @PostMapping("/{medicalExpenseId}/payments/{transactionId}")
    fun linkPaymentTransaction(
        @PathVariable medicalExpenseId: Long,
        @PathVariable transactionId: Long,
    ): ResponseEntity<MedicalExpense> {
        logger.info("POST /medical-expenses/$medicalExpenseId/payments/$transactionId - Linking payment transaction")

        return try {
            val updatedExpense = standardizedMedicalExpenseService.linkPaymentTransaction(medicalExpenseId, transactionId)
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
        @PathVariable medicalExpenseId: Long,
    ): ResponseEntity<MedicalExpense> {
        logger.info("DELETE /medical-expenses/$medicalExpenseId/payments - Unlinking payment transaction")

        return try {
            val updatedExpense = standardizedMedicalExpenseService.unlinkPaymentTransaction(medicalExpenseId)
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
        @PathVariable medicalExpenseId: Long,
    ): ResponseEntity<MedicalExpense> {
        logger.info("PUT /medical-expenses/$medicalExpenseId/sync-payment - Syncing payment amount")

        return try {
            val updatedExpense = standardizedMedicalExpenseService.updatePaidAmount(medicalExpenseId)
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
            val medicalExpenses = standardizedMedicalExpenseService.findUnpaidMedicalExpenses()
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
            val medicalExpenses = standardizedMedicalExpenseService.findPartiallyPaidMedicalExpenses()
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
            val medicalExpenses = standardizedMedicalExpenseService.findFullyPaidMedicalExpenses()
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
            val medicalExpenses = standardizedMedicalExpenseService.findMedicalExpensesWithoutTransaction()
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
            val medicalExpenses = standardizedMedicalExpenseService.findOverpaidMedicalExpenses()
            logger.info("Successfully retrieved ${medicalExpenses.size} overpaid medical expenses")
            ResponseEntity.ok(medicalExpenses)
        } catch (e: Exception) {
            logger.error("Error retrieving overpaid medical expenses", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/totals/year/{year}/paid")
    fun getTotalPaidAmountByYear(
        @PathVariable year: Int,
    ): ResponseEntity<Map<String, BigDecimal>> {
        logger.info("GET /medical-expenses/totals/year/$year/paid - Retrieving total paid amount by year")

        return try {
            val totalPaid = standardizedMedicalExpenseService.getTotalPaidAmountByYear(year)
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
            val totalUnpaid = standardizedMedicalExpenseService.getTotalUnpaidBalance()
            val response = mapOf("totalUnpaidBalance" to totalUnpaid)
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            logger.error("Error retrieving total unpaid balance", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }
}
