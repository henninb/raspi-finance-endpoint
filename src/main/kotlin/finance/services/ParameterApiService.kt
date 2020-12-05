package finance.services


import finance.domain.Parameter
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface ParameterApiService {
    //@GET("select/{parameterName}")
    //fun selectParameter(@Path("parameterName") String parameterName): Call<List<Parameter>>

    @GET("/select/{parameterName}")
    fun selectParameter(@Path("parameterName") parameterName: String): Call<List<Parameter>>
}