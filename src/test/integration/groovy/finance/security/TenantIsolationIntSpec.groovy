package finance.security

import finance.Application
import finance.domain.*
import finance.helpers.SmartAccountBuilder
import finance.helpers.SmartCategoryBuilder
import finance.helpers.SmartDescriptionBuilder
import finance.helpers.SmartParameterBuilder
import finance.helpers.SmartTransactionBuilder
import finance.repositories.AccountRepository
import finance.repositories.CategoryRepository
import finance.repositories.DescriptionRepository
import finance.repositories.ParameterRepository
import finance.repositories.TransactionRepository
import finance.services.AccountService
import finance.domain.ServiceResult
import finance.services.CategoryService
import finance.services.DescriptionService
import finance.services.ParameterService
import finance.services.TransactionService
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.annotation.Transactional
import spock.lang.Specification

@Slf4j
@ActiveProfiles("int")
@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = Application)
@Transactional
class TenantIsolationIntSpec extends Specification {

    private static final String OWNER_A = "user-a@test.com"
    private static final String OWNER_B = "user-b@test.com"

    @Autowired
    AccountService accountService

    @Autowired
    CategoryService categoryService

    @Autowired
    TransactionService transactionService

    @Autowired
    AccountRepository accountRepository

    @Autowired
    CategoryRepository categoryRepository

    @Autowired
    DescriptionRepository descriptionRepository

    @Autowired
    ParameterRepository parameterRepository

    @Autowired
    TransactionRepository transactionRepository

    void cleanup() {
        SecurityContextHolder.clearContext()
    }

    private void setSecurityContext(String username) {
        def authorities = [new SimpleGrantedAuthority("USER")]
        def auth = new UsernamePasswordAuthenticationToken(username, "N/A", authorities)
        SecurityContextHolder.getContext().setAuthentication(auth)
    }

    void 'test accounts created by user A are not visible to user B via repository'() {
        given:
        setSecurityContext(OWNER_A)
        Account accountA = SmartAccountBuilder.builderForOwner(OWNER_A)
            .withUniqueAccountName("isolationtest")
            .asDebit()
            .withMoniker("1000")
            .buildAndValidate()
        Account savedA = accountRepository.save(accountA)
        accountRepository.flush()

        when:
        Optional<Account> foundByA = accountRepository.findByOwnerAndAccountNameOwner(OWNER_A, savedA.accountNameOwner)
        Optional<Account> foundByB = accountRepository.findByOwnerAndAccountNameOwner(OWNER_B, savedA.accountNameOwner)

        then: 'owner A can see the account'
        foundByA.isPresent()
        foundByA.get().owner == OWNER_A

        and: 'owner B cannot see the account'
        !foundByB.isPresent()
    }

    void 'test accounts created by user A are not visible to user B via service'() {
        given:
        setSecurityContext(OWNER_A)
        Account accountA = SmartAccountBuilder.builderForOwner(OWNER_A)
            .withUniqueAccountName("serviceiso")
            .asCredit()
            .withMoniker("2000")
            .buildAndValidate()
        accountService.save(accountA)

        when: 'user A queries their accounts'
        setSecurityContext(OWNER_A)
        Optional<Account> foundByA = accountService.account(accountA.accountNameOwner)

        then:
        foundByA.isPresent()
        foundByA.get().owner == OWNER_A

        when: 'user B queries for the same account name'
        setSecurityContext(OWNER_B)
        Optional<Account> foundByB = accountService.account(accountA.accountNameOwner)

        then: 'user B cannot see user A account'
        !foundByB.isPresent()
    }

    void 'test categories created by user A are not visible to user B'() {
        given:
        setSecurityContext(OWNER_A)
        Category categoryA = SmartCategoryBuilder.builderForOwner(OWNER_A)
            .withUniqueCategoryName("isolationcat")
            .buildAndValidate()
        categoryRepository.save(categoryA)
        categoryRepository.flush()

        when:
        Optional<Category> foundByA = categoryRepository.findByOwnerAndCategoryName(OWNER_A, categoryA.categoryName)
        Optional<Category> foundByB = categoryRepository.findByOwnerAndCategoryName(OWNER_B, categoryA.categoryName)

        then: 'owner A can see the category'
        foundByA.isPresent()
        foundByA.get().owner == OWNER_A

        and: 'owner B cannot see the category'
        !foundByB.isPresent()
    }

