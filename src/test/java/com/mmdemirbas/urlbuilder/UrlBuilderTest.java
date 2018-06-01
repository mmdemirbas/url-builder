package com.mmdemirbas.urlbuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static com.mmdemirbas.urlbuilder.UrlBuilder.pair;

class UrlBuilderTest {
    @Test
    void sample() {
        Assertions.assertEquals(
                "http://foo.com/with%20spaces/path/with/varArgs/&=%3F%2F;matrix=param%3F?fancy%20%2B%20name=fancy?%3Dvalue#%23?=",
                com.mmdemirbas.urlbuilder.UrlBuilder.from("http", "foo.com")
                                                    .addPath("with spaces")
                                                    .addPaths("path", "with", "varArgs")
                                                    .addPath("&=?/", pair("matrix", "param?"))
                                                    .setQuery(pair("fancy + name", "fancy?=value"))
                                                    .setFragment("#?=")
                                                    .buildUrlString());
    }
}
