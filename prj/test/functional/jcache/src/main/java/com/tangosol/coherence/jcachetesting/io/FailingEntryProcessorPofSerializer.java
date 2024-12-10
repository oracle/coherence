/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcachetesting.io;

import com.tangosol.coherence.jcachetesting.FailingEntryProcessor;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofSerializer;
import com.tangosol.io.pof.PofWriter;

import java.io.IOException;

/**
 * Class description
 *
 * @version        Enter version here..., 13/09/13
 * @author         Enter your name here...
 */
public class FailingEntryProcessorPofSerializer
        implements PofSerializer
    {
    /**
     * Method description
     *
     * @param pofWriter
     * @param o
     *
     * @throws java.io.IOException
     */
    @Override
    public void serialize(PofWriter pofWriter, Object o)
            throws IOException
        {
        FailingEntryProcessor p = (FailingEntryProcessor) o;

        pofWriter.writeString(0, p.getClazz().getCanonicalName());
        pofWriter.writeRemainder(null);
        }

    /**
     * Method description
     *
     * @param pofReader
     *
     * @return
     *
     * @throws java.io.IOException
     */
    @Override
    public Object deserialize(PofReader pofReader)
            throws IOException
        {
        String className = pofReader.readString(0);

        pofReader.readRemainder();

        Class exceptionClass = null;

        try
            {
            exceptionClass = Class.forName(className);
            }
        catch (ClassNotFoundException e)
            {
            e.printStackTrace();    // To change body of catch statement use File | Settings | File Templates.
            }

        return new FailingEntryProcessor(exceptionClass != null ? exceptionClass : NullPointerException.class);

        }
    }
