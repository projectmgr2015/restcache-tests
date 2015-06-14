package pl.mjasion.restcache

import com.squareup.okhttp.*
import groovy.json.JsonBuilder
import org.springframework.beans.factory.annotation.Autowired
import pl.mjasion.restcache.domain.Api
import pl.mjasion.restcache.domain.ApiRepository
import pl.mjasion.restcache.domain.Cache
import pl.mjasion.restcache.domain.CacheRepository
import spock.lang.Unroll

class ResponseCodesIntegrationSpec extends IntegrationSpec {

    @Autowired ApiRepository apiRepository
    @Autowired CacheRepository cacheRepository

    static String apiKey = UUID.randomUUID().toString()
    static String cacheKey = 'cache_key'
    OkHttpClient okHttpClient = new OkHttpClient()

    static MediaType JSON = MediaType.parse("application/json; charset=utf-8")

    def setup() {
        apiRepository.deleteAll()
        cacheRepository.deleteAll()
        apiRepository.save(new Api(key: apiKey))
    }

    def cleanup() {
        apiRepository.deleteAll()
        cacheRepository.deleteAll()
    }

    def 'should return OK response on getting apiKey'() {
        given:
        Request apiRequest = new Request.Builder().url('http://localhost:8080/api').build()

        when:
        Response response = okHttpClient.newCall(apiRequest).execute()

        then:
        response.code() == 200
    }

    def 'should return OK response on getting list of cached values for given apiKey'() {
        given:
        Request allKeysRequest = new Request.Builder()
                .url("http://localhost:8080/api/${apiKey}")
                .build()

        when:
        Response response = okHttpClient.newCall(allKeysRequest).execute()

        then:
        response.code() == 200
    }

    def 'should return OK response on getting cache'() {
        given:
        Cache savedCache = new Cache(api: apiKey, key: cacheKey, value: 'saved_value')
        cacheRepository.save(savedCache)
        Request allKeysRequest = createCacheRequest('GET')

        when:
        Response response = okHttpClient.newCall(allKeysRequest).execute()

        then:
        response.code() == 200
    }

    def 'should return OK response on creating cache'() {
        given:
        Map cacheRequest = [cacheValue: 'somevalue']
        RequestBody body = RequestBody.create(JSON, new JsonBuilder(cacheRequest).toString())
        Request postRequest = createCacheRequest('POST', body)

        when:
        Response response = okHttpClient.newCall(postRequest).execute()

        then:
        response.code() == 200
    }

    def 'should return OK response on updating cache'() {
        given:
        Cache savedCache = new Cache(api: apiKey, key: cacheKey, value: 'someValue')
        cacheRepository.save(savedCache)
        String newValue = 'newValue'
        RequestBody requestBody = RequestBody.create(JSON, "{\"cacheValue\": \"$newValue\"}")
        Request request = createCacheRequest('PUT', requestBody)

        when:
        Response response = okHttpClient.newCall(request).execute()

        then:
        response.code() == 200
    }

    def 'should return OK response on deleting cache'() {
        given:
        Cache savedCache = new Cache(api: apiKey, key: cacheKey, value: 'someValue')
        cacheRepository.save(savedCache)
        Request deleteRequest = createCacheRequest('DELETE')

        when:
        Response response = okHttpClient.newCall(deleteRequest).execute()

        then:
        response.code() == 200
    }

    def 'should return CONFLICT response on create cache if cache key already exist'() {
        given:
        cacheRepository.save(new Cache(api: apiKey, key: cacheKey, value: 'someValue'))
        Map cacheRequest = [cacheValue: 'somevalue']
        RequestBody body = RequestBody.create(JSON, new JsonBuilder(cacheRequest).toString())
        Request postRequest = createCacheRequest('POST', body)

        when:
        Response response = okHttpClient.newCall(postRequest).execute()

        then:
        response.code() == 409
    }

    @Unroll
    def 'should return NOT_FOUND response on #methodName cache if apikey does not exist'() {
        given:
        String apiKey = UUID.randomUUID().toString()
        Request postRequest = new Request.Builder()
                .url("http://localhost:8080/api/${apiKey}/${cacheKey}")
                .method(methodName, methodBody)
                .build()

        when:
        Response response = okHttpClient.newCall(postRequest).execute()

        then:
        response.code() == 404

        where:
        methodName | methodBody
        'GET'      | null
        'POST'     | RequestBody.create(JSON, '{"cacheValue": "someValue"}')
        'PUT'      | RequestBody.create(JSON, '{"cacheValue": "someValue"}')
        'DELETE'   | null
    }

    @Unroll
    def 'should return BAD_REQUEST response on #methodName cache if no cacheValue passed'() {
        given:
        Map cacheRequest = [cacheValue: null]
        RequestBody body = RequestBody.create(JSON, new JsonBuilder(cacheRequest).toString())
        Request postRequest = createCacheRequest(methodName, body)

        when:
        Response response = okHttpClient.newCall(postRequest).execute()

        then:
        response.code() == 400

        where:
        methodName << ['POST', 'PUT']
    }

    @Unroll
    def 'should return NOT_FOUND response on #methodName if cache for given key does not exists'() {
        given:
        Request allKeysRequest = createCacheRequest(methodName, methodBody)

        when:
        Response response = okHttpClient.newCall(allKeysRequest).execute()

        then:
        response.code() == 404

        where:
        methodName | methodBody
        'GET'      | null
        'PUT'      | RequestBody.create(JSON, '{"cacheValue": "someValue"}')
        'DELETE'   | null
    }

    private Request createCacheRequest(String methodName, RequestBody methodBody = null) {
        return new Request.Builder()
                .url("http://localhost:8080/api/${apiKey}/${cacheKey}")
                .method(methodName, methodBody)
                .build()
    }

}
