package app.carlosribeiro.homemarket.presentation.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import app.carlosribeiro.homemarket.R
import app.carlosribeiro.homemarket.domain.model.AppUser
import app.carlosribeiro.homemarket.domain.model.UserRole

/** Top-level destinations, in the iOS tab order. History arrives with milestone M7. */
enum class MainTab(@StringRes val label: Int, @DrawableRes val icon: Int) {
    LIST(R.string.tab_list, R.drawable.ic_list),
    ADMIN(R.string.tab_admin, R.drawable.ic_admin_panel_settings),
    ACCOUNT(R.string.tab_account, R.drawable.ic_account_circle);

    companion object {
        /** The Admin tab is for the household admin only, as on iOS. */
        fun visibleFor(user: AppUser): List<MainTab> = if (user.role == UserRole.ADMIN) entries else entries - ADMIN
    }
}
