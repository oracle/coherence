/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.net.exabus;


/**
 * EndPoint provides an opaque representation of an address for a Bus.
 *
 * @author mf  2010.10.04
 */
public interface EndPoint
    {
    /**
     * Return the string representation of the EndPoint.
     *
     * @return the string representation of the EndPoint
     */
    public String getCanonicalName();

    /**
     * Return true iff the specified object in an EndPoint representing the
     * same bus as this EndPoint.
     *
     * @param o  the EndPoint to compare against
     *
     * @return true if the specified EndPoint is equal to this EndPoint
     */
    public boolean equals(Object o);

    /**
     * {@inheritDoc}
     */
    public int hashCode();
    }
