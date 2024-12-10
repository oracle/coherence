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
 * A CDI interceptor that returns cached value if present; otherwise it
 * returns and caches result of target method invocation.
 */
@Interceptor
@Priority(Interceptor.Priority.LIBRARY_BEFORE + 5)
public class CacheGetInterceptor
    extends AbstractCacheInterceptor
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create the {@link CacheGetInterceptor}
     *
     * @param coherence  the Coherence instance
     * @param extension  the Coherence CDI extension
     */
    @Inject
    public CacheGetInterceptor(@Name(Coherence.DEFAULT_NAME) Coherence coherence, CoherenceExtension extension)
        {
        super(coherence, extension);
        }

    // ---- interceptor methods ---------------------------------------------

    /**
     * Returns cached value if available; otherwise invokes target method and
     * stores invocation result in the cache before returning it.
     *
     * @param ctxInvocation  the invocation context
     *
     * @return the cached value if available; otherwise returns the result of
     * the invocation of the target method
     *
     * @throws Exception  if thrown by target method
     */
    @AroundInvoke
    public Object cacheGet(InvocationContext ctxInvocation)
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
        Object                     oValue  = cache.get(oKey);

        if (oValue == null)
            {
            oValue = ctxInvocation.proceed();
            cache.put(oKey, oValue);
            }
        return oValue;
        }
    }
