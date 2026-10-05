package app.carlosribeiro.homemarket.domain.model

/** An item to add to a list, with the fields written to a new `items/{itemId}` document. */
data class NewItem(
    val listId: String,
    val householdId: String,
    val name: String,
    val quantity: Int,
    val notes: String,
    val urgent: Boolean,
    val addedByUid: String,
    val addedByName: String,
    val approvalStatus: ApprovalStatus
)
