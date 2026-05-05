package finance.controllers

import finance.domain.*
import finance.services.TransactionService
import finance.services.MeterService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Specification
import spock.lang.Subject
import java.math.BigDecimal
import java.time.LocalDate
import java.sql.Timestamp

class TransactionControllerSpec extends Specification {

    TransactionService standardizedTransactionService = Mock()
    MeterService meterService = Mock()

    @Subject
    TransactionController controller = new TransactionController(standardizedTransactionService, meterService)

    // ===== STANDARDIZED ENDPOINTS TESTS =====

    def "findAllActive returns list of transactions and 200"() {
        given:
        def transactions = [createValidTransaction("guid-1", "checking_brian"), createValidTransaction("guid-2", "checking_brian")]
        and:
        standardizedTransactionService.findAllActive() >> ServiceResult.Success.of(transactions)

        when:
        ResponseEntity<List<Transaction>> response = controller.findAllActive()

        then:
        response.statusCode == HttpStatus.OK
        response.body.size() == 2
    }

    def "findAllActive returns 404 when no active transactions found"() {
        given:
        standardizedTransactionService.findAllActive() >> ServiceResult.NotFound.of("No transactions found")

        when:
        ResponseEntity<List<Transaction>> response = controller.findAllActive()

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        response.body == null
    }

    def "findAllActive returns 500 on system error"() {
        given:
        standardizedTransactionService.findAllActive() >> ServiceResult.SystemError.of(new RuntimeException("db down"))

        when:
        ResponseEntity<List<Transaction>> response = controller.findAllActive()

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        response.body == null
    }

    def "findById returns transaction when found"() {
        given:
        Transaction transaction = createValidTransaction("test-guid-123", "test_account")
        and:
        standardizedTransactionService.findById("test-guid-123") >> ServiceResult.Success.of(transaction)

        when:
        ResponseEntity<Transaction> response = controller.findById("test-guid-123")

        then:
        response.statusCode == HttpStatus.OK
        response.body.guid == "test-guid-123"
        response.body.accountNameOwner == "test_account"
    }

    def "findById returns 404 when transaction not found"() {
        given:
        standardizedTransactionService.findById("non-existent-guid") >> ServiceResult.NotFound.of("Transaction not found: non-existent-guid")

        when:
        ResponseEntity<Transaction> response = controller.findById("non-existent-guid")

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        response.body == null
        1 * meterService.incrementTransactionRestSelectNoneFoundCounter("unknown")
    }

    def "findById returns 500 on system error"() {
        given:
        standardizedTransactionService.findById("error-guid") >> ServiceResult.SystemError.of(new RuntimeException("Database error"))

        when:
        ResponseEntity<Transaction> response = controller.findById("error-guid")

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        response.body == null
    }

    def "findById returns 400 on validation error result type"() {
        given:
        standardizedTransactionService.findById("guid") >> ServiceResult.ValidationError.of([:])

        when:
        ResponseEntity<Transaction> response = controller.findById("guid")

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        response.body == null
    }

    def "save creates transaction successfully with 201"() {
        given:
        Transaction input = createValidTransaction("new-guid-456", "new_account")
        Transaction saved = createValidTransaction("new-guid-456", "new_account")
        saved.transactionId = 123L
        and:
        standardizedTransactionService.save(_ as Transaction) >> ServiceResult.Success.of(saved)

        when:
        ResponseEntity<Transaction> response = controller.save(input)

        then:
        response.statusCode == HttpStatus.CREATED
        response.body.transactionId == 123L
        response.body.guid == "new-guid-456"
    }

    def "save handles validation errors with 400"() {
        given:
        Transaction invalid = createValidTransaction("invalid-guid", "test_account")
        and:
        standardizedTransactionService.save(_ as Transaction) >> ServiceResult.ValidationError.of(["amount": "Amount must be positive"])

        when:
        ResponseEntity<Transaction> response = controller.save(invalid)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        response.body == null
    }

    def "save handles business errors with 409"() {
        given:
        Transaction duplicate = createValidTransaction("duplicate-guid", "test_account")
        and:
        standardizedTransactionService.save(_ as Transaction) >> ServiceResult.BusinessError.of("Transaction already exists", "DUPLICATE")

        when:
        ResponseEntity<Transaction> response = controller.save(duplicate)

        then:
        response.statusCode == HttpStatus.CONFLICT
        response.body == null
    }

