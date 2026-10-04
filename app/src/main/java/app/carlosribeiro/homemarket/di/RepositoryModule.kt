package app.carlosribeiro.homemarket.di

import app.carlosribeiro.homemarket.data.repository.FirebaseAuthRepository
import app.carlosribeiro.homemarket.domain.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindAuthRepository(impl: FirebaseAuthRepository): AuthRepository
}
