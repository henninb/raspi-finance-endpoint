package finance.services

import finance.domain.Category
import retrofit2.Call
import retrofit2.http.GET

interface CategoryApiService {
    @GET("select/active")
    fun listCategories(): Call<List<Category>>
}