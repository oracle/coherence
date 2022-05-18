/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcachetesting.io;

import com.tangosol.coherence.jcachetesting.CacheExpiryTests;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofSerializer;
import com.tangosol.io.pof.PofWriter;

import javax.cache.expiry.Duration;
import java.io.IOException;

/**
 * Class description
 *
 * @version        Enter version here..., 13/05/17
 * @author         Enter your name here...
 */
public class ParameterizedExpiryPolicySerializer
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
        CacheExpiryTests.ParameterizedExpiryPolicy p = (CacheExpiryTests.ParameterizedExpiryPolicy) o;

        pofWriter.writeObject(0, p.getExpiryForCreation());
        pofWriter.writeObject(1, p.getExpiryForAccess());
        pofWriter.writeObject(2, p.getExpiryForUpdate());

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
        Duration created  = (Duration) pofReader.readObject(0);
        Duration accessed = (Duration) pofReader.readObject(1);
        Duration modified = (Duration) pofReader.readObject(2);
        pofReader.readRemainder();

        Object result = new CacheExpiryTests.ParameterizedExpiryPolicy(created, accessed, modified);
        return result;
        }
    }
