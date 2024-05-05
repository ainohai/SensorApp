package fi.ainon.polarAppis.communication.polar.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fi.ainon.polarAppis.communication.polar.PolarConnection
import fi.ainon.polarAppis.communication.polar.PolarH10Connection
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface PolarConnectionModule {


    @Singleton
    @Binds
    fun bindsPolarH10Conn(
        h10Connection: PolarH10Connection
    ): PolarConnection

}
