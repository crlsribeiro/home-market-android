package app.carlosribeiro.homemarket.domain.usecase

import app.carlosribeiro.homemarket.domain.model.ItemDetail
import app.carlosribeiro.homemarket.domain.model.ItemPrice
import app.carlosribeiro.homemarket.domain.model.ItemResult
import app.carlosribeiro.homemarket.domain.model.ListItem
import app.carlosribeiro.homemarket.domain.model.UserRole
import app.carlosribeiro.homemarket.domain.repository.AuthRepository
import app.carlosribeiro.homemarket.domain.repository.ItemRepository
import app.carlosribeiro.homemarket.domain.repository.PurchaseRepository
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class ObserveItemDetailUseCase @Inject constructor(
    private val itemRepository: ItemRepository,
    private val observeItemPrice: ObserveItemPriceUseCase
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(itemId: String): Flow<ItemDetail?> = itemRepository.observeItem(itemId).flatMapLatest { item ->
        if (item == null) {
            flowOf(null)
        } else {
            combine(itemRepository.observeList(item.listId), observeItemPrice(item)) { list, price ->
                ItemDetail(item, list, price)
            }
        }
    }.distinctUntilChanged()
}

/**
 * iOS `ItemDetailView` price, admin only (docs/backend.md, decision 4): the purchase of the item's list,
 * then the first receipt line whose name matches the item's name, ignoring case. Members get null.
 */
class ObserveItemPriceUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val purchaseRepository: PurchaseRepository
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(item: ListItem): Flow<ItemPrice?> = authRepository.observeCurrentUser()
        .map { it?.role == UserRole.ADMIN && it.householdId == item.householdId }
        .distinctUntilChanged()
        .flatMapLatest { isAdmin -> if (isAdmin) priceOf(item) else flowOf(null) }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun priceOf(item: ListItem): Flow<ItemPrice> =
        purchaseRepository.observePurchaseOfList(item.listId).flatMapLatest { purchase ->
            if (purchase == null) {
                flowOf(ItemPrice(unitPrice = null, quantity = item.quantity))
            } else {
                purchaseRepository.observeItems(purchase.id).map { lines ->
                    val line = lines.firstOrNull { it.name.equals(item.name, ignoreCase = true) }
                    ItemPrice(unitPrice = line?.unitPrice, quantity = item.quantity)
                }
            }
        }
}

/** Any household member can edit the notes, as on the web and iOS apps. */
class UpdateItemNotesUseCase @Inject constructor(private val itemRepository: ItemRepository) {
    suspend operator fun invoke(itemId: String, notes: String): ItemResult = itemRepository.updateNotes(itemId, notes)
}

class RemoveItemUseCase @Inject constructor(private val itemRepository: ItemRepository) {
    suspend operator fun invoke(itemId: String): ItemResult = itemRepository.removeItem(itemId)
}

class ReplaceItemPhotoUseCase @Inject constructor(private val itemRepository: ItemRepository) {
    suspend operator fun invoke(item: ListItem, photo: ByteArray): ItemResult = itemRepository.replacePhoto(item, photo)
}
