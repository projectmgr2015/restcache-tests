package pl.mjasion.restcache

import com.squareup.okhttp.*
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.springframework.beans.factory.annotation.Autowired
import pl.mjasion.restcache.domain.Api
import pl.mjasion.restcache.domain.ApiRepository
import pl.mjasion.restcache.domain.Cache
import pl.mjasion.restcache.domain.CacheRepository

class CacheIntegrationSpec extends IntegrationSpec {

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

    def 'should get empty list of cached values for apiKey'() {
        given:
        Request allKeysRequest = new Request.Builder()
                .url("http://localhost:8080/api/${apiKey}")
                .build()

        expect:
        cacheRepository.count() == 0

        when:
        Response response = okHttpClient.newCall(allKeysRequest).execute()

        then:
        response.code() == 200
        def apiJson = new JsonSlurper().parseText(response.body().string())
        apiJson.size() == 0
    }

    def 'should get list of cached values for apiKey'() {
        given:
        Cache savedCache = new Cache(api: apiKey, key: cacheKey, value: 'saved_value')
        cacheRepository.save(savedCache)
        Request allKeysRequest = new Request.Builder()
                .url("http://localhost:8080/api/${apiKey}")
                .build()

        when:
        Response response = okHttpClient.newCall(allKeysRequest).execute()

        then:
        response.code() == 200
        def apiJson = new JsonSlurper().parseText(response.body().string())
        apiJson.size() == 1
        apiJson.first().key == savedCache.key
        apiJson.first().value == savedCache.value
    }

    def 'should get saved cache value for given key'() {
        given:
        Cache savedCache = new Cache(api: apiKey, key: cacheKey, value: 'saved_value')
        cacheRepository.save(savedCache)
        Request allKeysRequest = new Request.Builder()
                .url("http://localhost:8080/api/${apiKey}/${cacheKey}")
                .build()

        when:
        Response response = okHttpClient.newCall(allKeysRequest).execute()

        then:
        response.code() == 200
        def json = new JsonSlurper().parseText(response.body().string())
        json.api == savedCache.api
        json.key == savedCache.key
        json.value == savedCache.value
    }

    def 'should create cache'() {
        given:
        Map cacheRequest = [cacheValue: 'somevalue']
        RequestBody body = RequestBody.create(JSON, new JsonBuilder(cacheRequest).toString())
        Request postRequest = new Request.Builder()
                .url("http://localhost:8080/api/${apiKey}/${cacheKey}")
                .post(body)
                .build()

        when:
        Response response = okHttpClient.newCall(postRequest).execute()

        then:
        response.code() == 200
        Cache cache = cacheRepository.findAll().find { it.key == cacheKey }
        cache.value == cacheRequest.cacheValue
    }

    def 'should update cache'() {
        given:
        Cache savedCache = new Cache(api: apiKey, key: cacheKey, value: 'someValue')
        cacheRepository.save(savedCache)
        String newValue = 'newValue'
        RequestBody requestBody = RequestBody.create(JSON, "{\"cacheValue\": \"$newValue\"}")
        Request request = new Request.Builder()
                .url("http://localhost:8080/api/${apiKey}/${cacheKey}")
                .put(requestBody)
                .build()

        when:
        Response response = okHttpClient.newCall(request).execute()

        then:
        response.code() == 200
        Cache updatedCache = cacheRepository.findOne(savedCache.id)
        updatedCache.value == newValue
    }

    def 'should delete cache'() {
        given:
        Cache savedCache = new Cache(api: apiKey, key: cacheKey, value: 'someValue')
        cacheRepository.save(savedCache)
        Request deleteRequest = new Request.Builder()
                .url("http://localhost:8080/api/${apiKey}/${cacheKey}")
                .delete()
                .build()

        when:
        Response response = okHttpClient.newCall(deleteRequest).execute()

        then:
        response.code() == 200
        cacheRepository.exists(savedCache.id) == false
    }

}
