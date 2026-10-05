package app.carlosribeiro.homemarket.data.repository

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountDeletionGuardTest {

    private val guard = AccountDeletionGuard()

    @Test
    fun deletedAccount_staysMarkedSoItsDocumentIsNeverRecreated() = runTest {
        guard.deleting("u1") { assertTrue(guard.isDeleting("u1")) }

        assertTrue(guard.isDeleting("u1"))
        assertFalse(guard.isDeleting("u2"))
    }

    @Test
    fun failedDeletion_clearsTheMark() = runTest {
        val result = runCatching { guard.deleting("u1") { error("network") } }

        assertTrue(result.isFailure)
        assertFalse(guard.isDeleting("u1"))
    }
}
