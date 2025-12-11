package finance.controllers

import finance.domain.Account
import finance.domain.AccountType
import finance.services.AccountService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Specification
import spock.lang.Subject

import java.math.BigDecimal
import java.sql.Timestamp
import java.util.Optional

class StandardizedAccountControllerSpec extends Specification {

    finance.repositories.AccountRepository accountRepository = Mock()
    finance.repositories.ValidationAmountRepository validationAmountRepository = Mock()
    finance.repositories.TransactionRepository transactionRepository = Mock()
    AccountService accountService = new AccountService(accountRepository, validationAmountRepository, transactionRepository)

    @Subject
    AccountController controller = new AccountController(accountService)

    def setup() {
        def validator = Mock(jakarta.validation.Validator) {
            validate(_ as Object) >> ([] as Set)
        }
        def meterRegistry = new io.micrometer.core.instrument.simple.SimpleMeterRegistry()
        def meterService = new finance.services.MeterService(meterRegistry)

        accountService.validator = validator
        accountService.meterService = meterService
    }

    private static Account acct(Map args = [:]) {
        // Helper to build a valid Account with defaults
        new Account(
            accountId: (args.accountId ?: 0L) as Long,
            accountNameOwner: (args.accountNameOwner ?: "acct_test") as String,
            accountType: (args.accountType ?: AccountType.Credit) as AccountType,
            activeStatus: (args.activeStatus ?: true) as Boolean,
            moniker: (args.moniker ?: "1234") as String,
            outstanding: (args.outstanding ?: new BigDecimal("0.00")) as BigDecimal,
            future: (args.future ?: new BigDecimal("0.00")) as BigDecimal,
            cleared: (args.cleared ?: new BigDecimal("0.00")) as BigDecimal,
            dateClosed: (args.dateClosed ?: new Timestamp(0)) as Timestamp,
            validationDate: (args.validationDate ?: new Timestamp(System.currentTimeMillis())) as Timestamp
        )
    }

    // ===== STANDARDIZED ENDPOINTS =====

    def "findAllActive returns list when present"() {
        given:
        List<Account> accounts = [acct(accountNameOwner: "a_one"), acct(accountNameOwner: "a_two")]
        and:
        // Controller calls service.updateTotalsForAllAccounts() then service.findAllActive()
        accountRepository.findByActiveStatusOrderByAccountNameOwner(true) >> accounts

        when:
        ResponseEntity<List<Account>> response = controller.findAllActive()

        then:
        response.statusCode == HttpStatus.OK
        response.body.size() == 2
        response.body[0].accountNameOwner == "a_one"
    }

    def "findAllActive returns empty list when none"() {
        given:
        accountRepository.findByActiveStatusOrderByAccountNameOwner(true) >> []

        when:
        ResponseEntity<List<Account>> response = controller.findAllActive()

        then:
        response.statusCode == HttpStatus.OK
        response.body.isEmpty()
    }

