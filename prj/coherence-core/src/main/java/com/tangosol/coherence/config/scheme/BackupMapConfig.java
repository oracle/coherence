/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import java.util.Map;

import com.tangosol.coherence.config.builder.BuilderCustomization;
import com.tangosol.coherence.config.builder.MapBuilder;
import com.tangosol.config.expression.ParameterResolver;


/**
 * The {@link BackupMapConfig} interface exposes the configuration needed
 * to create an instance of a backup map, which is used by the distributed cache
 * to store backup data.
 *
 * @author pfm  2012.01.11
 * @since Coherence 12.1.2
 */
public interface BackupMapConfig
        extends BuilderCustomization<Map>
    {
    /**
     * Resolve the backup map type using the configuration specified
     * by the application.  The primary map is also need in special cases
     * to determine the type.
     *
     * @param resolver        the ParameterResolver
     * @param bldrPrimaryMap  the primary map builder which may be used to
     *                        determine the backup type
     *
     * @return the backup map type enumerated in {@link BackingMapScheme}
     */
    public int resolveType(ParameterResolver resolver, MapBuilder bldrPrimaryMap);

    /**
     * Return the name of the caching scheme to use as a backup map.
     * Note that the scheme name is used as a key to lookup the scheme
     * in the cache mapping.  This is in contrast with the scheme name
     * in the base {@link AbstractScheme} class which self-identifies a
     * scheme object.
     *
     * @param resolver  the ParameterResolver
     *
     * @return the scheme name
     */
    public String getBackupSchemeName(ParameterResolver resolver);

    /**
     * Return the root directory where the disk persistence manager stores files.
     * This is only valid for file-mapped type.
     *
     * @param resolver  the ParameterResolver
     *
     * @return the root directory
     */
    public String getDirectory(ParameterResolver resolver);

    /**
     * Return the initial buffer size in bytes for off-heap and file-mapped
     * backup maps.
     *
     * @param resolver  the ParameterResolver
     *
     * @return the write maximum batch size
     */
    public int getInitialSize(ParameterResolver resolver);

    /**
     * Return the maximum buffer size in bytes for off-heap and file-mapped
     * backup maps.
     *
     * @param resolver  the ParameterResolver
     *
     * @return the write maximum buffer size
     */
    public int getMaximumSize(ParameterResolver resolver);
    }
