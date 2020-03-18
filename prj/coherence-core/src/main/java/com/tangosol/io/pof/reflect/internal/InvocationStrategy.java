/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof.reflect.internal;

/**
 * An {@link InvocationStrategy} provides an abstraction of the underlying
 * mechanisms used to retrieve and set a property's value.
 *
 * @author hr
 *
 * @since 3.7.1
 *
 * @param <T>  the containing type
 * @param <P>  the property's type
 */
public interface InvocationStrategy<T,P>
    {
    /**
     * Returns the value of the property.
     *
     * @param container  container of this and all other properties
     * 
     * @return property value
     */
    public P get(T container);

    /**
     * Sets the parameter value to the property.
     * 
     * @param container  container of this and all other sibling properties
     * @param value      new value to assign to the property
     */
    public void set(T container, P value);
    }
