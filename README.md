Use this library to safely create valid, correctly encoded URL strings with a fluent API.

[![codecov](https://codecov.io/gh/mmdemirbas/url-builder/branch/master/graph/badge.svg)](https://codecov.io/gh/mmdemirbas/url-builder)
[![Codecov](https://img.shields.io/codecov/c/github/mmdemirbas/url-builder.svg)](https://codecov.io/gh/mmdemirbas/url-builder)

[![Travis](https://api.travis-ci.org/mmdemirbas/url-builder.svg)](https://travis-ci.org/mmdemirbas/url-builder)
[![Travis](https://img.shields.io/travis/mmdemirbas/url-builder.svg)](https://travis-ci.org/mmdemirbas/url-builder)

[![Build Status](https://semaphoreci.com/api/v1/mmdemirbas/url-builder/branches/master/badge.svg)](https://semaphoreci.com/mmdemirbas/url-builder)
[![Project Status: Active The project has reached a stable, usable state and is being actively developed.](https://www.repostatus.org/badges/latest/active.svg)](https://www.repostatus.org/#active)

[![Scrutinizer](https://img.shields.io/scrutinizer/g/mmdemirbas/url-builder.svg)](https://scrutinizer-ci.com/g/mmdemirbas/url-builder/)
[![Scrutinizer Coverage](https://img.shields.io/scrutinizer/coverage/g/mmdemirbas/url-builder.svg)](https://scrutinizer-ci.com/g/mmdemirbas/url-builder/)
[![Scrutinizer Build](https://img.shields.io/scrutinizer/build/g/mmdemirbas/url-builder.svg)](https://scrutinizer-ci.com/g/mmdemirbas/url-builder)


# Usage

Artifacts are released in [Bintray](https://bintray.com/). For gradle, use the `jcenter()` repository. For maven, [go here](https://bintray.com/bintray/jcenter) and click "Set me up".

Add this to the `dependencies` block in your `build.gradle`:

```groovy
compile 'com.mmdemirbas:url-builder:VERSION'
```

where `VERSION` is the latest released version.  If you're using Maven, know that your life could be greatly improved by switching to Gradle and use this dependency block:

```xml
<dependency>
    <groupId>com.mmdemirbas</groupId>
    <artifactId>url-builder</artifactId>
    <version>VERSION</version>
</dependency>
```

# Example

# todo: add example of other factory methods

```java
import com.mmdemirbas.urlbuilder.UrlBuilder;
import static com.mmdemirbas.urlbuilder.UrlBuilder.pair;

// showcase the different encoding rules used on different URL components
UrlBuilder.from("http", "foo.com")
          .addPath("with spaces")
          .addPaths("path", "with", "varArgs")
          .addPath("&=?/", pair("matrix", "param?"))
          .setQuery(pair("fancy + name", "fancy?=value"))
          .setFragment("#?=")
          .buildUrlString());

// produces:
// http://foo.com/with%20spaces/path/with/varArgs/&=%3F%2F;matrix=param%3F?fancy%20%2B%20name=fancy?%3Dvalue#%23?=
```

# Motivation

See [this blog post](http://blog.palominolabs.com/2013/10/03/creating-urls-correctly-and-safely/) for a through explanation.

Ideally, the Java SDK would provide a good way to build properly encoded URLs. Unfortunately, it does not.

[`URLEncoder`](http://docs.oracle.com/javase/7/docs/api/java/net/URLEncoder.html) seems like a thing that you want to use, but amazingly enough it actually does HTML form encoding, not URL encoding.

URL encoding is also not something that can be done once you've formed a complete URL string. If your URL is already correctly encoded, you do not need to do anything. If it is not, it is impossible to parse it into its constituent parts for subsequent encoding. You must construct a url piece by piece, correctly encoding each piece as you go, to end up with a valid URL string. The encoding rules are also different for different parts of the URL (path, query param, etc.)

 Since the URLs that we use in practice for HTTP have somewhat different rules than "generic" URLs, UrlBuilder errs on the side of usefulness for HTTP-specific URLs. Notably, this means that '+' is percent-encoded to avoid being interpreted as a space. Also, in the URL/URI specs, the query string's format is not defined, but in practice it is used to hold `key=value` pairs separated by `&`.

# Building

Run `./gradlew build`.

# Spring URL handling behaviour

URL handling behaviour of Spring MVC tested with
[SpringTest.kt](src/test/java/com/mmdemirbas/urlbuilder/SpringTest.kt)
and results summarized
[here](src/test/java/com/mmdemirbas/urlbuilder/SpringTest.md).

# Todo

1. Setup JMH
2. Publish artifacts & update build link