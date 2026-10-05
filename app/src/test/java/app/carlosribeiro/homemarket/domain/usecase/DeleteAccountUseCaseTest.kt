package app.carlosribeiro.homemarket.domain.usecase

import app.carlosribeiro.homemarket.domain.model.AppUser
import app.carlosribeiro.homemarket.domain.model.Household
import app.carlosribeiro.homemarket.domain.model.HouseholdError
import app.carlosribeiro.homemarket.domain.model.HouseholdResult
import app.carlosribeiro.homemarket.domain.model.ProfileError
import app.carlosribeiro.homemarket.domain.model.ProfileResult
import app.carlosribeiro.homemarket.domain.model.UserRole
import app.carlosribeiro.homemarket.domain.repository.AuthRepository
import app.carlosribeiro.homemarket.domain.repository.HouseholdRepository
import app.carlosribeiro.homemarket.domain.repository.ProfileRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class DeleteAccountUseCaseTest {

    private val authRepository = mockk<AuthRepository>()
    private val householdRepository = mockk<HouseholdRepository>()
    private val profileRepository = mockk<ProfileRepository>()
    private val useCase = DeleteAccountUseCase(authRepository, householdRepository, profileRepository)

    private fun signedIn(role: UserRole, householdId: String? = "h1") {
        every { authRepository.observeCurrentUser() } returns
            flowOf(AppUser("u1", "Maria", "maria@example.com", null, householdId, role))
    }

    private fun household(vararg memberUids: String) {
        every { householdRepository.observeHousehold("h1") } returns
            flowOf(Household("h1", "Casa Silva", "u1", "ABC12345", memberUids.toList()))
    }

    @Before
    fun defaults() {
        every { profileRepository.hasRecentSignIn() } returns true
        coEvery { profileRepository.deleteAccount() } returns ProfileResult.Success()
        coEvery { householdRepository.leaveHousehold(any(), any(), any()) } returns HouseholdResult.Success("h1")
    }

    @Test
    fun adminWithOtherMembers_handsOverTheAdminRoleFirst() = runTest {
        signedIn(UserRole.ADMIN)
        household("u1", "u2", "u3")

        assertEquals(ProfileResult.Success(), useCase())
        coVerifyOrder {
            householdRepository.leaveHousehold("h1", "u1", "u2")
            profileRepository.deleteAccount()
        }
    }

    @Test
    fun soloAdmin_leavesWithoutHandOver() = runTest {
        signedIn(UserRole.ADMIN)
        household("u1")

        assertEquals(ProfileResult.Success(), useCase())
        coVerify { householdRepository.leaveHousehold("h1", "u1", null) }
    }

    @Test
    fun member_leavesWithoutHandOver() = runTest {
        signedIn(UserRole.MEMBER)
        household("u1", "u2")

        assertEquals(ProfileResult.Success(), useCase())
        coVerify { householdRepository.leaveHousehold("h1", "u1", null) }
    }

    @Test
    fun userWithoutHousehold_onlyDeletesTheAccount() = runTest {
        signedIn(UserRole.MEMBER, householdId = null)

        assertEquals(ProfileResult.Success(), useCase())
        coVerify(exactly = 0) { householdRepository.leaveHousehold(any(), any(), any()) }
        coVerify { profileRepository.deleteAccount() }
    }

    @Test
    fun oldSignIn_asksToSignInAgainBeforeWritingAnything() = runTest {
        signedIn(UserRole.ADMIN)
        every { profileRepository.hasRecentSignIn() } returns false

        assertEquals(ProfileResult.Failure(ProfileError.REQUIRES_RECENT_LOGIN), useCase())
        coVerify(exactly = 0) { householdRepository.leaveHousehold(any(), any(), any()) }
        coVerify(exactly = 0) { profileRepository.deleteAccount() }
    }

    @Test
    fun failedLeave_keepsTheAccount() = runTest {
        signedIn(UserRole.MEMBER)
        household("u1", "u2")
        coEvery { householdRepository.leaveHousehold(any(), any(), any()) } returns
            HouseholdResult.Failure(HouseholdError.NETWORK)

        assertEquals(ProfileResult.Failure(ProfileError.NETWORK), useCase())
        coVerify(exactly = 0) { profileRepository.deleteAccount() }
    }

    @Test
    fun signedOut_isRejected() = runTest {
        every { authRepository.observeCurrentUser() } returns flowOf(null)

        assertEquals(ProfileResult.Failure(ProfileError.NOT_SIGNED_IN), useCase())
    }
}
