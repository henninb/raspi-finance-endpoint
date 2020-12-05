package finance.controllers

import finance.domain.Parameter
import finance.services.ParameterApiService
import org.jetbrains.annotations.NotNull
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.Path
import retrofit2.mock.BehaviorDelegate
import retrofit2.mock.MockRetrofit
import retrofit2.mock.NetworkBehavior
import spock.lang.Shared
import spock.lang.Specification

class ParameterControllerSpec extends Specification {
    static final String BASE_URL = "http://url.com"

    @Shared
    protected NetworkBehavior behavior = NetworkBehavior.create()

    @Shared
    protected ParameterApiService mockParameterService

    static class MockParameterService implements ParameterApiService {

        BehaviorDelegate<ParameterApiService> delegate

        MockParameterService(BehaviorDelegate<ParameterApiService> delegate) {
            this.delegate = delegate
        }

        @Override
        Call<List<Parameter>> selectParameter(@NotNull @Path("parameterName") String parameterName) {
            delegate.returningResponse().selectParameter(parameterName)
        }
    }

    void setupSpec() {
        Retrofit retrofit = new Retrofit.Builder().baseUrl(BASE_URL).build()
        MockRetrofit mockRetrofit = new MockRetrofit.Builder(retrofit).networkBehavior(behavior).build()
        BehaviorDelegate<ParameterApiService> delegate = mockRetrofit.create(ParameterApiService)

        mockParameterService = new MockParameterService(delegate)
    }

    void "test - should succeed"() {
        given:
        def response = mockParameterService.selectParameter('test').execute()

        expect:
        response.successful
    }

    void 'test -- selectParameter - should fail on 500'() {
        given:
        behavior.setFailurePercent(100)

        when:
        mockParameterService.selectParameter('test').execute()

        then:
        IOException ex = thrown(IOException)
    }
}
