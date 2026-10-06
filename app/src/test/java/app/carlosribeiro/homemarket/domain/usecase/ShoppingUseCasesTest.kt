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
import app.carlosribeiro.homemarket.domain.repository.PurchaseRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ShoppingUseCasesTest {

    private val authRepository = mockk<AuthRepository>()
    private val repository = mockk<ListLifecycleRepository>()
    private val adminCheck = AdminCheck(authRepository)
    private val start = StartShoppingUseCase(adminCheck, repository)
    private val abandon = AbandonShoppingUseCase(adminCheck, repository)
    private val purchaseRepository = mockk<PurchaseRepository>()
    private val clock = Clock.fixed(Instant.EPOCH, ZoneId.of("America/Sao_Paulo"))
    private val close = CloseShoppingListUseCase(adminCheck, repository, purchaseRepository, clock)
    private val toggle = TogglePurchasedUseCase(adminCheck, repository)
    private val notFound = MarkNotFoundUseCase(adminCheck, repository)
    private val invalid = AdminResult.Failure(AdminError.INVALID_STATUS)

    private fun signedIn(role: UserRole) {
        every { authRepository.observeCurrentUser() } returns
            flowOf(AppUser("u1", "Maria", "maria@example.com", null, "h1", role))
    }

    private fun list(status: ListStatus) = WeekList("l1", "h1", Instant.EPOCH, Instant.EPOCH, status, null)

    private fun item(status: ItemStatus) = ListItem(
        "i1", "l1", "h1", "Leite", 1, "", false, "u2", "João", status, ApprovalStatus.NOT_REQUIRED, false, null, null
    )

    @Before
    fun writesSucceed() {
        coEvery { repository.updateStatus(any(), any()) } returns AdminResult.Success
        coEvery { repository.weeklyCut(any()) } returns AdminResult.Success
        coEvery { repository.setItemStatus(any(), any()) } returns AdminResult.Success
        coEvery { repository.moveNotFoundToNextWeek(any()) } returns AdminResult.Success
        coEvery { purchaseRepository.createPurchaseForList(any(), any(), any()) } returns AdminResult.Success
    }

    @Test
    fun startShopping_fromOpenOrLockedWithItems() = runTest {
        signedIn(UserRole.ADMIN)
        val items = listOf(item(ItemStatus.PENDING))

        assertEquals(AdminResult.Success, start(list(ListStatus.OPEN), items))
        assertEquals(AdminResult.Success, start(list(ListStatus.LOCKED), items))
        coVerify(exactly = 2) { repository.updateStatus("l1", ListStatus.SHOPPING) }
    }

    @Test
    fun startShopping_notFromShoppingOrClosedNorEmpty() = runTest {
        signedIn(UserRole.ADMIN)
        val items = listOf(item(ItemStatus.PENDING))

        assertEquals(invalid, start(list(ListStatus.SHOPPING), items))
        assertEquals(invalid, start(list(ListStatus.CLOSED), items))
        assertEquals(invalid, start(list(ListStatus.OPEN), emptyList()))
        coVerify(exactly = 0) { repository.updateStatus(any(), any()) }
    }

    @Test
    fun abandon_goesBackToOpenOnlyFromShopping() = runTest {
        signedIn(UserRole.ADMIN)

        assertEquals(AdminResult.Success, abandon(list(ListStatus.SHOPPING)))
        coVerify { repository.updateStatus("l1", ListStatus.OPEN) }
        assertEquals(invalid, abandon(list(ListStatus.OPEN)))
        assertEquals(invalid, abandon(list(ListStatus.LOCKED)))
    }

    @Test
    fun closeList_runsTheWeeklyCutOnlyFromShopping() = runTest {
        signedIn(UserRole.ADMIN)

        assertEquals(AdminResult.Success, close(list(ListStatus.SHOPPING)))
        coVerify(exactly = 1) { repository.weeklyCut("l1") }
        assertEquals(invalid, close(list(ListStatus.OPEN)))
        coVerify(exactly = 1) { repository.weeklyCut(any()) }
    }

    @Test
    fun togglePurchased_switchesBetweenPendingAndPurchased() = runTest {
        signedIn(UserRole.ADMIN)

        toggle(item(ItemStatus.PENDING))
        coVerify { repository.setItemStatus("i1", ItemStatus.PURCHASED) }
        toggle(item(ItemStatus.PURCHASED))
        coVerify { repository.setItemStatus("i1", ItemStatus.PENDING) }
        assertEquals(invalid, toggle(item(ItemStatus.NOT_FOUND)))
        assertEquals(invalid, toggle(item(ItemStatus.ROLLED_OVER)))
    }

    @Test
    fun notAvailable_movesTheItemToNextWeekLikeIos() = runTest {
        signedIn(UserRole.ADMIN)

        assertEquals(AdminResult.Success, notFound(item(ItemStatus.PENDING)))
        coVerify { repository.moveNotFoundToNextWeek("i1") }
        coVerify(exactly = 0) { repository.setItemStatus(any(), ItemStatus.NOT_FOUND) }
        assertEquals(invalid, notFound(item(ItemStatus.PURCHASED)))
    }

    @Test
    fun member_cannotUseAnyShoppingAction() = runTest {
        signedIn(UserRole.MEMBER)
        val notAdmin = AdminResult.Failure(AdminError.NOT_ADMIN)

        assertEquals(notAdmin, start(list(ListStatus.OPEN), listOf(item(ItemStatus.PENDING))))
        assertEquals(notAdmin, abandon(list(ListStatus.SHOPPING)))
        assertEquals(notAdmin, close(list(ListStatus.SHOPPING)))
        assertEquals(notAdmin, toggle(item(ItemStatus.PENDING)))
        assertEquals(notAdmin, notFound(item(ItemStatus.PENDING)))
        coVerify(exactly = 0) { repository.updateStatus(any(), any()) }
        coVerify(exactly = 0) { repository.weeklyCut(any()) }
        coVerify(exactly = 0) { repository.setItemStatus(any(), any()) }
        coVerify(exactly = 0) { repository.moveNotFoundToNextWeek(any()) }
    }

    @Test
    fun closeList_createsThePurchaseWithThePortugueseWeekLabelAfterTheCut() = runTest {
        signedIn(UserRole.ADMIN)
        val monday = Instant.parse("2026-09-28T03:00:00Z")
        val list = WeekList("l1", "h1", monday, Instant.parse("2026-10-05T02:59:59Z"), ListStatus.SHOPPING, null)

        assertEquals(AdminResult.Success, close(list))
        coVerifyOrder {
            repository.weeklyCut("l1")
            purchaseRepository.createPurchaseForList("l1", "h1", "28 – 4 OUT")
        }
    }

    @Test
    fun closeList_failedCutCreatesNoPurchase() = runTest {
        signedIn(UserRole.ADMIN)
        val network = AdminResult.Failure(AdminError.NETWORK)
        coEvery { repository.weeklyCut(any()) } returns network

        assertEquals(network, close(list(ListStatus.SHOPPING)))
        coVerify(exactly = 0) { purchaseRepository.createPurchaseForList(any(), any(), any()) }
    }
}
