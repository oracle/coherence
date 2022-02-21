/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.http;

/**
 * An exception used to indicate a failure status to the server.
 *
 * @author Jonathan Knight 2022.01.25
 * @since 22.06
 */
public class HttpException
        extends RuntimeException
    {
    /**
     * Create a HttpException.
     */
    public HttpException()
        {
        this(500);
        }

    /**
     * Create a HttpException.
     *
     * @param status  the failure status code - a http status code.
     */
    public HttpException(int status)
        {
        this.status = status;
        }

    /**
     * Create a HttpException.
     *
     * @param status    the failure status code - a http status code.
     * @param sMessage  the error message
     */
    public HttpException(int status, String sMessage)
        {
        super(sMessage);
        this.status = status;
        }

    /**
     * Create a HttpException.
     *
     * @param status  the failure status code - a http status code.
     * @param cause   the optional cause
     */
    public HttpException(int status, Throwable cause)
        {
        super(cause);
        this.status = status;
        }

    /**
     * Return the http status code.
     *
     * @return the http status code
     */
    public int getStatus()
        {
        return status;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The failure status code - a http status code.
     */
    private final int status;
    }
