[![Build Status](https://travis-ci.org/davidgenn/HttpReplayingProxy.svg?branch=master)](https://travis-ci.org/davidgenn/HttpReplayingProxy)
# HttpReplayingProxy
## Introduction
The HttpReplayingProxy is a library that allows you to mock HTTP requests with pre-recorded responses and then keep these responses uptodate. 

HttpReplayingProxy is a webapp that can proxy calls to the 'real' URL and cache the results in a directory. 

The next time a call is made to this URL and the path, query parameters, body and headers match, the cached results will be returned. 

The cached result can be included in your project's test tree as test artifacts.

This means your component/integration tests can test against realistic data but do not require the downstream applications to be available. This removes a test time dependency on these downstream applications.

The cache can be reset at any time, allowing the fixtures to be refreshed and therefore kept current.

## What problem are we trying to solve?
Often times your application will make HTTP calls to another service. We may want to mock these calls in component tests so that we don't have a dependency on these apps when we're running our builds.

There a lots of libraries that allow us to mock these dependencies, but it's difficult to ensure that these mocked responses stay up to date. Eg. what happens if the validation of the request changes or the format of the response changes.

HttpReplayingProxy allows you to cache the responses, and save these test fixtures to disk (eg in the test tree of your project). Crucially, these fixtures can be refreshed periodically by setting an expiry time on them or explicitly clearing the cache. 

## Getting started     
Add the dependency to your project:

    <dependency>
        <groupId>com.github.davidgenn</groupId>
    	<artifactId>http-replaying-proxy</artifactId>
    	<version>0.1.0-SNAPSHOT</version>
    	<scope>test</scope>
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

## How do you keep the cached responses up to date?
You can either:

* Set a Time To Live on the cache configuration `new HttpReplayingProxyConfiguration().timeToLiveForCacheInSeconds(86400) // One day `
* Or you can rest the cache manually

You can reset the cache in two ways:

    FileBasedCache.reset("/some/directory/to/cache/in");

Or by setting the `reset.httpreplayingproxy.cache` System property to true. This clears the cache when the server starts. 

## How are headers treated?
You can decide how headers should be treated when looking for a previously cached result:

     new HttpReplayingProxyConfiguration()
       .treatHeaders(MatchHeaders.MATCH_NAME_AND_VALUE)
       ...
       
`MatchHeader` can take the following values:

`MATCH_NAME_AND_VALUE` The name and value of the headers must match.

`MATCH_NAME_ONLY` Only the name of the headers must match, the values are ignored.

`IGNORE_HEADERS` The headers are ignored completely.
     
## How does it work?
HttpReplayingProxy creates a Jetty server at `localhost` on whatever port you specify. This webapp proxies calls through to the specified URL and caches the result. 
The next time a call is made to this URL and the path, query parameters, body and headers match, the cached results will be returned.

## How does HTTP Replaying Proxy differ from [WireMock](http://wiremock.org/) or [Betamax](http://freeside.co/betamax/)?
There are lots of other fantastic HTTP mocking libraries out there. [Wiremock](http://wiremock.org/) and [Betamax](http://freeside.co/betamax/) are just two of them. 

HttpReplayingProxy's main difference is that it caches the real response from the actual server being mocked but then allows these cached responses to be periodically refreshed. 
This ensures that the cached responses can be trusted - they reflect what the real server would return and because they get updated, any changes to the behaviour of the downstream server get picked up. 
Eg. if the format of the response JSON is changed.

## How mature is this library?
HttpReplayingProxy is still in active development so there may well be teething problems. If you discover any, please let me know!

Thanks, David Genn davidggenn@gmail.com




