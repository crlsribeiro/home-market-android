package app.carlosribeiro.homemarket.domain.repository

import app.carlosribeiro.homemarket.domain.model.AdminResult
import app.carlosribeiro.homemarket.domain.model.Purchase
import app.carlosribeiro.homemarket.domain.model.PurchaseItem
import kotlinx.coroutines.flow.Flow

/** `purchases` and `purchaseItems` (docs/backend.md). Reads come from the local cache, kept in sync with Firestore. */
interface PurchaseRepository {
    /** The household's purchases, newest `createdAt` first. */
    fun observePurchases(householdId: String): Flow<List<Purchase>>

    fun observePurchase(purchaseId: String): Flow<Purchase?>

    fun observeItems(purchaseId: String): Flow<List<PurchaseItem>>

    /**
     * iOS `HistoryService.createPurchaseForList`: reuses the purchase of [listId] when one exists, otherwise
     * creates one with `total: 0`, `receiptUrl: null`, `receiptProcessed: false` and the client time.
     */
    suspend fun createPurchaseForList(listId: String, householdId: String, weekLabel: String): AdminResult

    /**
     * iOS `HistoryService.updateItemPrice`: writes `unitPrice`, `totalPrice` (times the stored quantity) and
     * [name] when it is not null, then recomputes the purchase `total` from every line.
     */
    suspend fun updateItem(purchaseId: String, itemId: String, name: String?, unitPrice: Double): AdminResult
}
