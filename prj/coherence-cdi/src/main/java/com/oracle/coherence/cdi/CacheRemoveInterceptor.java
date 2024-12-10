/*
 * Copyright (c) 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi;

import com.tangosol.net.Coherence;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;

import javax.annotation.Priority;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import javax.inject.Inject;

/**
 * A CDI interceptor that removes value from the cache and invokes target method.
 */
@Interceptor
@Priority(Interceptor.Priority.LIBRARY_BEFORE + 5)
public class CacheRemoveInterceptor
    extends AbstractCacheInterceptor
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create the {@link CacheRemoveInterceptor}
     *
     * @param coherence  the Coherence instance
     * @param extension  the Coherence CDI extension
     */
    @Inject
    public CacheRemoveInterceptor(@Name(Coherence.DEFAULT_NAME) Coherence coherence, CoherenceExtension extension)
        {
        super(coherence, extension);
        }

    // ---- interceptor methods ---------------------------------------------

    /**
     * Removes cached value.
     *
     * @param ctxInvocation  the invocation context
     *
     * @return  result of the invocation of the target method
     *
     * @throws Exception  if thrown by target method
     */
    @AroundInvoke
    public Object cacheRemove(InvocationContext ctxInvocation)
            throws Exception
        {
        CoherenceExtension.MethodInterceptorInfo mi = getExtension().interceptorInfo(ctxInvocation.getMethod());
        if (mi == null)
            {
            throw new IllegalStateException("Method interceptor data not ready in CDI extension for method " + ctxInvocation.getMethod());
            }

        Session                    session = getSession(mi.sessionName());
        NamedCache<Object, Object> cache   = session.getCache(mi.cacheName());
        Object                     oKey    = mi.cacheKeyFunction().apply(ctxInvocation.getParameters());

        cache.remove(oKey);
        return ctxInvocation.proceed();
        }
    }
