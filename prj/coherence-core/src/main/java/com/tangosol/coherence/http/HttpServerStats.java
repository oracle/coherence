/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.http;

/**
 * Defines basic statistics that can be collected by an HTTP server implementation.
 *
 * @author tam  2015.08.22
 * @since  12.2.1.1
 */
public interface HttpServerStats
    {
    /**
     * Return the number of requests serviced since the HTTP server was started
     * or the statistics were reset.
     *
     * @return the number of requests services
     */
    public long getRequestCount();

    /**
     * Return the average processing time in milliseconds.
     *
     * @return the average processing time in milliseconds
     */
    public float getAverageRequestTime();

    /**
     * Return the number of requests per second since the statistics were reset.
     *
     * @return the number of requests per second since the statistics were reset
     */
    public float getRequestsPerSecond();

    /**
     * Return the number of requests that caused errors.
     *
     * @return the number of requests that caused errors
     */
    public long getErrorCount();

    /**
     * Return the count of Http status codes with the given prefix.
     * E.g. 1=100-199, 2=200-299, 3=300-399, 4=400-499, 5=500-599
     *
     * @param nPrefix  the prefix to return count for
     *
     * @return the count of Http status codes with the given prefix
     */
    public long getHttpStatusCount(int nPrefix);

    /**
     * Reset the statistics.
     */
    public void resetStats();
    }
