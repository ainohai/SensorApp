package fi.ainon.polarAppis.worker.dataObject

import java.io.Serializable

@kotlinx.serialization.Serializable
data class SensorData (val type: String, val byteArr: ByteArray): Serializable