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

public class ClassConstant extends Constant
    {
    private int m_iName;

    public ClassConstant(DataInput stream)
            throws IOException
        {
        m_iName = stream.readUnsignedShort();
        }
    
    public int getNameIndex()
        {
        return m_iName;
        }

    public String getName()
        {
        return ((UtfConstant) m_aconst[m_iName]).getText().replace('/', '.');
        }

    public String getSimpleName()
        {
        String sName = getName();
        int    ofSep = sName.lastIndexOf('.');
        if (ofSep > 0)
            {
            return sName.substring(ofSep + 1);
            }

        return sName;
        }

    public String getPackageName()
        {
        String sName = getName();
        int    ofSep = sName.lastIndexOf('.');
        if (ofSep > 0)
            {
            return sName.substring(0, ofSep);
            }

        return "";
        }

    public String toString()
        {
        return "Class:  Name Index=" + m_iName + " (" + format(getName()) + ")";
        }
    }
