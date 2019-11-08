package finance.models

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import javax.persistence.*
import javax.validation.constraints.Min
import javax.validation.constraints.Size
import org.hibernate.annotations.Proxy

@Entity(name = "CategoryEntity")
@Proxy(lazy = false)
@Table(name = "t_category")
open class Category constructor(_categoryId: Long = 0L, _category: String = "" ) {

    constructor() : this(0L, "")

    //TODO: add active_status field

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Min(value = 0L)
    @JsonProperty
    var categoryId = _categoryId


    @Size(min = 1, max = 50)
    @Column(unique=true)
    @JsonProperty
    var category = _category

//    @JsonProperty
//    var dateUpdated = _dateUpdated
//
//    @JsonProperty
//    var dateAdded = _dateAdded

    override fun toString(): String = mapper.writeValueAsString(this)

    companion object {
        @JsonIgnore
        private val mapper = ObjectMapper()
    }
}
