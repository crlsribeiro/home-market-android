package app.carlosribeiro.homemarket.data.mapper

import app.carlosribeiro.homemarket.domain.model.ApprovalStatus
import app.carlosribeiro.homemarket.domain.model.ItemStatus
import app.carlosribeiro.homemarket.domain.model.ListStatus
import com.google.firebase.Timestamp
import java.time.Instant
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ListMapperTest {

    private val start = Instant.parse("2026-09-28T03:00:00Z")
    private val end = Instant.parse("2026-10-05T02:59:59.999Z")

    @Test
    fun listDocument_roundTripsToDomain() {
        val entity = ListMapper.documentToListEntity(
            "h1_2026-09-28",
            mapOf(
                "householdId" to "h1",
                "weekLabel" to "28 – 4 OUT",
                "weekStart" to Timestamp(Date.from(start)),
                "weekEnd" to Timestamp(Date.from(end)),
                "status" to "shopping",
                "createdAt" to Timestamp(Date.from(start)),
                "closedAt" to null
            )
        )
        val list = ListMapper.listEntityToDomain(entity)

        assertEquals("h1_2026-09-28", list.id)
        assertEquals("h1", list.householdId)
        assertEquals(start, list.weekStart)
        assertEquals(end, list.weekEnd)
        assertEquals(ListStatus.SHOPPING, list.status)
        assertEquals(start, list.createdAt)
    }

    @Test
    fun listDocument_withMissingFields_usesDefaults() {
        val list = ListMapper.listEntityToDomain(ListMapper.documentToListEntity("l1", emptyMap()))

        assertEquals(ListStatus.OPEN, list.status)
        assertNull(list.createdAt)
    }

    @Test
    fun itemDocument_mapsEveryField() {
        val entity = ListMapper.documentToItemEntity(
            "i1",
            mapOf(
                "listId" to "l1",
                "householdId" to "h1",
                "name" to "Leite",
                "quantity" to 3L,
                "notes" to "Integral",
                "urgent" to true,
                "addedByUid" to "u1",
                "addedByName" to "Maria",
                "status" to "not_found",
                "approvalStatus" to "pending",
                "notFoundResolved" to true,
                "photoURL" to "https://example.com/photo.jpg",
                "createdAt" to Timestamp(Date.from(start))
            )
        )
        val item = ListMapper.itemEntityToDomain(entity)

        assertEquals("l1", item.listId)
        assertEquals("Leite", item.name)
        assertEquals(3, item.quantity)
        assertEquals("Integral", item.notes)
        assertEquals(true, item.urgent)
        assertEquals("Maria", item.addedByName)
        assertEquals(ItemStatus.NOT_FOUND, item.status)
        assertEquals(ApprovalStatus.PENDING, item.approvalStatus)
        assertEquals(true, item.notFoundResolved)
        assertEquals("https://example.com/photo.jpg", item.photoUrl)
        assertEquals(start, item.createdAt)
    }

    @Test
    fun itemDocument_withMissingFields_usesTheIosDefaults() {
        val item = ListMapper.itemEntityToDomain(ListMapper.documentToItemEntity("i1", emptyMap()))

        assertEquals(1, item.quantity)
        assertEquals("", item.notes)
        assertEquals(false, item.urgent)
        assertEquals(ItemStatus.PENDING, item.status)
        assertEquals(ApprovalStatus.NOT_REQUIRED, item.approvalStatus)
        assertNull(item.photoUrl)
        assertNull(item.createdAt)
    }

    @Test
    fun statusValues_mapBothWays() {
        assertEquals(ItemStatus.ROLLED_OVER, ListMapper.itemStatus("rolled_over"))
        assertEquals(ItemStatus.PURCHASED, ListMapper.itemStatus("purchased"))
        assertEquals(ApprovalStatus.APPROVED, ListMapper.approvalStatus("approved"))
        assertEquals(ApprovalStatus.REJECTED, ListMapper.approvalStatus("rejected"))
        assertEquals(ListStatus.LOCKED, ListMapper.listStatus("locked"))
        assertEquals(ListStatus.CLOSED, ListMapper.listStatus("closed"))
    }
}
