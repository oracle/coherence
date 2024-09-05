/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;


import com.oracle.coherence.common.base.Classes;

import com.tangosol.coherence.config.builder.MapBuilder;

import com.tangosol.config.expression.NullParameterResolver;
import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.internal.net.ConfigurableCacheFactorySession;

import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.NamedQueue;
import com.tangosol.net.QueueService;
import com.tangosol.net.Session;
import com.tangosol.net.ValueTypeAssertion;

/**
 * The {@link QueueScheme} class is responsible for building a fully
 * configured instance of a {@link NamedQueue}.
 */
@SuppressWarnings("rawtypes")
public interface NamedQueueScheme<Q extends NamedQueue>
        extends QueueScheme<Q, QueueService>
    {
    default Q realize(String sName, Session session)
        {
        return realize(sName, session, null);
        }

    default Q realize(String sName, Session session, ClassLoader classLoader)
        {
        if (session instanceof ConfigurableCacheFactorySession)
            {
            ExtensibleConfigurableCacheFactory eccf = (ExtensibleConfigurableCacheFactory)
                    ((ConfigurableCacheFactorySession) session).getConfigurableCacheFactory();
            return realize(sName, eccf, classLoader);
            }
        else
            {
            throw new IllegalArgumentException("Session must be a ConfigurableCacheFactorySession");
            }
        }

    default Q realize(String sName, ExtensibleConfigurableCacheFactory eccf)
        {
        return realize(sName, eccf, null);
        }

    @SuppressWarnings("unchecked")
    default Q realize(String sName, ExtensibleConfigurableCacheFactory eccf, ClassLoader classLoader)
        {
        ParameterResolver       resolver     = new NullParameterResolver();
        ClassLoader             loader       = classLoader == null ? Classes.getContextClassLoader() : classLoader;
        MapBuilder.Dependencies dependencies = new MapBuilder.Dependencies(eccf, null,
                                                    loader, sName,null);

        return (Q) realize(ValueTypeAssertion.WITHOUT_TYPE_CHECKING, resolver, dependencies);
        }
    }
