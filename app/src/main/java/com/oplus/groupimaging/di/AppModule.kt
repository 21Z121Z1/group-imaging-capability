package com.oplus.groupimaging.di

import android.content.Context
import androidx.work.WorkManager
import com.oplus.groupimaging.data.GroupImagingDatabase
import com.oplus.groupimaging.data.GroupImagingRepository
import com.oplus.groupimaging.domain.repository.OplusInsightRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppProvidesModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GroupImagingDatabase =
        GroupImagingDatabase.build(context)

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
        WorkManager.getInstance(context)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AppBindsModule {
    @Binds
    @Singleton
    abstract fun bindRepository(impl: GroupImagingRepository): OplusInsightRepository
}
