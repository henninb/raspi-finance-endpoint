package finance

import finance.repositories.TransactionRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.FileSystemResource
import org.springframework.test.context.ActiveProfiles
import org.springframework.util.ResourceUtils
import spock.lang.Ignore
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.nio.file.Files


@ActiveProfiles("int")
@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CamelSpec extends  Specification {

    protected String baseName = new FileSystemResource("").file.absolutePath

    protected PollingConditions conditions = new PollingConditions(timeout: 20, initialDelay: 1.5, factor: 1.25)

    @Autowired
    protected TransactionRepository transactionRepository

    void 'test camel send data'() {
        given:
        File source = new File(ResourceUtils.getResource('/camel-input.json').file)
        File destination = new File("$baseName/int_json_in/${UUID.randomUUID()}.json")

        when:
        Files.copy(source.toPath(), destination.toPath())

        then:
        conditions.eventually {
            transactionRepository.findByGuid('3ea3be58-3993-46de-88a2-4ffc7f1d73bd').get().guid == '3ea3be58-3993-46de-88a2-4ffc7f1d73bd'
        }
        noExceptionThrown()
    }
}
