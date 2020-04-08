/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder.storemanager;

import com.tangosol.coherence.config.ParameterList;
import com.tangosol.coherence.config.ResolvableParameterList;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.config.expression.Parameter;
import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.io.nio.DirectStoreManager;


/**
 * The NioMemoryManagerBuilder class builds an instance of a DirectStoreManager.
 *
 * @author pfm  2011.11.30
 * @since Coherence 12.1.2
 */
public class NioMemoryManagerBuilder
        extends AbstractNioManagerBuilder<DirectStoreManager>
    {
    // ----- StoreManagerBuilder interface ----------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public DirectStoreManager realize(ParameterResolver resolver, ClassLoader loader, boolean fPaged)
        {
        validate(resolver);

        DirectStoreManager                       manager       = null;

        int                                      cbMaxSize     = (int) getMaximumSize(resolver);
        int                                      cbInitialSize = (int) getInitialSize(resolver);

        ParameterizedBuilder<DirectStoreManager> bldrInstance  = getCustomBuilder();

        if (bldrInstance == null)
            {
            // create the NIO manager
            manager = new DirectStoreManager(cbInitialSize, cbMaxSize);
            }
        else
            {
            // create the custom object that is implementing DirectStoreManager.
            ParameterList listArgs = new ResolvableParameterList();

            listArgs.add(new Parameter("initial-size", cbInitialSize));
            listArgs.add(new Parameter("max-size", cbMaxSize));

            manager = bldrInstance.realize(resolver, loader, listArgs);
            }

        return manager;
        }
    }
