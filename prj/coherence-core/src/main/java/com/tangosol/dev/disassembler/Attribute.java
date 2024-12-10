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

public class Attribute
    {
    private int         m_iName;
    private int         m_cb;
    private byte[]      m_ab;
    private Constant[]  m_aconst;

    public static Attribute[] readAttributes(DataInput stream, Constant[] aconst)
            throws IOException
        {
        int cAttr = stream.readUnsignedShort();
        Attribute[] aAttr = new Attribute[cAttr];
        for (int i = 0; i < cAttr; ++i)
            {
            aAttr[i] = new Attribute(stream, aconst);
            }
        return aAttr;
        }

    public Attribute(DataInput stream, Constant[] aconst)
            throws IOException
        {
        m_iName  = stream.readUnsignedShort();
        m_cb     = stream.readInt();
        m_ab     = new byte[m_cb];
        if (m_cb > 0)
            {
            stream.readFully(m_ab);
            }
        m_aconst = aconst;
        }

    public int getNameIndex()
        {
        return m_iName;
        }

    public String getName()
        {
        return ((UtfConstant) m_aconst[m_iName]).getText();
        }

    public byte[] getInfo()
        {
        return m_ab;
        }

    public String toString()
        {
        StringBuffer sb = new StringBuffer();

        sb.append("Attribute \"")
          .append(getName())
          .append("\":  ")
          .append(m_ab.length == 0 ? "<empty>" : "length=" + m_ab.length);

        return sb.toString();
        }
    }
