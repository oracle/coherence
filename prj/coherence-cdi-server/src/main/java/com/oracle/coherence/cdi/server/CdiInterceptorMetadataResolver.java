/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi.server;

import com.tangosol.net.events.EventInterceptor;
import com.tangosol.net.events.InterceptorMetadataResolver;

import org.jboss.weld.proxy.WeldClientProxy;

/**
 * An implementation of {@link com.tangosol.net.events.InterceptorMetadataResolver}
 * that knows how to extract interceptor metadata from a Weld proxy.
 *
 * @author Aleks Seovic  2020.04.03
 * @since 20.06
 */
@SuppressWarnings("unchecked")
public class CdiInterceptorMetadataResolver
        implements InterceptorMetadataResolver
    {
    @Override
    public Class<? extends EventInterceptor> getInterceptorClass(EventInterceptor eventInterceptor)
        {
        Class<? extends EventInterceptor> clazz = eventInterceptor.getClass();
        if (eventInterceptor instanceof WeldClientProxy)
            {
            clazz = (Class<? extends EventInterceptor>) clazz.getSuperclass();
            }
        return clazz;
        }
    }