    def "save handles system errors with 500"() {
        given:
        Transaction input = createValidTransaction("system-error-guid", "test_account")
        and:
        standardizedTransactionService.save(_ as Transaction) >> ServiceResult.SystemError.of(new RuntimeException("Database connection failed"))

        when:
        ResponseEntity<Transaction> response = controller.save(input)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        response.body == null
    }

    def "save returns 404 when service returns NotFound"() {
        given:
        Transaction input = createValidTransaction("unexpected-guid", "test_account")
        and:
        standardizedTransactionService.save(_ as Transaction) >> ServiceResult.NotFound.of("Unexpected result")

        when:
        ResponseEntity<Transaction> response = controller.save(input)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        response.body == null
    }

    def "update modifies transaction successfully with 200"() {
        given:
        String guid = "update-guid-789"
        Transaction input = createValidTransaction("original-guid", "test_account")
        Transaction updated = createValidTransaction(guid, "updated_account")
        and:
        standardizedTransactionService.update(_ as Transaction) >> { Transaction t ->
            assert t.guid == guid // Verify guid was set to path parameter
            ServiceResult.Success.of(updated)
        }

        when:
        ResponseEntity<Transaction> response = controller.update(guid, input)

        then:
        response.statusCode == HttpStatus.OK
        response.body.guid == guid
        response.body.accountNameOwner == "updated_account"
    }

    def "update returns 404 when transaction not found"() {
        given:
        Transaction input = createValidTransaction("not-found-guid", "test_account")
        and:
        standardizedTransactionService.update(_ as Transaction) >> ServiceResult.NotFound.of("Transaction not found")

        when:
        ResponseEntity<Transaction> response = controller.update("not-found-guid", input)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        response.body == null
    }

    def "update handles validation errors with 400"() {
        given:
        Transaction input = createValidTransaction("validation-error-guid", "test_account")
        and:
        standardizedTransactionService.update(_ as Transaction) >> ServiceResult.ValidationError.of(["category": "Invalid category"])

        when:
        ResponseEntity<Transaction> response = controller.update("validation-error-guid", input)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        response.body == null
    }

    def "update handles business errors with 409"() {
        given:
        Transaction input = createValidTransaction("business-error-guid", "test_account")
        and:
        standardizedTransactionService.update(_ as Transaction) >> ServiceResult.BusinessError.of("Business constraint violated", "CONSTRAINT")

        when:
        ResponseEntity<Transaction> response = controller.update("business-error-guid", input)

        then:
        response.statusCode == HttpStatus.CONFLICT
        response.body == null
    }

    def "update handles system errors with 500"() {
        given:
        Transaction input = createValidTransaction("system-update-error", "test_account")
        and:
        standardizedTransactionService.update(_ as Transaction) >> ServiceResult.SystemError.of(new RuntimeException("Update failed"))

        when:
        ResponseEntity<Transaction> response = controller.update("system-update-error", input)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        response.body == null
    }

    def "deleteById returns deleted transaction with 200"() {
        given:
        String guid = "delete-guid-101"
        Transaction existingTransaction = createValidTransaction(guid, "to_delete_account")
        and:
        standardizedTransactionService.deleteById(guid) >> ServiceResult.Success.of(existingTransaction)

        when:
        ResponseEntity<Transaction> response = controller.deleteById(guid)

        then:
        response.statusCode == HttpStatus.OK
        response.body.guid == guid
        response.body.accountNameOwner == "to_delete_account"
    }

    def "deleteById returns 404 when transaction not found for deletion"() {
        given:
        String guid = "non-existent-delete"
        and:
        standardizedTransactionService.deleteById(guid) >> ServiceResult.NotFound.of("Transaction not found")

        when:
        ResponseEntity<Transaction> response = controller.deleteById(guid)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        response.body == null
    }

    def "deleteById returns 404 when delete operation finds no entity"() {
        given:
        String guid = "delete-not-found"
        Transaction existingTransaction = createValidTransaction(guid, "existing_account")
        and:
        standardizedTransactionService.findById(guid) >> ServiceResult.Success.of(existingTransaction)
        standardizedTransactionService.deleteById(guid) >> ServiceResult.NotFound.of("Transaction not found for deletion")

        when:
        ResponseEntity<Transaction> response = controller.deleteById(guid)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        response.body == null
    }

