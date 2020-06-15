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

public class UnicodeConstant extends Constant
    {
    private String m_sText;

    public UnicodeConstant(DataInput stream)
            throws IOException
        {
        int    cch = stream.readUnsignedShort();
        char[] ach = new char[cch];
        for (int i = 0; i < cch; ++i)
            {
            ach[i] = stream.readChar();
            }
        m_sText = new String(ach);
        }
    
    public String getText()
        {
        return m_sText;
        }

    public String toString()
        {
        return "Unicode:  " + format(m_sText);
        }
    }
