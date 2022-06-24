/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.common;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.jcache.CoherenceBasedCachingProvider;

import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;

import com.tangosol.net.events.EventInterceptor;
import com.tangosol.net.events.application.LifecycleEvent;

import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.cache.Caching;

import javax.cache.spi.CachingProvider;

/**
 * Helpers for supporting Coherence JCache in Container Environment.
 * <p>
 * After ContainerAdapter has initialized Coherence, the Lifecycle event ACTIVATED
 * will trigger activation of Coherence JCache artifacts within a container context.
 * <p>
 * Provides support for configuring Coherence JCache from a GAR.
 *
 * @author jf  2014.07.28
 * @since Coherence 12.2.1
 */
public class ContainerHelper
    {
    // ----- ContainerHelper methods ----------------------------------------

    /**
     * Return true if running in a container environment.
     * @param eccf use resource registry from this eccf context to determine if running in a container or not.
     * @return true iff running in a container environment.
     */
    public static boolean isContainerContext(ExtensibleConfigurableCacheFactory eccf)
        {
        return false;
        }

    /**
     * Container specific solution to identify if running in container.
     * No longer used but keep around just in case needed in future.
     *
     * @return true iff running in a WLS Container context.
     *
     * @deprecated
     */
    public static boolean isWLSContainer()
        {
        String result = AccessController.doPrivileged(new PrivilegedAction<String>()
            {
            public String run()
                {
                return System.getProperty("weblogic.Name");
                }


            });

        return result != null;
        }

    // ----- inner class: LifecycleInterceptor ------------------------------

    /**
     * An {@link EventInterceptor} that initializes JCache for running within a container.
     * <p>
     * A Container-aware CoherenceBasedCachingProvider and CacheManager are created when
     * ECCF LifeCycle Event ACTIVATED occurs in a container context.
     */
    public static class JCacheLifecycleInterceptor
            implements EventInterceptor<LifecycleEvent>
        {
        @Override
        public void onEvent(LifecycleEvent event)
            {
            if (event.getType() == LifecycleEvent.Type.ACTIVATED)
                {
                m_nActivated++;

                // Coherence JCache ContainerHelper (ContainerAdapter??) instantiates
                // JCache artifacts for container using ECCF for container.
                // This events ECCF was initialized via GAR coherence-application.xml cache-config reference.
                // That is what we want for the CachingProvider and CacheManager.
                ConfigurableCacheFactory           ccf  = event.getConfigurableCacheFactory();
                ExtensibleConfigurableCacheFactory eccf = null;

                if (ccf instanceof ExtensibleConfigurableCacheFactory)
                    {
                    eccf = (ExtensibleConfigurableCacheFactory) ccf;
                    }
                }
            else if (event.getType() == LifecycleEvent.Type.DISPOSING)
                {
                // nothing to do here.
                //
                // JCache CacheManager is disposable and it is registered in
                // resource registry of ECCF.  Thus, when the ECCF is disposed, all its registered
                // resources are disposed, including the CoherenceBasedCacheManager created by
                // ACTIVATED event.
                }
            }

        /**
         * Added for Coherence JCache functional testing.
         *
         * @return how many times Activated event occurred.
         */
        public int getActivatedCount()
            {
            return m_nActivated;
            }

        /**
         * Added for Coherence JCache functional testing.
         *
         * @return true iff activated handler detected container context and activated JCache container context.
         */
        public boolean isContainerContext()
            {
            return m_fContainerContext;
            }

        // ----- data members -----------------------------------------------

        /**
         * non-zero iff jcache-lifecycle-interceptor is called with ACTIVATED event.
         *
         * {@see #getActivatedCount()}
         */
        private int m_nActivated = 0;

        /**
         * true iff activated within a container context.
         *
         * {@see #isContainerContext()}
         */
        private boolean m_fContainerContext = false;
        }
    }
