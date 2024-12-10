/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.base;


/**
 * The Associator interface facilitates the creation of a very generic
 * <a href="http://en.wikipedia.org/wiki/Equivalence_relation">
 * equivalence relation</a> between different objects and allows to group them
 * based on the equality of the "association key" object returned by the
 * {@link #getAssociatedKey} method.
 *
 * @author gg 2012.03.11
 *
 * @see Associated
 */
public interface Associator
    {
    /**
     * Determine the host key (or base) object to which the specified object is
     * associated.
     * <p>
     * <b>Note:</b> It's expected that the returned object is suitable to be used
     * as an immutable identity (e.g. a key in a Map).
     * <br>
     * <b>Note 2:</b> Circular associations are not permitted.

     * @param o  the object to obtain an association for
     *
     * @return the host key that for this object, or null if this object has no
     *         association
     */
    public Object getAssociatedKey(Object o);
    }