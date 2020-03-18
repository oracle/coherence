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

public class IntConstant extends Constant
    {
    private int m_nVal;

    public IntConstant(DataInput stream)
            throws IOException
        {
        m_nVal = stream.readInt();
        }
    
    public int getInt()
        {
        return m_nVal;
        }

    public String toString()
        {
        return "Int:  " + m_nVal + " (0x" + Integer.toHexString(m_nVal) + ")";
        }
    }
