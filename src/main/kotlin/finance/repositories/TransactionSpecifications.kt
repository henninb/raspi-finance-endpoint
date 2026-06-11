package finance.repositories

import finance.domain.ReoccurringType
import finance.domain.Transaction
import finance.domain.TransactionState
import finance.domain.TransactionType
import jakarta.persistence.criteria.Predicate
import org.springframework.data.jpa.domain.Specification
import java.math.BigDecimal
import java.time.LocalDate

object TransactionSpecifications {
    fun searchSpec(
        owner: String,
        accountNameOwner: String,
        search: String? = null,
        states: List<TransactionState>? = null,
        transactionTypes: List<TransactionType>? = null,
        reoccurringTypes: List<ReoccurringType>? = null,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
        minAmount: BigDecimal? = null,
        maxAmount: BigDecimal? = null,
    ): Specification<Transaction> =
        Specification { root, _, cb ->
            val predicates = mutableListOf<Predicate>()

            predicates.add(cb.equal(root.get<String>("owner"), owner))
            predicates.add(cb.equal(root.get<String>("accountNameOwner"), accountNameOwner))
            predicates.add(cb.equal(root.get<Boolean>("activeStatus"), true))

            if (!search.isNullOrBlank()) {
                val term = "%${search.lowercase()}%"
                val textOr =
                    mutableListOf(
                        cb.like(cb.lower(root.get("description")), term),
                        cb.like(cb.lower(root.get("category")), term),
                        cb.like(cb.lower(root.get("notes")), term),
                    )
                search.toBigDecimalOrNull()?.let { amt ->
                    textOr.add(cb.equal(root.get<BigDecimal>("amount"), amt))
                }
                predicates.add(cb.or(*textOr.toTypedArray()))
            }

            if (!states.isNullOrEmpty()) {
                predicates.add(root.get<TransactionState>("transactionState").`in`(states))
            }

            if (!transactionTypes.isNullOrEmpty()) {
                predicates.add(root.get<TransactionType>("transactionType").`in`(transactionTypes))
            }

            if (!reoccurringTypes.isNullOrEmpty()) {
                predicates.add(root.get<ReoccurringType>("reoccurringType").`in`(reoccurringTypes))
            }

            startDate?.let { predicates.add(cb.greaterThanOrEqualTo(root.get("transactionDate"), it)) }
            endDate?.let { predicates.add(cb.lessThanOrEqualTo(root.get("transactionDate"), it)) }
            minAmount?.let { predicates.add(cb.greaterThanOrEqualTo(root.get("amount"), it)) }
            maxAmount?.let { predicates.add(cb.lessThanOrEqualTo(root.get("amount"), it)) }

            cb.and(*predicates.toTypedArray())
        }
}
