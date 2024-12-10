/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.http;

/**
 * An interface that provides the path parameters for a http request.
 *
 * @author Jonathan Knight 2022.01.25
 * @since 22.06
 */
public interface PathParameters
    {
    /**
     * Returns the first path parameter value for the specified name,
     * or {@code null} if no parameter matched the name.
     *
     * @param sName  the name of the path parameter to obtain a value for
     *
     * @return the first path parameter value for the specified name,
     *         or {@code null} if no parameter matched the name
     */
    String getFirst(String sName);

    // ----- inner class: Empty ---------------------------------------------

    /**
     * An empty implementation of {@link PathParameters}.
     */
    class Empty
        implements PathParameters
        {
        /**
         * Private constructor for singleton.
         */
        private Empty()
            {
            }

        @Override
        public String getFirst(String sName)
            {
            return null;
            }
        }

    /**
     * The singleton instance of the empty {@link PathParameters}.
     */
    PathParameters EMPTY = new Empty();
    }
