package app.carlosribeiro.homemarket.presentation.admin

import app.carlosribeiro.homemarket.domain.model.ListItem

/** One bar of the "items per person this week" chart. */
data class PersonStat(val uid: String, val name: String, val itemNames: List<String>) {
    val itemCount: Int get() = itemNames.size
}

/**
 * iOS `WeeklyDashboardSection`: the current list's items grouped by who added them, most items first.
 * Grouped by uid, so two people with the same name never share a bar. An empty name stays empty and
 * the screen shows its "no name" label.
 */
fun personStats(items: List<ListItem>): List<PersonStat> = items
    .groupBy { it.addedByUid }
    .map { (uid, personItems) ->
        PersonStat(uid = uid, name = personItems.first().addedByName, itemNames = personItems.map { it.name })
    }
    .sortedByDescending { it.itemCount }
