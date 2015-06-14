package pl.mjasion.restcache

import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import com.squareup.okhttp.Response
import groovy.json.JsonSlurper
import org.springframework.beans.factory.annotation.Autowired
import pl.mjasion.restcache.domain.ApiRepository

class ApiIntegrationSpec extends IntegrationSpec {

    @Autowired ApiRepository apiRepository

    OkHttpClient okHttpClient = new OkHttpClient()
    Request apiRequest = new Request.Builder().url('http://localhost:8080/api').build()

    def setup() {
        apiRepository.deleteAll()
    }

    def cleanup() {
        apiRepository.deleteAll()
    }

    def 'should get apiKey'() {
        when:
        Response response = okHttpClient.newCall(apiRequest).execute()

        then:
        def apiJson = new JsonSlurper().parseText(response.body().string())
        apiJson.key ==~ /^[a-z\d-]{36}$/
    }

    def 'should apiKey be saved in db'() {
        expect:
        apiRepository.count() == 0

        when:
        Response response = okHttpClient.newCall(apiRequest).execute()

        then:
        def apiJson = new JsonSlurper().parseText(response.body().string())
        apiRepository.exists(apiJson.key)
    }
}

