/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder.storemanager;

import com.tangosol.io.BinaryStoreManager;

/**
 * A {@link BinaryStoreManagerBuilderCustomization} class is one that allows or potentially requires,
 * a {@link BinaryStoreManagerBuilder} for the purposes of realizing a {@link BinaryStoreManager}.
 *
 * @author bo  2012.02.10
 * @since Coherence 12.1.2
 */
public interface BinaryStoreManagerBuilderCustomization
    {
    /**
     * Obtains the {@link BinaryStoreManagerBuilder} for the {@link BinaryStoreManager}.
     *
     * @return the {@link BinaryStoreManagerBuilder}
     */
    public BinaryStoreManagerBuilder getBinaryStoreManagerBuilder();

    /**
     * Sets the {@link BinaryStoreManagerBuilder} for the {@link BinaryStoreManager}.
     *
     * @param bldr the {@link BinaryStoreManagerBuilder}
     */
    public void setBinaryStoreManagerBuilder(BinaryStoreManagerBuilder bldr);
    }
