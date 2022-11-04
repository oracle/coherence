/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.bedrock.junit;

import com.oracle.bedrock.Bedrock;
import com.oracle.bedrock.Option;
import com.oracle.bedrock.OptionsByType;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.MetaClass;
import com.oracle.bedrock.runtime.Profile;
import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.RoleName;
import com.oracle.bedrock.table.Cell;
import com.oracle.bedrock.table.Table;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.ScopedCacheFactoryBuilder;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StorageDisabledMember
        implements SessionBuilder
    {
    public StorageDisabledMember()
        {
        this(new Option[0]);
        }

    public StorageDisabledMember(Option[] aOption)
        {
        m_optionsByType = OptionsByType.of(aOption);
        }

    @Override
    public ConfigurableCacheFactory build(LocalPlatform platform, CoherenceCluster cluster, OptionsByType optionsByType)
        {
        // ----- establish the diagnostics output table -----

        Table diagnosticsTable = new Table();

        diagnosticsTable.getOptions().add(Table.orderByColumn(0));

        // establish a new set of options based on those provided
        optionsByType = OptionsByType.of(optionsByType);

        // ----- establish the options for launching a local storage-disabled member -----
        optionsByType.add(RoleName.of("client"));
        optionsByType.add(LocalStorage.disabled());

        optionsByType.addAll(m_optionsByType);

        optionsByType.addIfAbsent(CacheConfig.of("coherence-cache-config.xml"));

        // ----- notify the Profiles that we're about to launch an application -----

        MetaClass<CoherenceClusterMember> metaClass = new CoherenceClusterMember.MetaClass();

        for (Profile profile : optionsByType.getInstancesOf(Profile.class))
            {
            profile.onLaunching(platform, metaClass, optionsByType);
            }

        // ----- create local system properties based on those defined by the launch options -----

        // modify the current system properties to include/override those in the schema
        com.oracle.bedrock.runtime.java.options.SystemProperties systemProperties =
                optionsByType.get(com.oracle.bedrock.runtime.java.options.SystemProperties.class);

        Properties properties = systemProperties.resolve(platform, optionsByType);

        Table systemPropertiesTable = new Table();

        systemPropertiesTable.getOptions().add(Table.orderByColumn(0));
        systemPropertiesTable.getOptions().add(Cell.Separator.of(""));
        systemPropertiesTable.getOptions().add(Cell.DisplayNull.asEmptyString());

        for (String sName : properties.stringPropertyNames())
            {
            String sValue  = properties.getProperty(sName);
            String sSuffix = System.getProperties().containsKey(sName) ? "*" : "";
            systemPropertiesTable.addRow(sName + sSuffix, sValue);

            System.setProperty(sName, sValue);
            }

        diagnosticsTable.addRow("System Properties", systemPropertiesTable.toString());

        // ----- output the diagnostics -----

        if (LOGGER.isLoggable(Level.INFO))
            {
            LOGGER.log(Level.INFO,
                       "Oracle Bedrock " + Bedrock.getVersion() + ": Starting Storage Disabled Member...\n"
                               + "------------------------------------------------------------------------\n"
                               + diagnosticsTable + "\n"
                               + "------------------------------------------------------------------------\n");
            }

        // ----- establish the session -----

        // create the session
        String                   uriConfig = optionsByType.get(CacheConfig.class).getUri();
        ClassLoader              loader    = getClass().getClassLoader();
        ConfigurableCacheFactory session   = new ScopedCacheFactoryBuilder().getConfigurableCacheFactory(uriConfig, loader);

        // as this is a cluster member we have to join the cluster
        CacheFactory.ensureCluster();

        return session;
        }


    @Override
    public boolean equals(Object other)
        {
        return other instanceof StorageDisabledMember;
        }


    @Override
    public int hashCode()
        {
        return StorageDisabledMember.class.hashCode();
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link Logger} for this class.
     */
    private static final Logger LOGGER = Logger.getLogger(StorageDisabledMember.class.getName());

    private final OptionsByType m_optionsByType;
    }
