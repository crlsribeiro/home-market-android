package app.carlosribeiro.homemarket.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [HouseholdEntity::class, MemberEntity::class, WeekListEntity::class, ItemEntity::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class HomeMarketDatabase : RoomDatabase() {
    abstract fun householdDao(): HouseholdDao

    abstract fun listDao(): ListDao

    abstract fun itemDao(): ItemDao
}
