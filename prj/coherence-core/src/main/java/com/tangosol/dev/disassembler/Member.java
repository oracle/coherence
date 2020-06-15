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
import java.util.*;

public class Member extends Access
    {
    private int m_iName;
    private int m_iSig;
    private Attribute[] m_aAttr;
    protected Constant[]  m_aconst;

    public Member(DataInput stream, Constant[] aconst)
            throws IOException
        {
        super(stream);
        m_iName  = stream.readUnsignedShort();
        m_iSig   = stream.readUnsignedShort();
        m_aAttr  = Attribute.readAttributes(stream, aconst);
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

    public int getSignatureIndex()
        {
        return m_iSig;
        }

    public String getSignature()
        {
        return ((UtfConstant) m_aconst[m_iSig]).getText();
        }

    public Attribute[] getAttributes()
        {
        return (Attribute[]) m_aAttr.clone();
        }

    public int getAttributeCount()
        {
        return m_aAttr.length;
        }

    public Attribute getAttribute(int i)
        {
        return m_aAttr[i];
        }

    public Attribute getAttribute(String sAttr)
        {
        Attribute[] aAttr = m_aAttr;
        int cAttr = aAttr.length;
        for (int iAttr = 0; iAttr < cAttr; ++iAttr)
            {
            Attribute attr = aAttr[iAttr];
            if (sAttr.equals(attr.getName()))
                {
                return attr;
                }
            }
        return null;
        }

    /**
    * Parse the method signature into discrete return type and parameter
    * signatures.
    *
    * @param sSig the JVM method signature
    *
    * @return an array of JVM type signatures, where [0] is the return
    *         type and [1]..[c] are the parameter types.
    */
    public static String[] getParameters(String sSig)
        {
        // check for start of signature
        char[] ach = sSig.toCharArray();
        if (ach[0] != '(')
            {
            throw new IllegalArgumentException("JVM Method Signature must start with '('");
            }

        // reserve the first element for the return value
        Vector vect = new Vector();
        vect.addElement(null);

        // parse parameter signatures
        int of = 1;
        while (ach[of] != ')')
            {
            int cch = getTypeLength(ach, of);
            vect.addElement(new String(ach, of, cch));
            of += cch;
            }

        // return value starts after the parameter-stop character
        // and runs to the end of the method signature
        ++of;
        vect.setElementAt(new String(ach, of, ach.length - of), 0);

        String[] asSig = new String[vect.size()];
        vect.copyInto(asSig);

        return asSig;
        }

    private static int getTypeLength(char[] ach, int of)
        {
        switch (ach[of])
            {
            case 'V':
            case 'Z':
            case 'B':
            case 'C':
            case 'S':
            case 'I':
            case 'J':
            case 'F':
            case 'D':
                return 1;

            case '[':
                {
                int cch = 1;
                while (isDecimal(ach[++of]))
                    {
                    ++cch;
                    }
                return cch + getTypeLength(ach, of);
                }

            case 'L':
                {
                int cch = 2;
                while (ach[++of] != ';')
                    {
                    ++cch;
                    }
                return cch;
                }

            default:
                throw new IllegalArgumentException("JVM Type Signature cannot start with '" + ach[of] + "'");
            }
        }

    /**
    * Provide a Java source representation of a signature.
    *
    * @param sSig the JVM signature
    *
    * @return the Java source type
    */
    public static String getType(String sSig)
        {
        switch (sSig.charAt(0))
            {
            case 'V':
                return "void";
            case 'Z':
                return "boolean";
            case 'B':
                return "byte";
            case 'C':
                return "char";
            case 'S':
                return "short";
            case 'I':
                return "int";
            case 'J':
                return "long";
            case 'F':
                return "float";
            case 'D':
                return "double";

            case 'L':
                return sSig.substring(1, sSig.indexOf(';')).replace('/', '.');

            case '[':
                if (isDecimal(sSig.charAt(1)))
                    {
                    int    n   = 0;
                    int    of  = 1;
                    char   ch  = sSig.charAt(of);
                    while (isDecimal(ch))
                        {
                        n  = n * 10 + decimalValue(ch);
                        ch = sSig.charAt(++of);
                        }
                    return getType(sSig.substring(of)) + "[" + n + "]";
                    }
                else
                    {
                    return getType(sSig.substring(1)) + "[]";
                    }

            default:
                throw new IllegalArgumentException("JVM Type Signature cannot start with '" + sSig.charAt(0) + "'");
            }
        }
    }
