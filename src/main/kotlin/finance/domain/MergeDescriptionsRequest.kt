package finance.domain

import com.fasterxml.jackson.annotation.JsonProperty
import tools.jackson.databind.json.JsonMapper

data class MergeDescriptionsRequest(
    @param:JsonProperty("sourceNames") val sourceNames: List<String> = listOf(),
    @param:JsonProperty("targetName") val targetName: String = "",
) {
    override fun toString(): String = mapper.writeValueAsString(this)

    companion object {
        private val mapper = JsonMapper.builderWithJackson2Defaults().findAndAddModules().build()
    }
}
