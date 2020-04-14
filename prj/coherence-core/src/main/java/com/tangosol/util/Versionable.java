/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.util;


/**
* An interface for versionable data.
*
* @param <T>  the type of the version indicator
 *
* @author cp  2000.10.20
*/
public interface Versionable<T extends Comparable<? super T>>
    {
    /**
    * Get the version indicator for this object. The version indicator
    * should be an immutable object or one treated as an immutable, which
    * is to say that after the version is incremented, the previous version's
    * indicator reference will never be returned again.
    *
    * @return a non-null version value that implements the Comparable
    *         interface
    */
    public T getVersionIndicator();

    /**
    * Update the version to the next logical version indicator.
    *
    * @throws UnsupportedOperationException  if the object is immutable or
    *         if the object does not know how to increment its own version
    *         indicator
    */
    public void incrementVersion();

    /**
     * Return the value specifying if the versioning is enabled.
     *
     * @return a value specifying if the versioning is enabled
     */
    public default boolean isVersioningEnabled()
        {
        return getVersionIndicator() != null;
        }
    }