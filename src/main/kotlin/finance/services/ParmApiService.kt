package finance.services


import finance.domain.Parm
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface ParmApiService {
    //@GET("select/{parmName}")
    //fun selectParm(@Path("parmName") String parmName): Call<List<Parm>>

    @GET("/select/{parmName}")
    fun selectParm(@Path("parmName") parmName: String): Call<List<Parm>>
}