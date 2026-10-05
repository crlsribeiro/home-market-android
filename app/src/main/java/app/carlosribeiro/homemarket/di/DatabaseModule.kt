package app.carlosribeiro.homemarket.di

import android.content.Context
import androidx.room.Room
import app.carlosribeiro.homemarket.data.local.HomeMarketDatabase
import app.carlosribeiro.homemarket.data.local.HouseholdDao
import app.carlosribeiro.homemarket.data.local.ItemDao
import app.carlosribeiro.homemarket.data.local.ListDao
import app.carlosribeiro.homemarket.data.local.PurchaseDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): HomeMarketDatabase =
        Room.databaseBuilder(context, HomeMarketDatabase::class.java, "home-market.db")
            // Room only caches Firestore data, so a schema change can rebuild it from the listeners.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideHouseholdDao(database: HomeMarketDatabase): HouseholdDao = database.householdDao()

    @Provides
    fun provideListDao(database: HomeMarketDatabase): ListDao = database.listDao()

    @Provides
    fun provideItemDao(database: HomeMarketDatabase): ItemDao = database.itemDao()

    @Provides
    fun providePurchaseDao(database: HomeMarketDatabase): PurchaseDao = database.purchaseDao()
}
