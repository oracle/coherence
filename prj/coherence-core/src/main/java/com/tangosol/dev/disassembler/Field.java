/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.disassembler;


import com.tangosol.dev.compiler.java.LiteralToken;

import java.io.DataInput;
import java.io.IOException;


public class Field extends Member
    {
    public static Field[] readFields(DataInput stream, Constant[] aconst)
            throws IOException
        {
        int cFields = stream.readUnsignedShort();
        Field[] aFields = new Field[cFields];
        for (int i = 0; i < cFields; ++i)
            {
            aFields[i] = new Field(stream, aconst);
            }
        return aFields;
        }

    public Field(DataInput stream, Constant[] aconst)
            throws IOException
        {
        super(stream, aconst);
        }

    public String getType()
        {
        return getType(getSignature());
        }

    public boolean isConstant()
        {
        return getAttribute("ConstantValue") != null;
        }

    public String getConstant()
        {
        Attribute attr = getAttribute("ConstantValue");
        if (attr == null)
            {
            return null;
            }

        byte[] abConst = attr.getInfo();
        if (abConst == null || abConst.length != 2)
            {
            throw new RuntimeException("Illegal \"ConstantValue\" Attribute structure.");
            }

        int iConst  = (((int) abConst[0]) & 0xFF) << 8
                    | (((int) abConst[1]) & 0xFF);

        Constant constant = m_aconst[iConst];
        switch (constant.getType())
            {
            case Constant.CONSTANT_String:
                return LiteralToken.printableString(
                        ((StringConstant) constant).getText());
            case Constant.CONSTANT_Integer:
                int n = ((IntConstant) constant).getInt();
                switch (getSignature().charAt(0))
                    {
                    case 'Z':
                        return (n == 0 ? "false" : "true");
                    case 'C':
                        return LiteralToken.printableChar((char) n);
                    default:
                        return Integer.toString(n);
                    }
            case Constant.CONSTANT_Float:
                float fl = ((FloatConstant) constant).getFloat();
                return new StringBuffer().append(fl).append('F').toString();
            case Constant.CONSTANT_Long:
                long l = ((LongConstant) constant).getLong();
                return new StringBuffer().append(l).append('L').toString();
            case Constant.CONSTANT_Double:
                double dfl = ((DoubleConstant) constant).getDouble();
                return (Double.toString(dfl));
            default:
                throw new RuntimeException("Illegal \"ConstantValue\" data type (" + constant.getType() + ").");
            }
        }

    public String toString()
        {
        StringBuffer sb = new StringBuffer(super.toString(ACC_FIELD));
        if (sb.length() > 0)
            {
            sb.append(' ');
            }

        sb.append(getType())
          .append(' ')
          .append(getName());

        return sb.toString();
        }
    }

