package app.carlosribeiro.homemarket.domain.usecase

import app.carlosribeiro.homemarket.domain.model.AdminResult
import app.carlosribeiro.homemarket.domain.model.AppUser
import app.carlosribeiro.homemarket.domain.model.Purchase
import app.carlosribeiro.homemarket.domain.model.PurchaseDetail
import app.carlosribeiro.homemarket.domain.model.UserRole
import app.carlosribeiro.homemarket.domain.repository.AuthRepository
import app.carlosribeiro.homemarket.domain.repository.PurchaseRepository
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** Prices follow the web (docs/backend.md, decision 4): only the household admin reads purchases. */
private val AppUser?.adminHouseholdId: String?
    get() = this?.takeIf { it.role == UserRole.ADMIN }?.householdId

/** The History tab: the household's purchases, newest first. Members always get an empty list. */
class ObservePurchaseHistoryUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val repository: PurchaseRepository
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<List<Purchase>> = authRepository.observeCurrentUser()
        .map { it.adminHouseholdId }
        .distinctUntilChanged()
        .flatMapLatest { householdId ->
            if (householdId == null) flowOf(emptyList()) else repository.observePurchases(householdId)
        }
}

/** Purchase detail with its lines, or null when the purchase is gone or the user is not the admin. */
class ObservePurchaseDetailUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val repository: PurchaseRepository
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(purchaseId: String): Flow<PurchaseDetail?> = authRepository.observeCurrentUser()
        .map { it.adminHouseholdId }
        .distinctUntilChanged()
        .flatMapLatest { householdId ->
            if (householdId == null) {
                flowOf(null)
            } else {
                combine(repository.observePurchase(purchaseId), repository.observeItems(purchaseId)) {
                        purchase,
                        items
                    ->
                    purchase?.takeIf { it.householdId == householdId }?.let { PurchaseDetail(it, items) }
                }
            }
        }
}

/**
 * iOS `EditPurchaseItemView` save: a blank name keeps the stored one, and the price must be a number of
 * zero or more. The purchase total is recomputed by the repository.
 */
class EditPurchaseItemUseCase @Inject constructor(
    private val adminCheck: AdminCheck,
    private val repository: PurchaseRepository
) {
    suspend operator fun invoke(purchaseId: String, itemId: String, name: String, unitPrice: Double): AdminResult =
        adminCheck.guarded(unitPrice.isFinite() && unitPrice >= 0) {
            repository.updateItem(purchaseId, itemId, name.trim().ifEmpty { null }, unitPrice)
        }
}
