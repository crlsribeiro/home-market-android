package app.carlosribeiro.homemarket.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
data object LoginDestination

@Serializable
data object RegisterDestination

@Serializable
data object MainDestination

/** The property name must match [app.carlosribeiro.homemarket.presentation.item.ItemDetailViewModel.ITEM_ID_KEY]. */
@Serializable
data class ItemDetailDestination(val itemId: String)

/**
 * The property name must match
 * [app.carlosribeiro.homemarket.presentation.history.PurchaseDetailViewModel.PURCHASE_ID_KEY].
 */
@Serializable
data class PurchaseDetailDestination(val purchaseId: String)
