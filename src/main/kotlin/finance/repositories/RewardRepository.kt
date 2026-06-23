package finance.repositories

import finance.domain.Reward
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface RewardRepository : JpaRepository<Reward, Long> {
    fun findByOwnerAndRewardId(
        owner: String,
        rewardId: Long,
    ): Optional<Reward>

    fun findByOwnerAndAccountIdAndActiveStatusIsTrue(
        owner: String,
        accountId: Long,
    ): List<Reward>

    fun findByOwnerAndActiveStatusIsTrue(owner: String): List<Reward>
}
