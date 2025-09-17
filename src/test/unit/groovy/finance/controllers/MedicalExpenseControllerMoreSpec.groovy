package finance.controllers

import finance.domain.ClaimStatus
import finance.domain.MedicalExpense
import finance.exceptions.DuplicateMedicalExpenseException
import finance.services.IMedicalExpenseService
import org.springframework.http.HttpStatus
import org.springframework.orm.jpa.JpaSystemException
import org.springframework.web.server.ResponseStatusException
import spock.lang.Specification

class MedicalExpenseControllerMoreSpec extends Specification {

    IMedicalExpenseService service = Mock()
    MedicalExpenseController controller = new MedicalExpenseController(service)

    void "findAllActive returns 200 with list (standardized)"() {
        when:
        def resp = controller.findAllActive()

        then:
        1 * service.findAllMedicalExpenses() >> [new MedicalExpense(medicalExpenseId: 1L)]
        resp.statusCode == HttpStatus.OK
        resp.body.size() == 1
    }

    void "findById returns 404 when missing (standardized)"() {
        when:
        controller.findById(9L)

        then:
        1 * service.findMedicalExpenseById(9L) >> null
        thrown(ResponseStatusException)
    }

    void "save returns 201 (standardized)"() {
        given:
        def m = new MedicalExpense(medicalExpenseId: 0L)

        when:
        def resp = controller.save(m)

        then:
        1 * service.insertMedicalExpense(m) >> new MedicalExpense(medicalExpenseId: 2L)
        resp.statusCode == HttpStatus.CREATED
        resp.body.medicalExpenseId == 2L
    }

    void "update returns 404 when target missing (standardized)"() {
        given:
        def patch = new MedicalExpense(medicalExpenseId: 5L)

        when:
        controller.update(5L, patch)

        then:
        1 * service.findMedicalExpenseById(5L) >> null
        thrown(ResponseStatusException)
    }

    void "update success returns 200 (standardized)"() {
        given:
        def patch = new MedicalExpense(medicalExpenseId: 7L)

        when:
        def resp = controller.update(7L, patch)

        then:
        1 * service.findMedicalExpenseById(7L) >> new MedicalExpense(medicalExpenseId: 7L)
        1 * service.updateMedicalExpense({ it.medicalExpenseId == 7L }) >> { it[0] }
        resp.statusCode == HttpStatus.OK
        resp.body.medicalExpenseId == 7L
    }

    void "getAllMedicalExpenses legacy returns 200 and 500"() {
        when:
        def ok = controller.getAllMedicalExpenses()

        then:
        1 * service.findAllMedicalExpenses() >> []
        ok.statusCode == HttpStatus.OK

        when:
        controller.getAllMedicalExpenses()

        then:
        1 * service.findAllMedicalExpenses() >> { throw new RuntimeException('boom') }
        controller.getAllMedicalExpenses().statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    void "insertMedicalExpense legacy maps conflicts and errors"() {
        given:
        def m = new MedicalExpense(medicalExpenseId: 0L)

        when:
        def resp = controller.insertMedicalExpense(m)

        then:
        1 * service.insertMedicalExpense(m) >> new MedicalExpense(medicalExpenseId: 10L)
        resp.statusCode == HttpStatus.CREATED

        when:
        controller.insertMedicalExpense(m)

        then:
        1 * service.insertMedicalExpense(m) >> { throw new DuplicateMedicalExpenseException('dup') }
        def ex1 = thrown(ResponseStatusException)
        ex1.statusCode == HttpStatus.CONFLICT

        when:
        controller.insertMedicalExpense(m)

        then:
        1 * service.insertMedicalExpense(m) >> { throw new JpaSystemException(new RuntimeException('jpa')) }
        def ex2 = thrown(ResponseStatusException)
        ex2.statusCode == HttpStatus.CONFLICT
    }

    void "insertMedicalExpenseWithInsertEndpoint legacy maps conflicts and 500"() {
        given:
        def m = new MedicalExpense(medicalExpenseId: 0L)

        when:
        def resp = controller.insertMedicalExpenseWithInsertEndpoint(m)

        then:
        1 * service.insertMedicalExpense(m) >> new MedicalExpense(medicalExpenseId: 11L)
        resp.statusCode == HttpStatus.CREATED

        when:
        controller.insertMedicalExpenseWithInsertEndpoint(m)

        then:
        1 * service.insertMedicalExpense(m) >> { throw new DuplicateMedicalExpenseException('dup2') }
        def ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.CONFLICT
    }

    void "updateClaimStatus returns 200 when true else 404"() {
        when:
        def ok = controller.updateClaimStatusPut(1L, ClaimStatus.Approved)
        def nf = controller.updateClaimStatusPatch(1L, ClaimStatus.Denied)

        then:
        1 * service.updateClaimStatus(1L, ClaimStatus.Approved) >> true
        1 * service.updateClaimStatus(1L, ClaimStatus.Denied) >> false
        ok.statusCode == HttpStatus.OK
        nf.statusCode == HttpStatus.NOT_FOUND
    }

    void "softDeleteMedicalExpense returns 200 when true else 404"() {
        when:
        def ok = controller.softDeleteMedicalExpense(3L)
        def nf = controller.softDeleteMedicalExpense(4L)

        then:
        1 * service.softDeleteMedicalExpense(3L) >> true
        1 * service.softDeleteMedicalExpense(4L) >> false
        ok.statusCode == HttpStatus.OK
        nf.statusCode == HttpStatus.NOT_FOUND
    }

    void "get totals and counts endpoints return 200"() {
        when:
        def totals = controller.getMedicalTotalsByYear(2024)
        def counts = controller.getClaimStatusCounts()

        then:
        1 * service.getTotalBilledAmountByYear(2024) >> 1G
        1 * service.getTotalPatientResponsibilityByYear(2024) >> 2G
        1 * service.getTotalInsurancePaidByYear(2024) >> 3G
        1 * service.getClaimStatusCounts() >> [(ClaimStatus.Approved): 1L]
        totals.statusCode == HttpStatus.OK
        counts.statusCode == HttpStatus.OK
    }

    void "collection filter endpoints return 200"() {
        when:
        def s1 = controller.getMedicalExpensesByClaimStatus(ClaimStatus.Submitted)
        def s2 = controller.getOutOfNetworkExpenses()
        def s3 = controller.getOutstandingPatientBalances()
        def s4 = controller.getActiveOpenClaims()
        def s5 = controller.getMedicalExpensesByProcedureCode('X')
        def s6 = controller.getMedicalExpensesByDiagnosisCode('Y')

        then:
        1 * service.findMedicalExpensesByClaimStatus(ClaimStatus.Submitted) >> []
        1 * service.findOutOfNetworkExpenses() >> []
        1 * service.findOutstandingPatientBalances() >> []
        1 * service.findActiveOpenClaims() >> []
        1 * service.findMedicalExpensesByProcedureCode('X') >> []
        1 * service.findMedicalExpensesByDiagnosisCode('Y') >> []
        s1.statusCode == HttpStatus.OK
        s2.statusCode == HttpStatus.OK
        s3.statusCode == HttpStatus.OK
        s4.statusCode == HttpStatus.OK
        s5.statusCode == HttpStatus.OK
        s6.statusCode == HttpStatus.OK
    }
}

