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

public class ClassFile extends Access
    {
    private Constant[]  m_aconst;
    private int         m_iClass;
    private int         m_iSuper;
    private int[]       m_aiIface;
    private Field[]     m_afield;
    private Method[]    m_amethod;
    private Attribute[] m_aAttr;

    public ClassFile(DataInput stream)
            throws IOException
        {
        int nMagic = stream.readInt();
        if (nMagic != 0xCAFEBABE)
            {
            throw new IOException("Format Error:  Java .class files start with 0xCAFEBABE");
            }

        int nMinor = stream.readUnsignedShort();
        int nMajor = stream.readUnsignedShort();
/*
        if (nMajor != 45 || nMinor != 3)
            {
            throw new IOException("Format Error:  The Java .class file is an unsupported version (" + nMajor + "." + nMinor + ")");
            }
*/
        m_aconst = Constant.readConstants(stream);
        readAccess(stream);
        m_iClass = stream.readUnsignedShort();
        m_iSuper = stream.readUnsignedShort();

        int c = stream.readUnsignedShort();
        m_aiIface = new int[c];
        for (int i = 0; i < c; ++i)
            {
            m_aiIface[i] = stream.readUnsignedShort();
            }

        m_afield   = Field.readFields(stream, m_aconst);
        m_amethod  = Method.readMethods(stream, m_aconst, m_iClass);
        m_aAttr    = Attribute.readAttributes(stream, m_aconst);
        }

    public int getClassIndex()
        {
        return m_iClass;
        }

    public int getSuperIndex()
        {
        return m_iSuper;
        }

    public int getInterfaceCount()
        {
        return m_aiIface.length;
        }

    public int getInterfaceIndex(int i)
        {
        return m_aiIface[i];
        }

    public int getFieldCount()
        {
        return m_afield.length;
        }

    public Field getField(int i)
        {
        return m_afield[i];
        }

    public int getMethodCount()
        {
        return m_amethod.length;
        }

    public Method getMethod(int i)
        {
        return m_amethod[i];
        }

    public int getConstantCount()
        {
        return m_aconst.length;
        }

    public Constant getConstant(int i)
        {
        return m_aconst[i];
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

    public String toString()
        {
        StringBuffer sb = new StringBuffer();
        String sIndent = "    ";

        ClassConstant constThis = (ClassConstant) m_aconst[m_iClass];
        String        sThis     = constThis.getSimpleName();
        String        sPkgThis  = constThis.getPackageName();

        if (sPkgThis.length() > 0)
            {
            sb.append("package ")
              .append(sPkgThis)
              .append(";\n\n");
            }

        String sMod = super.toString(isInterface() ? ACC_IFACE : ACC_CLASS);
        if (sMod.length() > 0)
            {
            sb.append(sMod)
              .append(' ');
            }

        sb.append(isInterface() ? "interface " : "class ")
          .append(sThis);

        if (m_iSuper != 0)
            {
            ClassConstant constSuper = (ClassConstant) m_aconst[m_iSuper];
            if (!constSuper.getName().equals("java.lang.Object"))
                {
                sb.append(" extends ");
                if (sPkgThis.equals(constSuper.getPackageName()))
                    {
                    sb.append(constSuper.getSimpleName());
                    }
                else
                    {
                    sb.append(constSuper.getName());
                    }
                }
            }

        int c = m_aiIface.length;
        if (c > 0)
            {
            String sSubIndent = null;
            for (int i = 0; i < c; ++i)
                {
                if (i == 0)
                    {
                    String sKeyword;
                    if (isInterface())
                        {
                        sKeyword   = "extends ";
                        sSubIndent = "        ";
                        }
                    else
                        {
                        sKeyword   = "implements ";
                        sSubIndent = "           ";
                        }

                    sb.append('\n')
                      .append(sIndent)
                      .append(sKeyword);
                    }
                else
                    {
                    sb.append(",\n")
                      .append(sIndent)
                      .append(sSubIndent);
                    }

                ClassConstant constant = (ClassConstant) m_aconst[m_aiIface[i]];
                if (sPkgThis.equals(constant.getPackageName()))
                    {
                    sb.append(constant.getSimpleName());
                    }
                else
                    {
                    sb.append(constant.getName());
                    }
                }
            }

        return sb.toString();
        }

    public void dump(PrintWriter out)
        {
        String sIndent = "    ";

        out.println(toString());
        out.println(sIndent + '{');

        // fields
        int c = m_afield.length;
        for (int i = 0; i < c; ++i)
            {
            Field field = m_afield[i];

            out.print(sIndent + field.toString());

            if (field.isConstant())
                {
                out.println(" = " + field.getConstant() + ';');
                }
            else
                {
                out.println(';');
                }
            }

        // methods
        c = m_amethod.length;
        for (int i = 0; i < c; ++i)
            {
            if (i == 0 && m_afield.length > 0 || i > 0)
                {
                out.println();
                }

            m_amethod[i].dump(out, sIndent);
            }

        out.println(sIndent + '}');
        }

    /**
    * Indent the passed multi-line string.
    *
    * @param sText   the string to indent
    * @param sIndent a string used to indent each line
    *
    * @return the string, indented
    */
    public static String indentString(String sText, String sIndent, boolean fIndentFirstLine)
        {
        char[] ach = sText.toCharArray();
        int    cch = ach.length;

        StringBuffer sb = new StringBuffer();

        int iLine  = 0;
        int of     = 0;
        int ofPrev = 0;
        while (of < cch)
            {
            if (ach[of++] == '\n' || of == cch)
                {
                if (iLine++ > 0 || fIndentFirstLine)
                    {
                    sb.append(sIndent);
                    }

                sb.append(sText.substring(ofPrev, of));
                ofPrev = of;
                }
            }

        return sb.toString();
        // return sText;
        }
    }
