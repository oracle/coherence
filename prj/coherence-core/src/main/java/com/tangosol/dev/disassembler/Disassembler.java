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


/**
* @author  1997.07.30  cp   Original programmer.
* @author  1998.01.06  cp   Updating
*/
public class Disassembler
        extends Base
    {
    public static void main(String asArgs[]) throws Throwable
        {
        try
            {
            String      sName  = (asArgs.length > 0 && asArgs[0] != null && asArgs[0].charAt(0) != '-' ? asArgs[0] : "");
            if (sName.length() <= 0)
                {
                throw new IllegalArgumentException("Disassembler:  Name of class required!");
                }

            InputStream in     = ClassLoader.getSystemResourceAsStream(sName.replace('.', '/').concat(".class"));
            DataInput   stream = new DataInputStream(in);
            ClassFile   clz    = new ClassFile(stream);

            // scan for "-dump" option
            for (int i = 0; i < asArgs.length; ++i)
                {
                if (asArgs[i] != null && (   asArgs[i].startsWith("-d")
                                          || asArgs[i].startsWith("-D")))
                    {
                    outClass(clz);
                    break;
                    }
                }

            // out(clz.toString());
            clz.dump(getOut());

            out();
            try
                {
                stream.readByte();
                out("WARNING!!!  Stream not exhausted!!!");
                }
            catch (EOFException e)
                {
                }
            }
        catch (Throwable t)
            {
            out("Caught \"" + t + "\", stack trace:");
            out(t);
            out("(end stack trace)");
            }
        }

    public static void outClass(ClassFile clz)
        {
        out();
        out("Dumping class information:");

        out();
        int c = clz.getConstantCount();
        if (c == 0)
            {
            out("No constants.");
            }
        else
            {
            out("Constants (" + c + "):");
            for (int i = 0; i < c; ++i)
                {
                Constant constant = clz.getConstant(i);
                out(format(i) + ' ' + (constant == null ? "<null>" : constant.toString()));
                }
            }

        out();
        c = clz.getAttributeCount();
        if (c == 0)
            {
            out("No attributes.");
            }
        else
            {
            outAttributes(clz.getAttributes(), "");
            }

        out();
        c = clz.getFieldCount();
        if (c == 0)
            {
            out("No fields.");
            }
        else
            {
            out("Fields (" + c + "):");
            for (int i = 0; i < c; ++i)
                {
                Field field = clz.getField(i);
                out(format(i) + ' ' + (field == null ? "<null>" : ("name=" + field.getName() + ", signature=" + field.getSignature())));
                if (field != null && field.getAttributeCount() > 0)
                    {
                    outAttributes(field.getAttributes(), "    ");
                    }
                }
            }

        out();
        c = clz.getMethodCount();
        if (c == 0)
            {
            out("No methods.");
            }
        else
            {
            out("Methods (" + c + "):");
            for (int i = 0; i < c; ++i)
                {
                Method method = clz.getMethod(i);
                out(format(i) + ' ' + (method == null ? "<null>" : method.toString()));
                if (method != null && method.getAttributeCount() > 0)
                    {
                    outAttributes(method.getAttributes(), "    ");
                    }
                }
            }
        }

    public static void outAttributes(Attribute[] aAttr, String sIndent)
        {
        int c = aAttr.length;
        out(sIndent + "Attributes (" + c + "):");
        for (int i = 0; i < c; ++i)
            {
            Attribute attribute = aAttr[i];
            out(sIndent + format(i) + ' ' +
                    (attribute == null ? "<null>" : attribute.toString()));
            if (attribute != null)
                {
                String sHexDump = toHexDump(attribute.getInfo(), 16);
                out(indentString(sHexDump, sIndent, true));
                }
            }
        }

    public static String format(int n)
        {
        return "[" + toDecString(n, 4) + '/' + toHexString(n, 4) + ']';
        }
    }
