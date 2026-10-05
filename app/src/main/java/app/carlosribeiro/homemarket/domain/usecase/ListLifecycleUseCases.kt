package app.carlosribeiro.homemarket.domain.usecase

import app.carlosribeiro.homemarket.domain.model.AdminError
import app.carlosribeiro.homemarket.domain.model.AdminResult
import app.carlosribeiro.homemarket.domain.model.ApprovalStatus
import app.carlosribeiro.homemarket.domain.model.ListItem
import app.carlosribeiro.homemarket.domain.model.ListStatus
import app.carlosribeiro.homemarket.domain.model.UserRole
import app.carlosribeiro.homemarket.domain.model.WeekList
import app.carlosribeiro.homemarket.domain.repository.AuthRepository
import app.carlosribeiro.homemarket.domain.repository.ListLifecycleRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Role check shared by the admin use cases: members get [AdminError.NOT_ADMIN], never a write. */
class AdminCheck @Inject constructor(private val authRepository: AuthRepository) {
    suspend fun error(): AdminError? {
        val user = authRepository.observeCurrentUser().first()
        return when {
            user == null -> AdminError.NOT_SIGNED_IN
            user.role != UserRole.ADMIN || user.householdId == null -> AdminError.NOT_ADMIN
            else -> null
        }
    }
}

private suspend fun AdminCheck.guarded(allowed: Boolean, write: suspend () -> AdminResult): AdminResult {
    val error = error() ?: if (allowed) null else AdminError.INVALID_STATUS
    return if (error == null) write() else AdminResult.Failure(error)
}

/** Web admin panel "lock": `open → locked`. Items added from now on wait for approval. */
class LockListUseCase @Inject constructor(
    private val adminCheck: AdminCheck,
    private val repository: ListLifecycleRepository
) {
    suspend operator fun invoke(list: WeekList): AdminResult = adminCheck.guarded(list.status == ListStatus.OPEN) {
        repository.updateStatus(list.id, ListStatus.LOCKED)
    }
}

/** Reopen a locked list: `locked → open`. */
class ReopenListUseCase @Inject constructor(
    private val adminCheck: AdminCheck,
    private val repository: ListLifecycleRepository
) {
    suspend operator fun invoke(list: WeekList): AdminResult = adminCheck.guarded(list.status == ListStatus.LOCKED) {
        repository.updateStatus(list.id, ListStatus.OPEN)
    }
}

class ApproveItemUseCase @Inject constructor(
    private val adminCheck: AdminCheck,
    private val repository: ListLifecycleRepository
) {
    suspend operator fun invoke(item: ListItem): AdminResult =
        adminCheck.guarded(item.approvalStatus == ApprovalStatus.PENDING) { repository.approveItem(item.id) }
}

class RejectItemUseCase @Inject constructor(
    private val adminCheck: AdminCheck,
    private val repository: ListLifecycleRepository
) {
    suspend operator fun invoke(item: ListItem): AdminResult =
        adminCheck.guarded(item.approvalStatus == ApprovalStatus.PENDING) { repository.rejectItem(item.id) }
}

/** The admin panel's list and approval actions, grouped for the view model. */
class ListAdminActions @Inject constructor(
    val lockList: LockListUseCase,
    val reopenList: ReopenListUseCase,
    val approveItem: ApproveItemUseCase,
    val rejectItem: RejectItemUseCase
)
