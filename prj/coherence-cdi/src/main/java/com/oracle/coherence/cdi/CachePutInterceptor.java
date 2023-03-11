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
 * A CDI interceptor that stores parameter value in the Coherence cache.
 */
@Interceptor
@Priority(Interceptor.Priority.LIBRARY_BEFORE + 5)
public class CachePutInterceptor
    extends AbstractCacheInterceptor
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create the {@link CachePutInterceptor}
     *
     * @param coherence  the Coherence instance
     * @param extension  the Coherence CDI extension
     */
    @Inject
    public CachePutInterceptor(@Name(Coherence.DEFAULT_NAME) Coherence coherence, CoherenceExtension extension)
        {
        super(coherence, extension);
        }

    // ---- interceptor methods ---------------------------------------------

    /**
     * Stores parameter annotated with {@link CacheValue} into cache, invokes
     * target method and returns result of the invocation.
     *
     * @param ctxInvocation  the invocation context
     *
     * @return  the result of the invocation of the target method
     *
     * @throws Exception  if thrown by target method
     */
    @AroundInvoke
    public Object cachePut(InvocationContext ctxInvocation)
            throws Exception
        {
        CoherenceExtension.MethodInterceptorInfo mi = getExtension().interceptorInfo(ctxInvocation.getMethod());
        if (mi == null)
            {
            throw new IllegalStateException("Method interceptor data not ready in CDI extension for method " + ctxInvocation.getMethod());
            }

        Session                    session      = getSession(mi.sessionName());
        NamedCache<Object, Object> cache        = session.getCache(mi.cacheName());
        Object                     oKey         = mi.cacheKeyFunction().apply(ctxInvocation.getParameters());
        Object[]                   aoParameters = ctxInvocation.getParameters();
        Object                     oValue       = aoParameters[mi.getValueParameterIndex()];

        cache.put(oKey, oValue);
        return ctxInvocation.proceed();
        }
    }
