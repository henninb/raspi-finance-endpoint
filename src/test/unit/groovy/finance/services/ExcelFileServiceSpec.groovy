package finance.services

import finance.configurations.CustomProperties
import finance.domain.Transaction
import finance.helpers.TransactionBuilder
import org.springframework.core.io.FileSystemResource

@SuppressWarnings("GroovyAccessibility")
class ExcelFileServiceSpec extends BaseServiceSpec {
    protected String baseName = new FileSystemResource("").file.absolutePath
    CustomProperties customProperties = new CustomProperties(excludedAccounts: [], excelPassword: 'monday1', excelInputFilePath: baseName + '/excel_in')
    ExcelFileService excelFileService = new ExcelFileService(customProperties, transactionService, meterService)

    void 'test try to open a file that is not found'() {
        when:
        excelFileService.processProtectedExcelFile('file-not-found.xlsm')

        then:
        thrown(FileNotFoundException)
        0 * _
    }

    void 'test try to open a file that exists, and with a valid password'() {
        given:
        Transaction transaction = TransactionBuilder.builder().build()
        when:
        excelFileService.processProtectedExcelFile('finance_test_db_master.xlsm')

        then:
        17 * meterRegistryMock.counter(_) >> counter
        17 * counter.increment()
        17 * transactionRepositoryMock.findByAccountNameOwnerAndActiveStatusOrderByTransactionDateDesc(_, true) >> [transaction]
        0 * _
    }

    void 'test try to open a file that exists, but with an invalid password'() {
        when:
        excelFileService.processProtectedExcelFile('finance_test_db-bad-password.xlsm')

        then:
        thrown(RuntimeException)
        0 * _
    }
}