    def "findAllActive returns 500 on system error"() {
        given:
        accountRepository.findByActiveStatusOrderByAccountNameOwner(true) >> { throw new RuntimeException("db down") }

        when:
        ResponseEntity<List<Account>> response = controller.findAllActive()

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    def "findAllActive returns 500 on business error"() {
        given:
        accountRepository.findByActiveStatusOrderByAccountNameOwner(true) >> { throw new IllegalStateException("bad state") }

        when:
        ResponseEntity<List<Account>> response = controller.findAllActive()

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    def "findById returns account when found"() {
        given:
        String id = "acct_alpha"
        Account acc = acct(accountNameOwner: id)
        and:
        accountRepository.findByAccountNameOwner(id) >> Optional.of(acc)

        when:
        ResponseEntity<Account> response = controller.findById(id)

        then:
        response.statusCode == HttpStatus.OK
        response.body.accountNameOwner == id
    }

    def "findById returns 404 when missing"() {
        given:
        String id = "missing_acct"
        and:
        accountRepository.findByAccountNameOwner(id) >> Optional.empty()

        when:
        ResponseEntity<Account> response = controller.findById(id)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
    }

    def "findById returns 500 on system error"() {
        given:
        String id = "acct_err"
        and:
        accountRepository.findByAccountNameOwner(id) >> { throw new RuntimeException("boom") }

        when:
        ResponseEntity<Account> response = controller.findById(id)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    def "findById returns 500 on business error"() {
        given:
        String id = "acct_err2"
        and:
        accountRepository.findByAccountNameOwner(id) >> { throw new IllegalStateException("bad") }

        when:
        ResponseEntity<Account> response = controller.findById(id)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    def "save creates account and returns 201"() {
        given:
        Account toCreate = acct(accountId: 0L, accountNameOwner: "new_acct")
        and:
        // Ensure not duplicate
        accountRepository.findByAccountNameOwner("new_acct") >> Optional.empty()
        accountRepository.saveAndFlush(_ as Account) >> { Account a -> a.accountId = 42L; return a }

        when:
        ResponseEntity<Account> response = controller.save(toCreate)

        then:
        response.statusCode == HttpStatus.CREATED
        response.body.accountId == 42L
    }

    def "save returns 409 when duplicate exists"() {
        given:
        Account dup = acct(accountId: 0L, accountNameOwner: "dup_acct")
        and:
        accountRepository.findByAccountNameOwner("dup_acct") >> Optional.of(acct(accountNameOwner: "dup_acct"))

        when:
        ResponseEntity<Account> response = controller.save(dup)

        then:
        response.statusCode == HttpStatus.CONFLICT
    }

    def "save returns 400 on validation error"() {
        given:
        Account invalid = acct(accountId: 0L, accountNameOwner: "acct_bad")
        and:
        def violatingValidator = Mock(jakarta.validation.Validator) {
            validate(_ as Object) >> ([Mock(jakarta.validation.ConstraintViolation)] as Set)
        }
        accountService.validator = violatingValidator
        accountRepository.findByAccountNameOwner("acct_bad") >> Optional.empty()

        when:
        ResponseEntity<Account> response = controller.save(invalid)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
    }

    def "save returns 500 on system error"() {
        given:
        Account toCreate = acct(accountId: 0L, accountNameOwner: "acct_sys")
        and:
        accountRepository.findByAccountNameOwner("acct_sys") >> Optional.empty()
        accountRepository.saveAndFlush(_ as Account) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<Account> response = controller.save(toCreate)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    def "update returns 200 when account exists"() {
        given:
        Account existing = acct(accountId: 9L, accountNameOwner: "acct_upd")
        Account patch = acct(accountId: 9L, accountNameOwner: "acct_upd", moniker: "5678")
        and:
        accountRepository.findByAccountNameOwner("acct_upd") >> Optional.of(existing)
        accountRepository.saveAndFlush(_ as Account) >> { Account a -> a }

        when:
        ResponseEntity<Account> response = controller.update("acct_upd", patch)

        then:
        response.statusCode == HttpStatus.OK
        response.body.moniker == "5678"
    }

    def "update returns 404 when account missing"() {
        given:
        Account patch = acct(accountId: 888L, accountNameOwner: "nope")
        and:
        accountRepository.findByAccountNameOwner("nope") >> Optional.empty()

        when:
        ResponseEntity<Account> response = controller.update("nope", patch)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
    }

    def "update returns 400 on validation error"() {
        given:
        Account existing = acct(accountId: 10L, accountNameOwner: "acct_val")
        Account patch = acct(accountId: 10L, accountNameOwner: "acct_val", moniker: "9999")
        and:
        accountRepository.findByAccountNameOwner("acct_val") >> Optional.of(existing)
        accountRepository.saveAndFlush(_ as Account) >> { throw new jakarta.validation.ConstraintViolationException("bad", [] as Set) }

        when:
        ResponseEntity<Account> response = controller.update("acct_val", patch)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
    }

    def "update returns 409 on business error"() {
        given:
        Account existing = acct(accountId: 11L, accountNameOwner: "acct_conf")
        Account patch = acct(accountId: 11L, accountNameOwner: "acct_conf")
        and:
        accountRepository.findByAccountNameOwner("acct_conf") >> Optional.of(existing)
        accountRepository.saveAndFlush(_ as Account) >> { throw new org.springframework.dao.DataIntegrityViolationException("dup") }

        when:
        ResponseEntity<Account> response = controller.update("acct_conf", patch)

        then:
        response.statusCode == HttpStatus.CONFLICT
    }

    def "update returns 500 on system error"() {
        given:
        Account existing = acct(accountId: 12L, accountNameOwner: "acct_sys")
        Account patch = acct(accountId: 12L, accountNameOwner: "acct_sys")
        and:
        accountRepository.findByAccountNameOwner("acct_sys") >> Optional.of(existing)
        accountRepository.saveAndFlush(_ as Account) >> { throw new RuntimeException("boom") }

        when:
        ResponseEntity<Account> response = controller.update("acct_sys", patch)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    def "deleteById returns 200 with deleted account when found"() {
        given:
        String id = "acct_del"
        Account existing = acct(accountId: 100L, accountNameOwner: id)
        and:
        // First for controller pre-fetch, second for service delete path
        2 * accountRepository.findByAccountNameOwner(id) >> Optional.of(existing)
        validationAmountRepository.findByAccountId(100L) >> []

        when:
        ResponseEntity<Account> response = controller.deleteById(id)

        then:
        response.statusCode == HttpStatus.OK
        response.body.accountNameOwner == id
    }

    def "deleteById returns 404 when missing"() {
        given:
        String id = "nope"
        and:
        accountRepository.findByAccountNameOwner(id) >> Optional.empty()

        when:
        ResponseEntity<Account> response = controller.deleteById(id)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
    }

    def "deleteById returns 500 on system error"() {
        given:
        String id = "acct_err_del"
        Account existing = acct(accountNameOwner: id)
        and:
        accountRepository.findByAccountNameOwner(id) >>> [Optional.of(existing), { throw new RuntimeException("db") }]

        when:
        ResponseEntity<Account> response = controller.deleteById(id)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    def "deleteById returns 500 on business error"() {
        given:
        String id = "acct_bus_del"
        Account existing = acct(accountNameOwner: id)
        and:
        2 * accountRepository.findByAccountNameOwner(id) >> Optional.of(existing)
        accountRepository.delete(_ as Account) >> { throw new org.springframework.dao.DataIntegrityViolationException("conflict") }

        when:
        ResponseEntity<Account> response = controller.deleteById(id)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== BUSINESS/LEGACY ENDPOINTS =====
    def "computeAccountTotals returns 200 with totals map"() {
        given:
        accountRepository.sumOfAllTransactionsByTransactionState("cleared") >> new BigDecimal("1.00")
        accountRepository.sumOfAllTransactionsByTransactionState("future") >> new BigDecimal("2.00")
        accountRepository.sumOfAllTransactionsByTransactionState("outstanding") >> new BigDecimal("3.00")

        when:
        ResponseEntity<Map<String, String>> response = controller.computeAccountTotals()

        then:
        response.statusCode == HttpStatus.OK
        response.body.containsKey("totalsCleared")
        response.body.containsKey("totalsFuture")
        response.body.containsKey("totalsOutstanding")
        response.body.containsKey("totals")
    }

    def "renameAccountNameOwner returns 200"() {
        given:
        Account existing = acct(accountId: 60L, accountNameOwner: "old")
        and:
        accountRepository.findByAccountNameOwner("old") >> Optional.of(existing)
        accountRepository.saveAndFlush(_ as Account) >> { Account a -> a }

        when:
        ResponseEntity<Account> response = controller.renameAccountNameOwner("old", "new")

        then:
        response.statusCode == HttpStatus.OK
        response.body.accountNameOwner == "new"
    }

    def "renameAccountNameOwner returns 409 on conflict"() {
        given:
        Account existing = acct(accountId: 61L, accountNameOwner: "old")
        and:
        accountRepository.findByAccountNameOwner("old") >> Optional.of(existing)
        accountRepository.saveAndFlush(_ as Account) >> { throw new org.springframework.dao.DataIntegrityViolationException("dup") }

        when:
        def resp = controller.renameAccountNameOwner("old", "new")

        then:
        thrown(org.springframework.web.server.ResponseStatusException)
    }

    def "deactivateAccount returns 200"() {
        given:
        Account existing = acct(accountId: 70L, accountNameOwner: "acct_d")
        and:
        accountRepository.findByAccountNameOwner("acct_d") >> Optional.of(existing)
        accountRepository.saveAndFlush(_ as Account) >> { Account a -> a }

        when:
        ResponseEntity<Account> response = controller.deactivateAccount("acct_d")

        then:
        response.statusCode == HttpStatus.OK
        !response.body.activeStatus
    }

    def "deactivateAccount returns 404 when missing"() {
        given:
        accountRepository.findByAccountNameOwner("missing") >> Optional.empty()

        when:
        controller.deactivateAccount("missing")

        then:
        def ex = thrown(org.springframework.web.server.ResponseStatusException)
        ex.statusCode == HttpStatus.NOT_FOUND
    }

    def "activateAccount returns 200"() {
        given:
        Account existing = acct(accountId: 80L, accountNameOwner: "acct_a", activeStatus: false)
        and:
        accountRepository.findByAccountNameOwner("acct_a") >> Optional.of(existing)
        accountRepository.saveAndFlush(_ as Account) >> { Account a -> a }

        when:
        ResponseEntity<Account> response = controller.activateAccount("acct_a")

        then:
        response.statusCode == HttpStatus.OK
        response.body.activeStatus
    }

    // ===== validation/refresh endpoint =====
    def "refreshValidationDates returns 204 on success"() {
        when:
        ResponseEntity<Void> response = controller.refreshValidationDates()

        then:
        response.statusCode == HttpStatus.NO_CONTENT
    }

    def "refreshValidationDates returns 500 on failure"() {
        given:
        accountRepository.updateValidationDateForAllAccounts() >> { throw new RuntimeException("boom") }

        when:
        ResponseEntity<Void> response = controller.refreshValidationDates()

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }
}
