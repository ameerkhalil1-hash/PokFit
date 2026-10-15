package com.pokopia.macros.di

import android.content.Context
import androidx.room.Room
import com.pokopia.macros.data.local.PokopiaDao
import com.pokopia.macros.data.local.PokopiaDatabase
import com.pokopia.macros.domain.math.MacroFactorEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PokopiaAppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PokopiaDatabase =
        Room.databaseBuilder(
            context,
            PokopiaDatabase::class.java,
            "pokopia_macros.db"
        ).fallbackToDestructiveMigration().build()

    @Provides
    @Singleton
    fun providePokopiaDao(db: PokopiaDatabase): PokopiaDao = db.pokopiaDao()

    @Provides
    @Singleton
    fun provideMacroFactorEngine(): MacroFactorEngine = MacroFactorEngine()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }
}
