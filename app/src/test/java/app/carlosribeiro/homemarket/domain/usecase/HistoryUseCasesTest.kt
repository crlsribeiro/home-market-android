package app.carlosribeiro.homemarket.domain.usecase

import app.carlosribeiro.homemarket.domain.model.AdminError
import app.carlosribeiro.homemarket.domain.model.AdminResult
import app.carlosribeiro.homemarket.domain.model.AppUser
import app.carlosribeiro.homemarket.domain.model.Purchase
import app.carlosribeiro.homemarket.domain.model.PurchaseDetail
import app.carlosribeiro.homemarket.domain.model.PurchaseItem
import app.carlosribeiro.homemarket.domain.model.UserRole
import app.carlosribeiro.homemarket.domain.repository.AuthRepository
import app.carlosribeiro.homemarket.domain.repository.PurchaseRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HistoryUseCasesTest {

    private val authRepository = mockk<AuthRepository>()
    private val repository = mockk<PurchaseRepository>()
    private val purchase = Purchase("p1", "l1", "h1", "28 – 4 OUT", null, null, 7.48, null, false, null, null)
    private val line = PurchaseItem("a", "p1", "MILK", 1, 3.49, 3.49)

    private fun signedIn(role: UserRole, householdId: String? = "h1") {
        every { authRepository.observeCurrentUser() } returns
            flowOf(AppUser("u1", "Maria", "maria@example.com", null, householdId, role))
    }

    @Test
    fun history_adminReadsTheHouseholdPurchases() = runTest {
        signedIn(UserRole.ADMIN)
        every { repository.observePurchases("h1") } returns flowOf(listOf(purchase))

        assertEquals(listOf(purchase), ObservePurchaseHistoryUseCase(authRepository, repository)().first())
    }

    @Test
    fun history_membersNeverReadPurchases() = runTest {
        signedIn(UserRole.MEMBER)

        assertEquals(emptyList<Purchase>(), ObservePurchaseHistoryUseCase(authRepository, repository)().first())
        verify(exactly = 0) { repository.observePurchases(any()) }
    }

    @Test
    fun detail_adminSeesTheLinesOfAPurchaseOfTheirHousehold() = runTest {
        signedIn(UserRole.ADMIN)
        every { repository.observePurchase("p1") } returns flowOf(purchase)
        every { repository.observeItems("p1") } returns flowOf(listOf(line))

        assertEquals(
            PurchaseDetail(purchase, listOf(line)),
            ObservePurchaseDetailUseCase(authRepository, repository)("p1").first()
        )
    }

    @Test
    fun detail_isHiddenFromMembersAndForOtherHouseholds() = runTest {
        every { repository.observePurchase("p1") } returns flowOf(purchase)
        every { repository.observeItems("p1") } returns flowOf(listOf(line))

        signedIn(UserRole.MEMBER)
        assertNull(ObservePurchaseDetailUseCase(authRepository, repository)("p1").first())
        signedIn(UserRole.ADMIN, householdId = "h2")
        assertNull(ObservePurchaseDetailUseCase(authRepository, repository)("p1").first())
    }

    @Test
    fun edit_trimsTheNameAndKeepsTheStoredOneWhenBlank() = runTest {
        signedIn(UserRole.ADMIN)
        coEvery { repository.updateItem(any(), any(), any(), any()) } returns AdminResult.Success
        val edit = EditPurchaseItemUseCase(AdminCheck(authRepository), repository)

        assertEquals(AdminResult.Success, edit("p1", "a", " Milk ", 3.5))
        assertEquals(AdminResult.Success, edit("p1", "a", "  ", 0.0))
        coVerify { repository.updateItem("p1", "a", "Milk", 3.5) }
        coVerify { repository.updateItem("p1", "a", null, 0.0) }
    }

    @Test
    fun edit_rejectsMembersAndNegativePrices() = runTest {
        val edit = EditPurchaseItemUseCase(AdminCheck(authRepository), repository)

        signedIn(UserRole.MEMBER)
        assertEquals(AdminResult.Failure(AdminError.NOT_ADMIN), edit("p1", "a", "Milk", 1.0))
        signedIn(UserRole.ADMIN)
        assertEquals(AdminResult.Failure(AdminError.INVALID_STATUS), edit("p1", "a", "Milk", -1.0))
        coVerify(exactly = 0) { repository.updateItem(any(), any(), any(), any()) }
    }
}
