package finance.services

import finance.configurations.CustomProperties
import org.springframework.core.io.FileSystemResource
import spock.lang.Ignore

class ExcelFileServiceSpec extends BaseServiceSpec {

    //private val customProperties: CustomProperties,
    protected String baseName = new FileSystemResource("").file.absolutePath
    CustomProperties customProperties = new CustomProperties(excludedAccounts: ['amazongift_brian', '401k_kari', '401k_brian', 'pension_brian', 'pension_kari','scottrade_ira_brian'], excelPassword: 'monday1', excelInputFilePath: baseName + '/excel_in')
    ExcelFileService excelFileService = new ExcelFileService(customProperties, transactionServiceMock, meterServiceMock)

    void 'test try to open a file that is not found'() {
        when:
        excelFileService.processProtectedExcelFile('file-not-found.xlsm')

        then:
        thrown(FileNotFoundException)
        0 * _
    }

    @Ignore
    void 'test try to open a file that exists, and with a valid password'() {
        when:
        excelFileService.processProtectedExcelFile('finance_db_master.xlsm')

        then:
        1 * transactionServiceMock.findByAccountNameOwnerOrderByTransactionDate('amazon-store_brian') >> []
        1 * transactionServiceMock.findByAccountNameOwnerOrderByTransactionDate('amazon_brian') >> []
        1 * transactionServiceMock.findByAccountNameOwnerOrderByTransactionDate('amex_brian') >> []
        50 * transactionServiceMock.findByAccountNameOwnerOrderByTransactionDate(_ as String) >> []
        0 * _
    }

    @Ignore
    void 'test try to open a file that exists, but with an invalid password'() {
        when:
        excelFileService.processProtectedExcelFile('finance_db_master.xlsm')

        then:
        thrown(RuntimeException)
        0 * _
    }
}
