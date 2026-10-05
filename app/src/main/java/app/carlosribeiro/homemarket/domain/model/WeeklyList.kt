package app.carlosribeiro.homemarket.domain.model

/**
 * What the list screen shows: the current list (if any), its items, and the household's "next week"
 * items (`rolled_over`), which keep the id of the list they were added to.
 */
data class WeeklyList(val currentList: WeekList?, val items: List<ListItem>, val nextWeekItems: List<ListItem>)