    def "deleteById handles system errors with 500"() {
        given:
        String guid = "delete-system-error"
        Transaction existingTransaction = createValidTransaction(guid, "system_error_account")
        and:
        standardizedTransactionService.findById(guid) >> ServiceResult.Success.of(existingTransaction)
        standardizedTransactionService.deleteById(guid) >> ServiceResult.SystemError.of(new RuntimeException("Delete failed"))

        when:
        ResponseEntity<Transaction> response = controller.deleteById(guid)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        response.body == null
    }

    def "deleteById returns 409 when transaction is referenced by a payment"() {
        given:
        String guid = "delete-payment-ref"
        and:
        standardizedTransactionService.deleteById(guid) >> ServiceResult.BusinessError.of(
            "Cannot delete transaction $guid because it is referenced by 1 payment. Please delete the related payments first.",
            "DATA_INTEGRITY_VIOLATION"
        )

        when:
        ResponseEntity<Transaction> response = controller.deleteById(guid)

        then:
        response.statusCode == HttpStatus.CONFLICT
        response.body == null
    }

    // ===== LEGACY ENDPOINTS TESTS =====

    def "selectByAccountNameOwner returns transactions for account"() {
        given:
        String accountNameOwner = "legacy_account_test"
        List<Transaction> transactions = [
            createValidTransaction("legacy-guid-1", accountNameOwner),
            createValidTransaction("legacy-guid-2", accountNameOwner)
        ]
        and:
        standardizedTransactionService.findByAccountNameOwnerOrderByTransactionDateStandardized(accountNameOwner) >> ServiceResult.Success.of(transactions)

        when:
        ResponseEntity<List<Transaction>> response = controller.selectByAccountNameOwner(accountNameOwner)

        then:
        response.statusCode == HttpStatus.OK
        response.body.size() == 2
        response.body[0].accountNameOwner == accountNameOwner
        response.body[1].accountNameOwner == accountNameOwner
    }

    def "selectByAccountNameOwner returns empty list when no transactions found"() {
        given:
        String accountNameOwner = "empty_account"
        and:
        standardizedTransactionService.findByAccountNameOwnerOrderByTransactionDateStandardized(accountNameOwner) >> ServiceResult.Success.of([])

        when:
        ResponseEntity<List<Transaction>> response = controller.selectByAccountNameOwner(accountNameOwner)

        then:
        response.statusCode == HttpStatus.OK
        response.body.isEmpty()
    }

    def "selectByAccountNameOwner returns 500 on service error"() {
        given:
        String accountNameOwner = "error_account"
        and:
        standardizedTransactionService.findByAccountNameOwnerOrderByTransactionDateStandardized(accountNameOwner) >> ServiceResult.SystemError.of(new RuntimeException("Service error"))

        when:
        ResponseEntity<List<Transaction>> response = controller.selectByAccountNameOwner(accountNameOwner)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        response.body == null
    }

    def "selectTotalsCleared returns totals for account"() {
        given:
        String accountNameOwner = "totals_account"
        Totals totals = createValidTotals()
        and:
        standardizedTransactionService.calculateActiveTotalsByAccountNameOwner(accountNameOwner) >> totals

        when:
        ResponseEntity<Totals> response = controller.selectTotalsCleared(accountNameOwner)

        then:
        response.statusCode == HttpStatus.OK
        response.body.totalsCleared == BigDecimal.valueOf(1000.00)
        response.body.totalsOutstanding == BigDecimal.valueOf(200.00)
        response.body.totalsFuture == BigDecimal.valueOf(300.00)
    }

    def "selectTotalsCleared throws on service error"() {
        given:
        String accountNameOwner = "totals_error_account"
        and:
        standardizedTransactionService.calculateActiveTotalsByAccountNameOwner(accountNameOwner) >> {
            throw new RuntimeException("Calculation failed")
        }

        when:
        controller.selectTotalsCleared(accountNameOwner)

        then:
        thrown(RuntimeException)
    }

