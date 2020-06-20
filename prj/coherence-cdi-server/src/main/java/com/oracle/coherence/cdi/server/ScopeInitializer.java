/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi.server;

import com.oracle.coherence.cdi.ConfigUri;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.net.CacheFactoryBuilder;
import com.tangosol.net.ConfigurableCacheFactory;

import com.tangosol.util.Base;

import javax.inject.Named;

/**
 * An interface that should be implemented by custom Coherence scopes in order
 * to enable their discovery and automatic initialization at startup.
 * <p>
 * Each class implementing this interface must be annotated with
 * {@link Named @Named} annotation representing the name of the scope being
 * initialized, and can optionally be annotated with {@link ConfigUri @ConfigUri}
 * annotation if a non-default configuration resource should be used.
 *
 * @author Aleks Seovic  2020.06.15
 * @since 20.06
 */
public interface ScopeInitializer
    {
    /**
     * Return the {@link ConfigurableCacheFactory} for this scope.
     *
     * @param builder  the {@link CacheFactoryBuilder} to use
     *
     * @return the {@link ConfigurableCacheFactory} for this scope
     */
    public default ConfigurableCacheFactory getConfigurableCacheFactory(CacheFactoryBuilder builder)
        {
        Named named = getClass().getAnnotation(Named.class);
        if (named == null)
            {
            Logger.warn("Scope initializer " + getClass().getName() + " does not have @Named annotation and will be ignored.");
            return null;
            }

        ConfigUri configUri = getClass().getAnnotation(ConfigUri.class);

        String sScopeName = named.value();
        String sConfigUri = configUri == null ? CacheFactoryBuilder.URI_DEFAULT : configUri.value();
        String sUri = sScopeName + '@' + sConfigUri;

        // use the class loader of the scope initializer to load configuration
        return builder.getConfigurableCacheFactory(sUri, Base.getContextClassLoader(this));
        }
    }
