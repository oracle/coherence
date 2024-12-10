/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.gradle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

import java.util.Objects;
import java.util.Properties;

/**
 * Helper class to provide certain Gradle build-time properties to the plugin.
 *
 * @author Gunnar Hillert  2023.03.16
 * @since 22.06.05
 */
public class CoherenceBuildTimeProperties
    {
    //----- constructors ----------------------------------------------------

    /**
     * Loads the Coherence Built-time properties from {@link #PROPERTIES_FILE_NAME}.
     */
    public CoherenceBuildTimeProperties()
        {
        Properties coherenceBuildTimeProperties = new Properties();
        try (InputStream is = CoherenceBuildTimeProperties.class.getResourceAsStream(PROPERTIES_FILE_NAME))
            {
            coherenceBuildTimeProperties.load(is);
            }
        catch (IOException e)
            {
            throw new IllegalStateException("Unable to load properties from InputStream");
            }

        String sCoherenceGroupId = coherenceBuildTimeProperties.getProperty("coherence-group-id");
        Objects.requireNonNull(sCoherenceGroupId, "Property coherence-group-id must not be null");

        String sCoherenceVersion = coherenceBuildTimeProperties.getProperty("coherence-version");
        Objects.requireNonNull(sCoherenceVersion, "Property coherence-version must not be null");

        // we need to ensure that any Windows file separators in the property are converted to
        // Unix separators to make a valid URL
        String sCoherenceLocalDependencyRepo = coherenceBuildTimeProperties.getProperty("local-dependency-repo")
                        .replace("\\", "/");

        Objects.requireNonNull(sCoherenceVersion, "Property local-dependency-repo must not be null");

        m_sCoherenceGroupId            = sCoherenceGroupId;
        m_sCoherenceVersion = sCoherenceVersion;
        m_sCoherenceLocalDependencyRepo = sCoherenceLocalDependencyRepo;

        LOGGER.info("Retrieved CoherenceBuildTimeProperty 'coherence-group-id' with value: {}", m_sCoherenceGroupId);
        LOGGER.info("Retrieved CoherenceBuildTimeProperty 'coherence-version' with value: {}", m_sCoherenceVersion);
        LOGGER.info("Retrieved CoherenceBuildTimeProperty 'local-dependency-repo' with value: {}", m_sCoherenceLocalDependencyRepo);
        }

    // ----- CoherenceBuildTimeProperties methods ---------------------------

    /**
     * Returns the Coherence GroupId.
     *
     * @return the Coherence GroupId
     */
    public String getCoherenceGroupId()
        {
        return m_sCoherenceGroupId;
        }

    /**
     * Returns the Coherence version.
     *
     * @return the Coherence version
     */
    public String getCoherenceVersion()
        {
        return m_sCoherenceVersion;
        }

    /**
     * Returns the local Coherence dependency repo location for the Maven dependencies.
     *
     * @return the local Coherence dependency repo location
     */
    public String getCoherenceLocalDependencyRepo()
        {
        return m_sCoherenceLocalDependencyRepo;
        }

    // ----- constants ------------------------------------------------------

    /**
     * Name of the properties file, that will provide built-time properties to the plugin.
     */
    public static final String PROPERTIES_FILE_NAME = "/coherence-gradle-plugin.properties";

    /**
     * Logger configuration.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(CoherenceBuildTimeProperties.class);

    // ----- data members ---------------------------------------------------

    /**
     * The Coherence GroupId.
     */
    private final String m_sCoherenceGroupId;

    /**
     * The version of the Coherence dependency.
     */
    private final String m_sCoherenceVersion;

    /**
     * The Maven repository to be used by tests holding the Maven artifacts.
     */
    private final String m_sCoherenceLocalDependencyRepo;
    }


