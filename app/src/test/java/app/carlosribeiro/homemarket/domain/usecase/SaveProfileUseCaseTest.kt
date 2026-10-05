package app.carlosribeiro.homemarket.domain.usecase

import app.carlosribeiro.homemarket.domain.model.AppUser
import app.carlosribeiro.homemarket.domain.model.PhoneCountry
import app.carlosribeiro.homemarket.domain.model.ProfileError
import app.carlosribeiro.homemarket.domain.model.ProfileResult
import app.carlosribeiro.homemarket.domain.model.UserRole
import app.carlosribeiro.homemarket.domain.repository.AuthRepository
import app.carlosribeiro.homemarket.domain.repository.ProfileRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SaveProfileUseCaseTest {

    private val authRepository = mockk<AuthRepository>()
    private val profileRepository = mockk<ProfileRepository>()
    private val useCase = SaveProfileUseCase(authRepository, profileRepository)
    private val user = AppUser("u1", "Maria", "maria@example.com", null, "h1", UserRole.MEMBER)

    @Before
    fun signedIn() {
        every { authRepository.observeCurrentUser() } returns flowOf(user)
        coEvery { profileRepository.updatePhone(any(), any()) } returns ProfileResult.Success()
    }

    @Test
    fun sameEmail_savesOnlyThePhone() = runTest {
        val result = useCase(ProfileInput("maria@example.com", "(11) 98765-4321", PhoneCountry.BRAZIL))

        assertEquals(ProfileResult.Success(), result)
        coVerify { profileRepository.updatePhone("(11) 98765-4321", "+55") }
        coVerify(exactly = 0) { profileRepository.requestEmailChange(any()) }
    }

    @Test
    fun newEmail_savesThePhoneThenSendsTheVerificationLink() = runTest {
        coEvery { profileRepository.requestEmailChange("nova@example.com") } returns
            ProfileResult.Success(emailVerificationSentTo = "nova@example.com")

        val result = useCase(ProfileInput(" nova@example.com ", "", PhoneCountry.UNITED_STATES))

        assertEquals(ProfileResult.Success(emailVerificationSentTo = "nova@example.com"), result)
        coVerify { profileRepository.updatePhone("", "+1") }
    }

    @Test
    fun incompletePhone_isRejected() = runTest {
        val result = useCase(ProfileInput("maria@example.com", "(11) 9876", PhoneCountry.BRAZIL))

        assertEquals(ProfileResult.Failure(ProfileError.PHONE_INCOMPLETE), result)
        coVerify(exactly = 0) { profileRepository.updatePhone(any(), any()) }
    }

    @Test
    fun invalidNewEmail_isRejected() = runTest {
        assertEquals(
            ProfileResult.Failure(ProfileError.EMAIL_INVALID),
            useCase(ProfileInput("maria", "", PhoneCountry.BRAZIL))
        )
    }

    @Test
    fun recentLoginRequired_isReturned() = runTest {
        coEvery { profileRepository.requestEmailChange(any()) } returns
            ProfileResult.Failure(ProfileError.REQUIRES_RECENT_LOGIN)

        assertEquals(
            ProfileResult.Failure(ProfileError.REQUIRES_RECENT_LOGIN),
            useCase(ProfileInput("nova@example.com", "", PhoneCountry.BRAZIL))
        )
    }

    @Test
    fun signedOut_isRejected() = runTest {
        every { authRepository.observeCurrentUser() } returns flowOf(null)

        assertEquals(
            ProfileResult.Failure(ProfileError.NOT_SIGNED_IN),
            useCase(ProfileInput("maria@example.com", "", PhoneCountry.BRAZIL))
        )
    }
}
