package app.carlosribeiro.homemarket.data.local

import androidx.room.Embedded

/** A cached purchase with the week of its list, when that list is cached too (docs/backend.md, decision 1). */
data class PurchaseWithWeek(@Embedded val purchase: PurchaseEntity, val listWeekStart: Long?, val listWeekEnd: Long?)
