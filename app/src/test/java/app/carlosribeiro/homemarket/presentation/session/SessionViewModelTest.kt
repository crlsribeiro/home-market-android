package app.carlosribeiro.homemarket.presentation.session

import app.carlosribeiro.homemarket.domain.model.AppUser
import app.carlosribeiro.homemarket.domain.model.UserRole
import app.carlosribeiro.homemarket.domain.usecase.ObserveCurrentUserUseCase
import app.carlosribeiro.homemarket.domain.usecase.SignOutUseCase
import app.carlosribeiro.homemarket.testing.MainDispatcherRule
import app.cash.turbine.test
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SessionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val user = AppUser("u1", "Maria", "maria@example.com", null, null, UserRole.MEMBER)
    private val currentUser = MutableStateFlow<AppUser?>(null)
    private val observeCurrentUser = mockk<ObserveCurrentUserUseCase> { every { this@mockk() } returns currentUser }
    private val signOut = mockk<SignOutUseCase>(relaxed = true)

    @Test
    fun followsTheSignedInUser() = runTest {
        val viewModel = SessionViewModel(observeCurrentUser, signOut)

        viewModel.state.test {
            assertEquals(SessionState.SignedOut, awaitItem())
            currentUser.value = user
            assertEquals(SessionState.SignedIn(user), awaitItem())
            currentUser.value = null
            assertEquals(SessionState.SignedOut, awaitItem())
        }
    }

    @Test
    fun onSignOut_callsTheUseCase() = runTest {
        SessionViewModel(observeCurrentUser, signOut).onSignOut()

        coVerify { signOut() }
    }
}
