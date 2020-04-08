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

public class StringConstant extends Constant
    {
    private int m_iString;

    public StringConstant(DataInput stream)
            throws IOException
        {
        m_iString = stream.readUnsignedShort();
        }
    
    public int getStringIndex()
        {
        return m_iString;
        }

    public String getText()
        {
        return ((UtfConstant) m_aconst[m_iString]).getText();
        }

    public String toString()
        {
        return "String:  String Index=" + m_iString + " (" + format(getText()) + ")";
        }
    }
