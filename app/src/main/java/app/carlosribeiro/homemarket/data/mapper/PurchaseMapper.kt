package app.carlosribeiro.homemarket.data.mapper

import app.carlosribeiro.homemarket.data.local.PurchaseEntity
import app.carlosribeiro.homemarket.data.local.PurchaseItemEntity
import app.carlosribeiro.homemarket.data.local.PurchaseWithWeek
import app.carlosribeiro.homemarket.domain.model.Purchase
import app.carlosribeiro.homemarket.domain.model.PurchaseItem
import com.google.firebase.Timestamp
import java.time.Instant
import java.util.Date

/** Field names of `purchases/{purchaseId}`, see docs/backend.md. */
object PurchaseFields {
    const val LIST_ID = "listId"
    const val HOUSEHOLD_ID = "householdId"
    const val WEEK_LABEL = "weekLabel"
    const val TOTAL = "total"
    const val RECEIPT_URL = "receiptUrl"
    const val RECEIPT_PROCESSED = "receiptProcessed"
    const val CREATED_AT = "createdAt"
    const val STORE_NAME = "storeName"
}

/** Field names of `purchaseItems/{purchaseItemId}`, see docs/backend.md. */
object PurchaseItemFields {
    const val PURCHASE_ID = "purchaseId"
    const val NAME = "name"
    const val QUANTITY = "quantity"
    const val UNIT_PRICE = "unitPrice"
    const val TOTAL_PRICE = "totalPrice"
}

/** Maps documents with the same defaults as the iOS `Purchase` and `PurchaseItem` initializers. */
object PurchaseMapper {
    fun documentToPurchaseEntity(id: String, data: Map<String, Any?>): PurchaseEntity = PurchaseEntity(
        id = id,
        listId = data[PurchaseFields.LIST_ID] as? String ?: "",
        householdId = data[PurchaseFields.HOUSEHOLD_ID] as? String ?: "",
        weekLabel = data[PurchaseFields.WEEK_LABEL] as? String ?: "",
        total = (data[PurchaseFields.TOTAL] as? Number)?.toDouble() ?: 0.0,
        receiptUrl = data[PurchaseFields.RECEIPT_URL] as? String,
        receiptProcessed = data[PurchaseFields.RECEIPT_PROCESSED] as? Boolean ?: false,
        createdAt = when (val value = data[PurchaseFields.CREATED_AT]) {
            is Timestamp -> value.toDate().time
            is Date -> value.time
            else -> null
        },
        storeName = data[PurchaseFields.STORE_NAME] as? String
    )

    fun documentToItemEntity(id: String, data: Map<String, Any?>): PurchaseItemEntity = PurchaseItemEntity(
        id = id,
        purchaseId = data[PurchaseItemFields.PURCHASE_ID] as? String ?: "",
        name = data[PurchaseItemFields.NAME] as? String ?: "",
        quantity = (data[PurchaseItemFields.QUANTITY] as? Number)?.toInt() ?: 1,
        unitPrice = (data[PurchaseItemFields.UNIT_PRICE] as? Number)?.toDouble() ?: 0.0,
        totalPrice = (data[PurchaseItemFields.TOTAL_PRICE] as? Number)?.toDouble() ?: 0.0
    )

    /** A cached list with no `weekStart` (stored as 0) counts as missing, so the label falls back. */
    fun toDomain(row: PurchaseWithWeek): Purchase {
        val entity = row.purchase
        val hasWeek = (row.listWeekStart ?: 0L) > 0L && (row.listWeekEnd ?: 0L) > 0L
        return Purchase(
            id = entity.id,
            listId = entity.listId,
            householdId = entity.householdId,
            weekLabel = entity.weekLabel,
            weekStart = row.listWeekStart?.takeIf { hasWeek }?.let(Instant::ofEpochMilli),
            weekEnd = row.listWeekEnd?.takeIf { hasWeek }?.let(Instant::ofEpochMilli),
            total = entity.total,
            receiptUrl = entity.receiptUrl,
            receiptProcessed = entity.receiptProcessed,
            createdAt = entity.createdAt?.let(Instant::ofEpochMilli),
            storeName = entity.storeName
        )
    }

    fun itemToDomain(entity: PurchaseItemEntity): PurchaseItem = PurchaseItem(
        id = entity.id,
        purchaseId = entity.purchaseId,
        name = entity.name,
        quantity = entity.quantity,
        unitPrice = entity.unitPrice,
        totalPrice = entity.totalPrice
    )
}
