package com.mmdemirbas.urlbuilder

import java.net.URL
import java.nio.charset.Charset

typealias KeyValue = Pair<String, String>

/**
 * Fluent builder for [Url] instances or url strings. Mainly intended for usage from Java code.
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
class UrlBuilder {
    private lateinit var scheme  : String
    private lateinit var host    : String
    private          var port    : Int?                     = null
    private          val path    : MutableList<PathSegment> = mutableListOf()
    private          var query   : Query?                   = null
    private          var fragment: String?                  = null

                 fun setScheme  (scheme            : String                               ) = apply { this.scheme   = scheme                                    }
                 fun setHost    (host              : String                               ) = apply { this.host     = host                                      }
                 fun setPort    (port              : Int?                                 ) = apply { this.port     = port                                      }
    @SafeVarargs fun addPath    (path              : String, vararg matrixParams: KeyValue) = apply { this.path     += PathSegment(path, matrixParams.asList()) }
    @SafeVarargs fun addPaths   (vararg paths      : String                               ) = apply { this.path     += paths.map { PathSegment(it) }}
                 fun resetPath  (                                                         ) = apply { this.path.clear()                                         }
                 fun resetQuery (                                                         ) = apply { this.query    = null                                      }
    @SafeVarargs fun setQuery   (vararg queryParams: KeyValue                             ) = apply { this.query    = Query.Html4(queryParams.asList())               }
                 fun setQuery   (unstructuredQuery : String                               ) = apply { this.query    = Query.Unstructured(unstructuredQuery)     }
                 fun setFragment(fragment          : String?                              ) = apply { this.fragment = fragment                                  }

    fun toUrlString() = toUrl().toUrlString()
    fun toUrl() = Url(scheme, host, port, path, query, fragment)

    companion object {
        @JvmStatic
        fun <A, B> pair(a: A, b: B) = Pair(a, b)

        @JvmOverloads
        @JvmStatic
        fun from(url: URL, charset: Charset = Charsets.UTF_8) = from(url.toUrl(charset))

        @JvmOverloads
        @JvmStatic
        fun from(url: String, charset: Charset = Charsets.UTF_8) = from(url.toUrl(charset))

        @JvmStatic
        fun from(url: Url) = UrlBuilder().apply {
            scheme    = url.scheme
            host      = url.host
            port      = url.port
            path     += url.path
            query     = url.query
            fragment  = url.fragment
        }

        @JvmOverloads
        @JvmStatic
        fun from(scheme: String, host: String, port: Int? = null) = UrlBuilder().apply {
            this.scheme = scheme
            this.host   = host
            this.port   = port
        }
    }
}