package fi.ainon.polarAppis.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fi.ainon.polarAppis.data.DefaultPolarDataRepository
import fi.ainon.polarAppis.data.PolarDataRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface DataModule {

    @Singleton
    @Binds
    fun bindsPolarDataRepository(
        polarDataRepository: DefaultPolarDataRepository
    ): PolarDataRepository
}
