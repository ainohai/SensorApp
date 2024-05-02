package fi.ainon.polarAppis.dataHandling.dataObject

enum class DataType {
    HR,
    ECG,
    ACC,
    PPG,
    PPI
}

enum class CollectionSetting {
    COLLECTION_TIME_IN_MIN,
    WORKER_START_TIME,
    TRIGGER_TIME_MILLIS,
    COLLECTION_INTERVAL_IN_MIN
}

enum class ConnectionSetting {
    SHOULD_BE_CONNECTED
}

enum class PolarDataSetting {
    ECG_RANGE,
    ECG_SAMPLE_RATE,
    ECG_RESOLUTION,
    ACC_RANGE,
    ACC_SAMPLE_RATE,
    ACC_RESOLUTION,
    PPG_SAMPLE_RATE,
    PPG_RESOLUTION,
    PPG_CHANNELS
}