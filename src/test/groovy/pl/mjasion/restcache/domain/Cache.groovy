package pl.mjasion.restcache.domain

import org.springframework.data.annotation.Id


class Cache {
    @Id
    String id

    String api

    String key

    String value
}
