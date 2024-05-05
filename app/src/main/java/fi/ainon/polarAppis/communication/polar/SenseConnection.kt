package fi.ainon.polarAppis.communication.polar

/*interface SensePolarConnection {

    fun getHr(): Flowable<PolarHrData>
    fun getPpi(): Flowable<PolarPpiData>
    fun getPpg(settings: PolarSensorSetting): Flowable<PolarPpgData>
}


class SenseConnection @Inject constructor(
    @ApplicationContext appContext: Context
) : DefaultPolarConnectionApi(appContext, BuildConfig.POLAR_SENSE), SensePolarConnection {


    override fun getHr(): Flowable<PolarHrData> {
        return startHrStream()
    }

    override fun getPpg(settings: PolarSensorSetting): Flowable<PolarPpgData> {
        return startPpgStream(settings)
    }

    override fun getPpi(): Flowable<PolarPpiData> {
        return startPpiStream()
    }
}*/