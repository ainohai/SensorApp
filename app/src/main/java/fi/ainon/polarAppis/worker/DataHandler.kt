package fi.ainon.polarAppis.worker

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fi.ainon.polarAppis.communication.dataServer.ServerDataConnection
import fi.ainon.polarAppis.worker.dataObject.AccData
import fi.ainon.polarAppis.worker.dataObject.DataType
import fi.ainon.polarAppis.worker.dataObject.EcgData
import fi.ainon.polarAppis.worker.dataObject.HrData
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
    fun handleAcc(data: AccData)
    fun handleHr(data: HrData)
}

@Singleton
class DefaultDataHandler  @Inject constructor(
    private val serverDataConnection: ServerDataConnection
) : DataHandler {


    override fun handleECG(data: EcgData) {

        val byteData = makeByteArr(data)
        serverDataConnection.addData(byteData, DataType.ECG)

        //Todo: Other handling
    }

    override fun handleAcc(data: AccData) {
        serverDataConnection.addData(makeByteArr(data), DataType.ACC)

        //Todo: Other handling
    }

    override fun handleHr(data: HrData) {
        serverDataConnection.addData(makeByteArr(data), DataType.HR)

        // Todo: Other handling
    }

    private fun makeByteArr(data: Any): ByteArray {
        val jsonString = Json.encodeToString(data)
        return jsonString.toByteArray(Charsets.UTF_16)
    }


}
