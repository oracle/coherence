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

public class Constant
        extends Base
    {
    public static final int CONSTANT_Class              = 7;
    public static final int CONSTANT_Fieldref           = 9;
    public static final int CONSTANT_Methodref          = 10;
    public static final int CONSTANT_InterfaceMethodref = 11;
    public static final int CONSTANT_String             = 8;
    public static final int CONSTANT_Integer            = 3;
    public static final int CONSTANT_Float              = 4;
    public static final int CONSTANT_Long               = 5;
    public static final int CONSTANT_Double             = 6;
    public static final int CONSTANT_NameAndType        = 12;
    public static final int CONSTANT_Utf8               = 1;
    public static final int CONSTANT_Unicode            = 2;

    public static Constant[] readConstants(DataInput stream)
            throws IOException
        {
        int cConst = stream.readUnsignedShort();
        Constant[] aconst = new Constant[cConst];
        for (int i = 1; i < cConst; ++i)
            {
            int      nType    = stream.readUnsignedByte();
            Constant constant = null;
            int      cExtra   = 0;

            switch (nType)
                {
                case CONSTANT_Class:
                    constant = new ClassConstant(stream);
                    break;
                case CONSTANT_Fieldref:
                case CONSTANT_Methodref:
                case CONSTANT_InterfaceMethodref:
                    constant = new RefConstant(stream);
                    break;
                case CONSTANT_String:
                    constant = new StringConstant(stream);
                    break;
                case CONSTANT_Integer:
                    constant = new IntConstant(stream);
                    break;
                case CONSTANT_Float:
                    constant = new FloatConstant(stream);
                    break;
                case CONSTANT_Long:
                    constant = new LongConstant(stream);
                    cExtra = 1;
                    break;
                case CONSTANT_Double:
                    constant = new DoubleConstant(stream);
                    cExtra = 1;
                    break;
                case CONSTANT_NameAndType:
                    constant = new NameTypeConstant(stream);
                    break;
                case CONSTANT_Utf8:
                    constant = new UtfConstant(stream);
                    break;
                case CONSTANT_Unicode:
                    constant = new UnicodeConstant(stream);
                    break;
                default:
                    throw new IOException("Invalid constant type " + nType + " for constant #" + i);
                }

            constant.m_aconst = aconst;
            constant.m_nType  = nType;

            aconst[i] = constant;
            i += cExtra;
            }

        return aconst;
        }

    public int getType()
        {
        return m_nType;
        }

    protected static String format(String s)
        {
        return LiteralToken.printableString(s);
        }

    protected int        m_nType;
    protected Constant[] m_aconst;
    }
