/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config;

import com.tangosol.config.expression.Parameter;
import com.tangosol.config.expression.ParameterResolver;

/**
 * An {@link ParameterList} is a strictly ordered and {@link Iterable} collection of {@link Parameter}s.
 *
 * @see Parameter
 * @see ParameterResolver
 *
 * @author bo  2011.09.14
 * @since Coherence 12.1.2
 */
public interface ParameterList
        extends Iterable<Parameter>
    {
    /**
     * Determines if there are any {@link Parameter}s in the {@link ParameterList}.
     *
     * @return <code>true</code> if there are {@link Parameter}s, <code>false</code> otherwise
     */
    public boolean isEmpty();

    /**
     * Obtains the number of {@link Parameter}s in the {@link ParameterList}.
     *
     * @return the number of {@link Parameter}s
     */
    public int size();

    /**
     * Adds a {@link Parameter} to the end of the {@link ParameterList} or replaces an existing {@link Parameter}
     * in the {@link ParameterList}.
     * <p>
     * Should a {@link Parameter} with the same name as the specified {@link Parameter} already exist in the list, the
     * specified {@link Parameter} will replace the existing {@link Parameter} in the list.
     *
     * @param parameter  the {@link Parameter} to add or replace
     */
    public void add(Parameter parameter);
    }
