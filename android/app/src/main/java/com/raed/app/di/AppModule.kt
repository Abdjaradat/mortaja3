package com.raed.app.di

import com.google.firebase.auth.FirebaseAuth
import com.raed.app.BuildConfig
import com.raed.app.data.api.RaedApi
import com.raed.app.data.api.buildRaedApi
import com.raed.app.data.local.SessionDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideRaedApi(sessionDataStore: SessionDataStore): RaedApi =
        buildRaedApi(
            baseUrl = BuildConfig.API_BASE_URL,
            sessionDataStore = sessionDataStore,
        )
}
