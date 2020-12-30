package finance.services

import com.fasterxml.jackson.databind.ObjectMapper
import finance.repositories.*
import spock.lang.Specification

import javax.validation.Validation
import javax.validation.Validator

class BaseServiceSpec extends Specification {
    protected AccountRepository accountRepositoryMock = GroovyMock(AccountRepository)
    protected Validator validatorMock = GroovyMock(Validator)
    protected MeterService meterServiceMock = GroovyMock(MeterService)
    protected ReceiptImageRepository receiptImageRepositoryMock = GroovyMock(ReceiptImageRepository)
    protected CategoryRepository categoryRepositoryMock = GroovyMock(CategoryRepository)
    protected DescriptionRepository descriptionRepositoryMock = GroovyMock(DescriptionRepository)
    protected PaymentRepository paymentRepositoryMock = GroovyMock(PaymentRepository)
    protected ParameterRepository parameterRepositoryMock = GroovyMock(ParameterRepository)
    protected TransactionRepository transactionRepositoryMock = GroovyMock(TransactionRepository)
    protected TransactionService transactionServiceMock = GroovyMock(TransactionService)
    protected ObjectMapper mapper = new ObjectMapper()
    Validator validator = Validation.buildDefaultValidatorFactory().getValidator()
}
