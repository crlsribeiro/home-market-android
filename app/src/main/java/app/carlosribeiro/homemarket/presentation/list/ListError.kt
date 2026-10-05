package app.carlosribeiro.homemarket.presentation.list

import androidx.annotation.StringRes
import app.carlosribeiro.homemarket.R
import app.carlosribeiro.homemarket.domain.model.ListError

@StringRes
fun ListError.messageRes(): Int = when (this) {
    ListError.CLOSED_THIS_WEEK -> R.string.list_error_closed_this_week
    ListError.NOT_ADMIN -> R.string.list_error_not_admin
    ListError.NETWORK -> R.string.auth_error_network
    ListError.NOT_SIGNED_IN, ListError.UNKNOWN -> R.string.auth_error_unknown
}
