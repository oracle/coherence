/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

/**
 * An exception to indicate that a required map does not exist.
 *
 * @author jk  2018.03.05
 * @since Coherence 14.1.1
 */
public class MapNotFoundException
        extends RuntimeException
    {
    /**
     * Create a {@link MapNotFoundException}.
     *
     * @param sMapName  the name of the missing map
     */
    public MapNotFoundException(String sMapName)
        {
        super("Map '" + sMapName + "' does not exist");
        }
    }
