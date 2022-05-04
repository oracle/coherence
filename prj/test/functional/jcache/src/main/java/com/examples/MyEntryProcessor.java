/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.examples;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import java.io.IOException;
import java.io.Serializable;

import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.MutableEntry;

/**
 * Example 37-1 An Example EntryProcessor Implementation
 *
 * Added POF support that is not in documentation.
 *
 * @version 12.2.1.0.0
 * @author  jf 2014/5/7
 */
public class MyEntryProcessor
        implements EntryProcessor<String, Integer, Integer>, Serializable, PortableObject
    {

    // ----- EntryProcessor interface ---------------------------------------
    public Integer process(MutableEntry<String, Integer> entry, Object... arguments)
            throws EntryProcessorException
        {
        if (entry.exists())
            {
            Integer current = entry.getValue();

            entry.setValue(current + 1);

            return current;
            }
        else
            {
            entry.setValue(0);

            return -1;
            }
        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader in)
            throws IOException
        {
        }

    @Override
    public void writeExternal(PofWriter out)
            throws IOException
        {
        }


    // ----- constants ------------------------------------------------------
    /** 
     * serialization
     */
    public static final long serialVersionUID = 1L;
    }
