/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcachetesting;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofSerializer;
import com.tangosol.io.pof.PofWriter;

import java.io.IOException;

import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created with IntelliJ IDEA.
 * User: jfialli
 * Date: 5/31/13
 * Time: 2:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class SimpleCacheLoaderPofSerializer
        implements PofSerializer
    {
    /**
     * Method description
     *
     * @param pofWriter
     * @param o
     *
     * @throws IOException
     */
    @Override
    public void serialize(PofWriter pofWriter, Object o)
            throws IOException
        {
        AbstractCoherenceCacheTests.SimpleCacheLoader ldr = (AbstractCoherenceCacheTests.SimpleCacheLoader) o;

        pofWriter.writeInt(0, ldr.getLoadCount());
        pofWriter.writeMap(1, ldr.getLoaded());
        pofWriter.writeRemainder(null);
        }

    /**
     * Method description
     *
     * @param pofReader
     *
     * @return
     *
     * @throws IOException
     */
    @Override
    public Object deserialize(PofReader pofReader)
            throws IOException
        {
        int ldCount = pofReader.readInt(0);
        Map map     = new ConcurrentHashMap();

        pofReader.readMap(1, map);

        AbstractCoherenceCacheTests.SimpleCacheLoader result = new AbstractCoherenceCacheTests.SimpleCacheLoader(ldCount,
                                                                  map);

        pofReader.readRemainder();

        return result;
        }
    }
