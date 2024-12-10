/*
 * Copyright (c) 2016, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.processor.AbstractProcessor;

import java.io.IOException;

/**
 * A portable {@link AbstractProcessor}. a convenience interface for implementations
 * having no properties to serialize.
 *
 * @param <K> the type of the Map entry key
 * @param <V> the type of the Map entry value
 * @param <R> the type of value returned by the EntryProcessor
 *
 * @author lh
 * @since 21.12
 */
public abstract class PortableAbstractProcessor<K, V, R>
        extends AbstractProcessor<K, V, R>
        implements PortableObject
    {
    // ----- PortableObject interface ---------------------------------------
    @Override
    public void readExternal(PofReader in) throws IOException
        {
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        }
    }
