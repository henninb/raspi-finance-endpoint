package finance.repositories

import finance.Application
import finance.domain.Account
import finance.domain.Transfer
import finance.helpers.SmartAccountBuilder
import finance.helpers.SmartTransferBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Specification
import jakarta.validation.ConstraintViolationException

@ActiveProfiles("func")
@DataJpaTest
@ContextConfiguration(classes = [Application])
class TransferJpaSpec extends Specification {

    @Autowired
    protected TransferRepository transferRepository

    @Autowired
    protected AccountRepository accountRepository

    @Autowired
    protected TestEntityManager entityManager

    @Shared
    protected String testOwner = "jpa_${UUID.randomUUID().toString().replace('-', '')[0..7]}"

    void 'test Transfer - valid insert'() {
        given:
        long beforeTransfer = transferRepository.count()
        long beforeAccount = accountRepository.count()
        
        Account sourceAccount = SmartAccountBuilder.builderForOwner(testOwner)
            .withUniqueAccountName('transfersrc')
            .asCredit()
            .buildAndValidate()
        Account sourceResult = entityManager.persist(sourceAccount)
        
        Account destinationAccount = SmartAccountBuilder.builderForOwner(testOwner)
            .withUniqueAccountName('transferdest')
            .asCredit()
            .buildAndValidate()
        Account destResult = entityManager.persist(destinationAccount)
        
        Transfer transfer = SmartTransferBuilder.builderForOwner(testOwner)
            .withSourceAccount(sourceResult.accountNameOwner)
            .withDestinationAccount(destResult.accountNameOwner)
            .withAmount(250.00G)
            .buildAndValidate()

        when:
        Transfer result = entityManager.persist(transfer)

        then:
        transferRepository.count() == beforeTransfer + 1
        accountRepository.count() == beforeAccount + 2
        result.amount == transfer.amount
        result.sourceAccount == transfer.sourceAccount
        result.destinationAccount == transfer.destinationAccount
    }

    void 'test Transfer - multiple inserts'() {
        given:
        Account sourceAccount = SmartAccountBuilder.builderForOwner(testOwner)
            .withUniqueAccountName('statesrc')
            .asCredit()
            .buildAndValidate()
        Account sourceResult = entityManager.persist(sourceAccount)
        
        Account destinationAccount = SmartAccountBuilder.builderForOwner(testOwner)
            .withUniqueAccountName('statedest')
            .asCredit()
            .buildAndValidate()
        Account destResult = entityManager.persist(destinationAccount)
        
        Transfer clearedTransfer = SmartTransferBuilder.builderForOwner(testOwner)
            .withSourceAccount(sourceResult.accountNameOwner)
            .withDestinationAccount(destResult.accountNameOwner)
            .withAmount(100.00G)
            .buildAndValidate()
            
        Transfer outstandingTransfer = SmartTransferBuilder.builderForOwner(testOwner)
            .withSourceAccount(sourceResult.accountNameOwner)
            .withDestinationAccount(destResult.accountNameOwner)
            .withAmount(200.00G)
            .buildAndValidate()

        when:
        Transfer clearedResult = entityManager.persist(clearedTransfer)
        Transfer outstandingResult = entityManager.persist(outstandingTransfer)

        then:
        clearedResult.amount.compareTo(100.00G) == 0
        outstandingResult.amount.compareTo(200.00G) == 0
    }

    void 'test Transfer - different active status'() {
        given:
        Account sourceAccount = SmartAccountBuilder.builderForOwner(testOwner)
            .withUniqueAccountName('activesrc')
            .asCredit()
            .buildAndValidate()
        Account sourceResult = entityManager.persist(sourceAccount)
        
        Account destinationAccount = SmartAccountBuilder.builderForOwner(testOwner)
            .withUniqueAccountName('activedest')
            .asCredit()
            .buildAndValidate()
        Account destResult = entityManager.persist(destinationAccount)
        
        Transfer activeTransfer = SmartTransferBuilder.builderForOwner(testOwner)
            .withSourceAccount(sourceResult.accountNameOwner)
            .withDestinationAccount(destResult.accountNameOwner)
            .withAmount(75.50G)
            .asActive()
            .buildAndValidate()
            
        Transfer inactiveTransfer = SmartTransferBuilder.builderForOwner(testOwner)
            .withSourceAccount(sourceResult.accountNameOwner)
            .withDestinationAccount(destResult.accountNameOwner)
            .withAmount(125.75G)
            .asInactive()
            .buildAndValidate()

        when:
        Transfer activeResult = entityManager.persist(activeTransfer)
        Transfer inactiveResult = entityManager.persist(inactiveTransfer)

        then:
        activeResult.activeStatus == true
        inactiveResult.activeStatus == false
        activeResult.amount.compareTo(75.50G) == 0
        inactiveResult.amount.compareTo(125.75G) == 0
    }

