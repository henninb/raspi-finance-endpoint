//package finance.routes
//
//import finance.Application
//import finance.domain.Transaction
//import finance.helpers.TransactionBuilder
//import finance.repositories.TransactionRepository
//import org.apache.camel.Exchange
//import org.springframework.beans.factory.annotation.Autowired
//import org.springframework.boot.test.context.SpringBootTest
//import org.springframework.test.context.ActiveProfiles
//import org.springframework.util.ResourceUtils
//
//@ActiveProfiles("func")
//@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//class JsonFileReaderRouteBuilderSpec extends BaseRouteBuilderSpec {
//
//    @Autowired
//    protected JsonFileReaderRouteBuilder jsonFileReaderRouteBuilder
//
//    @Autowired
//    protected TransactionRepository transactionRepository
//
//    void setup() {
//        camelContext = jsonFileReaderRouteBuilder.context
//        producer = camelContext.createProducerTemplate()
//        camelContext.start()
//
//        camelContext.routes.each { route -> route.setAutoStartup(true) }
//        producer.setDefaultEndpointUri(camelProperties.jsonFileReaderRoute)
//    }
//
//    void 'test jsonFileReaderRouteBuilder - 1 transaction'() {
//        given:
//        Transaction transaction = TransactionBuilder.builder()
//                .withAmount(0.00)
//                .withGuid(UUID.randomUUID().toString())
//                .build()
//        List<Transaction> transactions = [transaction]
//        String fileName = "${UUID.randomUUID()}-transactions.json"
//
//        when:
//        producer.sendBodyAndHeader(transactions.toString(), Exchange.FILE_NAME, fileName)
//
//        then:
//        conditions.eventually {
//            Transaction databaseTransaction = transactionRepository.findByGuid(transaction.guid).get()
//            databaseTransaction.guid == transaction.guid
//            ResourceUtils.getFile("${baseName}/func_json_in/.processed-successfully/${transaction.guid}").exists()
//        }
//    }
//
//    void 'test jsonFileReaderRouteBuilder - 2 transaction'() {
//        given:
//        Transaction transaction1 = TransactionBuilder.builder()
//                .withAmount(0.00)
//                .withAccountNameOwner('foo_brian')
//                .withDescription(UUID.randomUUID().toString())
//                .withGuid(UUID.randomUUID().toString())
//                .build()
//        Transaction transaction2 = TransactionBuilder.builder()
//                .withAmount(0.01)
//                .withGuid(UUID.randomUUID().toString())
//                .withDescription(UUID.randomUUID().toString())
//                .withAccountNameOwner('junk_brian')
//                .build()
//        List<Transaction> transactions = [transaction1, transaction2]
//        String fileName = "${UUID.randomUUID()}-transactions.json"
//
//        when:
//        producer.sendBodyAndHeader(transactions.toString(), Exchange.FILE_NAME, fileName)
//
//        then:
//        conditions.eventually {
//            Transaction databaseTransaction1 = transactionRepository.findByGuid(transaction1.guid).get()
//            databaseTransaction1.guid == transaction1.guid
//
//            Transaction databaseTransaction2 = transactionRepository.findByGuid(transaction2.guid).get()
//            databaseTransaction2.guid == transaction2.guid
//
//            ResourceUtils.getFile("${baseName}/func_json_in/.processed-successfully/${transaction2.guid}").exists()
//            ResourceUtils.getFile("${baseName}/func_json_in/.processed-successfully/${transaction1.guid}").exists()
//        }
//    }
//
//    void 'test jsonFileReaderRouteBuilder - 2 transaction - second transaction fails to insert'() {
//        given:
//        Transaction transaction1 = TransactionBuilder.builder()
//                .withAmount(0.00)
//                .withGuid(UUID.randomUUID().toString())
//                .build()
//        Transaction transaction2 = TransactionBuilder.builder()
//                .withAmount(0.01)
//                .withGuid(UUID.randomUUID().toString())
//                .withAccountNameOwner('')
//                .build()
//        List<Transaction> transactions = [transaction1, transaction2]
//        String fileName = "${UUID.randomUUID()}-transactions.json"
//
//        when:
//        producer.sendBodyAndHeader(transactions.toString(), Exchange.FILE_NAME, fileName)
//
//        then:
//        noExceptionThrown()
//        conditions.eventually {
//            transactionRepository.findByGuid(transaction1.guid).isEmpty()
//            transactionRepository.findByGuid(transaction2.guid).isEmpty()
//
//            ResourceUtils.getFile("${baseName}/func_json_in/.not-processed-failed-with-errors/$fileName").exists()
//        }
//    }
//
//    void 'test jsonFileReaderRouteBuilder - with empty description'() {
//        given:
//        Transaction transaction = TransactionBuilder.builder().withAmount(0.00).withGuid(UUID.randomUUID().toString()).withDescription('').build()
//        List<Transaction> transactions = [transaction]
//        String fileName = "${UUID.randomUUID()}-transactions.json"
//
//        when:
//        producer.sendBodyAndHeader(transactions.toString(), Exchange.FILE_NAME, fileName)
//
//        then:
//        conditions.eventually {
//            ResourceUtils.getFile("${baseName}/func_json_in/.not-processed-failed-with-errors/${fileName}").exists()
//        }
//    }
//
//    void 'test jsonFileReaderRouteBuilder - non json filename'() {
//        given:
//        Transaction transaction = TransactionBuilder.builder().withGuid(UUID.randomUUID().toString()).build()
//        List<Transaction> transactions = [transaction]
//        String fileName = "${UUID.randomUUID()}-transactions.txt"
//
//        when:
//        producer.sendBodyAndHeader(transactions.toString(), Exchange.FILE_NAME, fileName)
//
//        then:
//        conditions.eventually {
//            ResourceUtils.getFile("${baseName}/func_json_in/.not-processed-non-json-file/${fileName}").exists()
//        }
//    }
//
//    void 'test jsonFileReaderRouteBuilder - invalid payload'() {
//        given:
//        String fileName = UUID.randomUUID().toString() + ".json"
//
//        when:
//        producer.sendBodyAndHeader('invalid payload', Exchange.FILE_NAME, fileName)
//
//        then:
//        conditions.eventually {
//            ResourceUtils.getFile("${baseName}/func_json_in/.not-processed-json-parsing-errors/${fileName}").exists()
//        }
//    }
//}
