package app.carlosribeiro.homemarket.domain.model

/**
 * An item with the list it belongs to.
 *
 * @property price the admin's price row; null for members, who never see prices.
 */
data class ItemDetail(val item: ListItem, val list: WeekList?, val price: ItemPrice? = null) {
    /**
     * iOS hides "remove" when the list is closed. Next-week (`rolled_over`) items keep the id of a
     * closed list and can only leave the "next week" section by being removed, so they stay removable.
     */
    val canRemove: Boolean
        get() = item.status == ItemStatus.ROLLED_OVER || list?.status != ListStatus.CLOSED
}
