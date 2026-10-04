package app.carlosribeiro.homemarket.domain.usecase

import app.carlosribeiro.homemarket.domain.model.HouseholdError
import app.carlosribeiro.homemarket.domain.model.HouseholdResult
import app.carlosribeiro.homemarket.domain.repository.HouseholdRepository
import app.carlosribeiro.homemarket.domain.util.TokenGenerator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class HouseholdUseCasesTest {

    private val repository = mockk<HouseholdRepository>()
    private val tokens = TokenGenerator { length -> "x".repeat(length) }

    @Test
    fun create_blankName_failsWithoutCallingRepository() = runTest {
        val result = CreateHouseholdUseCase(repository, tokens)("   ", "u1")

        assertEquals(HouseholdResult.Failure(HouseholdError.NAME_REQUIRED), result)
        coVerify(exactly = 0) { repository.createHousehold(any(), any(), any(), any()) }
    }

    @Test
    fun create_usesA20CharacterIdAndAn8CharacterToken() = runTest {
        coEvery { repository.createHousehold(any(), any(), any(), any()) } answers
            { HouseholdResult.Success(firstArg()) }

        val result = CreateHouseholdUseCase(repository, tokens)("  Casa Silva ", "u1")

        assertEquals(HouseholdResult.Success("x".repeat(20)), result)
        coVerify { repository.createHousehold("x".repeat(20), "Casa Silva", "x".repeat(8), "u1") }
    }

    @Test
    fun join_blankToken_fails() = runTest {
        val result = JoinHouseholdUseCase(repository)(" ", "u1")

        assertEquals(HouseholdResult.Failure(HouseholdError.TOKEN_REQUIRED), result)
    }

    @Test
    fun join_trimsButKeepsTheTokenCase() = runTest {
        coEvery { repository.joinHousehold(any(), any()) } returns HouseholdResult.Success("h1")

        JoinHouseholdUseCase(repository)(" aBc12XyZ ", "u1")

        coVerify { repository.joinHousehold("aBc12XyZ", "u1") }
    }

    @Test
    fun join_unknownToken_returnsTokenNotFound() = runTest {
        coEvery { repository.joinHousehold(any(), any()) } returns
            HouseholdResult.Failure(HouseholdError.TOKEN_NOT_FOUND)

        val result = JoinHouseholdUseCase(repository)("WRONG123", "u1")

        assertEquals(HouseholdResult.Failure(HouseholdError.TOKEN_NOT_FOUND), result)
    }
}
