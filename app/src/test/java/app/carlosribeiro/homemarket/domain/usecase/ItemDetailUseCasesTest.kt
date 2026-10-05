package app.carlosribeiro.homemarket.domain.usecase

import app.carlosribeiro.homemarket.domain.model.AppUser
import app.carlosribeiro.homemarket.domain.model.ApprovalStatus
import app.carlosribeiro.homemarket.domain.model.ItemDetail
import app.carlosribeiro.homemarket.domain.model.ItemPrice
import app.carlosribeiro.homemarket.domain.model.ItemResult
import app.carlosribeiro.homemarket.domain.model.ItemStatus
import app.carlosribeiro.homemarket.domain.model.ListItem
import app.carlosribeiro.homemarket.domain.model.ListStatus
import app.carlosribeiro.homemarket.domain.model.Purchase
import app.carlosribeiro.homemarket.domain.model.PurchaseItem
import app.carlosribeiro.homemarket.domain.model.UserRole
import app.carlosribeiro.homemarket.domain.model.WeekList
import app.carlosribeiro.homemarket.domain.repository.AuthRepository
import app.carlosribeiro.homemarket.domain.repository.ItemRepository
import app.carlosribeiro.homemarket.domain.repository.PurchaseRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemDetailUseCasesTest {

    private val repository = mockk<ItemRepository>()
    private val authRepository = mockk<AuthRepository>()
    private val purchaseRepository = mockk<PurchaseRepository>()
    private val observePrice = ObserveItemPriceUseCase(authRepository, purchaseRepository)

    private fun signedIn(role: UserRole) {
        every { authRepository.observeCurrentUser() } returns
            flowOf(AppUser("u1", "Maria", "maria@example.com", null, "h1", role))
    }

    private fun item(status: ItemStatus = ItemStatus.PENDING) = ListItem(
        "i1", "l1", "h1", "Leite", 1, "", false, "u1", "Maria", status, ApprovalStatus.NOT_REQUIRED, false, null, null
    )

    private fun list(status: ListStatus) = WeekList("l1", "h1", Instant.EPOCH, Instant.EPOCH, status, null)

    @Test
    fun detail_combinesTheItemWithItsList() = runTest {
        every { repository.observeItem("i1") } returns flowOf(item())
        every { repository.observeList("l1") } returns flowOf(list(ListStatus.OPEN))
        signedIn(UserRole.MEMBER)

        assertEquals(
            ItemDetail(item(), list(ListStatus.OPEN), price = null),
            ObserveItemDetailUseCase(repository, observePrice)("i1").first()
        )
    }

    @Test
    fun deletedItem_hasNoDetail() = runTest {
        every { repository.observeItem("i1") } returns flowOf(null)

        assertNull(ObserveItemDetailUseCase(repository, observePrice)("i1").first())
        verify(exactly = 0) { repository.observeList(any()) }
    }

    @Test
    fun removeIsHiddenOnlyForItemsOfAClosedList() {
        assertTrue(ItemDetail(item(), list(ListStatus.OPEN)).canRemove)
        assertTrue(ItemDetail(item(), list(ListStatus.SHOPPING)).canRemove)
        assertTrue(ItemDetail(item(), null).canRemove)
        assertFalse(ItemDetail(item(), list(ListStatus.CLOSED)).canRemove)
        // Next-week items keep the closed list's id and must stay removable.
        assertTrue(ItemDetail(item(ItemStatus.ROLLED_OVER), list(ListStatus.CLOSED)).canRemove)
    }

    @Test
    fun writes_goToTheRepository() = runTest {
        val photo = byteArrayOf(1)
        coEvery { repository.updateNotes("i1", "Integral") } returns ItemResult.Success
        coEvery { repository.removeItem("i1") } returns ItemResult.Success
        coEvery { repository.replacePhoto(item(), photo) } returns ItemResult.Success

        assertEquals(ItemResult.Success, UpdateItemNotesUseCase(repository)("i1", "Integral"))
        assertEquals(ItemResult.Success, RemoveItemUseCase(repository)("i1"))
        assertEquals(ItemResult.Success, ReplaceItemPhotoUseCase(repository)(item(), photo))
        coVerify { repository.replacePhoto(item(), photo) }
    }

    @Test
    fun price_isTheMatchingReceiptLineForTheAdmin() = runTest {
        signedIn(UserRole.ADMIN)
        val purchase = Purchase("p1", "l1", "h1", "", null, null, 9.0, null, true, null, null)
        every { purchaseRepository.observePurchaseOfList("l1") } returns flowOf(purchase)
        every { purchaseRepository.observeItems("p1") } returns flowOf(
            listOf(PurchaseItem("a", "p1", "PAO", 1, 5.0, 5.0), PurchaseItem("b", "p1", "LEITE", 1, 4.0, 4.0))
        )

        val price = observePrice(item().copy(quantity = 3)).first()

        assertEquals(ItemPrice(unitPrice = 4.0, quantity = 3), price)
        assertEquals(12.0, price!!.total!!, 0.0)
    }

    @Test
    fun price_isUnavailableUntilAReceiptHasTheItem() = runTest {
        signedIn(UserRole.ADMIN)
        every { purchaseRepository.observePurchaseOfList("l1") } returns flowOf(null)

        val price = observePrice(item()).first()

        assertEquals(ItemPrice(unitPrice = null, quantity = 1), price)
        assertNull(price!!.total)
    }

    @Test
    fun price_isNeverReadForMembers() = runTest {
        signedIn(UserRole.MEMBER)

        assertNull(observePrice(item()).first())
        verify(exactly = 0) { purchaseRepository.observePurchaseOfList(any()) }
    }
}
