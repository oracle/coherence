/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net;

import com.oracle.coherence.common.base.Classes;
import com.oracle.coherence.common.base.Logger;

import com.tangosol.config.expression.ChainedParameterResolver;
import com.tangosol.config.expression.ParameterResolver;
import com.tangosol.config.expression.PropertiesParameterResolver;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheFactoryBuilder;
import com.tangosol.net.Coherence;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.Session;
import com.tangosol.net.SessionConfiguration;
import com.tangosol.net.SessionProvider;
import com.tangosol.net.events.EventInterceptor;
import com.tangosol.net.events.InterceptorRegistry;
import com.tangosol.util.RegistrationBehavior;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Supplier;

/**
 * The default {@link SessionProvider} used by Coherence to provide a
 * {@link com.tangosol.net.Session}.
 *
 * @author Jonathan Knight  2020.12.15
 * @since 20.12
 */
public class DefaultSessionProvider
        implements SessionProvider
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link DefaultSessionProvider} instance.
     */
    protected DefaultSessionProvider()
        {
        this(null);
        }

    /**
     * Create a {@link DefaultSessionProvider} instance.
     */
    DefaultSessionProvider(Supplier<CacheFactoryBuilder> cacheFactoryBuilder)
        {
        f_cacheFactoryBuilder = cacheFactoryBuilder == null ? CacheFactory::getCacheFactoryBuilder : cacheFactoryBuilder;
        List<SessionProvider> list   = new ArrayList<>();
        ServiceLoader<SessionProvider> loader = ServiceLoader.load(SessionProvider.class);
        for (SessionProvider provider : loader)
            {
            list.add(provider);
            }
        list.sort(Comparator.reverseOrder());
        f_listProvider = list;
        }

    // ----- DefaultSessionProvider methods ---------------------------------

    /**
     * Get the ultimate default {@link SessionProvider}.
     *
     * @return the ultimate default {@link SessionProvider}
     */
    public static SessionProvider getBaseProvider()
        {
        return RootProvider.INSTANCE;
        }

    // ----- SessionProvider methods ----------------------------------------

    @Override
    public int getPriority()
        {
        return DEFAULT_PRIORITY;
        }

    @Override
    public Context createSession(SessionConfiguration configuration, Context context)
        {
        Context result = null;

        // if the configuration has a provider try that first.
        if (configuration instanceof SessionProvider.Provider)
            {
            result = ((SessionProvider.Provider) configuration).getSessionProvider()
                    .map(p -> p.createSession(configuration, context))
                    .orElse(context);
            }

        if (result != null && result.isComplete())
            {
            return result;
            }

        for (SessionProvider provider : f_listProvider)
            {
            result = provider.createSession(configuration, context);
            if (result != null && result.isComplete())
                {
                return result;
                }
            }
        return ensureSession(configuration, context);
        }

    @Override
    public void close()
        {
        for (SessionProvider provider : f_listProvider)
            {
            try
                {
                provider.close();
                }
            catch (Throwable t)
                {
                Logger.err(t);
                }
            }
        }

    // ----- helper methods -------------------------------------------------

    protected Context ensureSession(SessionConfiguration configuration, Context context)
        {
        String              sConfigLocation = configuration.getConfigUri().orElse(CacheFactoryBuilder.URI_DEFAULT);
        String              scopeName       = context.getScopePrefix() + configuration.getScopeName();
        ClassLoader         loader          = configuration.getClassLoader().orElse(Classes.getContextClassLoader());
        String              name            = configuration.getName();
        String              sConfigUri      = ScopedUriScopeResolver.encodeScope(sConfigLocation, scopeName);
        Coherence.Mode      mode            = configuration.getMode().orElse(context.getMode());
        Map<String, String> map             = Collections.singletonMap("coherence.client", mode.getClient());
        ParameterResolver   resolverCfg     = configuration.getParameterResolver().orElse(null);

        if (resolverCfg == null)
            {
            resolverCfg = new PropertiesParameterResolver(map);
            }
        else
            {
            resolverCfg = new ChainedParameterResolver(resolverCfg, new PropertiesParameterResolver(map));
            }

        // this request assumes the class loader for the session can be used
        // for loading both the configuration descriptor and the cache models
        CacheFactoryBuilder      cfb     = f_cacheFactoryBuilder.get();
        ConfigurableCacheFactory factory = cfb.getConfigurableCacheFactory(sConfigUri, loader, resolverCfg);

        Iterable<? extends EventInterceptor<?>> interceptors = context.getInterceptors();
        if (interceptors != null)
            {
            InterceptorRegistry registry = factory.getInterceptorRegistry();
            for (EventInterceptor<?> interceptor : interceptors)
                {
                registry.registerEventInterceptor(interceptor, RegistrationBehavior.FAIL);
                }
            }

        ConfigurableCacheFactorySession session = new ConfigurableCacheFactorySession(factory, loader, name);
        session.activate();
        return context.complete(session);
        }

    @Override
    public void releaseSession(Session session)
        {
        f_cacheFactoryBuilder.get().releaseSession(session);
        }

    // ----- inner class: RootProvider -----------------------------------

    /**
     * The root session provider that does not delegate to any other provider.
     */
    private static class RootProvider
            extends DefaultSessionProvider
        {
        @Override
        public Context createSession(SessionConfiguration configuration, Context context)
            {
            return ensureSession(configuration, context);
            }

        // ----- constants --------------------------------------------------

        /**
         * The singleton {@link RootProvider} instance.
         */
        static RootProvider INSTANCE = new RootProvider();
        }

    // ----- constants ------------------------------------------------------

    /**
     * The singleton {@link DefaultSessionProvider} instance.
     */
    public static final DefaultSessionProvider INSTANCE = new DefaultSessionProvider();

    /**
     * The priority for this provider.
     */
    public static final int DEFAULT_PRIORITY = SessionProvider.PRIORITY;

    // ----- data members ---------------------------------------------------

    private final Supplier<CacheFactoryBuilder> f_cacheFactoryBuilder;

    private final List<SessionProvider> f_listProvider;
    }
