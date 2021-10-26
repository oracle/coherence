/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
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
 * @author lh
 * @since 21.12
 */
public abstract class PortableAbstractProcessor
        extends AbstractProcessor
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
