package app.carlosribeiro.homemarket.presentation.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import app.carlosribeiro.homemarket.R
import app.carlosribeiro.homemarket.domain.model.AppUser
import app.carlosribeiro.homemarket.domain.model.UserRole

/**
 * Top-level destinations, in the iOS tab order. [selectedIcon] is the filled variant shown while the tab
 * is selected (Material 3 navigation), or the same [icon] when there is none.
 */
enum class MainTab(@StringRes val label: Int, @DrawableRes val icon: Int, @DrawableRes val selectedIcon: Int = icon) {
    LIST(R.string.tab_list, R.drawable.ic_list),
    ADMIN(R.string.tab_admin, R.drawable.ic_admin_panel_settings, R.drawable.ic_admin_panel_settings_filled),
    HISTORY(R.string.tab_history, R.drawable.ic_history),
    ACCOUNT(R.string.tab_account, R.drawable.ic_account_circle, R.drawable.ic_account_circle_filled);

    companion object {
        /**
         * The Admin tab is for the household admin only, as on iOS. History is admin only too, like the
         * web (docs/backend.md, decision 4).
         */
        fun visibleFor(user: AppUser): List<MainTab> =
            if (user.role == UserRole.ADMIN) entries else entries - setOf(ADMIN, HISTORY)
    }
}
