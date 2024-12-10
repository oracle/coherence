/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package opentracing;

/**
 * Enumeration of Jaeger configuration properties.
 *
 * @author rl 2019
 * @since 14.1.1.0
 */
public enum JaegerConfigProperties
    {
    /**
     * Jaeger service name.
     */
    SERVICE_NAME,

     /**
      * Jaeger endpoint.
      */
    ENDPOINT;

    // ----- Object methods ---------------------------------------------

    @Override
    public String toString()
        {
        return PREFIX + super.toString();
        }

    /**
     * Prefix that will be added to do the {@link #toString()} representation of enumerates.
     */
    private static final String PREFIX = "JAEGER_";
    }
