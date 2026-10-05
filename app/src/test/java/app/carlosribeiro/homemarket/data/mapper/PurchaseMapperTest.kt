package app.carlosribeiro.homemarket.data.mapper

import app.carlosribeiro.homemarket.data.local.PurchaseEntity
import app.carlosribeiro.homemarket.data.local.PurchaseWithWeek
import com.google.firebase.Timestamp
import java.time.Instant
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class PurchaseMapperTest {

    @Test
    fun purchase_readsWholeNumbersAndUsesTheIosDefaults() {
        val created = Date(1_000L)
        val entity = PurchaseMapper.documentToPurchaseEntity(
            "p1",
            mapOf("listId" to "l1", "householdId" to "h1", "total" to 0L, "createdAt" to Timestamp(created))
        )

        assertEquals(0.0, entity.total, 0.0)
        assertEquals(1_000L, entity.createdAt)
        assertEquals("", entity.weekLabel)
        assertNull(entity.receiptUrl)
        assertFalse(entity.receiptProcessed)
        assertNull(entity.storeName)
    }

    @Test
    fun item_defaultsQuantityToOneAndReadsIntegerPrices() {
        val entity = PurchaseMapper.documentToItemEntity(
            "a",
            mapOf(
                "purchaseId" to "p1",
                "name" to "MILK",
                "unitPrice" to 3L
            )
        )

        assertEquals(1, entity.quantity)
        assertEquals(3.0, entity.unitPrice, 0.0)
        assertEquals(0.0, entity.totalPrice, 0.0)
    }

    @Test
    fun toDomain_takesTheWeekFromTheCachedListOrLeavesItMissing() {
        val entity = PurchaseEntity("p1", "l1", "h1", "28 – 4 OUT", 1.0, null, false, null, null)

        val linked = PurchaseMapper.toDomain(PurchaseWithWeek(entity, 10L, 20L))
        assertEquals(Instant.ofEpochMilli(10L), linked.weekStart)
        assertEquals(Instant.ofEpochMilli(20L), linked.weekEnd)

        assertNull(PurchaseMapper.toDomain(PurchaseWithWeek(entity, null, null)).weekStart)
        assertNull(PurchaseMapper.toDomain(PurchaseWithWeek(entity, 0L, 0L)).weekEnd)
    }
}
