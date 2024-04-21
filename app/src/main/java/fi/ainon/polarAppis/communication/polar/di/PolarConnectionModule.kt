package fi.ainon.polarAppis.communication.polar.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fi.ainon.polarAppis.communication.polar.DefaultPolarConnection
import fi.ainon.polarAppis.communication.polar.PolarConnection
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface PolarConnectionModule {

    @Singleton
    @Binds
    fun bindsPolarConn(
        polarConnection: DefaultPolarConnection
    ): PolarConnection
}
