package finance.helpers

import finance.domain.Description

class DescriptionBuilder {

    String description = 'foo'
    boolean activeStatus = true

    static DescriptionBuilder builder() {
        return new DescriptionBuilder()
    }

    Description build() {
        Description description = new Description()
        description.description = description
        description.activeStatus = activeStatus
        return description
    }

    DescriptionBuilder description(String description) {
        this.description = description
        return this
    }

    DescriptionBuilder activeStatus(Boolean activeStatus) {
        this.activeStatus = activeStatus
        return this
    }
}
