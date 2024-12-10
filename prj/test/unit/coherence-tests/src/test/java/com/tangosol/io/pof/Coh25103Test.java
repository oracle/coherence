/*
 * Copyright (c) 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof;

import com.tangosol.io.ByteArrayReadBuffer;
import com.tangosol.io.ReadBuffer;

import com.tangosol.io.pof.PofBufferReader.UserTypeReader;

import org.junit.Before;
import org.junit.Test;

/**
 * Test case for {@code COH-25103}.
 *
 * @author rl
 * @since 3.21.22
 */
public class Coh25103Test
    {
    // ----- test lifecycle -------------------------------------------------

    @Before
    public void before()
        {
        // initialize POF
        m_pofContext = new  ConfigurablePofContext();
        }

    // ----- tests ----------------------------------------------------------

    /**
     * Ensure it is possible to skip over a uniform Map containing
     * a {@code null} {@code T_CHAR_STRING} value.
     */
    @Test
    public void ensureFixCoh25103CharString()
            throws Exception
        {
        byte[] bytes = new byte[] {0x01,  // position 1
                                   0x5D,  // T_UNIFORM_MAP
                                   0x4E,  // key-type   -> T_CHAR_STRING
                                   0x4E,  // value-type -> T_CHAR_STRING
                                   0x01,  // one key/value pair
                                   0x01,  // string length 1
                                   0x44,  // letter 'D'
                                   0x64,  // V_REFERENCE_NULL
                                   0x40}; // EOS

        doReadRemainder(bytes);
        }

    /**
     * Ensure it is possible to skip over a uniform Map containing
     * a {@code null} {@code T_OCTET_STRING} value.
     */
    @Test
    public void ensureFixCoh25103OctetString()
            throws Exception
        {
        byte[] bytes = new byte[] {0x01,  // position 1
                                   0x5D,  // T_UNIFORM_MAP
                                   0x4C,  // key-type   -> T_OCTET_STRING
                                   0x4C,  // value-type -> T_OCTET_STRING
                                   0x01,  // one key/value pair
                                   0x01,  // string length 1
                                   0x44,  // letter 'D'
                                   0x64,  // V_REFERENCE_NULL
                                   0x40}; // EOS

        doReadRemainder(bytes);
        }

    // ----- helper methods -------------------------------------------------

    protected void doReadRemainder(byte[] bytes)
            throws Exception
        {
        ReadBuffer     readBuffer = new ByteArrayReadBuffer(bytes);
        UserTypeReader reader     = new UserTypeReader(readBuffer.getBufferInput(), m_pofContext, 99999, 0);

        // ensure skipping doesn't raise an exception
        reader.readRemainder();
        }

    // ----- data members ---------------------------------------------------

    /**
     * {@link PofContext} under test.
     */
    protected ConfigurablePofContext m_pofContext;
    }
