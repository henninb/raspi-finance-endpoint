package finance.helpers

import finance.domain.Description

class DescriptionBuilder {

    String descriptionName = 'foo'
    boolean activeStatus = true

    static DescriptionBuilder builder() {
        return new DescriptionBuilder()
    }

    Description build() {
        Description description = new Description()
        description.description = descriptionName
        description.activeStatus = activeStatus
        return description
    }

    DescriptionBuilder withDescription(String description) {
        this.descriptionName = description
        return this
    }

    DescriptionBuilder withActiveStatus(Boolean activeStatus) {
        this.activeStatus = activeStatus
        return this
    }
}
