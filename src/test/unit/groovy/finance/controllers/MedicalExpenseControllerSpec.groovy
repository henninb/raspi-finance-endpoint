package finance.controllers
import finance.configurations.ResilienceComponents

import finance.domain.ClaimStatus
import finance.domain.MedicalExpense
import finance.domain.ServiceResult
import finance.services.MedicalExpenseService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import spock.lang.Specification
import spock.lang.Subject

import java.math.BigDecimal

class StandardizedMedicalExpenseControllerSpec extends Specification {

    static final String TEST_OWNER = "test_owner"

    finance.repositories.MedicalExpenseRepository repo = Mock()
    jakarta.validation.Validator validator = Mock() { validate(_ as Object) >> ([] as Set) }
    finance.services.MeterService meterService = new finance.services.MeterService()
    MedicalExpenseService service = new MedicalExpenseService(repo, meterService, validator, ResilienceComponents.noOp())

    @Subject
    MedicalExpenseController controller = new MedicalExpenseController(service)

    def setup() {
        // Set up SecurityContext for TenantContext.getCurrentOwner()
        def auth = new UsernamePasswordAuthenticationToken(TEST_OWNER, null, [])
        SecurityContextHolder.getContext().setAuthentication(auth)
    }

    def cleanup() {
        SecurityContextHolder.clearContext()
    }

    private static MedicalExpense me(Long id = 0L) {
        new MedicalExpense(medicalExpenseId: id, owner: TEST_OWNER)
    }

    // ===== STANDARDIZED: findAllActive =====
    def "findAllActive returns 200 with list"() {
        given:
        repo.findByOwnerAndActiveStatusTrueOrderByServiceDateDesc(TEST_OWNER) >> [me(1L), me(2L)]

        when:
        ResponseEntity<List<MedicalExpense>> resp = controller.findAllActive()

        then:
        resp.statusCode == HttpStatus.OK
        resp.body.size() == 2
    }

    def "findAllActive returns 404 when none (service NotFound)"() {
        given:
        repo.findByOwnerAndActiveStatusTrueOrderByServiceDateDesc(TEST_OWNER) >> { throw new jakarta.persistence.EntityNotFoundException("none") }

        when:
        ResponseEntity<List<MedicalExpense>> resp = controller.findAllActive()

        then:
        resp.statusCode == HttpStatus.NOT_FOUND
    }

