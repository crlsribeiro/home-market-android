package app.carlosribeiro.homemarket.domain.usecase

import app.carlosribeiro.homemarket.domain.model.AppUser
import app.carlosribeiro.homemarket.domain.model.ApprovalStatus
import app.carlosribeiro.homemarket.domain.model.ItemError
import app.carlosribeiro.homemarket.domain.model.ItemResult
import app.carlosribeiro.homemarket.domain.model.ItemStatus
import app.carlosribeiro.homemarket.domain.model.ListItem
import app.carlosribeiro.homemarket.domain.model.UserRole
import app.carlosribeiro.homemarket.domain.repository.AuthRepository
import app.carlosribeiro.homemarket.domain.repository.ItemRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResolveNotFoundUseCaseTest {

    private val authRepository = mockk<AuthRepository>()
    private val itemRepository = mockk<ItemRepository>()
    private val useCase = ResolveNotFoundUseCase(authRepository, itemRepository)

    private val notFound = ListItem(
        "i1", "l1", "h1", "Leite", 1, "", false, "u2", "João",
        ItemStatus.NOT_FOUND, ApprovalStatus.NOT_REQUIRED, false, null, null
    )

    private fun signedIn(uid: String?) {
        every { authRepository.observeCurrentUser() } returns
            flowOf(uid?.let { AppUser(it, "João", "joao@example.com", null, "h1", UserRole.MEMBER) })
    }

    @Test
    fun author_resolvesTheirNotFoundItem() = runTest {
        signedIn("u2")
        coEvery { itemRepository.resolveNotFound("i1") } returns ItemResult.Success

        assertEquals(ItemResult.Success, useCase(notFound))
        coVerify { itemRepository.resolveNotFound("i1") }
    }

    @Test
    fun otherMembers_cannotResolveIt() = runTest {
        signedIn("u3")

        assertEquals(ItemResult.Failure(ItemError.UNKNOWN), useCase(notFound))
        coVerify(exactly = 0) { itemRepository.resolveNotFound(any()) }
    }

    @Test
    fun signedOut_isRejected() = runTest {
        signedIn(null)

        assertEquals(ItemResult.Failure(ItemError.NOT_SIGNED_IN), useCase(notFound))
    }

    @Test
    fun onlyUnresolvedNotFoundItemsAwaitADecision() {
        assertTrue(notFound.awaitsNotFoundDecisionBy("u2"))
        assertFalse(notFound.awaitsNotFoundDecisionBy("u3"))
        assertFalse(notFound.copy(notFoundResolved = true).awaitsNotFoundDecisionBy("u2"))
        assertFalse(notFound.copy(status = ItemStatus.PENDING).awaitsNotFoundDecisionBy("u2"))
    }
}
