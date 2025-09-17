package finance.services

import finance.domain.ClaimStatus
import finance.domain.MedicalExpense
import finance.repositories.MedicalExpenseRepository
import spock.lang.Specification

import java.math.BigDecimal
import java.sql.Date

class MedicalExpenseServiceAdditionalSpec extends Specification {

    MedicalExpenseRepository repo = Mock()
    MedicalExpenseService svc = new MedicalExpenseService(repo)

    void "query helpers proxy to repository (date ranges and filters)"() {
        given:
        def start = Date.valueOf('2024-01-01')
        def end = Date.valueOf('2024-12-31')

        when:
        svc.findMedicalExpensesByServiceDateRange(start, end)
        svc.findMedicalExpensesByAccountIdAndDateRange(7L, start, end)
        svc.findMedicalExpensesByProviderId(2L)
        svc.findMedicalExpensesByFamilyMemberId(3L)
        svc.findMedicalExpensesByFamilyMemberAndDateRange(3L, start, end)
        svc.findMedicalExpensesByClaimStatus(ClaimStatus.Processing)
        svc.findOutOfNetworkExpenses()
        svc.findOutstandingPatientBalances()
        svc.findActiveOpenClaims()
        svc.findUnpaidMedicalExpenses()
        svc.findPartiallyPaidMedicalExpenses()
        svc.findFullyPaidMedicalExpenses()
        svc.findMedicalExpensesWithoutTransaction()
        svc.findOverpaidMedicalExpenses()

        then:
        1 * repo.findByServiceDateBetweenAndActiveStatusTrue(start, end) >> []
        1 * repo.findByAccountIdAndServiceDateBetween(7L, start, end) >> []
        1 * repo.findByProviderIdAndActiveStatusTrue(2L) >> []
        1 * repo.findByFamilyMemberIdAndActiveStatusTrue(3L) >> []
        1 * repo.findByFamilyMemberIdAndServiceDateBetween(3L, start, end) >> []
        1 * repo.findByClaimStatusAndActiveStatusTrue(ClaimStatus.Processing) >> []
        1 * repo.findByIsOutOfNetworkAndActiveStatusTrue(true) >> []
        1 * repo.findOutstandingPatientBalances() >> []
        1 * repo.findActiveOpenClaims() >> []
        1 * repo.findUnpaidMedicalExpenses() >> []
        1 * repo.findPartiallyPaidMedicalExpenses() >> []
        1 * repo.findFullyPaidMedicalExpenses() >> []
        1 * repo.findMedicalExpensesWithoutTransaction() >> []
        1 * repo.findOverpaidMedicalExpenses() >> []
    }

    void "totals default to zero on null and return values when present"() {
        when:
        def pr = svc.getTotalPatientResponsibilityByYear(2024)
        def ip = svc.getTotalInsurancePaidByYear(2024)
        def paid = svc.getTotalPaidAmountByYear(2024)
        def unpaid = svc.getTotalUnpaidBalance()

        then:
        1 * repo.getTotalPatientResponsibilityByYear(2024) >> null
        1 * repo.getTotalInsurancePaidByYear(2024) >> null
        1 * repo.getTotalPaidAmountByYear(2024) >> null
        1 * repo.getTotalUnpaidBalance() >> null
        pr == BigDecimal.ZERO
        ip == BigDecimal.ZERO
        paid == BigDecimal.ZERO
        unpaid == BigDecimal.ZERO

        when:
        def pr2 = svc.getTotalPatientResponsibilityByYear(2025)
        def ip2 = svc.getTotalInsurancePaidByYear(2025)
        def paid2 = svc.getTotalPaidAmountByYear(2025)
        def unpaid2 = svc.getTotalUnpaidBalance()

        then:
        1 * repo.getTotalPatientResponsibilityByYear(2025) >> new BigDecimal('12.34')
        1 * repo.getTotalInsurancePaidByYear(2025) >> new BigDecimal('56.78')
        1 * repo.getTotalPaidAmountByYear(2025) >> new BigDecimal('90.12')
        1 * repo.getTotalUnpaidBalance() >> new BigDecimal('3.21')
        pr2 == new BigDecimal('12.34')
        ip2 == new BigDecimal('56.78')
        paid2 == new BigDecimal('90.12')
        unpaid2 == new BigDecimal('3.21')
    }

    void "claim status counts aggregates by enum"() {
        when:
        def map = svc.getClaimStatusCounts()

        then:
        ClaimStatus.values().each { status ->
            1 * repo.countByClaimStatusAndActiveStatusTrue(status) >> 0L
        }
        map.keySet().containsAll(ClaimStatus.values() as Set)
    }
}
