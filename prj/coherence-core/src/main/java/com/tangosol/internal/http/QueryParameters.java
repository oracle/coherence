/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.http;

/**
 * An interface that provides the query parameters for a http request.
 *
 * @author Jonathan Knight 2022.01.25
 * @since 22.06
 */
public interface QueryParameters
    {
    /**
     * Returns the first query parameter value for the specified name,
     * or {@code null} if no parameter matched the name.
     *
     * @param sName  the name of the query parameter to obtain a value for
     *
     * @return the first query parameter value for the specified name,
     *         or {@code null} if no parameter matched the name
     */
    String getFirst(String sName);

    /**
     * Returns the first query parameter value for the specified name,
     * converted to a boolean value.
     *
     * @param sName  the name of the query parameter to obtain a value for
     *
     * @return the first query parameter value for the specified name,
     *         converted to a boolean value, or {@code false} if there is
     *         no parameter with the specified name
     */
    default boolean getFirstAsBoolean(String sName)
        {
        return Boolean.parseBoolean(getFirst(sName));
        }

    // ----- inner class: Empty ---------------------------------------------

    /**
     * An empty implementation of {@link QueryParameters}.
     */
    class Empty
        implements QueryParameters
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
    QueryParameters EMPTY = new Empty();
    }
