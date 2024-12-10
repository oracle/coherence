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

public class NameTypeConstant extends Constant
    {
    private int m_iName;
    private int m_iSig;

    public NameTypeConstant(DataInput stream)
            throws IOException
        {
        m_iName = stream.readUnsignedShort();
        m_iSig  = stream.readUnsignedShort();
        }
    
    public int getNameIndex()
        {
        return m_iName;
        }

    public String getName()
        {
        return ((UtfConstant) m_aconst[m_iName]).getText();
        }

    public int getSignatureIndex()
        {
        return m_iSig;
        }

    public String getSignature()
        {
        return ((UtfConstant) m_aconst[m_iSig]).getText();
        }

    public String toString()
        {
        return "Name/Type:  Name Index=" + m_iName + " (" + format(getName()) + ")"
                + ", Type Index=" + m_iSig + " (" + format(getSignature()) + ")";
        }
    }
