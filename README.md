# HttpReplayingProxy
## Introduction
The HttpReplayingProxy is a library that allows you to mock HTTP requests with realistic responses. 

HttpReplayingProxy is a webapp that can proxy calls to the 'real' URL and cache the results in a directory. The next time a call is made, the cached result is returned. 

The cached result can be included in your project's test tree as test artifacts.

This means your component/integration tests can test against realistic data but do not require the downstream applications to be available.

The cache can be reset at any time, allowing the fixtures to be refreshed and therefore kept current.
## Getting started
Checkout the source and install it:

     mvn clean install
     
Add the dependency to your project:

    <dependency>
        <groupId>com.httpreplayingproxy</groupId>
    	<artifactId>http-replaying-proxy</artifactId>
    	<version>0.1.0-SNAPSHOT</version>
    </dependency>
    
Use the following in your test:

    HttpReplayingProxyConfiguration configuration =
                new HttpReplayingProxyConfiguration()
                        .urlToProxyTo("https://service-I-call.com")
                        .portToHostOn(8585)
                        .withRootDirectoryForCache("/some/directory/to/cache/in");

    server = new HttpReplayingProxy(configuration).start();

Your application should be configured to call `http://localhost:8585` instead of `https://service-I-call.com`.

The responses will be cached in `/some/directory/to/cache/in`. This could be the test directory of your project and then the fixtures can be committed alongside your tests.

To reset the cache:

    HttpReplayingProxy.reset("/some/directory/to/cache/in");
     
## How does it work?
HttpReplayingProxy creates a Jetty server at `localhost` on whatever port you specify.
## How mature is this library?
HttpReplayingProxy is still in active development so there may well be teething problems. If you discover any, please let me know!

Thanks, David Genn davidggenn@gmail.com




