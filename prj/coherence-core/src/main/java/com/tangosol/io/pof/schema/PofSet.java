/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof.schema;

import java.util.HashSet;

/**
 * Schema representation of POF set.
 *
 * @author as  2013.11.18
 */
public class PofSet
        extends PofCollection
    {
    /**
     * Construct {@code PofSet} instance.
     */
    public PofSet()
        {
        super(HashSet.class);
        }

    /**
     * Construct {@code PofSet} instance.
     *
     * @param sSetClass      the type of set
     * @param sElementClass  the type of set elements
     */
    public PofSet(String sSetClass, String sElementClass)
        {
        super(sSetClass, sElementClass);
        }
    }
