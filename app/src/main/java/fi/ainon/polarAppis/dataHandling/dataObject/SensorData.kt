package fi.ainon.polarAppis.dataHandling.dataObject

import java.io.Serializable

@kotlinx.serialization.Serializable
data class SensorData (val type: String, val byteArr: ByteArray): Serializable