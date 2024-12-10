/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest;

/**
 * An interface that must be implemented by key converters.
 * <p>
 * Key converters are used to convert cache entry keys to string and string
 * representations of the keys that are used in RESTful URLs into
 * appropriate object instance that can be used to access cache entries.
 *
 * @author as  2011.06.08
*/
public interface KeyConverter
    {
    /**
     * Convert a string representation of a key into its object form.
     *
     * @param sKey  key as a string
     *
     * @return key in its object form
     */
    public Object fromString(String sKey);

    /**
     * Convert an object representation of a key into its string form.
     *
     * @param oKey  key in its original object form
     *
     * @return string representation of a key
     */
    String toString(Object oKey);
    }
