/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof;


import com.tangosol.io.ByteArrayReadBuffer;
import com.tangosol.io.ByteArrayWriteBuffer;
import com.tangosol.io.ReadBuffer;
import com.tangosol.io.WriteBuffer;

import com.tangosol.util.Base;


/**
* Base class for all POF-related unit tests.
*
* @author jh  2006.12.27
*/
public abstract class AbstractPofTest
        extends Base
    {
    protected void initPOFReader()
        {
        m_rb     = new ByteArrayReadBuffer(m_ab);
        m_bi     = m_rb.getBufferInput();
        m_ctx    = new SimplePofContext();
        m_reader = new PofBufferReader(m_bi, m_ctx);
        }

    protected void initPOFWriter()
        {
        m_ab     = new byte[1000];
        m_wb     = new ByteArrayWriteBuffer(m_ab);
        m_bo     = m_wb.getBufferOutput();
        m_ctx    = new SimplePofContext();
        m_writer = new PofBufferWriter(m_bo, m_ctx);
        }

    protected byte[]                   m_ab;
    protected SimplePofContext         m_ctx;
    protected PofBufferReader          m_reader;
    protected PofBufferWriter          m_writer;
    protected ReadBuffer               m_rb;
    protected ReadBuffer.BufferInput   m_bi;
    protected WriteBuffer              m_wb;
    protected WriteBuffer.BufferOutput m_bo;
    }
