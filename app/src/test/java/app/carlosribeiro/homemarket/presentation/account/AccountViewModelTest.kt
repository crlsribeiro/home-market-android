package app.carlosribeiro.homemarket.presentation.account

import app.carlosribeiro.homemarket.domain.model.AppUser
import app.carlosribeiro.homemarket.domain.model.PhoneCountry
import app.carlosribeiro.homemarket.domain.model.ProfileError
import app.carlosribeiro.homemarket.domain.model.ProfileResult
import app.carlosribeiro.homemarket.domain.model.UserRole
import app.carlosribeiro.homemarket.domain.usecase.DeleteAccountUseCase
import app.carlosribeiro.homemarket.domain.usecase.ObserveCurrentUserUseCase
import app.carlosribeiro.homemarket.domain.usecase.ObserveHouseholdUseCase
import app.carlosribeiro.homemarket.domain.usecase.ProfileInput
import app.carlosribeiro.homemarket.domain.usecase.SaveProfileUseCase
import app.carlosribeiro.homemarket.domain.usecase.UpdateAvatarUseCase
import app.carlosribeiro.homemarket.domain.util.PhotoCompressor
import app.carlosribeiro.homemarket.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class AccountViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val user = AppUser(
        "u1",
        "Maria Silva",
        "maria@example.com",
        null,
        null,
        UserRole.MEMBER,
        phone = "(415) 555-0100",
        phoneCountryCode = "+1"
    )
    private val observeUser = mockk<ObserveCurrentUserUseCase> { every { this@mockk() } returns flowOf(user) }
    private val observeHousehold = mockk<ObserveHouseholdUseCase>()
    private val saveProfile = mockk<SaveProfileUseCase>()
    private val updateAvatar = mockk<UpdateAvatarUseCase>()
    private val deleteAccount = mockk<DeleteAccountUseCase>()
    private val photo = byteArrayOf(1)
    private val compressor = PhotoCompressor { uri -> if (uri == "content://ok") photo else null }

    private fun viewModel() =
        AccountViewModel(observeUser, observeHousehold, saveProfile, updateAvatar, compressor, deleteAccount)

    @Test
    fun form_startsFromTheStoredProfile() {
        val state = viewModel().state.value

        assertEquals("maria@example.com", state.email)
        assertEquals("(415) 555-0100", state.phone)
        assertEquals(PhoneCountry.UNITED_STATES, state.country)
    }

    @Test
    fun phone_isMaskedAndChangingCountryClearsIt() {
        val viewModel = viewModel()

        viewModel.onEvent(AccountUiEvent.CountryChanged(PhoneCountry.BRAZIL))
        assertEquals("", viewModel.state.value.phone)
        viewModel.onEvent(AccountUiEvent.PhoneChanged("11987654321"))
        assertEquals("(11) 98765-4321", viewModel.state.value.phone)
    }

    @Test
    fun save_showsTheCheckEmailMessage() = runTest {
        coEvery { saveProfile(any()) } returns ProfileResult.Success(emailVerificationSentTo = "nova@example.com")
        val viewModel = viewModel()
        viewModel.onEvent(AccountUiEvent.EmailChanged("nova@example.com"))

        viewModel.onEvent(AccountUiEvent.Save)

        assertEquals(AccountMessage.CheckNewEmail("nova@example.com"), viewModel.state.value.message)
        assertFalse(viewModel.state.value.isSaving)
        coVerify { saveProfile(ProfileInput("nova@example.com", "(415) 555-0100", PhoneCountry.UNITED_STATES)) }
    }

    @Test
    fun saveFailure_isShown() = runTest {
        coEvery { saveProfile(any()) } returns ProfileResult.Failure(ProfileError.PHONE_INCOMPLETE)
        val viewModel = viewModel()

        viewModel.onEvent(AccountUiEvent.Save)

        assertEquals(ProfileError.PHONE_INCOMPLETE, viewModel.state.value.error)
    }

    @Test
    fun photo_isCompressedAndUploaded() = runTest {
        coEvery { updateAvatar(photo) } returns ProfileResult.Success()
        val viewModel = viewModel()

        viewModel.onEvent(AccountUiEvent.PhotoPicked("content://ok"))

        assertEquals(AccountMessage.PhotoUpdated, viewModel.state.value.message)
        assertFalse(viewModel.state.value.isUploadingPhoto)
    }

    @Test
    fun deleteDialog_opensAndCancels() {
        val viewModel = viewModel()

        viewModel.onEvent(AccountUiEvent.AskDelete)
        assertEquals(DeleteAccountState(), viewModel.state.value.delete)
        viewModel.onEvent(AccountUiEvent.CancelDelete)
        assertNull(viewModel.state.value.delete)
    }

    @Test
    fun deleteFailure_staysInTheDialog() = runTest {
        coEvery { deleteAccount() } returns ProfileResult.Failure(ProfileError.REQUIRES_RECENT_LOGIN)
        val viewModel = viewModel()
        viewModel.onEvent(AccountUiEvent.AskDelete)

        viewModel.onEvent(AccountUiEvent.ConfirmDelete)

        assertEquals(DeleteAccountState(error = ProfileError.REQUIRES_RECENT_LOGIN), viewModel.state.value.delete)
        coVerify { deleteAccount() }
    }
}
