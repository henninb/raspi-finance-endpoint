package finance.services

import com.fasterxml.jackson.databind.ObjectMapper
import finance.repositories.*
import finance.utils.Constants
import io.micrometer.core.instrument.*
import org.springframework.core.io.FileSystemResource
import spock.lang.Specification

import javax.validation.Validation
import javax.validation.Validator

class BaseServiceSpec extends Specification {
    protected AccountRepository accountRepositoryMock = GroovyMock(AccountRepository)
    protected Validator validatorMock = GroovyMock(Validator)
    protected MeterRegistry meterRegistryMock = GroovyMock(MeterRegistry)
    protected MeterService meterService = new MeterService(meterRegistryMock)
    protected ReceiptImageRepository receiptImageRepositoryMock = GroovyMock(ReceiptImageRepository)
    protected CategoryRepository categoryRepositoryMock = GroovyMock(CategoryRepository)
    protected DescriptionRepository descriptionRepositoryMock = GroovyMock(DescriptionRepository)
    protected PaymentRepository paymentRepositoryMock = GroovyMock(PaymentRepository)
    protected ParameterRepository parameterRepositoryMock = GroovyMock(ParameterRepository)
    protected TransactionRepository transactionRepositoryMock = GroovyMock(TransactionRepository)
    protected TransactionService transactionServiceMock = GroovyMock(TransactionService)
    protected ObjectMapper mapper = new ObjectMapper()
    protected Validator validator = Validation.buildDefaultValidatorFactory().getValidator()
    protected String baseName = new FileSystemResource("").file.absolutePath
    protected Counter counter = Mock(Counter)
    protected AccountService accountService = new AccountService(accountRepositoryMock, validatorMock, meterService)
    protected ReceiptImageService receiptImageService = new ReceiptImageService(receiptImageRepositoryMock, validatorMock, meterService)
    protected CategoryService categoryService = new CategoryService(categoryRepositoryMock, validatorMock, meterService)
    protected TransactionService transactionService = new TransactionService(transactionRepositoryMock, accountService, categoryService, receiptImageService, validatorMock, meterService)
    protected ParameterService parameterService = new ParameterService(parameterRepositoryMock, validatorMock, meterService)
    protected PaymentService paymentService = new PaymentService(paymentRepositoryMock, transactionService, accountService, parameterService, validatorMock, meterService)

    //TODO: turn this into a method
    protected Tag validationExceptionTag = Tag.of(Constants.EXCEPTION_NAME_TAG, 'ValidationException')
    protected Tag runtimeExceptionTag = Tag.of(Constants.EXCEPTION_NAME_TAG, 'RuntimeException')
    protected Tag serverNameTag = Tag.of(Constants.SERVER_NAME_TAG, 'server')
    protected Tags validationExceptionTags = Tags.of(validationExceptionTag, serverNameTag)
    protected Tags runtimeExceptionTags = Tags.of(runtimeExceptionTag, serverNameTag)
    protected Meter.Id validationExceptionThrownMeter = new Meter.Id(Constants.EXCEPTION_THROWN_COUNTER, validationExceptionTags, null, null, Meter.Type.COUNTER)
    protected Meter.Id runtimeExceptionThrownMeter = new Meter.Id(Constants.EXCEPTION_THROWN_COUNTER, runtimeExceptionTags, null, null, Meter.Type.COUNTER)
    protected Meter.Id runtimeExceptionCaughtMeter = new Meter.Id(Constants.EXCEPTION_CAUGHT_COUNTER, runtimeExceptionTags, null, null, Meter.Type.COUNTER)

    static Meter.Id setMeterId(String counterName, String accountNameOwner) {
        Tag serverNameTag = Tag.of(Constants.SERVER_NAME_TAG, 'server')
        Tag accountNameOwnerTag = Tag.of(Constants.ACCOUNT_NAME_OWNER_TAG, accountNameOwner)
        Tags tags = Tags.of(accountNameOwnerTag, serverNameTag)
        return new Meter.Id(counterName, tags, null, null, Meter.Type.COUNTER)
    }
}
