package pl.mjasion.restcache

import org.springframework.boot.test.SpringApplicationContextLoader
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

@ContextConfiguration(classes = [RestCacheApp], loader = SpringApplicationContextLoader)
abstract class IntegrationSpec extends Specification {

}
