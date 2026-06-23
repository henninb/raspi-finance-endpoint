package finance.services

import finance.configurations.ResilienceComponents
import finance.domain.Reward
import finance.domain.ServiceResult
import finance.repositories.RewardRepository
import finance.utils.TenantContext
import finance.utils.orThrowNotFound
import jakarta.validation.Validator
import org.springframework.stereotype.Service

@Service
class RewardService
    constructor(
        private val rewardRepository: RewardRepository,
        meterService: MeterService,
        validator: Validator,
        resilienceComponents: ResilienceComponents,
    ) : CrudBaseService<Reward, Long>(meterService, validator, resilienceComponents) {
        override fun getEntityName(): String = "Reward"

        override fun findAllActive(): ServiceResult<List<Reward>> =
            handleServiceOperation("findAllActive", null) {
                rewardRepository.findByOwnerAndActiveStatusIsTrue(TenantContext.getCurrentOwner())
            }

        override fun findById(id: Long): ServiceResult<Reward> =
            handleServiceOperation("findById", id) {
                rewardRepository
                    .findByOwnerAndRewardId(TenantContext.getCurrentOwner(), id)
                    .orThrowNotFound("Reward", id)
            }

        override fun save(entity: Reward): ServiceResult<Reward> =
            handleServiceOperation("save", entity.rewardId) {
                entity.owner = TenantContext.getCurrentOwner()
                validateOrThrow(entity)
                rewardRepository.saveAndFlush(entity)
            }

        override fun update(entity: Reward): ServiceResult<Reward> =
            handleServiceOperation("update", entity.rewardId) {
                val owner = TenantContext.getCurrentOwner()
                validateOrThrow(entity)
                val existing =
                    rewardRepository
                        .findByOwnerAndRewardId(owner, entity.rewardId)
                        .orThrowNotFound("Reward", entity.rewardId)
                existing.multiplier = entity.multiplier
                existing.category = entity.category
                existing.cpp = entity.cpp
                existing.activeStatus = entity.activeStatus
                existing.dateUpdated = nowTimestamp()
                rewardRepository.saveAndFlush(existing)
            }

        override fun deleteById(id: Long): ServiceResult<Reward> =
            handleServiceOperation("deleteById", id) {
                val owner = TenantContext.getCurrentOwner()
                val reward =
                    rewardRepository
                        .findByOwnerAndRewardId(owner, id)
                        .orThrowNotFound("Reward", id)
                rewardRepository.delete(reward)
                reward
            }

        fun findByAccountId(accountId: Long): ServiceResult<List<Reward>> =
            handleServiceOperation("findByAccountId", accountId) {
                rewardRepository.findByOwnerAndAccountIdAndActiveStatusIsTrue(
                    TenantContext.getCurrentOwner(),
                    accountId,
                )
            }

        fun loadAllTiersGrouped(): Map<Long, List<Reward>> = findAllActive().getDataOrNull()?.groupBy { it.accountId } ?: emptyMap()
    }
