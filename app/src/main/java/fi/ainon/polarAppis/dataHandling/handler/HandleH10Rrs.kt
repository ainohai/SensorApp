package fi.ainon.polarAppis.dataHandling.handler

import android.util.Log
import fi.ainon.polarAppis.dataHandling.dataObject.HrData
import fi.ainon.polarAppis.dataHandling.di.DataHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.sqrt

class HandleH10Rrs @Inject constructor() : DataHandler<HrData, Double> {

    private val TAG = "HandleH10Rrs: "
    private var _RRMSSDFlow = MutableSharedFlow<Double>()
    private var RRMSSDFlow = _RRMSSDFlow.asSharedFlow()

    private var rrsBuffer: MutableList<Int> = mutableListOf()

    private val CALC_HRV_DELAY = 15 * 1000L

    init {
        CoroutineScope(Dispatchers.Default).launch {
            launchHrvCalc()
        }
    }

    override fun handle(data: HrData) {

        Log.d(TAG, "Handle hr")

        val rrsValues = data.samples.flatMap { hrSample -> hrSample.rrsMs }
        rrsBuffer.addAll(rrsValues)

    }

    override fun dataFlow(): SharedFlow<Double> {
        return RRMSSDFlow
    }

    private suspend fun launchHrvCalc() {
        while (true) {
            delay(CALC_HRV_DELAY)

            if (rrsBuffer.isNotEmpty()) {
                val lastIndex = rrsBuffer.lastIndex
                // Does not handle the last value, but it doesn't matter
                if (lastIndex > 0) {
                    val lastWanted = lastIndex - 1
                    val rrsValues = rrsBuffer.slice(0..lastWanted)
                    calcHrv(rrsValues)
                }
                rrsBuffer = rrsBuffer.slice(lastIndex..rrsBuffer.lastIndex).toMutableList()
            }
        }
    }

    private suspend fun calcHrv(raw: List<Int>) {

        val values = mutableListOf<Int>()
        for (i in 1..raw.lastIndex) {
            values.add(abs(raw[i] - raw[i - 1]))
        }
        if (values.isNotEmpty()) {
            val squared = values.map { value -> value * value }
            val sum = squared.reduce { acc, value -> acc + value }
            val mean = sum.toDouble() / squared.size
            val root = sqrt(mean)

            _RRMSSDFlow.emit(root)
        }
    }

}