    def "updateTransactionState updates state successfully"() {
        given:
        String guid = "state-update-guid"
        String stateValue = "cleared"
        Transaction result = createValidTransaction(guid, "state_account")
        result.transactionState = TransactionState.Cleared
        and:
        standardizedTransactionService.updateTransactionStateStandardized(guid, TransactionState.Cleared) >> ServiceResult.Success.of(result)

        when:
        ResponseEntity<Transaction> response = controller.updateTransactionState(guid, stateValue)

        then:
        response.statusCode == HttpStatus.OK
        response.body.guid == guid
        response.body.transactionState == TransactionState.Cleared
    }

    def "updateTransactionState handles case insensitive state values"() {
        given:
        String guid = "case-state-guid"
        String stateValue = "OUTSTANDING"
        Transaction result = createValidTransaction(guid, "case_account")
        result.transactionState = TransactionState.Outstanding
        and:
        standardizedTransactionService.updateTransactionStateStandardized(guid, TransactionState.Outstanding) >> ServiceResult.Success.of(result)

        when:
        ResponseEntity<Transaction> response = controller.updateTransactionState(guid, stateValue)

        then:
        response.statusCode == HttpStatus.OK
        response.body.transactionState == TransactionState.Outstanding
    }

    def "updateTransactionState throws 400 for invalid state"() {
        given:
        String guid = "invalid-state-guid"
        String invalidState = "invalid_state"

        when:
        controller.updateTransactionState(guid, invalidState)

        then:
        def ex = thrown(org.springframework.web.server.ResponseStatusException)
        ex.statusCode == HttpStatus.BAD_REQUEST
        ex.reason.contains("Invalid transaction state: $invalidState")
    }

    def "updateTransactionState throws 500 on service error"() {
        given:
        String guid = "state-error-guid"
        String stateValue = "future"
        and:
        standardizedTransactionService.updateTransactionStateStandardized(guid, TransactionState.Future) >> ServiceResult.SystemError.of(new RuntimeException("State update failed"))

        when:
        controller.updateTransactionState(guid, stateValue)

        then:
        def ex = thrown(org.springframework.web.server.ResponseStatusException)
        ex.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        ex.reason.contains("Failed to update transaction state")
    }

    def "insertFutureTransaction creates future transaction with 201"() {
        given:
        Transaction input = createValidTransaction("future-insert", "future_account")
        Transaction result = createValidTransaction("future-insert", "future_account")
        result.transactionId = 888L
        and:
        standardizedTransactionService.createAndSaveFutureTransaction(input) >> ServiceResult.Success.of(result)

        when:
        ResponseEntity<Transaction> response = controller.insertFutureTransaction(input)

        then:
        response.statusCode == HttpStatus.CREATED
        response.body.transactionId == 888L
        response.body.guid == "future-insert"
    }

    def "insertFutureTransaction throws 409 on data integrity violation"() {
        given:
        Transaction input = createValidTransaction("future-dup", "future_dup_account")
        and:
        standardizedTransactionService.createAndSaveFutureTransaction(input) >> ServiceResult.BusinessError.of("Duplicate future transaction", "DUPLICATE_ENTITY")

        when:
        controller.insertFutureTransaction(input)

        then:
        def ex = thrown(org.springframework.web.server.ResponseStatusException)
        ex.statusCode == HttpStatus.CONFLICT
        ex.reason.contains("Duplicate future transaction found")
    }

    def "insertFutureTransaction throws 400 on validation error"() {
        given:
        Transaction input = createValidTransaction("future-validation", "future_validation_account")
        and:
        standardizedTransactionService.createAndSaveFutureTransaction(input) >> ServiceResult.ValidationError.of(["future": "Future transaction validation failed"])

        when:
        controller.insertFutureTransaction(input)

        then:
        def ex = thrown(org.springframework.web.server.ResponseStatusException)
        ex.statusCode == HttpStatus.BAD_REQUEST
        ex.reason.contains("Validation error: {future=Future transaction validation failed}")
    }

