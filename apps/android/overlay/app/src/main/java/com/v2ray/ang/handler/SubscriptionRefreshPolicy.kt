package com.v2ray.ang.handler

import com.v2ray.ang.dto.entities.SubscriptionItem

/** Shared eligibility for scheduling and executing delayed subscription work. */
internal object SubscriptionRefreshPolicy {
    fun shouldSchedule(item: SubscriptionItem?): Boolean =
        item != null && item.enabled && item.autoUpdate && item.url.isNotBlank()
}
