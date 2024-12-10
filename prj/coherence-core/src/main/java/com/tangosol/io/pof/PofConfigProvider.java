/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof;

import java.util.Collections;
import java.util.Set;

/**
 * A provider of POF configuration URIs to load into a {@link ConfigurablePofContext}.
 * <p>
 * Implementations of this class are discovered using the {@link java.util.ServiceLoader}
 * and supplied POF configurations automatically loaded.
 *
 * @author Jonathan Knight  2022.06.03
 * @since 22.06
 */
public interface PofConfigProvider
    {
    /**
     * Provide a POF configuration files to load.
     *
     * @return a POF configuration files to load
     */
    String getConfigURI();

    /**
     * Provide a set of POF configuration files to load.
     *
     * @return a set of POF configuration files to load
     */
    default Set<String> getConfigURIs()
        {
        String sURI = getConfigURI();
        return sURI == null || sURI.isBlank()
                ? Collections.emptySet()
                : Collections.singleton(sURI.trim());
        }
    }
