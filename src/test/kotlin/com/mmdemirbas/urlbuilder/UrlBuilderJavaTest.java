package com.mmdemirbas.urlbuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URL;

import static com.mmdemirbas.urlbuilder.UrlBuilder.pair;

final class UrlBuilderJavaTest {
    private final String URL = "http://foo.com/with%20spaces/path/with/varArgs/&=%3F%2F;matrix=param%3F?fancy%20%2B%20name=fancy?%3Dvalue#%23?=";

    @Test
    void fromSchemeAndHost() {
        Assertions.assertEquals(URL,
                                UrlBuilder.from("http", "foo.com")
                                          .addPath("with spaces")
                                          .addPaths("path", "with", "varArgs")
                                          .addPath("&=?/", pair("matrix", "param?"))
                                          .setQuery(pair("fancy + name", "fancy?=value"))
                                          .setFragment("#?=")
                                          .toUrlString());
    }

    @Test
    void fromString() {
        Assertions.assertEquals(URL, UrlBuilder.from(URL).toUrlString());
    }

    @Test
    void fromURL() throws Exception {
        Assertions.assertEquals(URL, UrlBuilder.from(new URL(URL)).toUrlString());
    }
}
