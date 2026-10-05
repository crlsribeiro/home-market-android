package app.carlosribeiro.homemarket.data.mapper

import app.carlosribeiro.homemarket.data.local.ItemEntity
import app.carlosribeiro.homemarket.data.local.WeekListEntity
import app.carlosribeiro.homemarket.domain.model.ApprovalStatus
import app.carlosribeiro.homemarket.domain.model.ItemStatus
import app.carlosribeiro.homemarket.domain.model.ListItem
import app.carlosribeiro.homemarket.domain.model.ListStatus
import app.carlosribeiro.homemarket.domain.model.WeekList
import com.google.firebase.Timestamp
import java.time.Instant
import java.util.Date

/** Field names and values of `lists/{listId}`, see docs/backend.md. */
object ListFields {
    const val HOUSEHOLD_ID = "householdId"
    const val WEEK_LABEL = "weekLabel"
    const val WEEK_START = "weekStart"
    const val WEEK_END = "weekEnd"
    const val STATUS = "status"
    const val CREATED_AT = "createdAt"
    const val CLOSED_AT = "closedAt"

    const val STATUS_OPEN = "open"
    const val STATUS_LOCKED = "locked"
    const val STATUS_SHOPPING = "shopping"
    const val STATUS_CLOSED = "closed"
}

/** Field names and values of `items/{itemId}`, see docs/backend.md. */
object ItemFields {
    const val LIST_ID = "listId"
    const val HOUSEHOLD_ID = "householdId"
    const val NAME = "name"
    const val QUANTITY = "quantity"
    const val NOTES = "notes"
    const val URGENT = "urgent"
    const val ADDED_BY_UID = "addedByUid"
    const val ADDED_BY_NAME = "addedByName"
    const val STATUS = "status"
    const val APPROVAL_STATUS = "approvalStatus"
    const val NOT_FOUND_RESOLVED = "notFoundResolved"
    const val PHOTO_URL = "photoURL"
    const val CREATED_AT = "createdAt"

    const val STATUS_PENDING = "pending"
    const val STATUS_PURCHASED = "purchased"
    const val STATUS_NOT_FOUND = "not_found"
    const val STATUS_ROLLED_OVER = "rolled_over"

    const val APPROVAL_NOT_REQUIRED = "not_required"
    const val APPROVAL_PENDING = "pending"
    const val APPROVAL_APPROVED = "approved"
    const val APPROVAL_REJECTED = "rejected"
}

/** Maps documents with the same defaults as the iOS app (`WeekList` and `ListItem` initializers). */
object ListMapper {
    fun documentToListEntity(id: String, data: Map<String, Any?>): WeekListEntity = WeekListEntity(
        id = id,
        householdId = data[ListFields.HOUSEHOLD_ID] as? String ?: "",
        weekStart = data[ListFields.WEEK_START].toEpochMillis() ?: 0L,
        weekEnd = data[ListFields.WEEK_END].toEpochMillis() ?: 0L,
        status = data[ListFields.STATUS] as? String ?: ListFields.STATUS_OPEN,
        createdAt = data[ListFields.CREATED_AT].toEpochMillis()
    )

    fun listEntityToDomain(entity: WeekListEntity): WeekList = WeekList(
        id = entity.id,
        householdId = entity.householdId,
        weekStart = Instant.ofEpochMilli(entity.weekStart),
        weekEnd = Instant.ofEpochMilli(entity.weekEnd),
        status = listStatus(entity.status),
        createdAt = entity.createdAt?.let(Instant::ofEpochMilli)
    )

    fun documentToItemEntity(id: String, data: Map<String, Any?>): ItemEntity = ItemEntity(
        id = id,
        listId = data[ItemFields.LIST_ID] as? String ?: "",
        householdId = data[ItemFields.HOUSEHOLD_ID] as? String ?: "",
        name = data[ItemFields.NAME] as? String ?: "",
        quantity = (data[ItemFields.QUANTITY] as? Number)?.toInt() ?: 1,
        notes = data[ItemFields.NOTES] as? String ?: "",
        urgent = data[ItemFields.URGENT] as? Boolean ?: false,
        addedByUid = data[ItemFields.ADDED_BY_UID] as? String ?: "",
        addedByName = data[ItemFields.ADDED_BY_NAME] as? String ?: "",
        status = data[ItemFields.STATUS] as? String ?: ItemFields.STATUS_PENDING,
        approvalStatus = data[ItemFields.APPROVAL_STATUS] as? String ?: ItemFields.APPROVAL_NOT_REQUIRED,
        notFoundResolved = data[ItemFields.NOT_FOUND_RESOLVED] as? Boolean ?: false,
        photoUrl = data[ItemFields.PHOTO_URL] as? String,
        createdAt = data[ItemFields.CREATED_AT].toEpochMillis()
    )

    fun itemEntityToDomain(entity: ItemEntity): ListItem = ListItem(
        id = entity.id,
        listId = entity.listId,
        householdId = entity.householdId,
        name = entity.name,
        quantity = entity.quantity,
        notes = entity.notes,
        urgent = entity.urgent,
        addedByUid = entity.addedByUid,
        addedByName = entity.addedByName,
        status = itemStatus(entity.status),
        approvalStatus = approvalStatus(entity.approvalStatus),
        notFoundResolved = entity.notFoundResolved,
        photoUrl = entity.photoUrl,
        createdAt = entity.createdAt?.let(Instant::ofEpochMilli)
    )

    /** Unknown values fall back to the iOS defaults: `open`, `pending` and `not_required`. */
    fun listStatus(value: String): ListStatus = when (value) {
        ListFields.STATUS_LOCKED -> ListStatus.LOCKED
        ListFields.STATUS_SHOPPING -> ListStatus.SHOPPING
        ListFields.STATUS_CLOSED -> ListStatus.CLOSED
        else -> ListStatus.OPEN
    }

    fun itemStatus(value: String): ItemStatus = when (value) {
        ItemFields.STATUS_PURCHASED -> ItemStatus.PURCHASED
        ItemFields.STATUS_NOT_FOUND -> ItemStatus.NOT_FOUND
        ItemFields.STATUS_ROLLED_OVER -> ItemStatus.ROLLED_OVER
        else -> ItemStatus.PENDING
    }

    fun approvalStatus(value: String): ApprovalStatus = when (value) {
        ItemFields.APPROVAL_PENDING -> ApprovalStatus.PENDING
        ItemFields.APPROVAL_APPROVED -> ApprovalStatus.APPROVED
        ItemFields.APPROVAL_REJECTED -> ApprovalStatus.REJECTED
        else -> ApprovalStatus.NOT_REQUIRED
    }

    private fun Any?.toEpochMillis(): Long? = when (this) {
        is Timestamp -> toDate().time
        is Date -> time
        else -> null
    }
}
