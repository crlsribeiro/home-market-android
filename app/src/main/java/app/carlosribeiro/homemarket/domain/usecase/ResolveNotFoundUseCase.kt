package app.carlosribeiro.homemarket.domain.usecase

import app.carlosribeiro.homemarket.domain.model.ItemError
import app.carlosribeiro.homemarket.domain.model.ItemResult
import app.carlosribeiro.homemarket.domain.model.ListItem
import app.carlosribeiro.homemarket.domain.repository.AuthRepository
import app.carlosribeiro.homemarket.domain.repository.ItemRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Web `NotFoundModal` (docs/backend.md, decision 3): the member who added a not-found item resolves
 * it. "Keep" and "discard" write the same thing: the item moves to next week and is marked resolved.
 */
class ResolveNotFoundUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val itemRepository: ItemRepository
) {
    suspend operator fun invoke(item: ListItem): ItemResult {
        val uid = authRepository.observeCurrentUser().first()?.uid
        return when {
            uid == null -> ItemResult.Failure(ItemError.NOT_SIGNED_IN)
            !item.awaitsNotFoundDecisionBy(uid) -> ItemResult.Failure(ItemError.UNKNOWN)
            else -> itemRepository.resolveNotFound(item.id)
        }
    }
}
