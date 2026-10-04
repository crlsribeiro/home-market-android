package app.carlosribeiro.homemarket.di

import app.carlosribeiro.homemarket.data.repository.FirebaseAuthRepository
import app.carlosribeiro.homemarket.domain.repository.AuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    fun provideAuthRepository(impl: FirebaseAuthRepository): AuthRepository = impl
}
