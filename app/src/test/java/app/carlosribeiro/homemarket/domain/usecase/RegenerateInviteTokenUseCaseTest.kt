package app.carlosribeiro.homemarket.domain.usecase

import app.carlosribeiro.homemarket.domain.model.AppUser
import app.carlosribeiro.homemarket.domain.model.HouseholdError
import app.carlosribeiro.homemarket.domain.model.HouseholdResult
import app.carlosribeiro.homemarket.domain.model.UserRole
import app.carlosribeiro.homemarket.domain.repository.AuthRepository
import app.carlosribeiro.homemarket.domain.repository.HouseholdRepository
import app.carlosribeiro.homemarket.domain.util.TokenGenerator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RegenerateInviteTokenUseCaseTest {

    private val authRepository = mockk<AuthRepository>()
    private val householdRepository = mockk<HouseholdRepository>()
    private val tokens = TokenGenerator { length -> "N".repeat(length) }
    private val useCase = RegenerateInviteTokenUseCase(authRepository, householdRepository, tokens)

    private fun signedIn(role: UserRole, householdId: String? = "h1") {
        every { authRepository.observeCurrentUser() } returns
            flowOf(AppUser("u1", "Maria", "maria@example.com", null, householdId, role))
    }

    @Test
    fun admin_getsANew8CharacterToken() = runTest {
        signedIn(UserRole.ADMIN)
        coEvery { householdRepository.updateInviteToken(any(), any()) } returns HouseholdResult.Success("h1")

        val result = useCase()

        assertEquals(HouseholdResult.Success("h1"), result)
        coVerify { householdRepository.updateInviteToken("h1", "NNNNNNNN") }
    }

    @Test
    fun member_isRejectedWithATypedError() = runTest {
        signedIn(UserRole.MEMBER)

        val result = useCase()

        assertEquals(HouseholdResult.Failure(HouseholdError.NOT_ADMIN), result)
        coVerify(exactly = 0) { householdRepository.updateInviteToken(any(), any()) }
    }

    @Test
    fun adminWithoutHousehold_isRejected() = runTest {
        signedIn(UserRole.ADMIN, householdId = null)

        assertEquals(HouseholdResult.Failure(HouseholdError.NOT_ADMIN), useCase())
    }

    @Test
    fun signedOut_isRejected() = runTest {
        every { authRepository.observeCurrentUser() } returns flowOf(null)

        assertEquals(HouseholdResult.Failure(HouseholdError.NOT_SIGNED_IN), useCase())
    }
}