    def "findAllActive returns 500 on error"() {
        given:
        repo.findByOwnerAndActiveStatusTrueOrderByServiceDateDesc(TEST_OWNER) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<List<MedicalExpense>> resp = controller.findAllActive()

        then:
        resp.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== STANDARDIZED: findById =====
    def "findById returns 200 when found"() {
        given:
        repo.findByOwnerAndMedicalExpenseIdAndActiveStatusTrue(TEST_OWNER, 11L) >> me(11L)

        when:
        ResponseEntity<MedicalExpense> resp = controller.findById(11L)

        then:
        resp.statusCode == HttpStatus.OK
        resp.body.medicalExpenseId == 11L
    }

    def "findById returns 404 when missing"() {
        given:
        repo.findByOwnerAndMedicalExpenseIdAndActiveStatusTrue(TEST_OWNER, 404L) >> null

        when:
        ResponseEntity<MedicalExpense> resp = controller.findById(404L)

        then:
        resp.statusCode == HttpStatus.NOT_FOUND
    }

    def "findById returns 500 on error"() {
        given:
        repo.findByOwnerAndMedicalExpenseIdAndActiveStatusTrue(TEST_OWNER, 500L) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<MedicalExpense> resp = controller.findById(500L)

        then:
        resp.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== STANDARDIZED: save =====
    def "save returns 201 on success"() {
        given:
        MedicalExpense input = me(0L)
        repo.save(_ as MedicalExpense) >> { MedicalExpense e -> e.medicalExpenseId = 99L; e }

        when:
        ResponseEntity<MedicalExpense> resp = controller.save(input)

        then:
        resp.statusCode == HttpStatus.CREATED
        resp.body.medicalExpenseId == 99L
    }

    def "save returns 400 on validation error"() {
        given:
        MedicalExpense invalid = me(0L)
        and:
        def violatingValidator = Mock(jakarta.validation.Validator) { validate(_ as Object) >> ([Mock(jakarta.validation.ConstraintViolation)] as Set) }
        def localService = new MedicalExpenseService(repo, meterService, violatingValidator, ResilienceComponents.noOp())
        def localController = new MedicalExpenseController(localService)

        when:
        ResponseEntity<MedicalExpense> resp = localController.save(invalid)

        then:
        resp.statusCode == HttpStatus.BAD_REQUEST
    }

    def "save returns 409 on duplicate (business error)"() {
        given:
        MedicalExpense input = me(0L)
        input.transactionId = 10L
        repo.findByOwnerAndTransactionId(TEST_OWNER, 10L) >> me(123L)

        when:
        ResponseEntity<MedicalExpense> resp = controller.save(input)

        then:
        resp.statusCode == HttpStatus.CONFLICT
    }

    def "save returns 500 on system error"() {
        given:
        MedicalExpense input = me(0L)
        repo.save(_ as MedicalExpense) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<MedicalExpense> resp = controller.save(input)

        then:
        resp.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== STANDARDIZED: update =====
    def "update returns 200 on success"() {
        given:
        MedicalExpense existing = me(21L)
        MedicalExpense patch = me(21L)
        repo.findByOwnerAndMedicalExpenseIdAndActiveStatusTrue(TEST_OWNER, 21L) >> existing
        repo.save(_ as MedicalExpense) >> { MedicalExpense m -> m }

        when:
        ResponseEntity<MedicalExpense> resp = controller.update(21L, patch)

        then:
        resp.statusCode == HttpStatus.OK
    }

    def "update returns 404 when missing"() {
        given:
        MedicalExpense patch = me(22L)
        repo.findByOwnerAndMedicalExpenseIdAndActiveStatusTrue(TEST_OWNER, 22L) >> null

        when:
        ResponseEntity<MedicalExpense> resp = controller.update(22L, patch)

        then:
        resp.statusCode == HttpStatus.NOT_FOUND
    }

    def "update returns 400 on validation error"() {
        given:
        MedicalExpense existing = me(23L)
        MedicalExpense patch = me(23L)
        repo.findByOwnerAndMedicalExpenseIdAndActiveStatusTrue(TEST_OWNER, 23L) >> existing
        repo.save(_ as MedicalExpense) >> { throw new jakarta.validation.ConstraintViolationException("bad", [] as Set) }

        when:
        ResponseEntity<MedicalExpense> resp = controller.update(23L, patch)

        then:
        resp.statusCode == HttpStatus.BAD_REQUEST
    }

    def "update returns 409 on business error"() {
        given:
        MedicalExpense existing = me(24L)
        MedicalExpense patch = me(24L)
        repo.findByOwnerAndMedicalExpenseIdAndActiveStatusTrue(TEST_OWNER, 24L) >> existing
        repo.save(_ as MedicalExpense) >> { throw new org.springframework.dao.DataIntegrityViolationException("dup") }

        when:
        ResponseEntity<MedicalExpense> resp = controller.update(24L, patch)

        then:
        resp.statusCode == HttpStatus.CONFLICT
    }

    def "update returns 500 on error"() {
        given:
        MedicalExpense existing = me(25L)
        MedicalExpense patch = me(25L)
        repo.findByOwnerAndMedicalExpenseIdAndActiveStatusTrue(TEST_OWNER, 25L) >> existing
        repo.save(_ as MedicalExpense) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<MedicalExpense> resp = controller.update(25L, patch)

        then:
        resp.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== STANDARDIZED: deleteById =====
    def "deleteById returns 200 with deleted entity"() {
        given:
        MedicalExpense existing = me(31L)
        repo.findByOwnerAndMedicalExpenseIdAndActiveStatusTrue(TEST_OWNER, 31L) >> existing
        repo.softDeleteByOwnerAndMedicalExpenseId(TEST_OWNER, 31L) >> 1

        when:
        ResponseEntity<MedicalExpense> resp = controller.deleteById(31L)

        then:
        resp.statusCode == HttpStatus.OK
        resp.body.medicalExpenseId == 31L
    }

    def "deleteById returns 404 when not found"() {
        given:
        repo.findByOwnerAndMedicalExpenseIdAndActiveStatusTrue(TEST_OWNER, 32L) >> null

        when:
        ResponseEntity<MedicalExpense> resp = controller.deleteById(32L)

        then:
        resp.statusCode == HttpStatus.NOT_FOUND
    }

    def "deleteById returns 500 on error"() {
        given:
        MedicalExpense existing = me(33L)
        repo.findByOwnerAndMedicalExpenseIdAndActiveStatusTrue(TEST_OWNER, 33L) >> existing
        repo.softDeleteByOwnerAndMedicalExpenseId(TEST_OWNER, 33L) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<MedicalExpense> resp = controller.deleteById(33L)

        then:
        resp.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== Claim status endpoints =====
    def "updateClaimStatusPatch returns 200 on success"() {
        given:
        repo.updateClaimStatusByOwner(TEST_OWNER, 41L, ClaimStatus.Paid) >> 1

        when:
        ResponseEntity<Map<String,String>> resp = controller.updateClaimStatusPatch(41L, ClaimStatus.Paid)

        then:
        resp.statusCode == HttpStatus.OK
        resp.body.get("message").contains("successfully")
    }

    def "updateClaimStatusPut returns 404 when not found"() {
        given:
        repo.updateClaimStatusByOwner(TEST_OWNER, 42L, ClaimStatus.Paid) >> 0

        when:
        ResponseEntity<Map<String,String>> resp = controller.updateClaimStatusPut(42L, ClaimStatus.Paid)

        then:
        resp.statusCode == HttpStatus.NOT_FOUND
    }

    def "updateClaimStatus returns 500 on error"() {
        given:
        repo.updateClaimStatusByOwner(TEST_OWNER, 43L, ClaimStatus.Paid) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<Map<String,String>> resp = controller.updateClaimStatusPatch(43L, ClaimStatus.Paid)

        then:
        resp.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== Payment link/unlink/sync =====
    def "linkPaymentTransaction returns 200"() {
        given:
        MedicalExpense existing = me(51L)
        repo.findByOwnerAndMedicalExpenseIdAndActiveStatusTrue(TEST_OWNER, 51L) >> existing
        repo.findByOwnerAndTransactionId(TEST_OWNER, 999L) >> null
        repo.save(_ as MedicalExpense) >> { MedicalExpense m -> m }

        when:
        ResponseEntity<MedicalExpense> resp = controller.linkPaymentTransaction(51L, 999L)

        then:
        resp.statusCode == HttpStatus.OK
    }

    def "linkPaymentTransaction returns 409 on duplicate"() {
        given:
        MedicalExpense existing = me(52L)
        repo.findByOwnerAndMedicalExpenseIdAndActiveStatusTrue(TEST_OWNER, 52L) >> existing
        repo.findByOwnerAndTransactionId(TEST_OWNER, 1000L) >> me(99L)

        when:
        controller.linkPaymentTransaction(52L, 1000L)

        then:
        def ex = thrown(org.springframework.web.server.ResponseStatusException)
        ex.statusCode == HttpStatus.CONFLICT
    }

    def "linkPaymentTransaction returns 400 on bad request"() {
        given:
        repo.findByOwnerAndMedicalExpenseIdAndActiveStatusTrue(TEST_OWNER, 53L) >> null

        when:
        ResponseEntity<MedicalExpense> resp = controller.linkPaymentTransaction(53L, 2000L)

        then:
        resp.statusCode == HttpStatus.BAD_REQUEST
    }

    def "unlinkPaymentTransaction returns 200"() {
        given:
        MedicalExpense existing = me(61L)
        repo.findByOwnerAndMedicalExpenseIdAndActiveStatusTrue(TEST_OWNER, 61L) >> existing
        repo.save(_ as MedicalExpense) >> { MedicalExpense m -> m }

        when:
        ResponseEntity<MedicalExpense> resp = controller.unlinkPaymentTransaction(61L)

        then:
        resp.statusCode == HttpStatus.OK
    }

    def "unlinkPaymentTransaction returns 400 on bad request"() {
        given:
        repo.findByOwnerAndMedicalExpenseIdAndActiveStatusTrue(TEST_OWNER, 62L) >> null

        when:
        ResponseEntity<MedicalExpense> resp = controller.unlinkPaymentTransaction(62L)

        then:
        resp.statusCode == HttpStatus.BAD_REQUEST
    }

    def "syncPaymentAmount returns 200"() {
        given:
        MedicalExpense existing = me(71L)
        repo.findByOwnerAndMedicalExpenseIdAndActiveStatusTrue(TEST_OWNER, 71L) >> existing
        repo.save(_ as MedicalExpense) >> { MedicalExpense m -> m }

        when:
        ResponseEntity<MedicalExpense> resp = controller.syncPaymentAmount(71L)

        then:
        resp.statusCode == HttpStatus.OK
    }

    def "syncPaymentAmount returns 400 when not found"() {
        given:
        repo.findByOwnerAndMedicalExpenseIdAndActiveStatusTrue(TEST_OWNER, 72L) >> null

        when:
        ResponseEntity<MedicalExpense> resp = controller.syncPaymentAmount(72L)

        then:
        resp.statusCode == HttpStatus.BAD_REQUEST
    }

    def "deleteById returns 404 when medical expense not found"() {
        given:
        MedicalExpenseService mockService = Mock()
        mockService.deleteById(_ as Long) >> ServiceResult.NotFound.of("not found")
        MedicalExpenseController controllerWithMockedService = new MedicalExpenseController(mockService)

        when:
        ResponseEntity<MedicalExpense> response = controllerWithMockedService.deleteById(1L)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        response.body == null
    }

    // ===== getAllMedicalExpenses (legacy GET /all) =====
    def "getAllMedicalExpenses returns 200 with list"() {
        given:
        repo.findByOwnerAndActiveStatusTrueOrderByServiceDateDesc(TEST_OWNER) >> [me(1L), me(2L)]

        when:
        ResponseEntity<List<MedicalExpense>> resp = controller.getAllMedicalExpenses()

        then:
        resp.statusCode == HttpStatus.OK
        resp.body.size() == 2
    }

    def "getAllMedicalExpenses returns 200 with empty list on error"() {
        given:
        repo.findByOwnerAndActiveStatusTrueOrderByServiceDateDesc(TEST_OWNER) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<List<MedicalExpense>> resp = controller.getAllMedicalExpenses()

        then:
        resp.statusCode == HttpStatus.OK
        resp.body.isEmpty()
    }

    def "getAllMedicalExpenses returns 500 when service throws"() {
        given:
        MedicalExpenseService mockService = Mock()
        mockService.findAllMedicalExpenses() >> { throw new RuntimeException("db") }
        MedicalExpenseController ctrl = new MedicalExpenseController(mockService)

        when:
        ResponseEntity<List<MedicalExpense>> resp = ctrl.getAllMedicalExpenses()

        then:
        resp.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== updateMedicalExpense (legacy PUT /update/{id}) =====
    def "updateMedicalExpense returns 200 on success"() {
        given:
        MedicalExpense existing = me(81L)
        MedicalExpense patch = me(81L)
        repo.findByOwnerAndMedicalExpenseIdAndActiveStatusTrue(TEST_OWNER, 81L) >> existing
        repo.save(_ as MedicalExpense) >> { MedicalExpense m -> m }

        when:
        ResponseEntity<MedicalExpense> resp = controller.updateMedicalExpense(81L, patch)

        then:
        resp.statusCode == HttpStatus.OK
    }

    def "updateMedicalExpense returns 400 when not found"() {
        given:
        MedicalExpense patch = me(82L)
        repo.findByOwnerAndMedicalExpenseIdAndActiveStatusTrue(TEST_OWNER, 82L) >> null

        when:
        ResponseEntity<MedicalExpense> resp = controller.updateMedicalExpense(82L, patch)

        then:
        resp.statusCode == HttpStatus.BAD_REQUEST
    }

    def "updateMedicalExpense returns 500 on system error"() {
        given:
        MedicalExpense existing = me(83L)
        MedicalExpense patch = me(83L)
        repo.findByOwnerAndMedicalExpenseIdAndActiveStatusTrue(TEST_OWNER, 83L) >> existing
        repo.save(_ as MedicalExpense) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<MedicalExpense> resp = controller.updateMedicalExpense(83L, patch)

        then:
        resp.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== getMedicalExpenseById (legacy GET /select/{id}) =====
    def "getMedicalExpenseById returns 200 when found"() {
        given:
        repo.findByOwnerAndMedicalExpenseIdAndActiveStatusTrue(TEST_OWNER, 91L) >> me(91L)

        when:
        ResponseEntity<MedicalExpense> resp = controller.getMedicalExpenseById(91L)

        then:
        resp.statusCode == HttpStatus.OK
        resp.body.medicalExpenseId == 91L
    }

    def "getMedicalExpenseById returns 404 when not found"() {
        given:
        repo.findByOwnerAndMedicalExpenseIdAndActiveStatusTrue(TEST_OWNER, 92L) >> null

        when:
        ResponseEntity<MedicalExpense> resp = controller.getMedicalExpenseById(92L)

        then:
        resp.statusCode == HttpStatus.NOT_FOUND
    }

    def "getMedicalExpenseById returns 500 on error"() {
        given:
        repo.findByOwnerAndMedicalExpenseIdAndActiveStatusTrue(TEST_OWNER, 93L) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<MedicalExpense> resp = controller.getMedicalExpenseById(93L)

        then:
        resp.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== getMedicalExpenseByTransactionId =====
    def "getMedicalExpenseByTransactionId returns 200 when found"() {
        given:
        repo.findByOwnerAndTransactionId(TEST_OWNER, 101L) >> me(1L)

        when:
        ResponseEntity<MedicalExpense> resp = controller.getMedicalExpenseByTransactionId(101L)

        then:
        resp.statusCode == HttpStatus.OK
    }

    def "getMedicalExpenseByTransactionId returns 404 when not found"() {
        given:
        repo.findByOwnerAndTransactionId(TEST_OWNER, 102L) >> null

        when:
        ResponseEntity<MedicalExpense> resp = controller.getMedicalExpenseByTransactionId(102L)

        then:
        resp.statusCode == HttpStatus.NOT_FOUND
    }

    def "getMedicalExpenseByTransactionId returns 500 on error"() {
        given:
        repo.findByOwnerAndTransactionId(TEST_OWNER, 103L) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<MedicalExpense> resp = controller.getMedicalExpenseByTransactionId(103L)

        then:
        resp.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== getMedicalExpensesByAccountId =====
    def "getMedicalExpensesByAccountId returns 200 with list"() {
        given:
        repo.findByOwnerAndAccountId(TEST_OWNER, 10L) >> [me(1L)]

        when:
        ResponseEntity<List<MedicalExpense>> resp = controller.getMedicalExpensesByAccountId(10L)

        then:
        resp.statusCode == HttpStatus.OK
        resp.body.size() == 1
    }

    def "getMedicalExpensesByAccountId returns 500 on error"() {
        given:
        repo.findByOwnerAndAccountId(TEST_OWNER, 11L) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<List<MedicalExpense>> resp = controller.getMedicalExpensesByAccountId(11L)

        then:
        resp.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== getMedicalExpensesByAccountIdAndDateRange =====
    def "getMedicalExpensesByAccountIdAndDateRange returns 200"() {
        given:
        def start = java.time.LocalDate.of(2024, 1, 1)
        def end = java.time.LocalDate.of(2024, 12, 31)
        repo.findByOwnerAndAccountIdAndServiceDateBetween(TEST_OWNER, 10L, start, end) >> [me(1L)]

        when:
        ResponseEntity<List<MedicalExpense>> resp = controller.getMedicalExpensesByAccountIdAndDateRange(10L, start, end)

        then:
        resp.statusCode == HttpStatus.OK
    }

    def "getMedicalExpensesByAccountIdAndDateRange returns 500 on error"() {
        given:
        def start = java.time.LocalDate.of(2024, 1, 1)
        def end = java.time.LocalDate.of(2024, 12, 31)
        repo.findByOwnerAndAccountIdAndServiceDateBetween(TEST_OWNER, 11L, start, end) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<List<MedicalExpense>> resp = controller.getMedicalExpensesByAccountIdAndDateRange(11L, start, end)

        then:
        resp.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== getMedicalExpensesByProviderId =====
    def "getMedicalExpensesByProviderId returns 200 with list"() {
        given:
        repo.findByOwnerAndProviderIdAndActiveStatusTrue(TEST_OWNER, 20L) >> [me(1L)]

        when:
        ResponseEntity<List<MedicalExpense>> resp = controller.getMedicalExpensesByProviderId(20L)

        then:
        resp.statusCode == HttpStatus.OK
    }

    def "getMedicalExpensesByProviderId returns 500 on error"() {
        given:
        repo.findByOwnerAndProviderIdAndActiveStatusTrue(TEST_OWNER, 21L) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<List<MedicalExpense>> resp = controller.getMedicalExpensesByProviderId(21L)

        then:
        resp.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== getMedicalExpensesByFamilyMemberId =====
    def "getMedicalExpensesByFamilyMemberId returns 200 with list"() {
        given:
        repo.findByOwnerAndFamilyMemberIdAndActiveStatusTrue(TEST_OWNER, 30L) >> [me(1L)]

        when:
        ResponseEntity<List<MedicalExpense>> resp = controller.getMedicalExpensesByFamilyMemberId(30L)

        then:
        resp.statusCode == HttpStatus.OK
    }

    def "getMedicalExpensesByFamilyMemberId returns 500 on error"() {
        given:
        repo.findByOwnerAndFamilyMemberIdAndActiveStatusTrue(TEST_OWNER, 31L) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<List<MedicalExpense>> resp = controller.getMedicalExpensesByFamilyMemberId(31L)

        then:
        resp.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== getMedicalExpensesByFamilyMemberAndDateRange =====
    def "getMedicalExpensesByFamilyMemberAndDateRange returns 200"() {
        given:
        def start = java.time.LocalDate.of(2024, 1, 1)
        def end = java.time.LocalDate.of(2024, 12, 31)
        repo.findByOwnerAndFamilyMemberIdAndServiceDateBetween(TEST_OWNER, 30L, start, end) >> [me(1L)]

        when:
        ResponseEntity<List<MedicalExpense>> resp = controller.getMedicalExpensesByFamilyMemberAndDateRange(30L, start, end)

        then:
        resp.statusCode == HttpStatus.OK
    }

    def "getMedicalExpensesByFamilyMemberAndDateRange returns 500 on error"() {
        given:
        def start = java.time.LocalDate.of(2024, 1, 1)
        def end = java.time.LocalDate.of(2024, 12, 31)
        repo.findByOwnerAndFamilyMemberIdAndServiceDateBetween(TEST_OWNER, 31L, start, end) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<List<MedicalExpense>> resp = controller.getMedicalExpensesByFamilyMemberAndDateRange(31L, start, end)

        then:
        resp.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== getMedicalExpensesByClaimStatus =====
    def "getMedicalExpensesByClaimStatus returns 200 with list"() {
        given:
        repo.findByOwnerAndClaimStatusAndActiveStatusTrue(TEST_OWNER, ClaimStatus.Paid) >> [me(1L)]

        when:
        ResponseEntity<List<MedicalExpense>> resp = controller.getMedicalExpensesByClaimStatus(ClaimStatus.Paid)

        then:
        resp.statusCode == HttpStatus.OK
    }

    def "getMedicalExpensesByClaimStatus returns 500 on error"() {
        given:
        repo.findByOwnerAndClaimStatusAndActiveStatusTrue(TEST_OWNER, ClaimStatus.Submitted) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<List<MedicalExpense>> resp = controller.getMedicalExpensesByClaimStatus(ClaimStatus.Submitted)

        then:
        resp.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== getOutOfNetworkExpenses =====
    def "getOutOfNetworkExpenses returns 200 with list"() {
        given:
        repo.findByOwnerAndIsOutOfNetworkAndActiveStatusTrue(TEST_OWNER, true) >> [me(1L)]

        when:
        ResponseEntity<List<MedicalExpense>> resp = controller.getOutOfNetworkExpenses()

        then:
        resp.statusCode == HttpStatus.OK
    }

    def "getOutOfNetworkExpenses returns 500 on error"() {
        given:
        repo.findByOwnerAndIsOutOfNetworkAndActiveStatusTrue(TEST_OWNER, true) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<List<MedicalExpense>> resp = controller.getOutOfNetworkExpenses()

        then:
        resp.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== getOutstandingPatientBalances =====
    def "getOutstandingPatientBalances returns 200 with list"() {
        given:
        repo.findOutstandingPatientBalancesByOwner(TEST_OWNER) >> [me(1L)]

        when:
        ResponseEntity<List<MedicalExpense>> resp = controller.getOutstandingPatientBalances()

        then:
        resp.statusCode == HttpStatus.OK
    }

    def "getOutstandingPatientBalances returns 500 on error"() {
        given:
        repo.findOutstandingPatientBalancesByOwner(TEST_OWNER) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<List<MedicalExpense>> resp = controller.getOutstandingPatientBalances()

        then:
        resp.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== getActiveOpenClaims =====
    def "getActiveOpenClaims returns 200 with list"() {
        given:
        repo.findActiveOpenClaimsByOwner(TEST_OWNER) >> [me(1L)]

        when:
        ResponseEntity<List<MedicalExpense>> resp = controller.getActiveOpenClaims()

        then:
        resp.statusCode == HttpStatus.OK
    }

    def "getActiveOpenClaims returns 500 on error"() {
        given:
        repo.findActiveOpenClaimsByOwner(TEST_OWNER) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<List<MedicalExpense>> resp = controller.getActiveOpenClaims()

        then:
        resp.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== softDeleteMedicalExpense (legacy DELETE /delete/{id}) =====
    def "softDeleteMedicalExpense returns 200 on success"() {
        given:
        repo.softDeleteByOwnerAndMedicalExpenseId(TEST_OWNER, 111L) >> 1

        when:
        ResponseEntity<Map<String, String>> resp = controller.softDeleteMedicalExpense(111L)

        then:
        resp.statusCode == HttpStatus.OK
        resp.body.get("message").contains("deleted")
    }

    def "softDeleteMedicalExpense returns 404 when not found"() {
        given:
        repo.softDeleteByOwnerAndMedicalExpenseId(TEST_OWNER, 112L) >> 0

        when:
        ResponseEntity<Map<String, String>> resp = controller.softDeleteMedicalExpense(112L)

        then:
        resp.statusCode == HttpStatus.NOT_FOUND
    }

    def "softDeleteMedicalExpense returns 500 on error"() {
        given:
        repo.softDeleteByOwnerAndMedicalExpenseId(TEST_OWNER, 113L) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<Map<String, String>> resp = controller.softDeleteMedicalExpense(113L)

        then:
        resp.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== getMedicalTotalsByYear =====
    def "getMedicalTotalsByYear returns 200 with totals map"() {
        given:
        repo.getTotalBilledAmountByOwnerAndYear(TEST_OWNER, 2024) >> new BigDecimal("1000.00")
        repo.getTotalPatientResponsibilityByOwnerAndYear(TEST_OWNER, 2024) >> new BigDecimal("200.00")
        repo.getTotalInsurancePaidByOwnerAndYear(TEST_OWNER, 2024) >> new BigDecimal("800.00")

        when:
        ResponseEntity<Map<String, BigDecimal>> resp = controller.getMedicalTotalsByYear(2024)

        then:
        resp.statusCode == HttpStatus.OK
        resp.body.containsKey("totalBilled")
        resp.body.containsKey("totalPatientResponsibility")
        resp.body.containsKey("totalInsurancePaid")
    }

    def "getMedicalTotalsByYear returns 500 on error"() {
        given:
        repo.getTotalBilledAmountByOwnerAndYear(TEST_OWNER, 2024) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<Map<String, BigDecimal>> resp = controller.getMedicalTotalsByYear(2024)

        then:
        resp.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== getClaimStatusCounts =====
    def "getClaimStatusCounts returns 200 with counts map"() {
        given:
        repo.countByOwnerAndClaimStatusAndActiveStatusTrue(TEST_OWNER, _ as ClaimStatus) >> 3L

        when:
        ResponseEntity<Map<ClaimStatus, Long>> resp = controller.getClaimStatusCounts()

        then:
        resp.statusCode == HttpStatus.OK
        !resp.body.isEmpty()
    }

    def "getClaimStatusCounts returns 500 on error"() {
        given:
        repo.countByOwnerAndClaimStatusAndActiveStatusTrue(TEST_OWNER, _ as ClaimStatus) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<Map<ClaimStatus, Long>> resp = controller.getClaimStatusCounts()

        then:
        resp.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== getMedicalExpensesByProcedureCode =====
    def "getMedicalExpensesByProcedureCode returns 200 with list"() {
        given:
        repo.findByOwnerAndProcedureCodeAndActiveStatusTrue(TEST_OWNER, "99213") >> [me(1L)]

        when:
        ResponseEntity<List<MedicalExpense>> resp = controller.getMedicalExpensesByProcedureCode("99213")

        then:
        resp.statusCode == HttpStatus.OK
    }

    def "getMedicalExpensesByProcedureCode returns 500 on error"() {
        given:
        repo.findByOwnerAndProcedureCodeAndActiveStatusTrue(TEST_OWNER, "99213") >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<List<MedicalExpense>> resp = controller.getMedicalExpensesByProcedureCode("99213")

        then:
        resp.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== getMedicalExpensesByDiagnosisCode =====
    def "getMedicalExpensesByDiagnosisCode returns 200 with list"() {
        given:
        repo.findByOwnerAndDiagnosisCodeAndActiveStatusTrue(TEST_OWNER, "Z00.00") >> [me(1L)]

        when:
        ResponseEntity<List<MedicalExpense>> resp = controller.getMedicalExpensesByDiagnosisCode("Z00.00")

        then:
        resp.statusCode == HttpStatus.OK
    }

    def "getMedicalExpensesByDiagnosisCode returns 500 on error"() {
        given:
        repo.findByOwnerAndDiagnosisCodeAndActiveStatusTrue(TEST_OWNER, "Z00.00") >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<List<MedicalExpense>> resp = controller.getMedicalExpensesByDiagnosisCode("Z00.00")

        then:
        resp.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== getMedicalExpensesByDateRange =====
    def "getMedicalExpensesByDateRange returns 200 with list"() {
        given:
        def start = java.time.LocalDate.of(2024, 1, 1)
        def end = java.time.LocalDate.of(2024, 12, 31)
        repo.findByOwnerAndServiceDateBetweenAndActiveStatusTrue(TEST_OWNER, start, end) >> [me(1L)]

        when:
        ResponseEntity<List<MedicalExpense>> resp = controller.getMedicalExpensesByDateRange(start, end)

        then:
        resp.statusCode == HttpStatus.OK
    }

    def "getMedicalExpensesByDateRange returns 500 on error"() {
        given:
        def start = java.time.LocalDate.of(2024, 1, 1)
        def end = java.time.LocalDate.of(2024, 12, 31)
        repo.findByOwnerAndServiceDateBetweenAndActiveStatusTrue(TEST_OWNER, start, end) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<List<MedicalExpense>> resp = controller.getMedicalExpensesByDateRange(start, end)

        then:
        resp.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== getUnpaidMedicalExpenses =====
    def "getUnpaidMedicalExpenses returns 200 with list"() {
        given:
        repo.findUnpaidMedicalExpensesByOwner(TEST_OWNER) >> [me(1L)]

        when:
        ResponseEntity<List<MedicalExpense>> resp = controller.getUnpaidMedicalExpenses()

        then:
        resp.statusCode == HttpStatus.OK
    }

    def "getUnpaidMedicalExpenses returns 500 on error"() {
        given:
        repo.findUnpaidMedicalExpensesByOwner(TEST_OWNER) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<List<MedicalExpense>> resp = controller.getUnpaidMedicalExpenses()

        then:
        resp.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== getPartiallyPaidMedicalExpenses =====
    def "getPartiallyPaidMedicalExpenses returns 200 with list"() {
        given:
        repo.findPartiallyPaidMedicalExpensesByOwner(TEST_OWNER) >> [me(1L)]

        when:
        ResponseEntity<List<MedicalExpense>> resp = controller.getPartiallyPaidMedicalExpenses()

        then:
        resp.statusCode == HttpStatus.OK
    }

    def "getPartiallyPaidMedicalExpenses returns 500 on error"() {
        given:
        repo.findPartiallyPaidMedicalExpensesByOwner(TEST_OWNER) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<List<MedicalExpense>> resp = controller.getPartiallyPaidMedicalExpenses()

        then:
        resp.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== getFullyPaidMedicalExpenses =====
    def "getFullyPaidMedicalExpenses returns 200 with list"() {
        given:
        repo.findFullyPaidMedicalExpensesByOwner(TEST_OWNER) >> [me(1L)]

        when:
        ResponseEntity<List<MedicalExpense>> resp = controller.getFullyPaidMedicalExpenses()

        then:
        resp.statusCode == HttpStatus.OK
    }

    def "getFullyPaidMedicalExpenses returns 500 on error"() {
        given:
        repo.findFullyPaidMedicalExpensesByOwner(TEST_OWNER) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<List<MedicalExpense>> resp = controller.getFullyPaidMedicalExpenses()

        then:
        resp.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== getMedicalExpensesWithoutTransaction =====
    def "getMedicalExpensesWithoutTransaction returns 200 with list"() {
        given:
        repo.findMedicalExpensesWithoutTransactionByOwner(TEST_OWNER) >> [me(1L)]

        when:
        ResponseEntity<List<MedicalExpense>> resp = controller.getMedicalExpensesWithoutTransaction()

        then:
        resp.statusCode == HttpStatus.OK
    }

    def "getMedicalExpensesWithoutTransaction returns 500 on error"() {
        given:
        repo.findMedicalExpensesWithoutTransactionByOwner(TEST_OWNER) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<List<MedicalExpense>> resp = controller.getMedicalExpensesWithoutTransaction()

        then:
        resp.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== getOverpaidMedicalExpenses =====
    def "getOverpaidMedicalExpenses returns 200 with list"() {
        given:
        repo.findOverpaidMedicalExpensesByOwner(TEST_OWNER) >> [me(1L)]

        when:
        ResponseEntity<List<MedicalExpense>> resp = controller.getOverpaidMedicalExpenses()

        then:
        resp.statusCode == HttpStatus.OK
    }

    def "getOverpaidMedicalExpenses returns 500 on error"() {
        given:
        repo.findOverpaidMedicalExpensesByOwner(TEST_OWNER) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<List<MedicalExpense>> resp = controller.getOverpaidMedicalExpenses()

        then:
        resp.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== getTotalPaidAmountByYear =====
    def "getTotalPaidAmountByYear returns 200 with total"() {
        given:
        repo.getTotalPaidAmountByOwnerAndYear(TEST_OWNER, 2024) >> new BigDecimal("500.00")

        when:
        ResponseEntity<Map<String, BigDecimal>> resp = controller.getTotalPaidAmountByYear(2024)

        then:
        resp.statusCode == HttpStatus.OK
        resp.body.get("totalPaid") == new BigDecimal("500.00")
    }

    def "getTotalPaidAmountByYear returns 500 on error"() {
        given:
        repo.getTotalPaidAmountByOwnerAndYear(TEST_OWNER, 2024) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<Map<String, BigDecimal>> resp = controller.getTotalPaidAmountByYear(2024)

        then:
        resp.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== getTotalUnpaidBalance =====
    def "getTotalUnpaidBalance returns 200 with total"() {
        given:
        repo.getTotalUnpaidBalanceByOwner(TEST_OWNER) >> new BigDecimal("300.00")

        when:
        ResponseEntity<Map<String, BigDecimal>> resp = controller.getTotalUnpaidBalance()

        then:
        resp.statusCode == HttpStatus.OK
        resp.body.get("totalUnpaidBalance") == new BigDecimal("300.00")
    }

    def "getTotalUnpaidBalance returns 500 on error"() {
        given:
        repo.getTotalUnpaidBalanceByOwner(TEST_OWNER) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<Map<String, BigDecimal>> resp = controller.getTotalUnpaidBalance()

        then:
        resp.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }
}
