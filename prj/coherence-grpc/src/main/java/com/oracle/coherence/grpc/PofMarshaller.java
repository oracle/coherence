/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.grpc;

import com.tangosol.io.pof.ConfigurablePofContext;
import com.tangosol.io.pof.PofContext;

/**
 * gRPC Marshaller that uses POF for serialization.
 *
 * @author Aleks Seovic  2019.09.10
 */
public class PofMarshaller
        extends SerializerMarshaller<Object>
    {
    /**
     * Construct {@code PofMarshaller} instance.
     *
     * @param pofConfig the name of POF configuration file to use
     */
    public PofMarshaller(String pofConfig)
        {
        this(new ConfigurablePofContext(pofConfig));
        }

    /**
     * Construct {@code PofMarshaller} instance.
     *
     * @param pofContext the {@link PofContext} to use
     */
    public PofMarshaller(PofContext pofContext)
        {
        super(pofContext, Object.class);
        }
    }
