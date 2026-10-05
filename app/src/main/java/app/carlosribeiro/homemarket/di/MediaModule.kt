package app.carlosribeiro.homemarket.di

import app.carlosribeiro.homemarket.data.media.AndroidPhotoCompressor
import app.carlosribeiro.homemarket.data.media.MlKitReceiptTextRecognizer
import app.carlosribeiro.homemarket.domain.receipt.ReceiptTextRecognizer
import app.carlosribeiro.homemarket.domain.util.PhotoCompressor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Photo compression and on-device text recognition. */
@Module
@InstallIn(SingletonComponent::class)
object MediaModule {
    @Provides
    fun providePhotoCompressor(impl: AndroidPhotoCompressor): PhotoCompressor = impl

    @Provides
    fun provideReceiptTextRecognizer(impl: MlKitReceiptTextRecognizer): ReceiptTextRecognizer = impl
}
