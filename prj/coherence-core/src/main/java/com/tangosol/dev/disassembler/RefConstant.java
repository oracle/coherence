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

public class RefConstant extends Constant
    {
    private int m_iClass;
    private int m_iNameType;

    public RefConstant(DataInput stream)
            throws IOException
        {
        m_iClass    = stream.readUnsignedShort();
        m_iNameType = stream.readUnsignedShort();
        }
    
    public int getClassIndex()
        {
        return m_iClass;
        }

    public int getNameTypeIndex()
        {
        return m_iNameType;
        }

    public String getClassName()
        {
        return ((ClassConstant) m_aconst[m_iClass]).getName();
        }

    public String getRefName()
        {
        return ((NameTypeConstant) m_aconst[m_iNameType]).getName();
        }

    public String getSignature()
        {
        return ((NameTypeConstant) m_aconst[m_iNameType]).getSignature();
        }

    public String toString()
        {
        String s = null;

        switch (m_nType)
            {
            case CONSTANT_Fieldref:
                s = "Field";
                break;
            case CONSTANT_Methodref:
                s = "Method";
                break;
            case CONSTANT_InterfaceMethodref:
                s = "Interface Method";
                break;
            }

        return s + ":  Class Index=" + m_iClass + " (" + format(getClassName()) + ")"
                + ", Name/Type Index=" + m_iNameType
                + " (" + format(getRefName()) + "/" + format(getSignature()) + ")";
        }
    }
