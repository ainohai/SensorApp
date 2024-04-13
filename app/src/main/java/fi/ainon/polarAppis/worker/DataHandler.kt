package fi.ainon.polarAppis.worker

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fi.ainon.polarAppis.communication.dataServer.ServerDataConnection
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
interface DataHandlerModule {

    @Binds
    fun bindDataHandler(defaultDataHandler: DefaultDataHandler): DataHandler
}

interface DataHandler {
    fun handleECG(data: EcgData)
}

@Singleton
class DefaultDataHandler  @Inject constructor(
    private val serverDataConnection: ServerDataConnection
) : DataHandler {

    override fun handleECG(data: EcgData) {

        val jsonString = Json.encodeToString(data)
        val byteData = jsonString.toByteArray(Charsets.UTF_16)
        serverDataConnection.addData(byteData)
    }

}
