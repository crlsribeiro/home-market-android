package app.carlosribeiro.homemarket.domain.usecase

import app.carlosribeiro.homemarket.domain.model.AppUser
import app.carlosribeiro.homemarket.domain.model.HouseholdError
import app.carlosribeiro.homemarket.domain.model.HouseholdResult
import app.carlosribeiro.homemarket.domain.model.ProfileError
import app.carlosribeiro.homemarket.domain.model.ProfileResult
import app.carlosribeiro.homemarket.domain.model.UserRole
import app.carlosribeiro.homemarket.domain.repository.AuthRepository
import app.carlosribeiro.homemarket.domain.repository.HouseholdRepository
import app.carlosribeiro.homemarket.domain.repository.ProfileRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * iOS `DeleteAccountView`: an admin with other members hands the admin role to another member, then
 * the user leaves the household, and `users/{uid}` and the Auth account are deleted last.
 *
 * Unlike iOS, the "requires recent login" case is detected before anything is written, so the
 * household and the user document are never changed when Firebase would refuse the deletion.
 */
class DeleteAccountUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val householdRepository: HouseholdRepository,
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(): ProfileResult {
        val user = authRepository.observeCurrentUser().first()
        return when {
            user == null -> ProfileResult.Failure(ProfileError.NOT_SIGNED_IN)
            !profileRepository.hasRecentSignIn() -> ProfileResult.Failure(ProfileError.REQUIRES_RECENT_LOGIN)
            else -> leaveHousehold(user).takeIf { it is ProfileResult.Failure } ?: profileRepository.deleteAccount()
        }
    }

    /** Leaves the household first, handing the admin role over when needed. Success when there is none. */
    private suspend fun leaveHousehold(user: AppUser): ProfileResult {
        val householdId = user.householdId ?: return ProfileResult.Success()
        // The member list comes from the cache; wait briefly for the listener rather than skip the hand-off.
        val household = withTimeoutOrNull(HOUSEHOLD_TIMEOUT_MILLIS) {
            householdRepository.observeHousehold(householdId).filterNotNull().first()
        }
        val left = household?.let {
            val promote = it.memberUids.firstOrNull { uid -> uid != user.uid }.takeIf { user.role == UserRole.ADMIN }
            householdRepository.leaveHousehold(householdId, user.uid, promote)
        }
        return when (left) {
            null -> ProfileResult.Failure(ProfileError.NETWORK)
            is HouseholdResult.Failure -> ProfileResult.Failure(left.error.toProfileError())
            is HouseholdResult.Success -> ProfileResult.Success()
        }
    }

    private fun HouseholdError.toProfileError(): ProfileError =
        if (this == HouseholdError.NETWORK) ProfileError.NETWORK else ProfileError.UNKNOWN

    private companion object {
        const val HOUSEHOLD_TIMEOUT_MILLIS = 10_000L
    }
}
