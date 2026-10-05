package app.carlosribeiro.homemarket.domain.usecase

import app.carlosribeiro.homemarket.domain.model.ItemDetail
import app.carlosribeiro.homemarket.domain.model.ItemResult
import app.carlosribeiro.homemarket.domain.model.ListItem
import app.carlosribeiro.homemarket.domain.repository.ItemRepository
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class ObserveItemDetailUseCase @Inject constructor(private val itemRepository: ItemRepository) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(itemId: String): Flow<ItemDetail?> = itemRepository.observeItem(itemId).flatMapLatest { item ->
        if (item == null) {
            flowOf(null)
        } else {
            itemRepository.observeList(item.listId).map { list -> ItemDetail(item, list) }
        }
    }.distinctUntilChanged()
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