    void 'test service-level account listing only returns current tenant data'() {
        given: 'user A creates an account'
        setSecurityContext(OWNER_A)
        Account accountA = SmartAccountBuilder.builderForOwner(OWNER_A)
            .withUniqueAccountName("listisoa")
            .asDebit()
            .withMoniker("3000")
            .buildAndValidate()
        accountService.save(accountA)

        and: 'user B creates an account'
        setSecurityContext(OWNER_B)
        Account accountB = SmartAccountBuilder.builderForOwner(OWNER_B)
            .withUniqueAccountName("listisob")
            .asDebit()
            .withMoniker("4000")
            .buildAndValidate()
        accountService.save(accountB)

        when: 'user A lists their accounts'
        setSecurityContext(OWNER_A)
        List<Account> accountsA = accountService.accounts()

        then: 'user A only sees their own accounts'
        accountsA.every { it.owner == OWNER_A }
        accountsA.any { it.accountNameOwner == accountA.accountNameOwner }
        !accountsA.any { it.accountNameOwner == accountB.accountNameOwner }

        when: 'user B lists their accounts'
        setSecurityContext(OWNER_B)
        List<Account> accountsB = accountService.accounts()

        then: 'user B only sees their own accounts'
        accountsB.every { it.owner == OWNER_B }
        accountsB.any { it.accountNameOwner == accountB.accountNameOwner }
        !accountsB.any { it.accountNameOwner == accountA.accountNameOwner }
    }

    void 'test same account name can exist for different owners'() {
        given:
        String sharedAccountName = "shared_account"

        setSecurityContext(OWNER_A)
        Account accountA = SmartAccountBuilder.builderForOwner(OWNER_A)
            .withAccountNameOwner(sharedAccountName)
            .asDebit()
            .withMoniker("5000")
            .buildAndValidate()
        accountRepository.save(accountA)

        setSecurityContext(OWNER_B)
        Account accountB = SmartAccountBuilder.builderForOwner(OWNER_B)
            .withAccountNameOwner(sharedAccountName)
            .asDebit()
            .withMoniker("6000")
            .buildAndValidate()
        accountRepository.save(accountB)
        accountRepository.flush()

        when:
        Optional<Account> foundA = accountRepository.findByOwnerAndAccountNameOwner(OWNER_A, sharedAccountName)
        Optional<Account> foundB = accountRepository.findByOwnerAndAccountNameOwner(OWNER_B, sharedAccountName)

        then: 'both owners have the same account name but different records'
        foundA.isPresent()
        foundB.isPresent()
        foundA.get().owner == OWNER_A
        foundB.get().owner == OWNER_B
        foundA.get().accountId != foundB.get().accountId
    }

    void 'test same category name can exist for different owners'() {
        given:
        String sharedCategoryName = "sharedcategory"

        setSecurityContext(OWNER_A)
        Category catA = SmartCategoryBuilder.builderForOwner(OWNER_A)
            .withCategoryName(sharedCategoryName)
            .buildAndValidate()
        categoryRepository.save(catA)

        setSecurityContext(OWNER_B)
        Category catB = SmartCategoryBuilder.builderForOwner(OWNER_B)
            .withCategoryName(sharedCategoryName)
            .buildAndValidate()
        categoryRepository.save(catB)
        categoryRepository.flush()

        when:
        Optional<Category> foundA = categoryRepository.findByOwnerAndCategoryName(OWNER_A, sharedCategoryName)
        Optional<Category> foundB = categoryRepository.findByOwnerAndCategoryName(OWNER_B, sharedCategoryName)

        then: 'both owners have the same category name but different records'
        foundA.isPresent()
        foundB.isPresent()
        foundA.get().owner == OWNER_A
        foundB.get().owner == OWNER_B
        foundA.get().categoryId != foundB.get().categoryId
    }

    void 'test descriptions created by user A are not visible to user B'() {
        given:
        setSecurityContext(OWNER_A)
        Description descA = SmartDescriptionBuilder.builderForOwner(OWNER_A)
            .withUniqueDescriptionName("isolationdesc")
            .buildAndValidate()
        descriptionRepository.save(descA)
        descriptionRepository.flush()

        when:
        Optional<Description> foundByA = descriptionRepository.findByOwnerAndDescriptionName(OWNER_A, descA.descriptionName)
        Optional<Description> foundByB = descriptionRepository.findByOwnerAndDescriptionName(OWNER_B, descA.descriptionName)

        then: 'owner A can see the description'
        foundByA.isPresent()
        foundByA.get().owner == OWNER_A

        and: 'owner B cannot see the description'
        !foundByB.isPresent()
    }

    void 'test same description name can exist for different owners'() {
        given:
        String sharedDescName = "shareddescription"

        setSecurityContext(OWNER_A)
        Description descA = SmartDescriptionBuilder.builderForOwner(OWNER_A)
            .withDescriptionName(sharedDescName)
            .buildAndValidate()
        descriptionRepository.save(descA)

        setSecurityContext(OWNER_B)
        Description descB = SmartDescriptionBuilder.builderForOwner(OWNER_B)
            .withDescriptionName(sharedDescName)
            .buildAndValidate()
        descriptionRepository.save(descB)
        descriptionRepository.flush()

        when:
        Optional<Description> foundA = descriptionRepository.findByOwnerAndDescriptionName(OWNER_A, sharedDescName)
        Optional<Description> foundB = descriptionRepository.findByOwnerAndDescriptionName(OWNER_B, sharedDescName)

        then:
        foundA.isPresent()
        foundB.isPresent()
        foundA.get().owner == OWNER_A
        foundB.get().owner == OWNER_B
        foundA.get().descriptionId != foundB.get().descriptionId
    }

