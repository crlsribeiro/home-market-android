package app.carlosribeiro.homemarket.domain.usecase

import app.carlosribeiro.homemarket.domain.model.AppUser
import app.carlosribeiro.homemarket.domain.model.ApprovalStatus
import app.carlosribeiro.homemarket.domain.model.ItemError
import app.carlosribeiro.homemarket.domain.model.ItemResult
import app.carlosribeiro.homemarket.domain.model.ListError
import app.carlosribeiro.homemarket.domain.model.ListResult
import app.carlosribeiro.homemarket.domain.model.ListStatus
import app.carlosribeiro.homemarket.domain.model.NewItem
import app.carlosribeiro.homemarket.domain.model.UserRole
import app.carlosribeiro.homemarket.domain.model.Week
import app.carlosribeiro.homemarket.domain.model.WeekList
import app.carlosribeiro.homemarket.domain.repository.AuthRepository
import app.carlosribeiro.homemarket.domain.repository.ListRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AddItemUseCaseTest {

    private val authRepository = mockk<AuthRepository>()
    private val listRepository = mockk<ListRepository>()
    private val clock = Clock.fixed(Instant.parse("2026-10-01T15:00:00Z"), ZoneId.of("America/Sao_Paulo"))
    private val useCase = AddItemUseCase(authRepository, listRepository, clock)

    private val member = AppUser("u2", "João Souza", "joao@example.com", null, "h1", UserRole.MEMBER)
    private val input = ItemInput(name = "  Leite  ", quantity = 2, notes = "Integral", urgent = true)
    private val photo = byteArrayOf(1, 2, 3)

    private fun list(status: ListStatus) = WeekList("h1_2026-09-28", "h1", Instant.EPOCH, Instant.EPOCH, status, null)

    private fun signedIn(user: AppUser? = member) {
        every { authRepository.observeCurrentUser() } returns flowOf(user)
    }

    private fun currentList(list: WeekList?) {
        every { listRepository.observeCurrentList("h1") } returns flowOf(list)
    }

    @Test
    fun openList_addsATrimmedItemThatNeedsNoApproval() = runTest {
        signedIn()
        currentList(list(ListStatus.OPEN))
        val item = slot<NewItem>()
        coEvery { listRepository.addItem(capture(item), photo) } returns ItemResult.Success

        assertEquals(ItemResult.Success, useCase(input, photo))
        assertEquals(
            NewItem(
                listId = "h1_2026-09-28",
                householdId = "h1",
                name = "Leite",
                quantity = 2,
                notes = "Integral",
                urgent = true,
                addedByUid = "u2",
                addedByName = "João Souza",
                approvalStatus = ApprovalStatus.NOT_REQUIRED
            ),
            item.captured
        )
    }

    @Test
    fun lockedOrShoppingList_itemWaitsForApproval() = runTest {
        signedIn()
        val item = slot<NewItem>()
        coEvery { listRepository.addItem(capture(item), null) } returns ItemResult.Success

        currentList(list(ListStatus.LOCKED))
        useCase(input, null)
        assertEquals(ApprovalStatus.PENDING, item.captured.approvalStatus)

        currentList(list(ListStatus.SHOPPING))
        useCase(input, null)
        assertEquals(ApprovalStatus.PENDING, item.captured.approvalStatus)
    }

    @Test
    fun noCurrentList_createsThisWeeksListFirst() = runTest {
        signedIn()
        currentList(null)
        val week = slot<Week>()
        coEvery { listRepository.createWeekList("h1", capture(week)) } returns ListResult.Success(list(ListStatus.OPEN))
        coEvery { listRepository.addItem(any(), any()) } returns ItemResult.Success

        assertEquals(ItemResult.Success, useCase(input, null))
        assertEquals(LocalDate.of(2026, 9, 28), week.captured.monday)
        coVerify { listRepository.addItem(match { it.listId == "h1_2026-09-28" }, null) }
    }

    @Test
    fun listClosedEarlyThisWeek_addsNothing() = runTest {
        signedIn()
        currentList(null)
        coEvery { listRepository.createWeekList(any(), any()) } returns ListResult.Success(list(ListStatus.CLOSED))

        assertEquals(ItemResult.Failure(ItemError.CLOSED_THIS_WEEK), useCase(input, null))
        coVerify(exactly = 0) { listRepository.addItem(any(), any()) }
    }

    @Test
    fun listCreationOffline_returnsNetworkError() = runTest {
        signedIn()
        currentList(null)
        coEvery { listRepository.createWeekList(any(), any()) } returns ListResult.Failure(ListError.NETWORK)

        assertEquals(ItemResult.Failure(ItemError.NETWORK), useCase(input, null))
    }

    @Test
    fun quantity_isKeptBetween1And99() = runTest {
        signedIn()
        currentList(list(ListStatus.OPEN))
        val item = slot<NewItem>()
        coEvery { listRepository.addItem(capture(item), null) } returns ItemResult.Success

        useCase(input.copy(quantity = 0), null)
        assertEquals(1, item.captured.quantity)

        useCase(input.copy(quantity = 150), null)
        assertEquals(99, item.captured.quantity)
    }

    @Test
    fun blankName_isRejected() = runTest {
        signedIn()

        assertEquals(ItemResult.Failure(ItemError.NAME_REQUIRED), useCase(input.copy(name = "   "), null))
    }

    @Test
    fun signedOutOrWithoutHousehold_isRejected() = runTest {
        signedIn(null)
        assertEquals(ItemResult.Failure(ItemError.NOT_SIGNED_IN), useCase(input, null))

        signedIn(member.copy(householdId = null))
        assertEquals(ItemResult.Failure(ItemError.NOT_SIGNED_IN), useCase(input, null))
    }

    @Test
    fun photoUploadFailure_isReturned() = runTest {
        signedIn()
        currentList(list(ListStatus.OPEN))
        coEvery { listRepository.addItem(any(), photo) } returns ItemResult.Failure(ItemError.PHOTO_UPLOAD)

        assertEquals(ItemResult.Failure(ItemError.PHOTO_UPLOAD), useCase(input, photo))
    }
}
