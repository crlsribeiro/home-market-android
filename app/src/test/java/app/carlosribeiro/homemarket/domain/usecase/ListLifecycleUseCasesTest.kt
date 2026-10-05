package app.carlosribeiro.homemarket.domain.usecase

import app.carlosribeiro.homemarket.domain.model.AdminError
import app.carlosribeiro.homemarket.domain.model.AdminResult
import app.carlosribeiro.homemarket.domain.model.AppUser
import app.carlosribeiro.homemarket.domain.model.ApprovalStatus
import app.carlosribeiro.homemarket.domain.model.ItemStatus
import app.carlosribeiro.homemarket.domain.model.ListItem
import app.carlosribeiro.homemarket.domain.model.ListStatus
import app.carlosribeiro.homemarket.domain.model.UserRole
import app.carlosribeiro.homemarket.domain.model.WeekList
import app.carlosribeiro.homemarket.domain.repository.AuthRepository
import app.carlosribeiro.homemarket.domain.repository.ListLifecycleRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ListLifecycleUseCasesTest {

    private val authRepository = mockk<AuthRepository>()
    private val repository = mockk<ListLifecycleRepository>()
    private val adminCheck = AdminCheck(authRepository)
    private val lock = LockListUseCase(adminCheck, repository)
    private val reopen = ReopenListUseCase(adminCheck, repository)
    private val approve = ApproveItemUseCase(adminCheck, repository)
    private val reject = RejectItemUseCase(adminCheck, repository)

    private fun signedIn(role: UserRole?, householdId: String? = "h1") {
        every { authRepository.observeCurrentUser() } returns
            flowOf(role?.let { AppUser("u1", "Maria", "maria@example.com", null, householdId, it) })
    }

    private fun list(status: ListStatus) = WeekList("l1", "h1", Instant.EPOCH, Instant.EPOCH, status, null)

    private fun item(approval: ApprovalStatus) = ListItem(
        "i1", "l1", "h1", "Leite", 1, "", false, "u2", "João", ItemStatus.PENDING, approval, false, null, null
    )

    @Test
    fun lock_movesAnOpenListToLocked() = runTest {
        signedIn(UserRole.ADMIN)
        coEvery { repository.updateStatus("l1", ListStatus.LOCKED) } returns AdminResult.Success

        assertEquals(AdminResult.Success, lock(list(ListStatus.OPEN)))
        coVerify { repository.updateStatus("l1", ListStatus.LOCKED) }
    }

    @Test
    fun lock_onlyFromOpen() = runTest {
        signedIn(UserRole.ADMIN)

        for (status in listOf(ListStatus.LOCKED, ListStatus.SHOPPING, ListStatus.CLOSED)) {
            assertEquals(AdminResult.Failure(AdminError.INVALID_STATUS), lock(list(status)))
        }
        coVerify(exactly = 0) { repository.updateStatus(any(), any()) }
    }

    @Test
    fun reopen_movesALockedListToOpen() = runTest {
        signedIn(UserRole.ADMIN)
        coEvery { repository.updateStatus("l1", ListStatus.OPEN) } returns AdminResult.Success

        assertEquals(AdminResult.Success, reopen(list(ListStatus.LOCKED)))
        coVerify { repository.updateStatus("l1", ListStatus.OPEN) }
    }

    @Test
    fun reopen_onlyFromLocked() = runTest {
        signedIn(UserRole.ADMIN)

        for (status in listOf(ListStatus.OPEN, ListStatus.SHOPPING, ListStatus.CLOSED)) {
            assertEquals(AdminResult.Failure(AdminError.INVALID_STATUS), reopen(list(status)))
        }
        coVerify(exactly = 0) { repository.updateStatus(any(), any()) }
    }

    @Test
    fun approveAndReject_pendingItems() = runTest {
        signedIn(UserRole.ADMIN)
        coEvery { repository.approveItem("i1") } returns AdminResult.Success
        coEvery { repository.rejectItem("i1") } returns AdminResult.Success

        assertEquals(AdminResult.Success, approve(item(ApprovalStatus.PENDING)))
        assertEquals(AdminResult.Success, reject(item(ApprovalStatus.PENDING)))
        coVerify { repository.approveItem("i1") }
        coVerify { repository.rejectItem("i1") }
    }

    @Test
    fun approveAndReject_onlyPendingItems() = runTest {
        signedIn(UserRole.ADMIN)

        for (approval in listOf(ApprovalStatus.NOT_REQUIRED, ApprovalStatus.APPROVED, ApprovalStatus.REJECTED)) {
            assertEquals(AdminResult.Failure(AdminError.INVALID_STATUS), approve(item(approval)))
            assertEquals(AdminResult.Failure(AdminError.INVALID_STATUS), reject(item(approval)))
        }
        coVerify(exactly = 0) { repository.approveItem(any()) }
        coVerify(exactly = 0) { repository.rejectItem(any()) }
    }

    @Test
    fun member_getsNotAdminForEveryAction() = runTest {
        signedIn(UserRole.MEMBER)
        val notAdmin = AdminResult.Failure(AdminError.NOT_ADMIN)

        assertEquals(notAdmin, lock(list(ListStatus.OPEN)))
        assertEquals(notAdmin, reopen(list(ListStatus.LOCKED)))
        assertEquals(notAdmin, approve(item(ApprovalStatus.PENDING)))
        assertEquals(notAdmin, reject(item(ApprovalStatus.PENDING)))
        coVerify(exactly = 0) { repository.updateStatus(any(), any()) }
    }

    @Test
    fun adminWithoutHousehold_getsNotAdmin() = runTest {
        signedIn(UserRole.ADMIN, householdId = null)

        assertEquals(AdminResult.Failure(AdminError.NOT_ADMIN), lock(list(ListStatus.OPEN)))
    }

    @Test
    fun signedOut_getsNotSignedIn() = runTest {
        signedIn(null)

        assertEquals(AdminResult.Failure(AdminError.NOT_SIGNED_IN), lock(list(ListStatus.OPEN)))
    }

    @Test
    fun repositoryFailure_isReturned() = runTest {
        signedIn(UserRole.ADMIN)
        coEvery { repository.updateStatus(any(), any()) } returns AdminResult.Failure(AdminError.NETWORK)

        assertEquals(AdminResult.Failure(AdminError.NETWORK), lock(list(ListStatus.OPEN)))
    }
}
