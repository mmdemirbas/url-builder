# JMH Benchmark Report
```
Benchmark                                                          Mode      Cnt         Score         Error  Units
BenchmarkTest.decodeLargeMix                                      thrpt       15      9935.295 ±     342.433  ops/s
BenchmarkTest.decodeSmallMix                                      thrpt       15   7934653.764 ± 1240391.953  ops/s
BenchmarkTest.encodeLargeMix                                      thrpt       15      2150.992 ±     217.036  ops/s
BenchmarkTest.encodeLargeSafe                                     thrpt       15     13284.980 ±     748.191  ops/s
BenchmarkTest.encodeLargeUnsafe                                   thrpt       15     13789.677 ±    1617.463  ops/s
BenchmarkTest.encodeSmallMix                                      thrpt       15   2085769.094 ±  249222.828  ops/s
BenchmarkTest.encodeSmallSafe                                     thrpt       15  10254851.453 ± 2044823.783  ops/s
BenchmarkTest.encodeSmallUnsafe                                   thrpt       15  12148524.286 ± 1532151.105  ops/s
BenchmarkTest.encodeTinyMix                                       thrpt       15   7310302.137 ±  435116.257  ops/s
BenchmarkTest.urlDecodeLargeMix                                   thrpt       15      3746.678 ±     113.121  ops/s
BenchmarkTest.urlDecodeSmallMix                                   thrpt       15   3348986.553 ±  305520.186  ops/s
BenchmarkTest.urlEncodeLargeMix                                   thrpt       15      2916.175 ±     187.095  ops/s
BenchmarkTest.urlEncodeSmallMix                                   thrpt       15   2690120.165 ±  145460.488  ops/s
BenchmarkTest.decodeLargeMix                                       avgt       15        ≈ 10⁻⁴                 s/op
BenchmarkTest.decodeSmallMix                                       avgt       15        ≈ 10⁻⁶                 s/op
BenchmarkTest.encodeLargeMix                                       avgt       15         0.002 ±       0.001   s/op
BenchmarkTest.encodeLargeSafe                                      avgt       15        ≈ 10⁻⁴                 s/op
BenchmarkTest.encodeLargeUnsafe                                    avgt       15        ≈ 10⁻⁴                 s/op
BenchmarkTest.encodeSmallMix                                       avgt       15        ≈ 10⁻⁶                 s/op
BenchmarkTest.encodeSmallSafe                                      avgt       15        ≈ 10⁻⁷                 s/op
BenchmarkTest.encodeSmallUnsafe                                    avgt       15        ≈ 10⁻⁷                 s/op
BenchmarkTest.encodeTinyMix                                        avgt       15        ≈ 10⁻⁶                 s/op
BenchmarkTest.urlDecodeLargeMix                                    avgt       15         0.001 ±       0.001   s/op
BenchmarkTest.urlDecodeSmallMix                                    avgt       15        ≈ 10⁻⁶                 s/op
BenchmarkTest.urlEncodeLargeMix                                    avgt       15         0.001 ±       0.001   s/op
BenchmarkTest.urlEncodeSmallMix                                    avgt       15        ≈ 10⁻⁶                 s/op
BenchmarkTest.decodeLargeMix                                     sample   135412        ≈ 10⁻³                 s/op
BenchmarkTest.decodeLargeMix:decodeLargeMix            ·p0.00    sample                 ≈ 10⁻⁴                 s/op
BenchmarkTest.decodeLargeMix:decodeLargeMix            ·p0.50    sample                 ≈ 10⁻⁴                 s/op
BenchmarkTest.decodeLargeMix:decodeLargeMix            ·p0.90    sample                 ≈ 10⁻³                 s/op
BenchmarkTest.decodeLargeMix:decodeLargeMix            ·p0.95    sample                  0.001                 s/op
BenchmarkTest.decodeLargeMix:decodeLargeMix            ·p0.99    sample                  0.001                 s/op
BenchmarkTest.decodeLargeMix:decodeLargeMix            ·p0.999   sample                  0.001                 s/op
BenchmarkTest.decodeLargeMix:decodeLargeMix            ·p0.9999  sample                  0.003                 s/op
BenchmarkTest.decodeLargeMix:decodeLargeMix            ·p1.00    sample                  0.004                 s/op
BenchmarkTest.decodeSmallMix                                     sample  1313714        ≈ 10⁻⁶                 s/op
BenchmarkTest.decodeSmallMix:decodeSmallMix            ·p0.00    sample                 ≈ 10⁻⁹                 s/op
BenchmarkTest.decodeSmallMix:decodeSmallMix            ·p0.50    sample                 ≈ 10⁻⁶                 s/op
BenchmarkTest.decodeSmallMix:decodeSmallMix            ·p0.90    sample                 ≈ 10⁻⁶                 s/op
BenchmarkTest.decodeSmallMix:decodeSmallMix            ·p0.95    sample                 ≈ 10⁻⁶                 s/op
BenchmarkTest.decodeSmallMix:decodeSmallMix            ·p0.99    sample                 ≈ 10⁻⁶                 s/op
BenchmarkTest.decodeSmallMix:decodeSmallMix            ·p0.999   sample                 ≈ 10⁻⁵                 s/op
BenchmarkTest.decodeSmallMix:decodeSmallMix            ·p0.9999  sample                 ≈ 10⁻⁴                 s/op
BenchmarkTest.decodeSmallMix:decodeSmallMix            ·p1.00    sample                  0.002                 s/op
BenchmarkTest.encodeLargeMix                                     sample    30439         0.001 ±       0.001   s/op
BenchmarkTest.encodeLargeMix:encodeLargeMix·p0.00                sample                  0.001                 s/op
BenchmarkTest.encodeLargeMix:encodeLargeMix·p0.50                sample                  0.001                 s/op
BenchmarkTest.encodeLargeMix:encodeLargeMix·p0.90                sample                  0.002                 s/op
BenchmarkTest.encodeLargeMix:encodeLargeMix·p0.95                sample                  0.002                 s/op
BenchmarkTest.encodeLargeMix:encodeLargeMix·p0.99                sample                  0.003                 s/op
BenchmarkTest.encodeLargeMix:encodeLargeMix·p0.999               sample                  0.003                 s/op
BenchmarkTest.encodeLargeMix:encodeLargeMix·p0.9999              sample                  0.005                 s/op
BenchmarkTest.encodeLargeMix:encodeLargeMix·p1.00                sample                  0.005                 s/op
BenchmarkTest.encodeLargeSafe                                    sample   184162        ≈ 10⁻⁴                 s/op
BenchmarkTest.encodeLargeSafe:encodeLargeSafe·p0.00              sample                 ≈ 10⁻⁴                 s/op
BenchmarkTest.encodeLargeSafe:encodeLargeSafe·p0.50              sample                 ≈ 10⁻⁴                 s/op
BenchmarkTest.encodeLargeSafe:encodeLargeSafe·p0.90              sample                 ≈ 10⁻⁴                 s/op
BenchmarkTest.encodeLargeSafe:encodeLargeSafe·p0.95              sample                 ≈ 10⁻³                 s/op
BenchmarkTest.encodeLargeSafe:encodeLargeSafe·p0.99              sample                 ≈ 10⁻³                 s/op
BenchmarkTest.encodeLargeSafe:encodeLargeSafe·p0.999             sample                  0.001                 s/op
BenchmarkTest.encodeLargeSafe:encodeLargeSafe·p0.9999            sample                  0.002                 s/op
BenchmarkTest.encodeLargeSafe:encodeLargeSafe·p1.00              sample                  0.005                 s/op
BenchmarkTest.encodeLargeUnsafe                                  sample   105926        ≈ 10⁻³                 s/op
BenchmarkTest.encodeLargeUnsafe:encodeLargeUnsafe·p0.00          sample                 ≈ 10⁻⁴                 s/op
BenchmarkTest.encodeLargeUnsafe:encodeLargeUnsafe·p0.50          sample                 ≈ 10⁻³                 s/op
BenchmarkTest.encodeLargeUnsafe:encodeLargeUnsafe·p0.90          sample                  0.001                 s/op
BenchmarkTest.encodeLargeUnsafe:encodeLargeUnsafe·p0.95          sample                  0.001                 s/op
BenchmarkTest.encodeLargeUnsafe:encodeLargeUnsafe·p0.99          sample                  0.002                 s/op
BenchmarkTest.encodeLargeUnsafe:encodeLargeUnsafe·p0.999         sample                  0.011                 s/op
BenchmarkTest.encodeLargeUnsafe:encodeLargeUnsafe·p0.9999        sample                  0.025                 s/op
BenchmarkTest.encodeLargeUnsafe:encodeLargeUnsafe·p1.00          sample                  0.030                 s/op
BenchmarkTest.encodeSmallMix                                     sample  1080755        ≈ 10⁻⁶                 s/op
BenchmarkTest.encodeSmallMix:encodeSmallMix·p0.00                sample                 ≈ 10⁻⁶                 s/op
BenchmarkTest.encodeSmallMix:encodeSmallMix·p0.50                sample                 ≈ 10⁻⁶                 s/op
BenchmarkTest.encodeSmallMix:encodeSmallMix·p0.90                sample                 ≈ 10⁻⁶                 s/op
BenchmarkTest.encodeSmallMix:encodeSmallMix·p0.95                sample                 ≈ 10⁻⁵                 s/op
BenchmarkTest.encodeSmallMix:encodeSmallMix·p0.99                sample                 ≈ 10⁻⁵                 s/op
BenchmarkTest.encodeSmallMix:encodeSmallMix·p0.999               sample                 ≈ 10⁻⁴                 s/op
BenchmarkTest.encodeSmallMix:encodeSmallMix·p0.9999              sample                  0.001                 s/op
BenchmarkTest.encodeSmallMix:encodeSmallMix·p1.00                sample                  0.014                 s/op
BenchmarkTest.encodeSmallSafe                                    sample  1260493        ≈ 10⁻⁶                 s/op
BenchmarkTest.encodeSmallSafe:encodeSmallSafe·p0.00              sample                 ≈ 10⁻⁹                 s/op
BenchmarkTest.encodeSmallSafe:encodeSmallSafe·p0.50              sample                 ≈ 10⁻⁶                 s/op
BenchmarkTest.encodeSmallSafe:encodeSmallSafe·p0.90              sample                 ≈ 10⁻⁶                 s/op
BenchmarkTest.encodeSmallSafe:encodeSmallSafe·p0.95              sample                 ≈ 10⁻⁶                 s/op
BenchmarkTest.encodeSmallSafe:encodeSmallSafe·p0.99              sample                 ≈ 10⁻⁶                 s/op
BenchmarkTest.encodeSmallSafe:encodeSmallSafe·p0.999             sample                 ≈ 10⁻⁵                 s/op
BenchmarkTest.encodeSmallSafe:encodeSmallSafe·p0.9999            sample                  0.001                 s/op
BenchmarkTest.encodeSmallSafe:encodeSmallSafe·p1.00              sample                  0.021                 s/op
BenchmarkTest.encodeSmallUnsafe                                  sample  1267549        ≈ 10⁻⁶                 s/op
BenchmarkTest.encodeSmallUnsafe:encodeSmallUnsafe·p0.00          sample                 ≈ 10⁻⁹                 s/op
BenchmarkTest.encodeSmallUnsafe:encodeSmallUnsafe·p0.50          sample                 ≈ 10⁻⁶                 s/op
BenchmarkTest.encodeSmallUnsafe:encodeSmallUnsafe·p0.90          sample                 ≈ 10⁻⁶                 s/op
BenchmarkTest.encodeSmallUnsafe:encodeSmallUnsafe·p0.95          sample                 ≈ 10⁻⁶                 s/op
BenchmarkTest.encodeSmallUnsafe:encodeSmallUnsafe·p0.99          sample                 ≈ 10⁻⁶                 s/op
BenchmarkTest.encodeSmallUnsafe:encodeSmallUnsafe·p0.999         sample                 ≈ 10⁻⁵                 s/op
BenchmarkTest.encodeSmallUnsafe:encodeSmallUnsafe·p0.9999        sample                 ≈ 10⁻⁴                 s/op
BenchmarkTest.encodeSmallUnsafe:encodeSmallUnsafe·p1.00          sample                  0.009                 s/op
BenchmarkTest.encodeTinyMix                                      sample  1446115        ≈ 10⁻⁶                 s/op
BenchmarkTest.encodeTinyMix:encodeTinyMix·p0.00                  sample                 ≈ 10⁻⁹                 s/op
BenchmarkTest.encodeTinyMix:encodeTinyMix·p0.50                  sample                 ≈ 10⁻⁶                 s/op
BenchmarkTest.encodeTinyMix:encodeTinyMix·p0.90                  sample                 ≈ 10⁻⁶                 s/op
BenchmarkTest.encodeTinyMix:encodeTinyMix·p0.95                  sample                 ≈ 10⁻⁶                 s/op
BenchmarkTest.encodeTinyMix:encodeTinyMix·p0.99                  sample                 ≈ 10⁻⁶                 s/op
BenchmarkTest.encodeTinyMix:encodeTinyMix·p0.999                 sample                 ≈ 10⁻⁵                 s/op
BenchmarkTest.encodeTinyMix:encodeTinyMix·p0.9999                sample                 ≈ 10⁻⁴                 s/op
BenchmarkTest.encodeTinyMix:encodeTinyMix·p1.00                  sample                  0.017                 s/op
BenchmarkTest.urlDecodeLargeMix                                  sample    41995         0.001 ±       0.001   s/op
BenchmarkTest.urlDecodeLargeMix:urlDecodeLargeMix·p0.00          sample                  0.001                 s/op
BenchmarkTest.urlDecodeLargeMix:urlDecodeLargeMix·p0.50          sample                  0.001                 s/op
BenchmarkTest.urlDecodeLargeMix:urlDecodeLargeMix·p0.90          sample                  0.002                 s/op
BenchmarkTest.urlDecodeLargeMix:urlDecodeLargeMix·p0.95          sample                  0.002                 s/op
BenchmarkTest.urlDecodeLargeMix:urlDecodeLargeMix·p0.99          sample                  0.003                 s/op
BenchmarkTest.urlDecodeLargeMix:urlDecodeLargeMix·p0.999         sample                  0.015                 s/op
BenchmarkTest.urlDecodeLargeMix:urlDecodeLargeMix·p0.9999        sample                  0.043                 s/op
BenchmarkTest.urlDecodeLargeMix:urlDecodeLargeMix·p1.00          sample                  0.051                 s/op
BenchmarkTest.urlDecodeSmallMix                                  sample  1394250        ≈ 10⁻⁶                 s/op
BenchmarkTest.urlDecodeSmallMix:urlDecodeSmallMix·p0.00          sample                 ≈ 10⁻⁹                 s/op
BenchmarkTest.urlDecodeSmallMix:urlDecodeSmallMix·p0.50          sample                 ≈ 10⁻⁶                 s/op
BenchmarkTest.urlDecodeSmallMix:urlDecodeSmallMix·p0.90          sample                 ≈ 10⁻⁶                 s/op
BenchmarkTest.urlDecodeSmallMix:urlDecodeSmallMix·p0.95          sample                 ≈ 10⁻⁶                 s/op
BenchmarkTest.urlDecodeSmallMix:urlDecodeSmallMix·p0.99          sample                 ≈ 10⁻⁶                 s/op
BenchmarkTest.urlDecodeSmallMix:urlDecodeSmallMix·p0.999         sample                 ≈ 10⁻⁴                 s/op
BenchmarkTest.urlDecodeSmallMix:urlDecodeSmallMix·p0.9999        sample                  0.001                 s/op
BenchmarkTest.urlDecodeSmallMix:urlDecodeSmallMix·p1.00          sample                  0.028                 s/op
BenchmarkTest.urlEncodeLargeMix                                  sample    30803         0.001 ±       0.001   s/op
BenchmarkTest.urlEncodeLargeMix:urlEncodeLargeMix·p0.00          sample                  0.001                 s/op
BenchmarkTest.urlEncodeLargeMix:urlEncodeLargeMix·p0.50          sample                  0.001                 s/op
BenchmarkTest.urlEncodeLargeMix:urlEncodeLargeMix·p0.90          sample                  0.002                 s/op
BenchmarkTest.urlEncodeLargeMix:urlEncodeLargeMix·p0.95          sample                  0.003                 s/op
BenchmarkTest.urlEncodeLargeMix:urlEncodeLargeMix·p0.99          sample                  0.005                 s/op
BenchmarkTest.urlEncodeLargeMix:urlEncodeLargeMix·p0.999         sample                  0.018                 s/op
BenchmarkTest.urlEncodeLargeMix:urlEncodeLargeMix·p0.9999        sample                  0.028                 s/op
BenchmarkTest.urlEncodeLargeMix:urlEncodeLargeMix·p1.00          sample                  0.032                 s/op
BenchmarkTest.urlEncodeSmallMix                                  sample  1189560        ≈ 10⁻⁶                 s/op
BenchmarkTest.urlEncodeSmallMix:urlEncodeSmallMix·p0.00          sample                 ≈ 10⁻⁸                 s/op
BenchmarkTest.urlEncodeSmallMix:urlEncodeSmallMix·p0.50          sample                 ≈ 10⁻⁶                 s/op
BenchmarkTest.urlEncodeSmallMix:urlEncodeSmallMix·p0.90          sample                 ≈ 10⁻⁶                 s/op
BenchmarkTest.urlEncodeSmallMix:urlEncodeSmallMix·p0.95          sample                 ≈ 10⁻⁵                 s/op
BenchmarkTest.urlEncodeSmallMix:urlEncodeSmallMix·p0.99          sample                 ≈ 10⁻⁵                 s/op
BenchmarkTest.urlEncodeSmallMix:urlEncodeSmallMix·p0.999         sample                 ≈ 10⁻⁴                 s/op
BenchmarkTest.urlEncodeSmallMix:urlEncodeSmallMix·p0.9999        sample                  0.002                 s/op
BenchmarkTest.urlEncodeSmallMix:urlEncodeSmallMix·p1.00          sample                  0.030                 s/op
BenchmarkTest.decodeLargeMix                                         ss       15         0.001 ±       0.001   s/op
BenchmarkTest.decodeSmallMix                                         ss       15        ≈ 10⁻⁵                 s/op
BenchmarkTest.encodeLargeMix                                         ss       15         0.002 ±       0.001   s/op
BenchmarkTest.encodeLargeSafe                                        ss       15        ≈ 10⁻³                 s/op
BenchmarkTest.encodeLargeUnsafe                                      ss       15         0.001 ±       0.001   s/op
BenchmarkTest.encodeSmallMix                                         ss       15        ≈ 10⁻⁴                 s/op
BenchmarkTest.encodeSmallSafe                                        ss       15        ≈ 10⁻⁵                 s/op
BenchmarkTest.encodeSmallUnsafe                                      ss       15        ≈ 10⁻⁵                 s/op
BenchmarkTest.encodeTinyMix                                          ss       15        ≈ 10⁻⁵                 s/op
BenchmarkTest.urlDecodeLargeMix                                      ss       15         0.004 ±       0.001   s/op
BenchmarkTest.urlDecodeSmallMix                                      ss       15        ≈ 10⁻⁴                 s/op
BenchmarkTest.urlEncodeLargeMix                                      ss       15         0.008 ±       0.006   s/op
BenchmarkTest.urlEncodeSmallMix                                      ss       15        ≈ 10⁻⁴                 s/op
```