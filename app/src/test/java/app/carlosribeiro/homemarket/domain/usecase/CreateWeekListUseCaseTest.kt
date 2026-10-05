package app.carlosribeiro.homemarket.domain.usecase

import app.carlosribeiro.homemarket.domain.model.AppUser
import app.carlosribeiro.homemarket.domain.model.ListError
import app.carlosribeiro.homemarket.domain.model.ListResult
import app.carlosribeiro.homemarket.domain.model.ListStatus
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

class CreateWeekListUseCaseTest {

    private val authRepository = mockk<AuthRepository>()
    private val listRepository = mockk<ListRepository>()
    private val clock = Clock.fixed(Instant.parse("2026-10-01T15:00:00Z"), ZoneId.of("America/Sao_Paulo"))
    private val useCase = CreateWeekListUseCase(authRepository, listRepository, clock)

    private fun signedIn(role: UserRole, householdId: String? = "h1") {
        every { authRepository.observeCurrentUser() } returns
            flowOf(AppUser("u1", "Maria", "maria@example.com", null, householdId, role))
    }

    private fun list(status: ListStatus) = WeekList(
        id = "h1_2026-09-28",
        householdId = "h1",
        weekStart = Instant.EPOCH,
        weekEnd = Instant.EPOCH,
        status = status,
        createdAt = null
    )

    @Test
    fun admin_createsThisWeeksList() = runTest {
        signedIn(UserRole.ADMIN)
        val week = slot<Week>()
        coEvery { listRepository.createWeekList("h1", capture(week)) } returns ListResult.Success(list(ListStatus.OPEN))

        val result = useCase()

        assertEquals(ListResult.Success(list(ListStatus.OPEN)), result)
        assertEquals(LocalDate.of(2026, 9, 28), week.captured.monday)
        assertEquals("28 – 4 OUT", week.captured.label)
    }

    @Test
    fun existingActiveList_isReturned() = runTest {
        signedIn(UserRole.ADMIN)
        coEvery { listRepository.createWeekList(any(), any()) } returns ListResult.Success(list(ListStatus.SHOPPING))

        assertEquals(ListResult.Success(list(ListStatus.SHOPPING)), useCase())
    }

    @Test
    fun listClosedEarlyThisWeek_isRejected() = runTest {
        signedIn(UserRole.ADMIN)
        coEvery { listRepository.createWeekList(any(), any()) } returns ListResult.Success(list(ListStatus.CLOSED))

        assertEquals(ListResult.Failure(ListError.CLOSED_THIS_WEEK), useCase())
    }

    @Test
    fun repositoryFailure_isReturned() = runTest {
        signedIn(UserRole.ADMIN)
        coEvery { listRepository.createWeekList(any(), any()) } returns ListResult.Failure(ListError.NETWORK)

        assertEquals(ListResult.Failure(ListError.NETWORK), useCase())
    }

    @Test
    fun member_isRejected() = runTest {
        signedIn(UserRole.MEMBER)

        assertEquals(ListResult.Failure(ListError.NOT_ADMIN), useCase())
        coVerify(exactly = 0) { listRepository.createWeekList(any(), any()) }
    }

    @Test
    fun userWithoutHousehold_isRejected() = runTest {
        signedIn(UserRole.ADMIN, householdId = null)

        assertEquals(ListResult.Failure(ListError.NOT_ADMIN), useCase())
    }

    @Test
    fun signedOut_isRejected() = runTest {
        every { authRepository.observeCurrentUser() } returns flowOf(null)

        assertEquals(ListResult.Failure(ListError.NOT_SIGNED_IN), useCase())
    }
}
