/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder;


/**
 * A class that implements {@link BuilderCustomization} is one that allows an alternate
 * builder, as a {@link ParameterizedBuilder}, to be provided so that the said class
 * may use it for realizing objects.
 *
 * @author pfm  2011.11.30
 * @since Coherence 12.1.2
 */
public interface BuilderCustomization<T>
    {
    /**
     * Obtains the custom {@link ParameterizedBuilder}.
     *
     * @return the {@link ParameterizedBuilder}
     */
    public ParameterizedBuilder<T> getCustomBuilder();

    /**
     * Sets the {@link ParameterizedBuilder} to be used as the alternate builder.
     *
     * @param bldr  the ParameterizedBuilder
     */
    public void setCustomBuilder(ParameterizedBuilder<T> bldr);
    }
