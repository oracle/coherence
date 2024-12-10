/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.protobuf;

import com.tangosol.io.pof.PofConfigProvider;

/**
 * A {@link PofConfigProvider} that automatically loads the
 * Coherence protobuf POF configuration file.
 */
public class ProtobufPofConfigProvider
        implements PofConfigProvider
    {
    @Override
    public String getConfigURI()
        {
        return "coherence-protobuf-pof-config.xml";
        }
    }
