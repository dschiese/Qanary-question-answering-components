package eu.wdaqua.qanary.platypus_wrapper;

import org.springframework.boot.test.context.TestConfiguration;

@TestConfiguration
public class RestTemplateCacheLiveTestConfiguration {
    // define here the current CaffeineCacheManager configuration
    static {
        System.setProperty("platypus.endpoint.url", "http://some-platypus-endpoint-url.com/endpoint");
//        System.setProperty("platypus.endpoint.url", "http://127.0.0.1:41021/platypus/answer_raw");
        System.setProperty("qanary.webservicecalls.cache.specs",
                "maximumSize=1000,expireAfterAccess=" + TestPlatypusQueryBuilder.MAX_TIME_SPAN_SECONDS + "s");
    }

}