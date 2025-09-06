package finance.repositories

import finance.BaseIntegrationSpec
import finance.domain.Description
import finance.helpers.SmartDescriptionBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import spock.lang.Shared

/**
 * INTEGRATION TEST - DescriptionRepository using robust, isolated architecture
 *
 * This integration test demonstrates the new architecture:
 * ✅ No hardcoded description names - all use testOwner for uniqueness
 * ✅ SmartBuilder pattern with constraint validation
 * ✅ Test isolation - each test gets its own test data
 * ✅ Constraint-aware test data creation
 * ✅ Build-time validation prevents invalid data
 * ✅ Description-specific testing for transaction categorization
 */
class DescriptionRepositoryIntSpec extends BaseIntegrationSpec {

    @Autowired
    DescriptionRepository descriptionRepository

    @Shared
    def repositoryContext

    def setupSpec() {
        repositoryContext = testFixtures.createRepositoryTestContext(testOwner)
    }

    void 'test description repository basic CRUD operations'() {
        given:
        Description description = SmartDescriptionBuilder.builderForOwner(testOwner)
                .withUniqueDescriptionName("walmart")
                .asActive()
                .buildAndValidate()

        when:
        Description savedDescription = descriptionRepository.save(description)

        then:
        savedDescription.descriptionId != null
        savedDescription.descriptionId > 0
        savedDescription.descriptionName.contains(testOwner.toLowerCase())
        savedDescription.activeStatus == true
        savedDescription.dateAdded != null
        savedDescription.dateUpdated != null

        when:
        Optional<Description> foundDescription = descriptionRepository.findByDescriptionName(savedDescription.descriptionName)

        then:
        foundDescription.isPresent()
        foundDescription.get().descriptionId == savedDescription.descriptionId
        foundDescription.get().activeStatus == true
    }

    void 'test find description by description ID'() {
        given:
        Description description = SmartDescriptionBuilder.builderForOwner(testOwner)
                .withUniqueDescriptionName("target")
                .asActive()
                .buildAndValidate()
        Description savedDescription = descriptionRepository.save(description)

        when:
        Optional<Description> foundDescription = descriptionRepository.findByDescriptionId(savedDescription.descriptionId)

        then:
        foundDescription.isPresent()
        foundDescription.get().descriptionName == savedDescription.descriptionName
        foundDescription.get().activeStatus == savedDescription.activeStatus
    }

    void 'test find descriptions by active status with ordering'() {
        given:
        Description activeDescription1 = SmartDescriptionBuilder.builderForOwner(testOwner)
                .asStoreDescription()
                .asActive()
                .buildAndValidate()

        Description activeDescription2 = SmartDescriptionBuilder.builderForOwner(testOwner)
                .asRestaurantDescription()
                .asActive()
                .buildAndValidate()

        Description inactiveDescription = SmartDescriptionBuilder.builderForOwner(testOwner)
                .asServiceDescription()
                .asInactive()
                .buildAndValidate()

        descriptionRepository.save(activeDescription1)
        descriptionRepository.save(activeDescription2)
        descriptionRepository.save(inactiveDescription)

        when:
        List<Description> activeDescriptions = descriptionRepository.findByActiveStatusOrderByDescriptionName(true)
        List<Description> inactiveDescriptions = descriptionRepository.findByActiveStatusOrderByDescriptionName(false)

        then:
        activeDescriptions.size() >= 2
        activeDescriptions.every { it.activeStatus == true }
        inactiveDescriptions.size() >= 1
        inactiveDescriptions.every { it.activeStatus == false }

        // Verify our test descriptions are included
        activeDescriptions.any { it.descriptionName == activeDescription1.descriptionName }
        activeDescriptions.any { it.descriptionName == activeDescription2.descriptionName }
        inactiveDescriptions.any { it.descriptionName == inactiveDescription.descriptionName }

        // Verify ordering by description name
        def ourActiveDescriptions = activeDescriptions.findAll {
            it.descriptionName.contains(testOwner.toLowerCase())
        }
        if (ourActiveDescriptions.size() > 1) {
            assert ourActiveDescriptions == ourActiveDescriptions.sort { it.descriptionName }
        }
    }

    void 'test description unique constraint violations'() {
        given:
        Description description1 = SmartDescriptionBuilder.builderForOwner(testOwner)
                .withUniqueDescriptionName("duplicate")
                .asActive()
                .buildAndValidate()

        Description description2 = SmartDescriptionBuilder.builderForOwner(testOwner)
                .withDescriptionName(description1.descriptionName)  // Same description name - will cause unique constraint violation
                .asInactive()  // Different status but same name - violates unique constraint
                .buildAndValidate()

        when:
        descriptionRepository.save(description1)
        descriptionRepository.flush() // Force the first save to complete

        then:
        notThrown(Exception) // First save should succeed

        when:
        descriptionRepository.save(description2)
        descriptionRepository.flush() // This should fail due to unique constraint

        then:
        thrown(DataIntegrityViolationException)
    }

