/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.config.expression;

/**
 * A {@link ParameterResolver} provides a mechanism resolve and lookup named {@link Parameter}s.
 *
 * @author bo  2011.06.22
 * @since Coherence 12.1.2
 */
public interface ParameterResolver
    {
    /**
     * Obtains the specified named {@link Parameter}.
     *
     * @param sName  the name of the {@link Parameter}
     *
     * @return the {@link Parameter} or <code>null</code> if the {@link Parameter} can't be resolved
     */
    public Parameter resolve(String sName);
    }
