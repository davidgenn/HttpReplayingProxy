package com.github.davidgenn.httpreplayingproxy.proxy;

/**
 * How headers should be treated when matching requests.
 */
public enum MatchHeaders {

    /**
     * Don't match headers when looking for a previously cached response.
     */
    IGNORE_HEADERS,

    /**
     * Both header name and value must match when looking for a previously cached response.
     */
    MATCH_NAME_AND_VALUE,

    /**
     * Only header name must match when looking for a previously cached response.
     */
    MATCH_NAME_ONLY;
}
