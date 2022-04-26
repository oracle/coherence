/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.docker;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.net.CacheFactory;

import com.tangosol.util.InvocableMap;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A test EntryProcessor to verify the classpath.
 *
 * @param <K>  the type of cache entry key
 * @param <V>  the type of cache entry value
 *
 * @author Jonathan Knight  2020.06.30
 */
public class TestProcessor<K, V>
        implements InvocableMap.EntryProcessor<K, V, Boolean>, ExternalizableLite
    {
    @Override
    public Boolean process(InvocableMap.Entry<K, V> entry)
        {
        CacheFactory.log("Entering InfoProcessor");
        boolean fResult;
        try
            {
            Class.forName("org.junit.Test");
            fResult = true;
            }
        catch (Throwable t)
            {
            t.printStackTrace();
            fResult = false;
            }
        CacheFactory.log("Leaving InfoProcessor - result=" + fResult);
        return fResult;
        }

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        }
    }
