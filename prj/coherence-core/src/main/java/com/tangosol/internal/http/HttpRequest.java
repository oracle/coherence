/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.http;

import com.tangosol.util.ResourceRegistry;

import java.io.IOException;

import java.io.InputStream;
import java.net.URI;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

/**
 * A simple http request.
 *
 * @author Jonathan Knight 2022.01.25
 * @since 22.06
 */
public interface HttpRequest
    {
    /**
     * Returns the request method.
     *
     * @return the request method
     */
    HttpMethod getMethod();

    /**
     * Returns the base request URI.
     *
     * @return the base request URI.
     */
    URI getBaseURI();

    /**
     * Returns the request {@link URI}.
     *
     * @return the request {@link URI}
     */
    URI getRequestURI();

    /**
     * Get a message header as a single string value.
     *
     * @param name the message header.
     * @return the message header value. If the message header is not present then
     *         {@code null} is returned. If the message header is present but has no
     *         value then the empty string is returned. If the message header is present
     *         more than once then the values of joined together and separated by a ','
     *         character.
     */
    String getHeaderString(String name);

    /**
     * Returns the request {@link QueryParameters}.
     * <p>
     * This method will always return a {@link QueryParameters} instance,
     * even if there were no query parameters.
     *
     * @return the request {@link QueryParameters}
     */
    QueryParameters getQueryParameters();

    /**
     * Returns the first query parameter value for the specified name,
     * or {@code null} if no parameter matched the name.
     *
     * @param sName  the name of the query parameter to obtain a value for
     *
     * @return the first query parameter value for the specified name,
     *         or {@code null} if no parameter matched the name
     */
    default String getFirstQueryParameter(String sName)
        {
        return getQueryParameters().getFirst(sName);
        }

    /**
     * Set the request {@link PathParameters}.
     *
     * @param parameters  the request {@link PathParameters}
     */
    void setPathParameters(PathParameters parameters);

    /**
     * Returns the request {@link PathParameters}.
     * <p>
     * This method will always return a {@link PathParameters} instance,
     * even if there were no path parameters.
     *
     * @return the request {@link PathParameters}
     */
    PathParameters getPathParameters();

    /**
     * Returns the first path parameter value for the specified name,
     * or {@code null} if no parameter matched the name.
     *
     * @param sName  the name of the path parameter to obtain a value for
     *
     * @return the first path parameter value for the specified name,
     *         or {@code null} if no parameter matched the name
     */
    default String getFirstPathParameter(String sName)
        {
        return getPathParameters().getFirst(sName);
        }

    /**
     * Return the request body deserialized into a map.
     *
     * @return  the request body deserialized into a map
     */
    InputStream getBody();

    /**
     * Returns the request body, using the specified json parser function.
     *
     * @param fnParser  the parser function
     *
     * @return  the request body
     */
    Map<String, Object> getJsonBody(Function<InputStream, Map<String, Object>> fnParser);

    /**
     * Return the {@link ResourceRegistry} used to register resources
     * with a request that can later be accessed by request handlers.
     *
     * @return the request {@link ResourceRegistry}
     */
    ResourceRegistry getResourceRegistry();
    }
