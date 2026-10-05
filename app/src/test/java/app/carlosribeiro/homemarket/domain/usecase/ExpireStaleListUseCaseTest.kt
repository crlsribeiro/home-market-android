package app.carlosribeiro.homemarket.domain.usecase

import app.carlosribeiro.homemarket.domain.model.AdminError
import app.carlosribeiro.homemarket.domain.model.AdminResult
import app.carlosribeiro.homemarket.domain.model.ListResult
import app.carlosribeiro.homemarket.domain.model.ListStatus
import app.carlosribeiro.homemarket.domain.model.Week
import app.carlosribeiro.homemarket.domain.model.WeekList
import app.carlosribeiro.homemarket.domain.repository.ListLifecycleRepository
import app.carlosribeiro.homemarket.domain.repository.ListRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import io.mockk.slot
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpireStaleListUseCaseTest {

    private val lifecycle = mockk<ListLifecycleRepository>()
    private val lists = mockk<ListRepository>()

    // Tuesday 6 October 2026 in São Paulo.
    private val clock = Clock.fixed(Instant.parse("2026-10-06T15:00:00Z"), ZoneId.of("America/Sao_Paulo"))
    private val useCase = ExpireStaleListUseCase(lifecycle, lists, clock)

    private fun list(weekEnd: Instant) = WeekList(
        "h1_2026-09-28",
        "h1",
        Instant.parse("2026-09-28T03:00:00Z"),
        weekEnd,
        ListStatus.OPEN,
        null
    )

    private val lastWeek = list(Instant.parse("2026-10-05T02:59:59.999Z"))
    private val thisWeek = list(Instant.parse("2026-10-12T02:59:59.999Z"))

    @Test
    fun staleList_isCutThenThisWeeksListIsCreated() = runTest {
        val week = slot<Week>()
        coEvery { lifecycle.weeklyCut("h1_2026-09-28") } returns AdminResult.Success
        coEvery { lists.createWeekList("h1", capture(week)) } returns ListResult.Success(thisWeek)

        assertEquals(AdminResult.Success, useCase(lastWeek))
        coVerifyOrder {
            lifecycle.weeklyCut("h1_2026-09-28")
            lists.createWeekList("h1", any())
        }
        assertEquals(LocalDate.of(2026, 10, 5), week.captured.monday)
    }

    @Test
    fun currentWeek_isLeftAlone() = runTest {
        assertFalse(useCase.isStale(thisWeek))
        assertNull(useCase(thisWeek))
        coVerify(exactly = 0) { lifecycle.weeklyCut(any()) }
    }

    @Test
    fun isStale_onlyAfterTheWeekEnded() {
        assertTrue(useCase.isStale(lastWeek))
        assertFalse(useCase.isStale(list(clock.instant())))
    }

    @Test
    fun failedCut_createsNoNewList() = runTest {
        coEvery { lifecycle.weeklyCut(any()) } returns AdminResult.Failure(AdminError.NETWORK)

        assertEquals(AdminResult.Failure(AdminError.NETWORK), useCase(lastWeek))
        coVerify(exactly = 0) { lists.createWeekList(any(), any()) }
    }
}