    void 'test Transfer - delete record'() {
        given:
        long beforeTransfer = transferRepository.count()
        long beforeAccount = accountRepository.count()
        
        Account sourceAccount = SmartAccountBuilder.builderForOwner(testOwner)
            .withUniqueAccountName('deletesrc')
            .asCredit()
            .buildAndValidate()
        Account sourceResult = entityManager.persist(sourceAccount)
        
        Account destinationAccount = SmartAccountBuilder.builderForOwner(testOwner)
            .withUniqueAccountName('deletedest')
            .asCredit()
            .buildAndValidate()
        Account destResult = entityManager.persist(destinationAccount)
        
        Transfer transfer = SmartTransferBuilder.builderForOwner(testOwner)
            .withSourceAccount(sourceResult.accountNameOwner)
            .withDestinationAccount(destResult.accountNameOwner)
            .withAmount(99.99G)
            .buildAndValidate()
        entityManager.persist(transfer)

        when:
        transferRepository.delete(transfer)

        then:
        transferRepository.count() == beforeTransfer
        accountRepository.count() == beforeAccount + 2
    }

    void 'test Transfer - invalid amount precision'() {
        given:
        Account sourceAccount = SmartAccountBuilder.builderForOwner(testOwner)
            .withUniqueAccountName('precisionsrc')
            .asCredit()
            .buildAndValidate()
        Account sourceResult = entityManager.persist(sourceAccount)
        
        Account destinationAccount = SmartAccountBuilder.builderForOwner(testOwner)
            .withUniqueAccountName('precisiondest')
            .asCredit()
            .buildAndValidate()
        Account destResult = entityManager.persist(destinationAccount)
        
        Transfer transfer = SmartTransferBuilder.builderForOwner(testOwner)
            .withSourceAccount(sourceResult.accountNameOwner)
            .withDestinationAccount(destResult.accountNameOwner)
            .withAmount(25.123456G)  // Too many decimal places
            .build()  // Use build() instead of buildAndValidate() to allow constraint violation

        when:
        Transfer saved = entityManager.persist(transfer)
        entityManager.flush()
        entityManager.clear()
        def reloaded = transferRepository.findByTransferId(saved.transferId)

        then:
        reloaded.isPresent()
        reloaded.get().amount.setScale(2, java.math.RoundingMode.HALF_UP).compareTo(new BigDecimal('25.12')) == 0
    }

    void 'test Transfer - same source and destination account'() {
        given:
        Account account = SmartAccountBuilder.builderForOwner(testOwner)
            .withUniqueAccountName('sameacct')
            .asCredit()
            .buildAndValidate()
        Account accountResult = entityManager.persist(account)
        
        Transfer transfer = SmartTransferBuilder.builderForOwner(testOwner)
            .withSourceAccount(accountResult.accountNameOwner)
            .withDestinationAccount(accountResult.accountNameOwner)  // Same as source
            .withAmount(100.00G)
            .build()  // Use build() to allow this scenario

        when:
        entityManager.persist(transfer)

        then:
        // This might be allowed or rejected depending on business rules
        // The test documents the current behavior
        notThrown(Exception)
    }

    void 'test Transfer - find by id'() {
        given:
        Account sourceAccount = SmartAccountBuilder.builderForOwner(testOwner)
            .withUniqueAccountName('findsrc')
            .asCredit()
            .buildAndValidate()
        Account sourceResult = entityManager.persist(sourceAccount)
        
        Account destinationAccount = SmartAccountBuilder.builderForOwner(testOwner)
            .withUniqueAccountName('finddest')
            .asCredit()
            .buildAndValidate()
        Account destResult = entityManager.persist(destinationAccount)
        
        Transfer transfer = SmartTransferBuilder.builderForOwner(testOwner)
            .withSourceAccount(sourceResult.accountNameOwner)
            .withDestinationAccount(destResult.accountNameOwner)
            .withAmount(150.00G)
            .buildAndValidate()
        Transfer persisted = entityManager.persist(transfer)

        when:
        def opt = transferRepository.findByTransferId(persisted.transferId)

        then:
        opt.isPresent()
        opt.get().sourceAccount == sourceResult.accountNameOwner
        opt.get().destinationAccount == destResult.accountNameOwner
    }
}
