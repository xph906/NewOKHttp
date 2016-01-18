Jan 14, 2016 11:57:56 AM okhttp3.sample.DebugClass main
INFO: start to prepare info
Jan 14, 2016 11:57:56 AM okhttp3.sample.DebugClass main
INFO: start to send requests
Jan 14, 2016 11:57:56 AM okhttp3.RealCall$ApplicationInterceptorChain proceed
INFO: DefaultInterceptor Sending request https://api.github.com/repos/square/okhttp/contributors
Jan 14, 2016 11:57:56 AM okhttp3.sample.LoggingInterceptor intercept
INFO: ApplicationInterceptor Sending request https://api.github.com/repos/square/okhttp/contributors on null
Jan 14, 2016 11:57:56 AM okhttp3.RealCall$ApplicationInterceptorChain proceed
INFO: DefaultInterceptor Sending request https://api.github.com/repos/square/okhttp/contributors
Jan 14, 2016 11:57:56 AM okhttp3.internal.http.HttpEngine sendRequest
INFO: HTTPEngine Search cache: 2ms
Jan 14, 2016 11:57:56 AM okhttp3.internal.http.StreamAllocation findConnection
INFO: findConnection:   1. searching pool: 0
Jan 14, 2016 11:57:57 AM okhttp3.internal.http.RouteSelector resetNextInetSocketAddress
INFO: DNS lookup delay 1109
Jan 14, 2016 11:57:57 AM okhttp3.internal.http.RouteSelector nextProxy
INFO: DNS/Proxy lookup/Select delay 1109
Jan 14, 2016 11:57:57 AM okhttp3.internal.http.StreamAllocation findConnection
INFO: find route(DNS) information: 1111
Jan 14, 2016 11:57:57 AM okhttp3.internal.http.StreamAllocation findConnection
INFO: findConnection:   2. create new connection: 408
Jan 14, 2016 11:57:57 AM okhttp3.internal.http.HttpEngine sendRequest
INFO: HTTPEngine connect: 1522ms
Jan 14, 2016 11:57:57 AM okhttp3.sample.LoggingInterceptor intercept
INFO: NetworkInterceptor Sending request https://api.github.com/repos/square/okhttp/contributors on Connection{api.github.com:443, proxy=DIRECT hostAddress=api.github.com/192.30.252.127:443 cipherSuite=TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA protocol=http/1.1}
Jan 14, 2016 11:57:58 AM okhttp3.sample.LoggingInterceptor intercept
INFO: NetworkInterceptor Received response for https://api.github.com/repos/square/okhttp/contributors in 86.2ms
Jan 14, 2016 11:57:58 AM okhttp3.RealCall getResponse
INFO: HTTPEngine sendRequest:1526, readResponse 93
Jan 14, 2016 11:57:58 AM okhttp3.RealCall$ApplicationInterceptorChain proceed
INFO: DefaultInterceptor WITHOUT Interceptor Received response https://api.github.com/repos/square/okhttp/contributors for 1627.5ms
Jan 14, 2016 11:57:58 AM okhttp3.sample.LoggingInterceptor intercept
INFO: ApplicationInterceptor Received response for https://api.github.com/repos/square/okhttp/contributors in 1629.4ms
Jan 14, 2016 11:57:58 AM okhttp3.RealCall$ApplicationInterceptorChain proceed
INFO: DefaultInterceptor  WITH  Interceptor Received response https://api.github.com/repos/square/okhttp/contributors for 1631.3ms
Jan 14, 2016 11:57:58 AM okhttp3.RealCall execute
INFO: RealCall: Latency of executing request: 1632 url:https://api.github.com/repos/square/okhttp/contributors
Jan 14, 2016 11:57:58 AM okhttp3.sample.DebugClass main
INFO: sent requests
Jan 14, 2016 11:57:58 AM okhttp3.sample.DebugClass main
INFO: received body
