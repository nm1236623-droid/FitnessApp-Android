package com.example.fitness.coach.cloud

import com.example.fitness.data.TrainingPlan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/**
 * Local in-memory store for received plans.
 *
 * IMPORTANT: this is intentionally separated from [com.example.fitness.data.TrainingPlanRepository]
 * so that imported coach plans do NOT merge with the user's own training plans.
 */
class CoachRemotePlanRepository {
    private val _items = MutableStateFlow<List<RemotePlanItem>>(emptyList())
    val items: Flow<List<RemotePlanItem>> = _items.asStateFlow()

    /** Backward compatible: exposes just plans for older UI. */
    val plans: Flow<List<TrainingPlan>> = items.map { list ->
        list.map { it.plan }
    }

    fun upsertCoachPlans(coachId: String, incomingPlans: List<TrainingPlan>) {
        // Dedupe by plan id; prefer last occurrence.
        val map = LinkedHashMap<String, RemotePlanItem>()
        _items.value.forEach { map[it.plan.id] = it }
        incomingPlans.forEach { plan ->
            map[plan.id] = RemotePlanItem(plan = plan, sourceCoachId = coachId, isInbox = false)
        }
        _items.value = map.values.toList()
    }

    fun upsertInboxPlans(incoming: List<RemotePlanItem>) {
        val map = LinkedHashMap<String, RemotePlanItem>()
        _items.value.forEach { map[it.plan.id] = it }
        incoming.forEach { item ->
            map[item.plan.id] = item.copy(isInbox = true)
        }
        _items.value = map.values.toList()
    }

    /** Remove a single remote plan from the cache (used by trainee UI). */
    fun removeById(planId: String) {
        _items.value = _items.value.filterNot { it.plan.id == planId }
    }

    /** Remove multiple remote plans from the cache. */
    fun removeByIds(planIds: Set<String>) {
        if (planIds.isEmpty()) return
        _items.value = _items.value.filterNot { it.plan.id in planIds }
    }

    fun getItem(planId: String): RemotePlanItem? = _items.value.firstOrNull { it.plan.id == planId }

    fun clear() {
        _items.value = emptyList()
    }
}
