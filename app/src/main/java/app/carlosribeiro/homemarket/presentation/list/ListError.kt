package app.carlosribeiro.homemarket.presentation.list

import androidx.annotation.StringRes
import app.carlosribeiro.homemarket.R
import app.carlosribeiro.homemarket.domain.model.ItemError
import app.carlosribeiro.homemarket.domain.model.ListError

@StringRes
fun ListError.messageRes(): Int = when (this) {
    ListError.CLOSED_THIS_WEEK -> R.string.list_error_closed_this_week
    ListError.NOT_ADMIN -> R.string.list_error_not_admin
    ListError.NETWORK -> R.string.auth_error_network
    ListError.NOT_SIGNED_IN, ListError.UNKNOWN -> R.string.auth_error_unknown
}

@StringRes
fun ItemError.messageRes(): Int = when (this) {
    ItemError.NAME_REQUIRED -> R.string.add_item_error_name_required
    ItemError.CLOSED_THIS_WEEK -> R.string.list_error_closed_this_week
    ItemError.PHOTO_UPLOAD -> R.string.add_item_error_photo
    ItemError.NETWORK -> R.string.auth_error_network
    ItemError.NOT_SIGNED_IN, ItemError.UNKNOWN -> R.string.auth_error_unknown
}
