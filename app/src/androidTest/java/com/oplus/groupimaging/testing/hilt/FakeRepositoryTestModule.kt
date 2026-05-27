package com.oplus.groupimaging.testing.hilt

import com.oplus.groupimaging.di.AppBindsModule
import com.oplus.groupimaging.domain.repository.OplusInsightRepository
import com.oplus.groupimaging.testing.FakeInsightRepository
import com.oplus.groupimaging.testing.HarnessScenario
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

object TestRepositoryController {
    private val repository = FakeInsightRepository()

    init {
        HarnessScenario.emptyLibrary().applyTo(repository)
    }

    fun setScenario(scenario: HarnessScenario) {
        scenario.applyTo(repository)
    }

    fun repository(): FakeInsightRepository = repository
}

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [AppBindsModule::class],
)
object FakeRepositoryTestModule {
    @Provides
    @Singleton
    fun provideFakeRepository(): FakeInsightRepository = TestRepositoryController.repository()

    @Provides
    @Singleton
    fun provideRepository(fakeRepository: FakeInsightRepository): OplusInsightRepository = fakeRepository
}
