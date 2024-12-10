/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.oracle.coherence.common.util.MemorySize;

import com.tangosol.coherence.config.builder.MapBuilder;
import com.tangosol.coherence.config.builder.NamedEventInterceptorBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.config.annotation.Injectable;
import com.tangosol.config.expression.Expression;
import com.tangosol.config.expression.LiteralExpression;
import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.internal.net.service.grid.DefaultPartitionedCacheDependencies;

import com.tangosol.internal.net.service.grid.PartitionedCacheDependencies;
import com.tangosol.net.CacheService;

import java.util.List;
import java.util.Map;

/**
 * The {@link DistributedScheme} class builds a distributed cache.
 *
 * @author pfm  2011.12.06
 * @since Coherence 12.1.2
 */
public class DistributedScheme
        extends AbstractCachingScheme<PartitionedCacheDependencies>
        implements ClusteredCachingScheme, BundlingScheme
    {
    // ----- constructors  --------------------------------------------------

    /**
     * Constructs a {@link DistributedScheme}.
     */
    public DistributedScheme()
        {
        this(new DefaultPartitionedCacheDependencies());
        }

    /**
     * Constructs a {@link DistributedScheme}.
     *
     * @param deps the {@link PartitionedCacheDependencies} to use
     */
    protected DistributedScheme(PartitionedCacheDependencies deps)
        {
        m_serviceDependencies = deps;
        m_mgrBundle           = null;

        // the default BackingMapScheme is a LocalScheme
        m_schemeBackingMap = new BackingMapScheme();
        m_schemeBackingMap.setInnerScheme(new LocalScheme());
        }

    // ----- ServiceScheme interface  ---------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String getServiceType()
        {
        return CacheService.TYPE_DISTRIBUTED;
        }

    /**
     * {@inheritDoc}
     */
    public List<NamedEventInterceptorBuilder> getEventInterceptorBuilders()
        {
        return m_listEventInterceptorBuilders;
        }

    // ----- ServiceBuilder interface  --------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRunningClusterNeeded()
        {
        return true;
        }

    // ----- BundlingScheme interface  --------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public BundleManager getBundleManager()
        {
        return m_mgrBundle;
        }

    // ----- ClusteredCachingScheme interface -------------------------------

    /**
     * Return the {@link BackingMapScheme} which builds the backing map for
     * the clustered scheme.
     *
     * @return the scheme
     */
    public BackingMapScheme getBackingMapScheme()
        {
        return m_schemeBackingMap;
        }

    /**
     * Set the {@link BackingMapScheme} which builds the backing map for
     * the clustered scheme.
     *
     * @param scheme  the scheme builder
     */
    @Injectable("backing-map-scheme")
    public void setBackingMapScheme(BackingMapScheme scheme)
        {
        m_schemeBackingMap = scheme;
        }

    // ----- DistributedScheme methods --------------------------------------

    /**
     * Return the {@link BackupMapConfig} which is used to configure
     * the backup map.
     *
     * @return the backup map configuration
     */
    public BackupMapConfig getBackupMapConfig()
        {
        BackupMapConfig config = m_configBackup;

        if (config == null)
            {
            m_configBackup = config = new BackupConfig();
            }

        return config;
        }

    /**
     * Set the {@link BundleManager}.
     *
     * @param mgrBundle  the BundleManager
     */
    @Injectable("operation-bundling")
    public void setBundleManager(BundleManager mgrBundle)
        {
        m_mgrBundle = mgrBundle;
        }

    /**
     * Set the {@link BackupMapConfig} which is used to configure
     * a backup map.
     *
     * @param config  the backup map configuration
     */
    @Injectable("backup-storage")
    public void setBackupMapConfig(BackupMapConfig config)
        {
        m_configBackup = config;
        }

    /**
     * Sets the {@link List} of {@link NamedEventInterceptorBuilder}s for the {@link DistributedScheme}.
     *
     * @param listBuilders  the {@link List} of {@link NamedEventInterceptorBuilder}s
     */
    @Injectable("interceptors")
    public void setEventInterceptorBuilders(List<NamedEventInterceptorBuilder> listBuilders)
        {
        m_listEventInterceptorBuilders = listBuilders;
        }

    // ----- inner class BackupConfig ---------------------------------------

    /**
     * The {@link BackupConfig} class manages configuration for the partitioned
     * cache backup map.
     */
    public static class BackupConfig
            extends AbstractScheme
            implements BackupMapConfig
        {
        // ----- BackupMapConfig interface ----------------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        public int resolveType(ParameterResolver resolver, MapBuilder bldrPrimaryMap)
            {
            String sType = getType(resolver);
            int    nType = BackingMapScheme.ON_HEAP;

            if (sType == null || sType.isEmpty())
                {
                // COH-7138 default to flash if the backing map is ram or flash
                if (bldrPrimaryMap instanceof FlashJournalScheme || bldrPrimaryMap instanceof RamJournalScheme)
                    {
                    nType = BackingMapScheme.FLASHJOURNAL;
                    }
                }
            else
                {
                nType = translateType(sType);
                }

            return nType;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getDirectory(ParameterResolver resolver)
            {
            return m_exprDirectory.evaluate(resolver);
            }

        /**
         * Set the root directory where the disk persistence manager stores files.
         * This is only valid for file-mapped type.
         *
         * @param expr  the directory name
         */
        @Injectable
        public void setDirectory(Expression<String> expr)
            {
            m_exprDirectory = expr;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getInitialSize(ParameterResolver resolver)
            {
            return (int) m_exprInitialSize.evaluate(resolver).getByteCount();
            }

        /**
         * Return the initial buffer size in bytes for off-heap and file-mapped
         * backup maps.
         *
         * @param expr  the initial buffer size
         */
        @Injectable
        public void setInitialSize(Expression<MemorySize> expr)
            {
            m_exprInitialSize = expr;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getMaximumSize(ParameterResolver resolver)
            {
            return (int) m_exprMaximumSize.evaluate(resolver).getByteCount();
            }

        /**
         * Set the maximum buffer size in bytes for off-heap and file-mapped
         * backup maps.
         *
         * @param expr  the maximum buffer size
         */
        @Injectable
        public void setMaximumSize(Expression<MemorySize> expr)
            {
            m_exprMaximumSize = expr;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getBackupSchemeName(ParameterResolver resolver)
            {
            return m_exprBackupSchemeName.evaluate(resolver);
            }

        /**
         * Set the name of the caching scheme to use as a backup map.
         *
         * @param expr  the scheme name
         */
        @Injectable("scheme-name")
        public void setBackupSchemeName(Expression<String> expr)
            {
            m_exprBackupSchemeName = expr;
            }

        /**
         * Return the type of storage to hold the backup data.  NOTE: this
         * is private, public access must be through resolveType.
         *
         * @param resolver  the ParameterResolver
         *
         * @return the write maximum buffer size
         */
        private String getType(ParameterResolver resolver)
            {
            return m_exrpType.evaluate(resolver);
            }

        /**
         * Set the type of storage to hold the backup data.
         *
         * @param expr  the maximum buffer size
         */
        @Injectable
        public void setType(Expression<String> expr)
            {
            m_exrpType = expr;
            }

        // ----- BuilderCustomization interface -----------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        public ParameterizedBuilder<Map> getCustomBuilder()
            {
            return m_bldrCustom;
            }

        /**
         * Set the InstanceBuilder that builds the custom instance.
         *
         * @param bldr  the InstanceBuilder
         */
        public void setCustomBuilder(ParameterizedBuilder<Map> bldr)
            {
            m_bldrCustom = bldr;
            }

        // ----- internal ---------------------------------------------------

        /**
         * Translate the backup map type string.
         *
         * @param sType  the map type
         *
         * @return the translated type enumerated in {@link BackingMapScheme}
         */
        protected int translateType(String sType)
            {
            int nType = -1;

            if (sType.equalsIgnoreCase("custom"))
                {
                nType = BackingMapScheme.CUSTOM;
                }
            else if (sType.equalsIgnoreCase("off-heap"))
                {
                nType = BackingMapScheme.OFF_HEAP;
                }
            else if (sType.equalsIgnoreCase("on-heap"))
                {
                nType = BackingMapScheme.ON_HEAP;
                }
            else if (sType.equalsIgnoreCase("file-mapped"))
                {
                nType = BackingMapScheme.FILE_MAPPED;
                }
            else if (sType.equalsIgnoreCase("scheme"))
                {
                nType = BackingMapScheme.SCHEME;
                }
            else
                {
                throw new IllegalArgumentException("Invalid backup storage type " + sType);
                }

            return nType;
            }

        // ----- data members -----------------------------------------------

        /**
         * The directory.
         */
        private Expression<String> m_exprDirectory = new LiteralExpression<String>("");

        /**
         * The initial backup map size.
         */
        private Expression<MemorySize> m_exprInitialSize = new LiteralExpression<MemorySize>(new MemorySize("1M"));

        /**
         * The maximum backup map size.
         */
        private Expression<MemorySize> m_exprMaximumSize = new LiteralExpression<MemorySize>(new MemorySize("1024M"));

        /**
         * The backup scheme name.
         */
        private Expression<String> m_exprBackupSchemeName = new LiteralExpression<String>("");

        /**
         * The backup map type.
         */
        private Expression<String> m_exrpType = new LiteralExpression<String>("");

        /**
         * The {@link ParameterizedBuilder} used to build the custom instance.
         */
        private ParameterizedBuilder<Map> m_bldrCustom;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The backing map scheme.
     */
    private BackingMapScheme m_schemeBackingMap;

    /**
     * The backup map configuration.
     */
    private BackupMapConfig m_configBackup;

    /**
     * The {@link BundleManager}.
     */
    private BundleManager m_mgrBundle;

    /**
     * The {@link List} of {@link NamedEventInterceptorBuilder}s associated with {@link DistributedScheme}.
     */
    private List<NamedEventInterceptorBuilder> m_listEventInterceptorBuilders;
    }
