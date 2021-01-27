package finance.services

import com.fasterxml.jackson.databind.ObjectMapper
import finance.repositories.*
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
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


    //TODO: turn this into a method
    Tag validationExceptionTag = Tag.of('exception.name.tag', 'ValidationException')
    Tag serverNameTag = Tag.of('server.name.tag', 'server')
    Tags tags = Tags.of(validationExceptionTag, serverNameTag)
    Meter.Id id = new Meter.Id("exception.caught.counter", tags, null, null, Meter.Type.COUNTER)

}
