package app.carlosribeiro.homemarket.domain.usecase

import app.carlosribeiro.homemarket.domain.model.ApprovalStatus
import app.carlosribeiro.homemarket.domain.model.ItemDetail
import app.carlosribeiro.homemarket.domain.model.ItemResult
import app.carlosribeiro.homemarket.domain.model.ItemStatus
import app.carlosribeiro.homemarket.domain.model.ListItem
import app.carlosribeiro.homemarket.domain.model.ListStatus
import app.carlosribeiro.homemarket.domain.model.WeekList
import app.carlosribeiro.homemarket.domain.repository.ItemRepository
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

    private fun item(status: ItemStatus = ItemStatus.PENDING) = ListItem(
        "i1", "l1", "h1", "Leite", 1, "", false, "u1", "Maria", status, ApprovalStatus.NOT_REQUIRED, false, null, null
    )

    private fun list(status: ListStatus) = WeekList("l1", "h1", Instant.EPOCH, Instant.EPOCH, status, null)

    @Test
    fun detail_combinesTheItemWithItsList() = runTest {
        every { repository.observeItem("i1") } returns flowOf(item())
        every { repository.observeList("l1") } returns flowOf(list(ListStatus.OPEN))

        assertEquals(ItemDetail(item(), list(ListStatus.OPEN)), ObserveItemDetailUseCase(repository)("i1").first())
    }

    @Test
    fun deletedItem_hasNoDetail() = runTest {
        every { repository.observeItem("i1") } returns flowOf(null)

        assertNull(ObserveItemDetailUseCase(repository)("i1").first())
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
}