    def "insertFutureTransaction throws 500 on unexpected error"() {
        given:
        Transaction input = createValidTransaction("future-error", "future_error_account")
        and:
        standardizedTransactionService.createAndSaveFutureTransaction(input) >> ServiceResult.SystemError.of(new RuntimeException("Future transaction creation failed"))

        when:
        controller.insertFutureTransaction(input)

        then:
        def ex = thrown(org.springframework.web.server.ResponseStatusException)
        ex.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    def "changeTransactionAccountNameOwner updates account successfully"() {
        given:
        def payload = new finance.controllers.dto.TransactionAccountChangeInputDto(
            "340c315d-39ad-4a02-a294-84a74c1c7ddc", "new_account_owner"
        )
        Transaction result = createValidTransaction("340c315d-39ad-4a02-a294-84a74c1c7ddc", "new_account_owner")
        and:
        standardizedTransactionService.changeAccountNameOwnerStandardized("new_account_owner", "340c315d-39ad-4a02-a294-84a74c1c7ddc") >> ServiceResult.Success.of(result)

        when:
        ResponseEntity<Transaction> response = controller.changeTransactionAccountNameOwner(payload)

        then:
        response.statusCode == HttpStatus.OK
        response.body.guid == "340c315d-39ad-4a02-a294-84a74c1c7ddc"
        response.body.accountNameOwner == "new_account_owner"
    }

    def "changeTransactionAccountNameOwner returns 404 when not found"() {
        given:
        def payload = new finance.controllers.dto.TransactionAccountChangeInputDto(
            "340c315d-39ad-4a02-a294-84a74c1c7ddc", "new_account_owner"
        )
        and:
        standardizedTransactionService.changeAccountNameOwnerStandardized("new_account_owner", "340c315d-39ad-4a02-a294-84a74c1c7ddc") >> ServiceResult.NotFound.of("Transaction or account not found")

        when:
        ResponseEntity<Transaction> response = controller.changeTransactionAccountNameOwner(payload)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
    }

    def "changeTransactionAccountNameOwner returns 500 on system error"() {
        given:
        def payload = new finance.controllers.dto.TransactionAccountChangeInputDto(
            "340c315d-39ad-4a02-a294-84a74c1c7ddc", "error_account"
        )
        and:
        standardizedTransactionService.changeAccountNameOwnerStandardized("error_account", "340c315d-39ad-4a02-a294-84a74c1c7ddc") >> new ServiceResult.SystemError(new RuntimeException("Account change failed"))

        when:
        ResponseEntity<Transaction> response = controller.changeTransactionAccountNameOwner(payload)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    def "updateTransactionReceiptImageByGuid updates image successfully"() {
        given:
        String guid = "image-update-guid"
        String payload = "base64encodedimagedata"
        ReceiptImage result = createValidReceiptImage(123L, 456L)
        and:
        standardizedTransactionService.updateTransactionReceiptImageByGuidStandardized(guid, payload) >> ServiceResult.Success.of(result)

        when:
        ResponseEntity<ReceiptImage> response = controller.updateTransactionReceiptImageByGuid(guid, payload)

        then:
        response.statusCode == HttpStatus.OK
        response.body.receiptImageId == 123L
        response.body.transactionId == 456L
    }

    def "updateTransactionReceiptImageByGuid throws 500 on service error"() {
        given:
        String guid = "image-error-guid"
        String payload = "invalidimagedata"
        and:
        standardizedTransactionService.updateTransactionReceiptImageByGuidStandardized(guid, payload) >> ServiceResult.SystemError.of(new RuntimeException("Image processing failed"))

        when:
        controller.updateTransactionReceiptImageByGuid(guid, payload)

        then:
        def ex = thrown(org.springframework.web.server.ResponseStatusException)
        ex.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        ex.reason.contains("Failed to update receipt image")
    }

    def "selectTransactionsByCategory returns transactions for category"() {
        given:
        String categoryName = "test_category"
        List<Transaction> transactions = [
            createValidTransaction("cat-guid-1", "cat_account_1"),
            createValidTransaction("cat-guid-2", "cat_account_2")
        ]
        and:
        standardizedTransactionService.findTransactionsByCategoryStandardized(categoryName) >> ServiceResult.Success.of(transactions)

        when:
        ResponseEntity<List<Transaction>> response = controller.selectTransactionsByCategory(categoryName)

        then:
        response.statusCode == HttpStatus.OK
        response.body.size() == 2
        response.body[0].guid == "cat-guid-1"
        response.body[1].guid == "cat-guid-2"
    }

    def "selectTransactionsByCategory returns empty list when none found"() {
        given:
        String categoryName = "empty_category"
        and:
        standardizedTransactionService.findTransactionsByCategoryStandardized(categoryName) >> ServiceResult.Success.of([])

        when:
        ResponseEntity<List<Transaction>> response = controller.selectTransactionsByCategory(categoryName)

        then:
        response.statusCode == HttpStatus.OK
        response.body.isEmpty()
    }

    def "selectTransactionsByCategory returns 500 on service error"() {
        given:
        String categoryName = "error_category"
        and:
        standardizedTransactionService.findTransactionsByCategoryStandardized(categoryName) >> ServiceResult.SystemError.of(new RuntimeException("Category search failed"))

        when:
        ResponseEntity<List<Transaction>> response = controller.selectTransactionsByCategory(categoryName)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        response.body == null
    }

    def "selectTransactionsByDescription returns transactions for description"() {
        given:
        String descriptionName = "test_description"
        List<Transaction> transactions = [
            createValidTransaction("desc-guid-1", "desc_account_1"),
            createValidTransaction("desc-guid-2", "desc_account_2")
        ]
        and:
        standardizedTransactionService.findTransactionsByDescriptionStandardized(descriptionName) >> ServiceResult.Success.of(transactions)

        when:
        ResponseEntity<List<Transaction>> response = controller.selectTransactionsByDescription(descriptionName)

        then:
        response.statusCode == HttpStatus.OK
        response.body.size() == 2
        response.body[0].guid == "desc-guid-1"
        response.body[1].guid == "desc-guid-2"
    }

    def "selectTransactionsByDescription returns empty list when none found"() {
        given:
        String descriptionName = "empty_description"
        and:
        standardizedTransactionService.findTransactionsByDescriptionStandardized(descriptionName) >> ServiceResult.Success.of([])

        when:
        ResponseEntity<List<Transaction>> response = controller.selectTransactionsByDescription(descriptionName)

        then:
        response.statusCode == HttpStatus.OK
        response.body.isEmpty()
    }

    def "selectTransactionsByDescription returns 500 on service error"() {
        given:
        String descriptionName = "error_description"
        and:
        standardizedTransactionService.findTransactionsByDescriptionStandardized(descriptionName) >> ServiceResult.SystemError.of(new RuntimeException("Description search failed"))

        when:
        ResponseEntity<List<Transaction>> response = controller.selectTransactionsByDescription(descriptionName)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        response.body == null
    }

    // ===== findAllActivePaged =====
    def "findAllActivePaged returns page of transactions"() {
        given:
        def transactions = [createValidTransaction("p1", "acct1")]
        def pageable = org.springframework.data.domain.PageRequest.of(0, 20)
        def page = new org.springframework.data.domain.PageImpl<>(transactions, pageable, 1)
        and:
        standardizedTransactionService.findAllActive(pageable) >> ServiceResult.Success.of(page)

        when:
        ResponseEntity<?> response = controller.findAllActivePaged(pageable)

        then:
        response.statusCode == HttpStatus.OK
    }

    // ===== findByDateRange =====
    def "findByDateRange returns page of transactions for given range"() {
        given:
        def start = LocalDate.of(2024, 1, 1)
        def end = LocalDate.of(2024, 1, 31)
        def transactions = [createValidTransaction("dr1", "acct1")]
        def page = new org.springframework.data.domain.PageImpl<>(transactions)
        and:
        standardizedTransactionService.findTransactionsByDateRangeStandardized(start, end, _) >> ServiceResult.Success.of(page)

        when:
        ResponseEntity<?> response = controller.findByDateRange(start, end, 0, 20)

        then:
        response.statusCode == HttpStatus.OK
    }

    // ===== selectByAccountNameOwnerPaged =====
    def "selectByAccountNameOwnerPaged returns page of transactions for account"() {
        given:
        def accountNameOwner = "test_account"
        def transactions = [createValidTransaction("pp1", accountNameOwner)]
        def pageable = org.springframework.data.domain.PageRequest.of(0, 10)
        def page = new org.springframework.data.domain.PageImpl<>(transactions, pageable, 1)
        and:
        standardizedTransactionService.findByAccountNameOwnerOrderByTransactionDateStandardized(accountNameOwner, pageable) >> ServiceResult.Success.of(page)

        when:
        ResponseEntity<?> response = controller.selectByAccountNameOwnerPaged(accountNameOwner, pageable)

        then:
        response.statusCode == HttpStatus.OK
    }

    // ===== updateTransactionState error branches =====
    def "updateTransactionState returns 404 when transaction not found"() {
        given:
        standardizedTransactionService.updateTransactionStateStandardized("missing-guid", TransactionState.Cleared) >> ServiceResult.NotFound.of("Transaction not found")

        when:
        ResponseEntity<?> response = controller.updateTransactionState("missing-guid", "cleared")

        then:
        response.statusCode == HttpStatus.NOT_FOUND
    }

    def "updateTransactionState throws 400 on ValidationError"() {
        given:
        standardizedTransactionService.updateTransactionStateStandardized("guid-1", TransactionState.Cleared) >> ServiceResult.ValidationError.of(["state": "invalid"])

        when:
        controller.updateTransactionState("guid-1", "cleared")

        then:
        def ex = thrown(org.springframework.web.server.ResponseStatusException)
        ex.statusCode == HttpStatus.BAD_REQUEST
    }

    def "updateTransactionState throws 400 on BusinessError"() {
        given:
        standardizedTransactionService.updateTransactionStateStandardized("guid-2", TransactionState.Cleared) >> ServiceResult.BusinessError.of("Cannot update", "DOMAIN_RULE_VIOLATION")

        when:
        controller.updateTransactionState("guid-2", "cleared")

        then:
        def ex = thrown(org.springframework.web.server.ResponseStatusException)
        ex.statusCode == HttpStatus.BAD_REQUEST
        ex.reason.contains("Cannot update")
    }

    // ===== deleteTransactionReceiptImageByGuid =====
    def "deleteTransactionReceiptImageByGuid returns 200 on success"() {
        given:
        standardizedTransactionService.deleteReceiptImageForTransactionByGuidStandardized("guid-del") >> ServiceResult.Success.of(null)

        when:
        ResponseEntity<Void> response = controller.deleteTransactionReceiptImageByGuid("guid-del")

        then:
        response.statusCode == HttpStatus.OK
    }

    def "deleteTransactionReceiptImageByGuid returns 404 when not found"() {
        given:
        standardizedTransactionService.deleteReceiptImageForTransactionByGuidStandardized("guid-missing") >> ServiceResult.NotFound.of("not found")

        when:
        ResponseEntity<Void> response = controller.deleteTransactionReceiptImageByGuid("guid-missing")

        then:
        response.statusCode == HttpStatus.NOT_FOUND
    }

    def "deleteTransactionReceiptImageByGuid returns 500 on error"() {
        given:
        standardizedTransactionService.deleteReceiptImageForTransactionByGuidStandardized("guid-err") >> ServiceResult.SystemError.of(new RuntimeException("error"))

        when:
        ResponseEntity<Void> response = controller.deleteTransactionReceiptImageByGuid("guid-err")

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== Helper Methods =====

    private Transaction createValidTransaction(String guid, String accountNameOwner) {
        Transaction transaction = new Transaction(
            0L,                                    // transactionId
            "test_owner",                          // owner
            guid,                                  // guid
            1L,                                   // accountId
            AccountType.Credit,                   // accountType
            TransactionType.Expense,             // transactionType
            accountNameOwner,                     // accountNameOwner
            LocalDate.of(2024, 1, 1),            // transactionDate
            "test description",                   // description
            "test_category",                      // category
            BigDecimal.valueOf(100.00),          // amount
            TransactionState.Outstanding,        // transactionState
            true,                                // activeStatus
            ReoccurringType.Undefined,           // reoccurringType
            "test notes"                         // notes
        )

        // Set additional fields
        transaction.dateUpdated = new Timestamp(System.currentTimeMillis())
        transaction.dateAdded = new Timestamp(System.currentTimeMillis())

        return transaction
    }

    private Totals createValidTotals() {
        return new Totals(
            BigDecimal.valueOf(300.00),  // totalsFuture
            BigDecimal.valueOf(1000.00), // totalsCleared
            BigDecimal.valueOf(1500.00), // totals
            BigDecimal.valueOf(200.00)   // totalsOutstanding
        )
    }

    private ReceiptImage createValidReceiptImage(Long receiptImageId, Long transactionId) {
        ReceiptImage receiptImage = new ReceiptImage(receiptImageId, "test_owner", transactionId, true)
        receiptImage.image = "test image data".bytes
        receiptImage.thumbnail = "test thumbnail data".bytes
        receiptImage.imageFormatType = ImageFormatType.Png
        return receiptImage
    }
}