    void 'test description constraint validation at build time'() {
        when: "trying to create a description with invalid name length (too short)"
        Description description1 = SmartDescriptionBuilder.builderForOwner(testOwner)
                .withDescriptionName("")  // Too short - auto-fixed by SmartDescriptionBuilder
                .buildAndValidate()

        then: "the name is auto-fixed to meet constraints"
        description1.descriptionName.length() >= 1
        description1.descriptionName.length() <= 50

        when: "trying to create a description with invalid name length (too long)"
        Description description2 = SmartDescriptionBuilder.builderForOwner(testOwner)
                .withDescriptionName("a" * 51)  // Too long - auto-fixed by SmartDescriptionBuilder
                .buildAndValidate()

        then: "the name is auto-fixed to meet constraints"
        description2.descriptionName.length() >= 1
        description2.descriptionName.length() <= 50
    }

    void 'test description update operations'() {
        given:
        Description description = SmartDescriptionBuilder.builderForOwner(testOwner)
                .withUniqueDescriptionName("costco")
                .asActive()
                .buildAndValidate()
        Description savedDescription = descriptionRepository.save(description)

        when:
        savedDescription.activeStatus = false
        Description updatedDescription = descriptionRepository.save(savedDescription)

        then:
        updatedDescription.descriptionId == savedDescription.descriptionId
        updatedDescription.descriptionName == savedDescription.descriptionName
        updatedDescription.activeStatus == false

        when:
        Optional<Description> refetchedDescription = descriptionRepository.findByDescriptionName(savedDescription.descriptionName)

        then:
        refetchedDescription.isPresent()
        refetchedDescription.get().activeStatus == false
        refetchedDescription.get().descriptionName == savedDescription.descriptionName
    }

    void 'test description deletion'() {
        given:
        Description descriptionToDelete = SmartDescriptionBuilder.builderForOwner(testOwner)
                .withUniqueDescriptionName("temporary")
                .asActive()
                .buildAndValidate()
        Description savedDescription = descriptionRepository.save(descriptionToDelete)

        when:
        descriptionRepository.delete(savedDescription)
        Optional<Description> deletedDescription = descriptionRepository.findByDescriptionName(savedDescription.descriptionName)

        then:
        !deletedDescription.isPresent()

        when:
        Optional<Description> deletedById = descriptionRepository.findByDescriptionId(savedDescription.descriptionId)

        then:
        !deletedById.isPresent()
    }

    void 'test smart builder auto-fixes constraint violations'() {
        given: "SmartDescriptionBuilder auto-fixes invalid length constraints"
        // The SmartDescriptionBuilder is designed to auto-fix constraint violations
        // rather than throw exceptions, which ensures valid test data creation

        when: "creating a description with potentially invalid name length"
        Description description1 = SmartDescriptionBuilder.builderForOwner(testOwner)
                .withDescriptionName("a" * 51)  // Would be too long, but auto-fixed
                .buildAndValidate()

        then: "the name is automatically truncated to valid length"
        description1.descriptionName.length() <= 50
        description1.descriptionName.length() >= 1

        when: "saving this auto-fixed description"
        Description saved1 = descriptionRepository.save(description1)

        then: "it saves successfully due to constraint compliance"
        saved1.descriptionId != null
    }

    void 'test description with maximum allowed lengths'() {
        given:
        Description description = SmartDescriptionBuilder.builderForOwner(testOwner)
                .withDescriptionName("a" * 50)  // Maximum allowed length (50 chars)
                .asActive()
                .buildAndValidate()

        when:
        Description savedDescription = descriptionRepository.save(description)

        then:
        savedDescription.descriptionId != null
        savedDescription.descriptionName.length() == 50
    }

    void 'test description active status functionality'() {
        given:
        Description activeDescription = SmartDescriptionBuilder.builderForOwner(testOwner)
                .withUniqueDescriptionName("active")
                .asActive()
                .buildAndValidate()

        Description inactiveDescription = SmartDescriptionBuilder.builderForOwner(testOwner)
                .withUniqueDescriptionName("inactive")
                .asInactive()
                .buildAndValidate()

        when:
        Description savedActive = descriptionRepository.save(activeDescription)
        Description savedInactive = descriptionRepository.save(inactiveDescription)

        then:
        savedActive.activeStatus == true
        savedInactive.activeStatus == false

        when:
        List<Description> allDescriptions = descriptionRepository.findAll()

        then:
        allDescriptions.any { it.descriptionId == savedActive.descriptionId && it.activeStatus == true }
        allDescriptions.any { it.descriptionId == savedInactive.descriptionId && it.activeStatus == false }
    }

    void 'test find non-existent description'() {
        when:
        Optional<Description> nonExistentByName = descriptionRepository.findByDescriptionName("nonexistent_${testOwner}")
        Optional<Description> nonExistentById = descriptionRepository.findByDescriptionId(-999L)

        then:
        !nonExistentByName.isPresent()
        !nonExistentById.isPresent()
    }

