/*
 * Copyright (c) 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof;

import com.tangosol.io.ByteArrayReadBuffer;
import com.tangosol.io.ReadBuffer;

import com.tangosol.io.pof.PofBufferReader.UserTypeReader;

import org.junit.Test;

/**
 * Test case for {@code COH-25103}.
 *
 * @author rl
 * @since 3.21.22
 */
public class Coh25103Test
    {
    /**
     * Ensure it is possible to skip over a uniform Map containing
     * a {@code null} {@link String} value.
     *
     * @throws Exception
     */
    @Test
    public void ensureFixCoh25103() throws Exception
        {
        ConfigurablePofContext ctx = new ConfigurablePofContext();

        byte[] bytes = new byte[] {0x01,  // position 1
                                   0x5D,  // T_UNIFORM_MAP
                                   0x4E,  // key-type   -> string
                                   0x4E,  // value-type -> string
                                   0x01,  // one key/value pair
                                   0x01,  // string length 1
                                   0x44,  // letter 'D'
                                   0x64,  // V_REFERENCE_NULL
                                   0x40}; // EOS

        ReadBuffer     readBuffer = new ByteArrayReadBuffer(bytes);
        UserTypeReader reader     = new UserTypeReader(readBuffer.getBufferInput(), ctx, 99999, 0);

        // ensure skipping doesn't raise an exception
        reader.readRemainder();
        }
    }
