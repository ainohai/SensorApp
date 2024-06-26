package fi.ainon.polarAppis.communication.dataServer

import android.util.Log
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fi.ainon.polarAppis.BuildConfig
import fi.ainon.polarAppis.dataHandling.dataObject.DataType
import fi.ainon.polarAppis.dataHandling.dataObject.SensorData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import javax.inject.Inject
import javax.inject.Singleton

private const val BASE_URL = BuildConfig.BACKEND_URL
private const val TAG = "ServerConnection: "

private interface ServerConnectionApi {
    @POST(value = "addData")
    fun addData(@Body data: SensorData) : retrofit2.Call<Void?>?

    @GET(value = "/")
    suspend fun getRoot(): String

}

@Module
@InstallIn(SingletonComponent::class)
internal object ServerDataConnectionModule {

    @Provides
    @Singleton
    fun providesServerConn(
    ): ServerDataConnection = DefaultServerConnection(okHttpCallFactory())

    @Provides
    @Singleton
    fun providesNetworkJson(): Json = Json {
        ignoreUnknownKeys = true
    }

    @Provides
    @Singleton
    fun okHttpCallFactory() = OkHttpClient.Builder()
        .addInterceptor(
            HttpLoggingInterceptor()
                .apply {
                    //if (BuildConfig.DEBUG) {
                    //    setLevel(HttpLoggingInterceptor.Level.BODY)
                    //}
                },
        )
        .build()


}

interface ServerDataConnection {

    fun addData(data: ByteArray, dataType: DataType)
    suspend fun test(name: String? = null): String
}


@Singleton
class DefaultServerConnection @Inject constructor(okhttpCallFactory: okhttp3.Call.Factory,) : ServerDataConnection {

    //Todo: improve. Just doesn't send things for a while if too many failures
    private val LIMIT_OF_FAILURES_BEFORE_NOT_TRYING = 30
    private val RETRY_TIME_IN_MILLIS = 5* 60 * 1000L
    private var failedRequestsInRow = 0
    private var shouldSendToApi = true


        private val networkApi = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .callFactory(okhttpCallFactory)
            .addConverterFactory(
                Json.asConverterFactory(
                    "application/json; charset=UTF8".toMediaType()))
            .build()
            .create(ServerConnectionApi::class.java)


    override suspend fun test(name: String?): String {
        val pong = networkApi.getRoot()
        Log.d(TAG, pong)
        return pong
    }

    //TODO: Does handle only best case scenario.
    override fun addData(data: ByteArray, dataType: DataType) {

        if (!shouldSendToApi) {
            //Log.v(TAG, "No api call")
            return
        }

        val call = networkApi.addData(SensorData(dataType.name, data))

        call?.enqueue(object : Callback<Void?> {
            override fun onResponse(call: retrofit2.Call<Void?>?, response: Response<Void?>?) {
                if (response?.isSuccessful == true) {
                    failedRequestsInRow = 0
                    Log.d(TAG, "Success")
                } else {
                    handleFailure()
                    Log.d(TAG, "Unsuccess " + response?.body())

                }
            }
            override fun onFailure(call: retrofit2.Call<Void?>, t: Throwable) {
                handleFailure()
                Log.d(TAG, "Failure")

            }
        })
    }

    private fun handleFailure () {
        failedRequestsInRow += 1
        if (failedRequestsInRow >= LIMIT_OF_FAILURES_BEFORE_NOT_TRYING) {
            shouldSendToApi = false
            Log.d(TAG, "Too many failures. No api calls are made for a while.")
        }

        CoroutineScope(Dispatchers.Default).launch {
            delay(RETRY_TIME_IN_MILLIS)
            // Retun api calls after delay
            Log.d(TAG, "Making api calls again")
        }

    }
}
