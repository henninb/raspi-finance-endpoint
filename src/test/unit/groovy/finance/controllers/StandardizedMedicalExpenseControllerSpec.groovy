package finance.controllers

import finance.domain.ClaimStatus
import finance.domain.MedicalExpense
import finance.services.StandardizedMedicalExpenseService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Specification
import spock.lang.Subject

import java.math.BigDecimal
import java.sql.Date

class StandardizedMedicalExpenseControllerSpec extends Specification {

    finance.repositories.MedicalExpenseRepository repo = Mock()
    StandardizedMedicalExpenseService service = new StandardizedMedicalExpenseService(repo)

    @Subject
    MedicalExpenseController controller = new MedicalExpenseController(service)

    def setup() {
        def validator = Mock(jakarta.validation.Validator) { validate(_ as Object) >> ([] as Set) }
        def meterRegistry = new io.micrometer.core.instrument.simple.SimpleMeterRegistry()
        def meterService = new finance.services.MeterService(meterRegistry)
        service.validator = validator
        service.meterService = meterService
    }

    private static MedicalExpense me(Long id = 0L) {
        new MedicalExpense(medicalExpenseId: id)
    }

    // ===== STANDARDIZED: findAllActive =====
    def "findAllActive returns 200 with list"() {
        given:
        repo.findByActiveStatusTrueOrderByServiceDateDesc() >> [me(1L), me(2L)]

        when:
        ResponseEntity<List<MedicalExpense>> resp = controller.findAllActive()

        then:
        resp.statusCode == HttpStatus.OK
        resp.body.size() == 2
    }

    def "findAllActive returns 404 when none (service NotFound)"() {
        given:
        repo.findByActiveStatusTrueOrderByServiceDateDesc() >> { throw new jakarta.persistence.EntityNotFoundException("none") }

        when:
        ResponseEntity<List<MedicalExpense>> resp = controller.findAllActive()

        then:
        resp.statusCode == HttpStatus.NOT_FOUND
    }

