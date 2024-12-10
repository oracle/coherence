/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder;

import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.CacheService;
import com.tangosol.net.ConfigurableCacheFactory;

import com.tangosol.util.Base;
import com.tangosol.util.MapListener;

import java.util.Map;

/**
 * The {@link MapBuilder} interface is used by a builder to create an instance
 * of a {@link Map} that is a local to a Java process.
 *
 * @author pfm  2011.11.30
 * @since Coherence 12.1.2
 */
public interface MapBuilder
    {
    /**
     * Realize a {@link Map} based on the state of the {@link MapBuilder},
     * resolvable parameters and provided {@link Dependencies}.
     *
     * @param resolver      the {@link ParameterResolver}
     * @param dependencies  the {@link Dependencies} for realizing the {@link Map}
     *
     * @return a {@link Map}
     */
    public Map realizeMap(ParameterResolver resolver, Dependencies dependencies);

    /**
     * The commonly required {@link Dependencies} for realizing a {@link Map}
     * with a {@link MapBuilder}.
     */
    public static class Dependencies
        {
        // ----- constructors ----------------------------------------------

        /**
         * Constructs a {@link Dependencies}.
         * <p>
         * Note: In some circumstances the properties encapsulated by a {@link Dependencies}
         * may not be available or not required.  In these cases the properties
         * will be a default value or <code>null</code>.
         *
         * @param ccf                   the {@link ConfigurableCacheFactory}
         * @param ctxBackingMapManager  the BackingMapManagerContext
         * @param loader                the {@link ClassLoader}
         * @param sCacheName            the cache name
         * @param sServiceType          the service type
         */
        public Dependencies(ConfigurableCacheFactory ccf, BackingMapManagerContext ctxBackingMapManager,
                            ClassLoader loader, String sCacheName, String sServiceType)
            {
            this(ccf, ctxBackingMapManager, loader, sCacheName, sServiceType, null);
            }

        /**
         * Constructs a {@link Dependencies}.
         * <p>
         * Note: In some circumstances the properties encapsulated by a {@link Dependencies}
         * may not be available or not required.  In these cases the properties
         * will be a default value or <code>null</code>.
         *
         * @param ccf                   the {@link ConfigurableCacheFactory}
         * @param ctxBackingMapManager  the BackingMapManagerContext
         * @param loader                the {@link ClassLoader}
         * @param sCacheName            the cache name
         * @param sServiceType          the service type
         */
        public Dependencies(ConfigurableCacheFactory ccf, BackingMapManagerContext ctxBackingMapManager,
                            ClassLoader loader, String sCacheName, String sServiceType,
                            Map<Map, MapListener> mapMapListeners)
            {
            m_ccf                  = ccf;
            m_ctxBackingMapManager = ctxBackingMapManager;
            m_contextClassLoader   = loader == null ? Base.getContextClassLoader() : loader;
            m_sCacheName           = sCacheName;
            m_sServiceType         = sServiceType;
            m_fBinaryMap           = ctxBackingMapManager != null &&
                                        (CacheService.TYPE_DISTRIBUTED.equals(sServiceType) ||
                                         CacheService.TYPE_FEDERATED.equals(sServiceType));
            m_mapMapListeners      = mapMapListeners;
            }

        // ----- Dependencies methods --------------------------------------

        /**
         * Return true if the map is binary.
         *
         * @return  true if the map is binary
         */
        public boolean isBinary()
            {
            return m_fBinaryMap;
            }

        /**
         * Return true if the map is a backup map.
         *
         * @return  true if the map is a backup map
         */
        public boolean isBackup()
            {
            return m_fBackup;
            }

        /**
         * Set the flag indicating that the map is a backup map.
         *
         * @param fBackup  true if the map is a backup map
         */
        public void setBackup(boolean fBackup)
            {
            m_fBackup = fBackup;
            }

        /**
         * Return true if the map is in blind mode.
         *
         * @return true if the map is in blind mode
         */
        public boolean isBlind()
            {
            return m_fBlind;
            }

        /**
         * Set the flag indicating that the map is in blind mode.
         *
         * @param fBlind  true if the map is in blind mode.
         */
        public void setBlind(boolean fBlind)
            {
            m_fBlind = fBlind;
            }

        /**
         * Return the {@link BackingMapManagerContext}.
         *
         * @return the BackingMapManagerContext
         */
        public BackingMapManagerContext getBackingMapManagerContext()
            {
            return m_ctxBackingMapManager;
            }

        /**
         * Return the cache name.
         *
         * @return  the cache name
         */
        public String getCacheName()
            {
            return m_sCacheName;
            }

        /**
         * Return the {@link ConfigurableCacheFactory} needed to create nested caches.
         *
         * @return the ConfigurableCacheFactory
         */
        public ConfigurableCacheFactory getConfigurableCacheFactory()
            {
            return m_ccf;
            }

        /**
         * Returns the {@link ClassLoader} to use in the context of
         * realizing {@link Map}s and other associated infrastructure.
         *
         * @return the {@link ClassLoader}
         */
        public ClassLoader getClassLoader()
            {
            return m_contextClassLoader;
            }

        /**
         * Return the Service type.
         *
         * @return the Service type
         */
        public String getServiceType()
            {
            return m_sServiceType;
            }

        /**
         * Obtains the registry of {@link MapListener}s, which is a {@link Map}
         * keyed by {@link Map} to their associated {@link MapListener}.
         *
         * @return the {@link Map} of {@link Map}s to {@link MapListener}s
         */
        public Map<Map, MapListener> getMapListenersRegistry()
            {
            return m_mapMapListeners;
            }

        // ----- data members ----------------------------------------------

        /**
         * The flag that indicates that the map is used as a backup.
         */
        private boolean m_fBackup;

        /**
         * The flag that indicates that the map is binary.
         */
        private boolean m_fBinaryMap;

        /**
         *  A flag to indicate the constructed map should operate in blind mode.
         */
        private boolean m_fBlind;

        /**
         * The {@link ConfigurableCacheFactory} in which the realized {@link Map}
         * will exist.
         */
        private ConfigurableCacheFactory m_ccf;

        /**
         * The {@link BackingMapManagerContext}.
         */
        private BackingMapManagerContext m_ctxBackingMapManager;

        /**
         * The cache name.
         */
        private String m_sCacheName;

        /**
         * The {@link ClassLoader}.
         */
        private ClassLoader m_contextClassLoader;

        /**
         * The Service type.
         */
        private String m_sServiceType;

        /**
         * A registry of {@link MapListener}s keyed by the {@link Map} to which
         * they are attached.
         */
        Map<Map, MapListener> m_mapMapListeners;
        }
    }
