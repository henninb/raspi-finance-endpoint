package finance.helpers

import finance.domain.Description
import java.sql.Timestamp
import java.util.*

class DescriptionBuilder {

    Long descriptionId = 0L
    String descriptionName = 'foo'
    Boolean activeStatus = true
    Timestamp dateAdded = new Timestamp(Calendar.getInstance().time.time)
    Timestamp dateUpdated = new Timestamp(Calendar.getInstance().time.time)
    Long descriptionCount = 0L

    static DescriptionBuilder builder() {
        return new DescriptionBuilder()
    }

    Description build() {
        Description description = new Description().with {
            descriptionId = this.descriptionId
            descriptionName = this.descriptionName
            activeStatus = this.activeStatus
            dateAdded = this.dateAdded
            dateUpdated = this.dateUpdated
            descriptionCount = this.descriptionCount
            return it
        }
        return description
    }

    DescriptionBuilder withDescriptionId(Long descriptionId) {
        this.descriptionId = descriptionId
        return this
    }

    DescriptionBuilder withDescription(String description) {
        this.descriptionName = description
        return this
    }

    DescriptionBuilder withDescriptionName(String descriptionName) {
        this.descriptionName = descriptionName
        return this
    }

    DescriptionBuilder withActiveStatus(Boolean activeStatus) {
        this.activeStatus = activeStatus
        return this
    }

    DescriptionBuilder withDateAdded(Timestamp dateAdded) {
        this.dateAdded = dateAdded
        return this
    }

    DescriptionBuilder withDateUpdated(Timestamp dateUpdated) {
        this.dateUpdated = dateUpdated
        return this
    }

    DescriptionBuilder withDescriptionCount(Long descriptionCount) {
        this.descriptionCount = descriptionCount
        return this
    }
}
