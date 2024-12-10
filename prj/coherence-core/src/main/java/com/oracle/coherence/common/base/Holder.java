/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.base;


/**
 * A Holder is a reference like object, i.e. it simply holds another object.
 *
 * @param <V>  the value type
 *
 * @author mf  2010.12.2
 */
public interface Holder<V>
    {
    /**
     * Specify the held object.
     *
     * @param value  the object to hold
     */
    public void set(V value);

    /**
     * Return the held object.
     *
     * @return the held object
     */
    public V get();
    }
