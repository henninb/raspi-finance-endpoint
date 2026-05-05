package finance.controllers.dto

import finance.domain.AccountType
import finance.domain.ClaimStatus
import finance.domain.TransactionState
import finance.domain.TransactionType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.LocalDate

class DtoDefaultParameterTest {

    @Test
    fun `AccountInputDto with default optional parameters`() {
        val dto = AccountInputDto(
            accountNameOwner = "checking_primary",
            accountType = AccountType.Checking,
        )
        assertNull(dto.accountId)
        assertEquals("checking_primary", dto.accountNameOwner)
        assertNull(dto.activeStatus)
        assertNull(dto.moniker)
        assertNull(dto.outstanding)
        assertNull(dto.cleared)
        assertNull(dto.future)
        assertNull(dto.dateClosed)
        assertNull(dto.validationDate)
    }

    @Test
    fun `CategoryInputDto with default optional parameters`() {
        val dto = CategoryInputDto(categoryName = "groceries")
        assertNull(dto.categoryId)
        assertEquals("groceries", dto.categoryName)
        assertNull(dto.activeStatus)
    }

    @Test
    fun `DescriptionInputDto with default optional parameters`() {
        val dto = DescriptionInputDto(descriptionName = "grocery_store")
        assertNull(dto.descriptionId)
        assertEquals("grocery_store", dto.descriptionName)
        assertNull(dto.activeStatus)
    }

    @Test
    fun `ParameterInputDto with default optional parameters`() {
        val dto = ParameterInputDto(parameterName = "setting_one", parameterValue = "value1")
        assertNull(dto.parameterId)
        assertEquals("setting_one", dto.parameterName)
        assertEquals("value1", dto.parameterValue)
        assertNull(dto.activeStatus)
    }

    @Test
    fun `PaymentInputDto with default optional parameters`() {
        val dto = PaymentInputDto(
            sourceAccount = "checking_primary",
            destinationAccount = "bills_payable",
            transactionDate = LocalDate.of(2024, 1, 15),
            amount = BigDecimal("100.00"),
        )
        assertNull(dto.paymentId)
        assertEquals("checking_primary", dto.sourceAccount)
        assertEquals("bills_payable", dto.destinationAccount)
        assertNull(dto.guidSource)
        assertNull(dto.guidDestination)
        assertNull(dto.activeStatus)
    }

    @Test
    fun `TransferInputDto with default optional parameters`() {
        val dto = TransferInputDto(
            sourceAccount = "checking_primary",
            destinationAccount = "savings_primary",
            transactionDate = LocalDate.of(2024, 1, 15),
            amount = BigDecimal("200.00"),
        )
        assertNull(dto.transferId)
        assertEquals("checking_primary", dto.sourceAccount)
        assertEquals("savings_primary", dto.destinationAccount)
        assertNull(dto.guidSource)
        assertNull(dto.guidDestination)
        assertNull(dto.activeStatus)
    }

    @Test
    fun `TransactionInputDto with default optional parameters`() {
        val dto = TransactionInputDto(
            accountType = AccountType.Debit,
            transactionType = TransactionType.Expense,
            accountNameOwner = "checking_primary",
            transactionDate = LocalDate.of(2024, 1, 15),
            description = "grocery store",
            category = "groceries",
            amount = BigDecimal("100.00"),
            transactionState = TransactionState.Outstanding,
        )
        assertNull(dto.transactionId)
        assertNull(dto.guid)
        assertNull(dto.accountId)
        assertNull(dto.activeStatus)
        assertNull(dto.reoccurringType)
        assertNull(dto.notes)
        assertNull(dto.dueDate)
        assertNull(dto.receiptImageId)
    }

    @Test
    fun `ValidationAmountInputDto with default optional parameters`() {
        val now = Timestamp(System.currentTimeMillis())
        val dto = ValidationAmountInputDto(
            accountId = 1L,
            validationDate = now,
            transactionState = TransactionState.Cleared,
            amount = BigDecimal("100.00"),
        )
        assertNull(dto.validationId)
        assertEquals(1L, dto.accountId)
        assertEquals(now, dto.validationDate)
        assertNull(dto.activeStatus)
    }

    @Test
    fun `MedicalExpenseInputDto with default optional parameters and all fields accessible`() {
        val serviceDate = LocalDate.of(2024, 1, 15)
        val dto = MedicalExpenseInputDto(
            serviceDate = serviceDate,
            billedAmount = BigDecimal("200.00"),
            insuranceDiscount = BigDecimal("50.00"),
            insurancePaid = BigDecimal("100.00"),
            patientResponsibility = BigDecimal("50.00"),
            isOutOfNetwork = false,
            claimNumber = "CLM-001",
            claimStatus = ClaimStatus.Submitted,
            paidAmount = BigDecimal("50.00"),
        )
        assertNull(dto.medicalExpenseId)
        assertNull(dto.transactionId)
        assertNull(dto.providerId)
        assertNull(dto.familyMemberId)
        assertEquals(serviceDate, dto.serviceDate)
        assertNull(dto.serviceDescription)
        assertNull(dto.procedureCode)
        assertNull(dto.diagnosisCode)
        assertEquals(BigDecimal("200.00"), dto.billedAmount)
        assertEquals(BigDecimal("50.00"), dto.insuranceDiscount)
        assertEquals(BigDecimal("100.00"), dto.insurancePaid)
        assertEquals(BigDecimal("50.00"), dto.patientResponsibility)
        assertNull(dto.paidDate)
        assertEquals(false, dto.isOutOfNetwork)
        assertEquals("CLM-001", dto.claimNumber)
        assertEquals(ClaimStatus.Submitted, dto.claimStatus)
        assertNull(dto.activeStatus)
        assertEquals(BigDecimal("50.00"), dto.paidAmount)
    }
}
