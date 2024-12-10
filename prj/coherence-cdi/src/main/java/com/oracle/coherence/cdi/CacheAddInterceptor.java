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
 * A CDI interceptor that always invokes target method and then
 * caches invocation result before returning it.
 */
@Interceptor
@Priority(Interceptor.Priority.LIBRARY_BEFORE + 5)
public class CacheAddInterceptor
    extends AbstractCacheInterceptor
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create the {@link CacheAddInterceptor}.
     *
     * @param coherence  the Coherence instance
     * @param extension  the Coherence CDI extension
     */
    @Inject
    public CacheAddInterceptor(@Name(Coherence.DEFAULT_NAME) Coherence coherence, CoherenceExtension extension)
        {
        super(coherence, extension);
        }

    // ---- interceptor methods ---------------------------------------------

    /**
     * Always invokes target method and caches invocation result before
     * returning it.
     *
     * @param ctxInvocation  the invocation context
     *
     * @return  a result of the invocation of the target method
     *
     * @throws Exception  if thrown by target method
     */
    @AroundInvoke
    public Object cacheAdd(InvocationContext ctxInvocation)
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
        Object                     oValue  = ctxInvocation.proceed();

        cache.put(oKey, oValue);
        return oValue;
        }
    }
