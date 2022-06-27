/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.bedrock.junit;

import com.oracle.bedrock.Bedrock;
import com.oracle.bedrock.OptionsByType;

import com.oracle.bedrock.options.Timeout;

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

import com.oracle.coherence.common.base.Exceptions;

import com.tangosol.net.Coherence;
import com.tangosol.net.CoherenceConfiguration;
import com.tangosol.net.SessionConfiguration;

import java.util.Properties;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A base class for {@link CoherenceBuilder} implementations.
 *
 * @author  Jonathan Knight 2022.06.25
 * @since 22.06
 */
public abstract class AbstractCoherenceBuilder
        implements CoherenceBuilder
    {
    /**
     * The {@link Logger} for this class.
     */
    private static final Logger LOGGER = Logger.getLogger(AbstractCoherenceBuilder.class.getName());

    @Override
    @SuppressWarnings("resource")
    public Coherence build(LocalPlatform platform, CoherenceCluster cluster, OptionsByType optionsByType)
        {
        // ----- establish the diagnostics output table -----

        Table diagnosticsTable = new Table();

        diagnosticsTable.getOptions().add(Table.orderByColumn(0));

        // establish a new set of options based on those provided
        optionsByType = OptionsByType.of(optionsByType);

        // ----- establish the options for launching a local storage-disabled member -----
        optionsByType.add(RoleName.of("client"));
        optionsByType.add(LocalStorage.disabled());
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

        Coherence.Mode mode = getMode();

        SessionConfiguration.Builder sessionBuilder = SessionConfiguration.builder()
                .withMode(mode)
                .withConfigUri(optionsByType.get(CacheConfig.class).getUri());

        for (String propertyName : properties.stringPropertyNames())
            {
            String propertyValue = properties.getProperty(propertyName);

            systemPropertiesTable.addRow(propertyName + (System.getProperties().containsKey(propertyName) ? "*" : ""),
                                         propertyValue);

            System.setProperty(propertyName, propertyValue.isEmpty() ? "" : propertyValue);
            sessionBuilder.withParameter(propertyName, propertyValue.isEmpty() ? "" : propertyValue);
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

        CoherenceConfiguration configuration = CoherenceConfiguration.builder()
                .withSession(sessionBuilder.build())
                .build();

        Coherence coherence;

        Timeout timeout = optionsByType.get(Timeout.class);
        try
            {
            coherence = Coherence.builder(configuration, mode).build()
                    .start()
                    .get(timeout.to(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS);
            }
        catch (InterruptedException | ExecutionException | TimeoutException e)
            {
            throw Exceptions.ensureRuntimeException(e, "Failed to start Coherence");
            }

        return coherence;
        }

    /**
     * Return the mode the Coherence instance and default session will run in.
     *
     * @return the mode the Coherence instance and default session will run in
     */
    protected abstract Coherence.Mode getMode();

    @Override
    public boolean equals(Object other)
        {
        return other instanceof AbstractCoherenceBuilder;
        }


    @Override
    public int hashCode()
        {
        return AbstractCoherenceBuilder.class.hashCode();
        }
    }
