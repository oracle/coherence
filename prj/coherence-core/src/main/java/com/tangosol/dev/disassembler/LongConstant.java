/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.disassembler;

import com.tangosol.dev.compiler.java.*;
import com.tangosol.util.*;
import java.io.*;

public class LongConstant extends Constant
    {
    private long m_lVal;

    public LongConstant(DataInput stream)
            throws IOException
        {
        m_lVal = stream.readLong();
        }
    
    public long getLong()
        {
        return m_lVal;
        }

    public String toString()
        {
        return "Long:  " + m_lVal + " (0x" + Long.toHexString(m_lVal) + ")";
        }
    }
