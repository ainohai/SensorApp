package fi.ainon.polarAppis.dataHandling.dataObject

enum class DataType {
    HR,
    ECG,
    ACC,
}

enum class DataSetting {
    ECG_RANGE,
    ECG_SAMPLE_RATE,
    ECG_RESOLUTION,
    ACC_RANGE,
    ACC_SAMPLE_RATE,
    ACC_RESOLUTION,
    COLLECTION_TIME_IN_S
}

enum class ConnectionSetting {
    SHOULD_BE_CONNECTED
}