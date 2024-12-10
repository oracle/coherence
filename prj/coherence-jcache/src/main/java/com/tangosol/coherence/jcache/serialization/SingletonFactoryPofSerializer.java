/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.serialization;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofSerializer;
import com.tangosol.io.pof.PofWriter;

import java.io.IOException;

import javax.cache.configuration.FactoryBuilder;

/**
 * JCache SingletonFactory PoF serializer
 *
 * @author jf  2013.09.09
 * @since Coherence 12.1.3
 */
public class SingletonFactoryPofSerializer
        implements PofSerializer
    {
    @Override
    public void serialize(PofWriter pofWriter, Object o)
            throws IOException
        {
        FactoryBuilder.SingletonFactory factory = (FactoryBuilder.SingletonFactory) o;

        pofWriter.writeObject(0, factory.create());
        pofWriter.writeRemainder(null);
        }

    @Override
    public Object deserialize(PofReader pofReader)
            throws IOException
        {
        Object instance = pofReader.readObject(0);

        pofReader.readRemainder();

        return new FactoryBuilder.SingletonFactory(instance);
        }
    }
