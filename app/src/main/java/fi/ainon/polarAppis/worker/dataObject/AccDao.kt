package fi.ainon.polarAppis.worker.dataObject

import java.io.Serializable


@kotlinx.serialization.Serializable
data class AccData(
    val samples: List<AccDataSample>,
): Serializable {

    /**
     * Polar accelerometer data sample
     *  @property timeStamp moment sample is taken in nanoseconds. The epoch of timestamp is 1.1.2000
     *  @property x axis value in millig (including gravity)
     *  @property y axis value in millig (including gravity)
     *  @property z axis value in millig (including gravity)
     */
    @kotlinx.serialization.Serializable
    data class AccDataSample(
        val timeStamp: Long,
        val x: Int,
        val y: Int,
        val z: Int
    ): Serializable
}