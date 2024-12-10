/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof.schema;

import java.util.ArrayList;

/**
 * Schema representation of POF list.
 *
 * @author as  2013.11.18
 */
public class PofList
        extends PofCollection
    {
    /**
     * Construct {@code PofList} instance.
     */
    public PofList()
        {
        super(ArrayList.class);
        }

    /**
     * Construct {@code PofList} instance.
     *
     * @param sListClass     the type of list
     * @param sElementClass  the type of list elements
     */
    public PofList(String sListClass, String sElementClass)
        {
        super(sListClass, sElementClass);
        }
    }
