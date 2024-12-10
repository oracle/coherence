/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.events;

/**
 * Supports custom resolution of interceptor class in cases when the
 * interceptor may be proxied, such as when using CDI.
 *
 * @author as  2020.04.01
 * @since Coherence 14.1.1
 */
public interface InterceptorMetadataResolver
    {
    /**
     * Return the actual interceptor class that the generic type arguments
     * can be reified from.
     *
     * @param interceptor  an interceptor to get the actual class from
     *
     * @return the actual interceptor class
     */
    Class<? extends EventInterceptor> getInterceptorClass(EventInterceptor interceptor);
    }