    void 'test parameters created by user A are not visible to user B'() {
        given:
        setSecurityContext(OWNER_A)
        Parameter paramA = SmartParameterBuilder.builderForOwner(OWNER_A)
            .withUniqueParameterName("isolationparam")
            .withUniqueParameterValue("valueA")
            .buildAndValidate()
        parameterRepository.save(paramA)
        parameterRepository.flush()

        when:
        Optional<Parameter> foundByA = parameterRepository.findByOwnerAndParameterName(OWNER_A, paramA.parameterName)
        Optional<Parameter> foundByB = parameterRepository.findByOwnerAndParameterName(OWNER_B, paramA.parameterName)

        then: 'owner A can see the parameter'
        foundByA.isPresent()
        foundByA.get().owner == OWNER_A

        and: 'owner B cannot see the parameter'
        !foundByB.isPresent()
    }

    void 'test transactions created by user A are not visible to user B via repository'() {
        given: 'user A creates an account and transaction'
        setSecurityContext(OWNER_A)
        Account accountA = SmartAccountBuilder.builderForOwner(OWNER_A)
            .withUniqueAccountName("txniso")
            .asDebit()
            .withMoniker("7000")
            .buildAndValidate()
        accountRepository.save(accountA)

        Category catA = SmartCategoryBuilder.builderForOwner(OWNER_A)
            .withUniqueCategoryName("txnisocat")
            .buildAndValidate()
        categoryRepository.save(catA)
        categoryRepository.flush()

        Description descA = SmartDescriptionBuilder.builderForOwner(OWNER_A)
            .withDescriptionName("isolation test txn")
            .buildAndValidate()
        descriptionRepository.save(descA)
        descriptionRepository.flush()

        Transaction txnA = SmartTransactionBuilder.builderForOwner(OWNER_A)
            .withAccountNameOwner(accountA.accountNameOwner)
            .withAccountId(accountA.accountId)
            .withAccountType(AccountType.Debit)
            .withCategory(catA.categoryName)
            .withDescription("isolation test txn")
            .withAmount("50.00")
            .buildAndValidate()
        transactionRepository.save(txnA)
        transactionRepository.flush()

        when:
        Optional<Transaction> foundByA = transactionRepository.findByOwnerAndGuid(OWNER_A, txnA.guid)
        Optional<Transaction> foundByB = transactionRepository.findByOwnerAndGuid(OWNER_B, txnA.guid)

        then: 'owner A can see the transaction'
        foundByA.isPresent()
        foundByA.get().owner == OWNER_A

        and: 'owner B cannot see the transaction'
        !foundByB.isPresent()
    }

    void 'test service-level category listing only returns current tenant data'() {
        given: 'user A creates a category'
        setSecurityContext(OWNER_A)
        Category catA = SmartCategoryBuilder.builderForOwner(OWNER_A)
            .withUniqueCategoryName("listcata")
            .buildAndValidate()
        categoryRepository.save(catA)

        and: 'user B creates a category'
        setSecurityContext(OWNER_B)
        Category catB = SmartCategoryBuilder.builderForOwner(OWNER_B)
            .withUniqueCategoryName("listcatb")
            .buildAndValidate()
        categoryRepository.save(catB)
        categoryRepository.flush()

        when: 'user A lists their categories'
        setSecurityContext(OWNER_A)
        def resultA = categoryService.findAllActive()
        List<Category> categoriesA = (resultA instanceof ServiceResult.Success) ? ((ServiceResult.Success<List<Category>>) resultA).data : []

        then: 'user A only sees their own categories'
        categoriesA.every { it.owner == OWNER_A }
        categoriesA.any { it.categoryName == catA.categoryName }
        !categoriesA.any { it.categoryName == catB.categoryName }

        when: 'user B lists their categories'
        setSecurityContext(OWNER_B)
        def resultB = categoryService.findAllActive()
        List<Category> categoriesB = (resultB instanceof ServiceResult.Success) ? ((ServiceResult.Success<List<Category>>) resultB).data : []

        then: 'user B only sees their own categories'
        categoriesB.every { it.owner == OWNER_B }
        categoriesB.any { it.categoryName == catB.categoryName }
        !categoriesB.any { it.categoryName == catA.categoryName }
    }
}
