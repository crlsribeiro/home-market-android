package app.carlosribeiro.homemarket.domain.usecase

import app.carlosribeiro.homemarket.domain.model.AuthError
import app.carlosribeiro.homemarket.domain.model.AuthResult
import app.carlosribeiro.homemarket.domain.model.Registration
import app.carlosribeiro.homemarket.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthUseCasesTest {

    private val repository = mockk<AuthRepository>(relaxed = true)

    @Test
    fun signIn_trimsEmailAndReturnsRepositoryResult() = runTest {
        val failure = AuthResult.Failure(AuthError.INVALID_CREDENTIALS)
        coEvery { repository.signIn(any(), any()) } returns failure

        val result = SignInUseCase(repository)("  maria@example.com ", "secret")

        assertEquals(failure, result)
        coVerify { repository.signIn("maria@example.com", "secret") }
    }

    @Test
    fun register_trimsNamesAndEmailButNotPassword() = runTest {
        coEvery { repository.register(any()) } returns AuthResult.Success

        val result = RegisterUseCase(repository)(
            Registration(
                firstName = " Maria ",
                lastName = " Silva",
                email = "maria@example.com ",
                password = " pw 12345"
            )
        )

        assertEquals(AuthResult.Success, result)
        coVerify {
            repository.register(Registration("Maria", "Silva", "maria@example.com", " pw 12345"))
        }
    }

    @Test
    fun signOut_delegatesToRepository() = runTest {
        SignOutUseCase(repository)()

        coVerify { repository.signOut() }
    }

    @Test
    fun signInWithGoogle_passesTheIdTokenToTheRepository() = runTest {
        coEvery { repository.signInWithGoogle("token") } returns AuthResult.Success

        assertEquals(AuthResult.Success, SignInWithGoogleUseCase(repository)("token"))
    }
}
