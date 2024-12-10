/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder.storemanager;

import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.io.BinaryStoreManager;


/**
 * The CustomStoreManagerBuilder class builds an instance of a custom
 * BinaryStoreManager.
 *
 * @author pfm  2011.11.30
 * @since Coherence 12.1.2
 */
public class CustomStoreManagerBuilder
        extends AbstractStoreManagerBuilder<BinaryStoreManager>
    {
    /**
     * {@inheritDoc}
     */
    @Override
    public BinaryStoreManager realize(ParameterResolver resolver, ClassLoader loader, boolean fPaged)
        {
        validate(resolver);

        ParameterizedBuilder<BinaryStoreManager> bldr = getCustomBuilder();

        return bldr.realize(resolver, loader, null);
        }
    }