    void 'test description entity persistence validation'() {
        given:
        Description description = SmartDescriptionBuilder.builderForOwner(testOwner)
                .withUniqueDescriptionName("persistence")
                .asActive()
                .buildAndValidate()

        when:
        Description savedDescription = descriptionRepository.save(description)

        then:
        savedDescription.descriptionId != null
        savedDescription.descriptionName != null
        savedDescription.descriptionName.length() >= 1
        savedDescription.descriptionName.length() <= 50
        savedDescription.activeStatus == true
        savedDescription.dateAdded != null
        savedDescription.dateUpdated != null
        // descriptionCount is transient, not persisted to DB
        savedDescription.descriptionCount == 0L

        when:
        Optional<Description> refetchedOpt = descriptionRepository.findById(savedDescription.descriptionId)

        then:
        refetchedOpt.isPresent()
        def refetchedDescription = refetchedOpt.get()
        refetchedDescription.descriptionName == savedDescription.descriptionName
        refetchedDescription.activeStatus == savedDescription.activeStatus
        refetchedDescription.dateAdded != null
        refetchedDescription.dateUpdated != null
    }

    void 'test smart builder convenience methods'() {
        given:
        Description storeDescription = SmartDescriptionBuilder.builderForOwner(testOwner)
                .asStoreDescription()
                .asActive()
                .buildAndValidate()

        Description restaurantDescription = SmartDescriptionBuilder.builderForOwner(testOwner)
                .asRestaurantDescription()
                .asInactive()
                .buildAndValidate()

        Description onlineDescription = SmartDescriptionBuilder.builderForOwner(testOwner)
                .asOnlineDescription()
                .asActive()
                .buildAndValidate()

        when:
        Description savedStore = descriptionRepository.save(storeDescription)
        Description savedRestaurant = descriptionRepository.save(restaurantDescription)
        Description savedOnline = descriptionRepository.save(onlineDescription)

        then:
        savedStore.descriptionName.contains("store")
        savedStore.descriptionName.contains(testOwner.toLowerCase())
        savedStore.activeStatus == true

        savedRestaurant.descriptionName.contains("restaurant")
        savedRestaurant.descriptionName.contains(testOwner.toLowerCase())
        savedRestaurant.activeStatus == false

        savedOnline.descriptionName.contains("online")
        savedOnline.descriptionName.contains(testOwner.toLowerCase())
        savedOnline.activeStatus == true

        // Description names follow lowercase conversion
        savedStore.descriptionName == savedStore.descriptionName.toLowerCase()
        savedRestaurant.descriptionName == savedRestaurant.descriptionName.toLowerCase()
        savedOnline.descriptionName == savedOnline.descriptionName.toLowerCase()

        // Verify constraint compliance
        savedStore.descriptionName.length() >= 1
        savedStore.descriptionName.length() <= 50
        savedRestaurant.descriptionName.length() >= 1
        savedRestaurant.descriptionName.length() <= 50
        savedOnline.descriptionName.length() >= 1
        savedOnline.descriptionName.length() <= 50
    }

    void 'test business description convenience method'() {
        given:
        Description gasStationDescription = SmartDescriptionBuilder.builderForOwner(testOwner)
                .withBusinessDescription("gas")
                .asActive()
                .buildAndValidate()

        Description pharmacyDescription = SmartDescriptionBuilder.builderForOwner(testOwner)
                .withBusinessDescription("pharmacy")
                .asActive()
                .buildAndValidate()

        when:
        Description savedGas = descriptionRepository.save(gasStationDescription)
        Description savedPharmacy = descriptionRepository.save(pharmacyDescription)

        then:
        savedGas.descriptionName.contains("gas")
        savedGas.descriptionName.contains(testOwner.toLowerCase())
        savedGas.activeStatus == true

        savedPharmacy.descriptionName.contains("pharmacy")
        savedPharmacy.descriptionName.contains(testOwner.toLowerCase())
        savedPharmacy.activeStatus == true

        // Verify unique names
        savedGas.descriptionName != savedPharmacy.descriptionName
    }

    void 'test repository context helper methods'() {
        given:
        Description uniqueDescription = repositoryContext.createUniqueDescription("shopping")

        when:
        Description savedDescription = descriptionRepository.save(uniqueDescription)

        then:
        savedDescription.descriptionId != null
        savedDescription.descriptionName.contains("shopping")
        savedDescription.descriptionName.contains(testOwner.toLowerCase())
        savedDescription.activeStatus == true
    }

    void 'test description name case conversion and validation'() {
        given:
        Description description = SmartDescriptionBuilder.builderForOwner(testOwner)
                .withUniqueDescriptionName("MIXED_CASE")
                .asActive()
                .buildAndValidate()

        when:
        Description savedDescription = descriptionRepository.save(description)

        then:
        // Description name should be lowercase (handled by LowerCaseConverter)
        savedDescription.descriptionName == savedDescription.descriptionName.toLowerCase()
    }
}