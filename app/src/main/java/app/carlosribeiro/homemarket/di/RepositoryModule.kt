package app.carlosribeiro.homemarket.di

import app.carlosribeiro.homemarket.data.media.AndroidPhotoCompressor
import app.carlosribeiro.homemarket.data.repository.FirebaseAuthRepository
import app.carlosribeiro.homemarket.data.repository.FirebaseHouseholdRepository
import app.carlosribeiro.homemarket.data.repository.FirebaseItemRepository
import app.carlosribeiro.homemarket.data.repository.FirebaseListRepository
import app.carlosribeiro.homemarket.domain.repository.AuthRepository
import app.carlosribeiro.homemarket.domain.repository.HouseholdRepository
import app.carlosribeiro.homemarket.domain.repository.ItemRepository
import app.carlosribeiro.homemarket.domain.repository.ListRepository
import app.carlosribeiro.homemarket.domain.util.PhotoCompressor
import app.carlosribeiro.homemarket.domain.util.RandomTokenGenerator
import app.carlosribeiro.homemarket.domain.util.TokenGenerator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    fun provideAuthRepository(impl: FirebaseAuthRepository): AuthRepository = impl

    @Provides
    fun provideHouseholdRepository(impl: FirebaseHouseholdRepository): HouseholdRepository = impl

    @Provides
    fun provideListRepository(impl: FirebaseListRepository): ListRepository = impl

    @Provides
    fun provideItemRepository(impl: FirebaseItemRepository): ItemRepository = impl

    @Provides
    fun providePhotoCompressor(impl: AndroidPhotoCompressor): PhotoCompressor = impl

    @Provides
    fun provideClock(): Clock = Clock.systemDefaultZone()

    @Provides
    fun provideTokenGenerator(impl: RandomTokenGenerator): TokenGenerator = impl
}
