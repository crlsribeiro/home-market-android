package app.carlosribeiro.homemarket.di

import android.content.Context
import androidx.room.Room
import app.carlosribeiro.homemarket.data.local.HomeMarketDatabase
import app.carlosribeiro.homemarket.data.local.HouseholdDao
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
        Room.databaseBuilder(context, HomeMarketDatabase::class.java, "home-market.db").build()

    @Provides
    fun provideHouseholdDao(database: HomeMarketDatabase): HouseholdDao = database.householdDao()
}