    def "findAllActive returns 500 on error"() {
        given:
        repo.findByActiveStatusTrueOrderByServiceDateDesc() >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<List<MedicalExpense>> resp = controller.findAllActive()

        then:
        resp.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== STANDARDIZED: findById =====
    def "findById returns 200 when found"() {
        given:
        repo.findByMedicalExpenseIdAndActiveStatusTrue(11L) >> me(11L)

        when:
        ResponseEntity<MedicalExpense> resp = controller.findById(11L)

        then:
        resp.statusCode == HttpStatus.OK
        resp.body.medicalExpenseId == 11L
    }

    def "findById returns 404 when missing"() {
        given:
        repo.findByMedicalExpenseIdAndActiveStatusTrue(404L) >> null

        when:
        ResponseEntity<MedicalExpense> resp = controller.findById(404L)

        then:
        resp.statusCode == HttpStatus.NOT_FOUND
    }

    def "findById returns 500 on error"() {
        given:
        repo.findByMedicalExpenseIdAndActiveStatusTrue(500L) >> { throw new RuntimeException("db") }

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
        service.validator = violatingValidator

        when:
        ResponseEntity<MedicalExpense> resp = controller.save(invalid)

        then:
        resp.statusCode == HttpStatus.BAD_REQUEST
    }

    def "save returns 409 on duplicate (business error)"() {
        given:
        MedicalExpense input = me(0L)
        input.transactionId = 10L
        repo.findByTransactionId(10L) >> me(123L)

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
        repo.findByMedicalExpenseIdAndActiveStatusTrue(21L) >> existing
        repo.save(_ as MedicalExpense) >> { MedicalExpense m -> m }

        when:
        ResponseEntity<MedicalExpense> resp = controller.update(21L, patch)

        then:
        resp.statusCode == HttpStatus.OK
    }

    def "update returns 404 when missing"() {
        given:
        MedicalExpense patch = me(22L)
        repo.findByMedicalExpenseIdAndActiveStatusTrue(22L) >> null

        when:
        ResponseEntity<MedicalExpense> resp = controller.update(22L, patch)

        then:
        resp.statusCode == HttpStatus.NOT_FOUND
    }

    def "update returns 400 on validation error"() {
        given:
        MedicalExpense existing = me(23L)
        MedicalExpense patch = me(23L)
        repo.findByMedicalExpenseIdAndActiveStatusTrue(23L) >> existing
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
        repo.findByMedicalExpenseIdAndActiveStatusTrue(24L) >> existing
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
        repo.findByMedicalExpenseIdAndActiveStatusTrue(25L) >> existing
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
        repo.findByMedicalExpenseIdAndActiveStatusTrue(31L) >> existing
        repo.softDeleteByMedicalExpenseId(31L) >> 1

        when:
        ResponseEntity<MedicalExpense> resp = controller.deleteById(31L)

        then:
        resp.statusCode == HttpStatus.OK
        resp.body.medicalExpenseId == 31L
    }

    def "deleteById returns 404 when not found"() {
        given:
        repo.findByMedicalExpenseIdAndActiveStatusTrue(32L) >> null

        when:
        ResponseEntity<MedicalExpense> resp = controller.deleteById(32L)

        then:
        resp.statusCode == HttpStatus.NOT_FOUND
    }

    def "deleteById returns 500 on error"() {
        given:
        MedicalExpense existing = me(33L)
        1 * repo.findByMedicalExpenseIdAndActiveStatusTrue(33L) >> existing
        repo.softDeleteByMedicalExpenseId(33L) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<MedicalExpense> resp = controller.deleteById(33L)

        then:
        resp.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== Claim status endpoints =====
    def "updateClaimStatusPatch returns 200 on success"() {
        given:
        repo.updateClaimStatus(41L, ClaimStatus.Paid) >> 1

        when:
        ResponseEntity<Map<String,String>> resp = controller.updateClaimStatusPatch(41L, ClaimStatus.Paid)

        then:
        resp.statusCode == HttpStatus.OK
        resp.body.get("message").contains("successfully")
    }

    def "updateClaimStatusPut returns 404 when not found"() {
        given:
        repo.updateClaimStatus(42L, ClaimStatus.Paid) >> 0

        when:
        ResponseEntity<Map<String,String>> resp = controller.updateClaimStatusPut(42L, ClaimStatus.Paid)

        then:
        resp.statusCode == HttpStatus.NOT_FOUND
    }

    def "updateClaimStatus returns 500 on error"() {
        given:
        repo.updateClaimStatus(43L, ClaimStatus.Paid) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<Map<String,String>> resp = controller.updateClaimStatusPatch(43L, ClaimStatus.Paid)

        then:
        resp.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== Payment link/unlink/sync =====
    def "linkPaymentTransaction returns 200"() {
        given:
        MedicalExpense existing = me(51L)
        repo.findByMedicalExpenseIdAndActiveStatusTrue(51L) >> existing
        repo.findByTransactionId(999L) >> null
        repo.save(_ as MedicalExpense) >> { MedicalExpense m -> m }

        when:
        ResponseEntity<MedicalExpense> resp = controller.linkPaymentTransaction(51L, 999L)

        then:
        resp.statusCode == HttpStatus.OK
    }

    def "linkPaymentTransaction returns 409 on duplicate"() {
        given:
        MedicalExpense existing = me(52L)
        repo.findByMedicalExpenseIdAndActiveStatusTrue(52L) >> existing
        repo.findByTransactionId(1000L) >> me(99L)

        when:
        controller.linkPaymentTransaction(52L, 1000L)

        then:
        def ex = thrown(org.springframework.web.server.ResponseStatusException)
        ex.statusCode == HttpStatus.CONFLICT
    }

    def "linkPaymentTransaction returns 400 on bad request"() {
        given:
        repo.findByMedicalExpenseIdAndActiveStatusTrue(53L) >> null

        when:
        ResponseEntity<MedicalExpense> resp = controller.linkPaymentTransaction(53L, 2000L)

        then:
        resp.statusCode == HttpStatus.BAD_REQUEST
    }

    def "unlinkPaymentTransaction returns 200"() {
        given:
        MedicalExpense existing = me(61L)
        repo.findByMedicalExpenseIdAndActiveStatusTrue(61L) >> existing
        repo.save(_ as MedicalExpense) >> { MedicalExpense m -> m }

        when:
        ResponseEntity<MedicalExpense> resp = controller.unlinkPaymentTransaction(61L)

        then:
        resp.statusCode == HttpStatus.OK
    }

    def "unlinkPaymentTransaction returns 400 on bad request"() {
        given:
        repo.findByMedicalExpenseIdAndActiveStatusTrue(62L) >> null

        when:
        ResponseEntity<MedicalExpense> resp = controller.unlinkPaymentTransaction(62L)

        then:
        resp.statusCode == HttpStatus.BAD_REQUEST
    }

    def "syncPaymentAmount returns 200"() {
        given:
        MedicalExpense existing = me(71L)
        repo.findByMedicalExpenseIdAndActiveStatusTrue(71L) >> existing
        repo.save(_ as MedicalExpense) >> { MedicalExpense m -> m }

        when:
        ResponseEntity<MedicalExpense> resp = controller.syncPaymentAmount(71L)

        then:
        resp.statusCode == HttpStatus.OK
    }

    def "syncPaymentAmount returns 400 when not found"() {
        given:
        repo.findByMedicalExpenseIdAndActiveStatusTrue(72L) >> null

        when:
        ResponseEntity<MedicalExpense> resp = controller.syncPaymentAmount(72L)

        then:
        resp.statusCode == HttpStatus.BAD_REQUEST
    }
}
