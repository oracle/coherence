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

public class UtfConstant extends Constant
    {
    private String m_sText;

    public UtfConstant(DataInput stream)
            throws IOException
        {
        m_sText = stream.readUTF();
        }
    
    public String getText()
        {
        return m_sText;
        }

    public String toString()
        {
        return "Utf:  " + format(m_sText);
        }
    }
