package app.carlosribeiro.homemarket.domain.usecase

import app.carlosribeiro.homemarket.domain.model.ApprovalStatus
import app.carlosribeiro.homemarket.domain.model.ItemStatus
import app.carlosribeiro.homemarket.domain.model.ListItem
import app.carlosribeiro.homemarket.domain.model.ListStatus
import app.carlosribeiro.homemarket.domain.model.WeekList
import app.carlosribeiro.homemarket.domain.model.WeeklyList
import app.carlosribeiro.homemarket.domain.repository.ListRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveWeeklyListUseCaseTest {

    private val repository = mockk<ListRepository>()
    private val useCase = ObserveWeeklyListUseCase(repository)

    private val list = WeekList("l1", "h1", Instant.EPOCH, Instant.EPOCH, ListStatus.OPEN, null)

    private fun item(id: String, status: ItemStatus) = ListItem(
        id, "l1", "h1", "Leite", 1, "", false, "u1", "Maria", status, ApprovalStatus.NOT_REQUIRED, false, null, null
    )

    @Test
    fun currentListWithItemsAndNextWeekItems() = runTest {
        val items = listOf(item("1", ItemStatus.PENDING))
        val nextWeek = listOf(item("2", ItemStatus.ROLLED_OVER))
        every { repository.observeCurrentList("h1") } returns flowOf(list)
        every { repository.observeItems("l1") } returns flowOf(items)
        every { repository.observeNextWeekItems("h1") } returns flowOf(nextWeek)

        assertEquals(WeeklyList(list, items, nextWeek), useCase("h1").first())
    }

    @Test
    fun noCurrentList_hasNoItemsButKeepsNextWeekItems() = runTest {
        val nextWeek = listOf(item("2", ItemStatus.ROLLED_OVER))
        every { repository.observeCurrentList("h1") } returns flowOf(null)
        every { repository.observeNextWeekItems("h1") } returns flowOf(nextWeek)

        assertEquals(WeeklyList(null, emptyList(), nextWeek), useCase("h1").first())
        verify(exactly = 0) { repository.observeItems(any()) }
    }
}
