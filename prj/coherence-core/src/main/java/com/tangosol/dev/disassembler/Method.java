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

public class Method extends Member
    {
    private int m_iClass;

    public static Method[] readMethods(DataInput stream, Constant[] aconst, int iClass)
            throws IOException
        {
        int cMethods = stream.readUnsignedShort();
        Method[] aMethods = new Method[cMethods];
        for (int i = 0; i < cMethods; ++i)
            {
            aMethods[i] = new Method(stream, aconst, iClass);
            }
        return aMethods;
        }

    public Method(DataInput stream, Constant[] aconst, int iClass)
            throws IOException
        {
        super(stream, aconst);
        m_iClass = iClass;
        }

    public String[] getParameters()
        {
        return getParameters(getSignature());
        }

    public String getActualDeclaration()
        {
        StringBuffer sb = new StringBuffer();

        String[] asSig   = getParameters();
        sb.append(getType(asSig[0]))
          .append(' ')
          .append(getName())
          .append('(');

        int c = asSig.length;
        for (int i = 1; i < c; ++i)
            {
            if (i > 1)
                {
                sb.append(", ");
                }
            sb.append(getType(asSig[i]));
            }

        sb.append(')');

        return sb.toString();
        }

    public String getDeclaration()
        {
        String sName = getName();
        if (sName.equals("<init>"))
            {
            StringBuffer sb = new StringBuffer();
            sb.append(((ClassConstant) m_aconst[m_iClass]).getSimpleName())
              .append('(');

            String[] asSig = getParameters();
            int c = asSig.length;
            for (int i = 1; i < c; ++i)
                {
                if (i > 1)
                    {
                    sb.append(", ");
                    }
                sb.append(getType(asSig[i]));
                }

            sb.append(')');
            return sb.toString();
            }
        else if (sName.equals("<clinit>"))
            {
            return isStatic() ? "" : "<clinit>";
            }
        else
            {
            return getActualDeclaration();
            }
        }

    public String[] getThrows()
        {
        Attribute attr = getAttribute("Exceptions");
        if (attr == null)
            {
            return null;
            }

        try
            {
            DataInput stream = new DataInputStream(
                    new ByteArrayInputStream(attr.getInfo()));

            int c = stream.readUnsignedShort();
            String[] as = new String[c];
            for (int i = 0; i < c; ++i)
                {
                as[i] = ((ClassConstant) m_aconst[stream.readUnsignedShort()]).getName();
                }

            return as;
            }
        catch (IOException e)
            {
            throw new RuntimeException("Illegal \"Exceptions\" Attribute structure.  (" + e.toString() + ")");
            }
        }

    public String toString()
        {
        StringBuffer sb = new StringBuffer(super.toString(ACC_METHOD));
        if (sb.length() > 0)
            {
            sb.append(' ');
            }

        sb.append(getDeclaration());

        return sb.toString();
        }

    public void dump(PrintWriter out, String sBaseIndent)
        {
        String sPkg    = ((ClassConstant) m_aconst[m_iClass]).getPackageName();
        String sIndent = "    ";

        out.print(sBaseIndent + toString());

        String[] asThrows = getThrows();
        if (asThrows != null && asThrows.length > 0)
            {
            String sThrowsIndent = sBaseIndent + sIndent + sIndent;
            int c = asThrows.length;
            for (int i = 0; i < c; ++i)
                {
                // if in this package, strip package name
                String sThrows = asThrows[i];
                if (sThrows.startsWith(sPkg) && sThrows.lastIndexOf('.') == sPkg.length())
                    {
                    sThrows = sThrows.substring(sPkg.length() + 1);
                    }

                if (i == 0)
                    {
                    out.println();
                    out.print(sThrowsIndent + "throws " + sThrows);
                    }
                else
                    {
                    out.println(',');
                    out.print(sThrowsIndent + "       " + sThrows);
                    }
                }
            }

        if (isAbstract())
            {
            out.println(';');
            }
        else
            {
            String sCodeIndent = sBaseIndent + sIndent;
            out.println();
            out.println(sCodeIndent + '{');
            dumpCode(out, sCodeIndent);
            out.println(sCodeIndent + '}');
            }
        }

    public void dumpCode(PrintWriter out, String sIndent)
        {
        if (isNative())
            {
            out.println(sIndent + "<native>");
            return;
            }

        Attribute attr   = getAttribute("Code");
        byte[]    abInfo = attr.getInfo();
        int       cbInfo = abInfo.length;

        try
            {
            // parse header stuff
            DataInput stream = new DataInputStream(
                    new ByteArrayInputStream(abInfo));

            out.println(sIndent + "#pragma max_stack " + stream.readUnsignedShort());
            out.println(sIndent + "#pragma max_locals " + stream.readUnsignedShort());

            // from VM Spec 4.7.4 Code_attribute structure
            int ofCode = 8;
            int cbCode = stream.readInt();

            // calculate number of digits required to show code offset
            int cHexDigits = 0;
            int cDecDigits = 0;

            int cbTemp = cbCode - 1;
            do
                {
                cHexDigits += 2;
                cbTemp     /= 0x100;
                }
            while (cbTemp > 0);

            cbTemp = cbCode - 1;
            do
                {
                cDecDigits += 1;
                cbTemp     /= 10;
                }
            while (cbTemp > 0);
            cDecDigits = Math.max(cDecDigits, 2);

            // set up header formatting for each line of output
            // "[dddd/xxxx] xx xx xx xx  "
            int    cchHead = cDecDigits + cHexDigits + 17;
            char[] achHead = new char[cchHead];

            for (int i = 0; i < cchHead; ++i)
                {
                achHead[i] = ' ';
                }

            achHead[0                          ] = '[';
            achHead[cDecDigits + 1             ] = '/';
            achHead[cDecDigits + cHexDigits + 2] = ']';

            // set up other formatting
            String sFill = "                         "; // 25 spaces
            String sTab  = "  ";                        // "tab" before notes

            // create a new stream just for reading the opcodes
            MarkedByteArrayInputStream streamRaw =
                    new MarkedByteArrayInputStream(abInfo, ofCode, cbCode);
            stream = new DataInputStream(streamRaw);

            // support for "multi-line" _switch opcodes
            boolean fSwitch = false;    // true during disassembling of _switch ops
            boolean fTable  = false;    // tableswitch:true, lookupswitch:false
            boolean fDft    = false;    // true if only default value remains
            int     cCases  = 0;        // number of remaining cases
            int     nCase   = 0;        // for tableswitch, the next case value
            int     ofOp    = 0;        // offset of original _switch instruction
            int     ofDft   = 0;        // offset from ofOp to jump to if no match

            while (true)
                {
                String  sParms = null;
                String  sNotes = null;
                String  sName  = null;

                if (fSwitch)
                    {
                    if (fDft || cCases == 0)
                        {
                        sParms = "default:  goto " + (ofOp + ofDft);
                        fSwitch = false;
                        fDft    = false;
                        }
                    else
                        {
                        int n  = (fTable ? nCase++ : stream.readInt());
                        int of = stream.readInt();

                        sParms = "case " + n + ":  goto " + (ofOp + of);

                        if (--cCases == 0)
                            {
                            fDft = true;
                            }
                        }
                    }
                else
                    {
                    int op = stream.readUnsignedByte();

                    sName = OPNAME [op];
                    if (sName == null)
                        {
                        throw new RuntimeException("Illegal byte code (" + op + ") at offset " + streamRaw.getMarkedOffset() + ".");
                        }

                    boolean fWide = (op == WIDE);
                    if (fWide)
                        {
                        op    = stream.readUnsignedByte();
                        sName = sName + ' ' + OPNAME [op];
                        }

                    // some opcodes have parameters
                    switch (op)
                        {
                        case ALOAD:
                        case ASTORE:
                        case DLOAD:
                        case DSTORE:
                        case FLOAD:
                        case FSTORE:
                        case ILOAD:
                        case ISTORE:
                        case LLOAD:
                        case LSTORE:
                        case RET:
                            // one or two byte index of local variable
                            {
                            int n = (fWide ? stream.readUnsignedShort()
                                           : stream.readUnsignedByte() );
                            sParms = Integer.toString(n);
                            }
                            break;

                        case ANEWARRAY:
                        case CHECKCAST:
                        case INSTANCEOF:
                        case NEW:
                            // two byte constant class index
                            {
                            int n = stream.readUnsignedShort();
                            sParms = Integer.toString(n);
                            sNotes = m_aconst[n].toString();
                            }
                            break;

                        case BIPUSH:
                            // one byte value
                            {
                            int n = stream.readUnsignedByte();
                            sParms = Integer.toString(n);
                            sNotes = "(0x" + Integer.toHexString(n) + ")";
                            }
                            break;

                        case GETFIELD:
                        case GETSTATIC:
                        case PUTFIELD:
                        case PUTSTATIC:
                            // two byte constant field ref index
                            {
                            int n = stream.readUnsignedShort();
                            sParms = Integer.toString(n);
                            sNotes = m_aconst[n].toString();
                            }
                            break;

                        case IFEQ:
                        case IFGE:
                        case IFGT:
                        case IFLE:
                        case IFLT:
                        case IFNE:
                        case IFNONNULL:
                        case IFNULL:
                        case IF_ACMPEQ:
                        case IF_ACMPNE:
                        case IF_ICMPEQ:
                        case IF_ICMPGE:
                        case IF_ICMPGT:
                        case IF_ICMPLE:
                        case IF_ICMPLT:
                        case IF_ICMPNE:
                        case GOTO:
                        case JSR:
                            // two byte opcode offset
                            {
                            ofOp    = streamRaw.getMarkedOffset();
                            int n   = stream.readShort();
                            sParms  = Integer.toString(ofOp + n);
                            }
                            break;

                        case GOTO_W:
                        case JSR_W:
                            // four byte opcode offset
                            {
                            ofOp    = streamRaw.getMarkedOffset();
                            int n   = stream.readInt();
                            sParms  = Integer.toString(ofOp + n);
                            }
                            break;

                        case IINC:
                            // one or two byte index of local variable
                            {
                            int v;
                            int n;
                            if (fWide)
                                {
                                v = stream.readUnsignedShort();
                                n = stream.readShort();
                                sNotes = "(0x" + Integer.toHexString(n & 0xFFFF) + ")";
                                }
                            else
                                {
                                v = stream.readUnsignedByte();
                                n = stream.readByte();
                                sNotes = "(0x" + Integer.toHexString(n & 0xFF) + ")";
                                }
                            sParms = Integer.toString(v) + "," + Integer.toString(n);
                            }
                            break;

                        case INVOKEINTERFACE:
                            // two byte interface method ref, one byte arg count,
                            // and one byte (zero)
                            {
                            int n = stream.readUnsignedShort();
                            int c = stream.readUnsignedByte();
                            stream.readUnsignedByte();

                            sParms = Integer.toString(n) + "," + Integer.toString(c);
                            sNotes = m_aconst[n].toString();
                            }
                            break;

                        case INVOKESPECIAL:
                        case INVOKESTATIC:
                        case INVOKEVIRTUAL:
                            // two byte method ref
                            {
                            int n = stream.readUnsignedShort();
                            sParms = Integer.toString(n);
                            sNotes = m_aconst[n].toString();
                            }
                            break;

                        case LDC:
                        case LDC_W:
                        case LDC2_W:
                            // one or two byte index of constant (int, float,
                            // string, long, or double)
                            {
                            int n = (op == LDC ? stream.readUnsignedByte()
                                               : stream.readUnsignedShort());
                            sParms = Integer.toString(n);
                            sNotes = m_aconst[n].toString();
                            }
                            break;

                        case LOOKUPSWITCH:
                            // NOP pad to 4-byte boundary
                            // 4-byte default (no match) offset from lookupswitch instruction
                            // 4-byte cases count
                            // [cases] 4-byte case value
                            //         4-byte offsets from lookupswitch instruction
                            {
                            fSwitch = true;
                            fTable  = false;
                            ofOp    = streamRaw.getMarkedOffset();

                            while ((streamRaw.getCurrentOffset() & 0x03) != 0)
                                {
                                if (stream.readUnsignedByte() != NOP)
                                    {
                                    throw new RuntimeException("lookupswitch not alligned using NOP at " + ofOp);
                                    }
                                }

                            ofDft  = stream.readInt();
                            cCases = stream.readInt();
                            }
                            break;

                        case MULTIANEWARRAY:
                            // two byte index of constant class
                            // one byte number of dimensions
                            {
                            int n = stream.readUnsignedShort();
                            int c = stream.readUnsignedByte();

                            sParms = Integer.toString(n) + "," + Integer.toString(c);
                            sNotes = m_aconst[n].toString();
                            }
                            break;

                        case NEWARRAY:
                            // one byte array type
                            {
                            int n = stream.readUnsignedByte();
                            sParms = Integer.toString(n);
                            switch (n)
                                {
                                case  4: sNotes = "boolean"; break;
                                case  5: sNotes = "char"   ; break;
                                case  6: sNotes = "float"  ; break;
                                case  7: sNotes = "double" ; break;
                                case  8: sNotes = "byte"   ; break;
                                case  9: sNotes = "short"  ; break;
                                case 10: sNotes = "int"    ; break;
                                case 11: sNotes = "long"   ; break;
                                }
                            }
                            break;

                        case SIPUSH:
                            // two byte short value
                            {
                            int n = stream.readShort();
                            sParms = Integer.toString(n);
                            sNotes = "(0x" + Integer.toHexString(n) + ")";
                            }
                            break;

                        case TABLESWITCH:
                            // NOP pad to 4-byte boundary
                            // 4-byte default (no match) offset from tableswitch instruction
                            // 4-byte low value
                            // 4-byte high value
                            // [high-low+1] 4-byte offsets from tableswitch instruction
                            {
                            fSwitch = true;
                            fTable  = true;
                            ofOp    = streamRaw.getMarkedOffset();

                            while ((streamRaw.getCurrentOffset() & 0x03) != 0)
                                {
                                if (stream.readUnsignedByte() != NOP)
                                    {
                                    throw new RuntimeException("tableswitch not alligned using NOP at " + ofOp);
                                    }
                                }

                            ofDft  = stream.readInt();
                            nCase  = stream.readInt();
                            cCases  = stream.readInt() - nCase + 1;
                            }
                            break;
                        }
                    }

                // format header
                int    ofStart = streamRaw.getMarkedOffset();
                int    ofEnd   = streamRaw.getCurrentOffset();
                byte[] abBytes = streamRaw.getMarkedBytes();
                formatOpHeader(achHead, ofStart, cDecDigits, cHexDigits, abBytes, 0);

                // format name, operands, and notes
                String sDisp = (sName == null ? "    " : sName + " ");
                if (sParms != null)
                    {
                    sDisp += sParms;
                    }
                if (sDisp.length() < 25)
                    {
                    sDisp = (sDisp + sFill).substring(0, 25);
                    }
                if (sNotes != null)
                    {
                    sDisp += sTab + sNotes;
                    }

                out.print(sIndent);
                out.print(achHead);
                out.println(sDisp);

                if (abBytes.length > 4)
                    {
                    for (int of = ofStart + 4; of < ofEnd; of += 4)
                        {
                        formatOpHeader(achHead, of, cDecDigits, cHexDigits, abBytes, of - ofStart);
                        out.print(sIndent);
                        out.println(achHead);
                        }
                    }

                if (streamRaw.available() == 0)
                    {
                    break;
                    }
                }
            }
        catch (IOException e)
            {
            out(e);
            throw new RuntimeException("Illegal \"Code\" Attribute structure.  (" + e.toString() + ")");
            }
        }

    /**
    * @param ach         format into this char array
    * @param nOffset     the code offset being displayed
    * @param cDecDigits  the number of digits to display the decimal offset
    * @param cHexDigits  the number of digits to display the hex offset
    * @param ab          the bytes to hex dump (up to 4 bytes dumped)
    * @param ofFirst     the offset into ab to start dumping at
    */
    private void formatOpHeader(char[] ach, int nOffset, int cDecDigits, int cHexDigits, byte[] ab, int iFirst)
        {
        //  0123456789012345678901234
        // "[dddd/xxxx] xx xx xx xx  "

        int of = cDecDigits;
        int n  = nOffset;
        while (of > 0)
            {
            ach[of--] = (char) ('0' + n % 10);
            n /= 10;
            }

        of = cDecDigits + cHexDigits + 1;
        for (int i = 0; i < cHexDigits; ++i)
            {
            ach[of--] = HEX[nOffset & 0x0F];
            nOffset >>= 4;
            }

        of = cDecDigits + cHexDigits + 4;
        int iLast = iFirst + 4;
        int cb = ab.length;
        for (int i = iFirst; i < iLast; ++i)
            {
            if (i >= cb)
                {
                ach[of  ] = ' ';
                ach[of+1] = ' ';
                }
            else
                {
                n = ((int) ab[i]) & 0xFF;
                ach[of  ] = HEX[(n & 0xF0) >> 4];
                ach[of+1] = HEX[(n & 0x0F)     ];
                }
            of += 3;
            }
        }

    /**
    * Hex characters.
    */
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    /**
    * opcodes:  enumerated by opcode value.
    */
    public static final int NOP             = 0x00;
    public static final int ACONST_NULL     = 0x01;
    public static final int ICONST_M1       = 0x02;
    public static final int ICONST_0        = 0x03;
    public static final int ICONST_1        = 0x04;
    public static final int ICONST_2        = 0x05;
    public static final int ICONST_3        = 0x06;
    public static final int ICONST_4        = 0x07;
    public static final int ICONST_5        = 0x08;
    public static final int LCONST_0        = 0x09;
    public static final int LCONST_1        = 0x0a;
    public static final int FCONST_0        = 0x0b;
    public static final int FCONST_1        = 0x0c;
    public static final int FCONST_2        = 0x0d;
    public static final int DCONST_0        = 0x0e;
    public static final int DCONST_1        = 0x0f;
    public static final int BIPUSH          = 0x10;
    public static final int SIPUSH          = 0x11;
    public static final int LDC             = 0x12;
    public static final int LDC_W           = 0x13;
    public static final int LDC2_W          = 0x14;
    public static final int ILOAD           = 0x15;
    public static final int LLOAD           = 0x16;
    public static final int FLOAD           = 0x17;
    public static final int DLOAD           = 0x18;
    public static final int ALOAD           = 0x19;
    public static final int ILOAD_0         = 0x1a;
    public static final int ILOAD_1         = 0x1b;
    public static final int ILOAD_2         = 0x1c;
    public static final int ILOAD_3         = 0x1d;
    public static final int LLOAD_0         = 0x1e;
    public static final int LLOAD_1         = 0x1f;
    public static final int LLOAD_2         = 0x20;
    public static final int LLOAD_3         = 0x21;
    public static final int FLOAD_0         = 0x22;
    public static final int FLOAD_1         = 0x23;
    public static final int FLOAD_2         = 0x24;
    public static final int FLOAD_3         = 0x25;
    public static final int DLOAD_0         = 0x26;
    public static final int DLOAD_1         = 0x27;
    public static final int DLOAD_2         = 0x28;
    public static final int DLOAD_3         = 0x29;
    public static final int ALOAD_0         = 0x2a;
    public static final int ALOAD_1         = 0x2b;
    public static final int ALOAD_2         = 0x2c;
    public static final int ALOAD_3         = 0x2d;
    public static final int IALOAD          = 0x2e;
    public static final int LALOAD          = 0x2f;
    public static final int FALOAD          = 0x30;
    public static final int DALOAD          = 0x31;
    public static final int AALOAD          = 0x32;
    public static final int BALOAD          = 0x33;
    public static final int CALOAD          = 0x34;
    public static final int SALOAD          = 0x35;
    public static final int ISTORE          = 0x36;
    public static final int LSTORE          = 0x37;
    public static final int FSTORE          = 0x38;
    public static final int DSTORE          = 0x39;
    public static final int ASTORE          = 0x3a;
    public static final int ISTORE_0        = 0x3b;
    public static final int ISTORE_1        = 0x3c;
    public static final int ISTORE_2        = 0x3d;
    public static final int ISTORE_3        = 0x3e;
    public static final int LSTORE_0        = 0x3f;
    public static final int LSTORE_1        = 0x40;
    public static final int LSTORE_2        = 0x41;
    public static final int LSTORE_3        = 0x42;
    public static final int FSTORE_0        = 0x43;
    public static final int FSTORE_1        = 0x44;
    public static final int FSTORE_2        = 0x45;
    public static final int FSTORE_3        = 0x46;
    public static final int DSTORE_0        = 0x47;
    public static final int DSTORE_1        = 0x48;
    public static final int DSTORE_2        = 0x49;
    public static final int DSTORE_3        = 0x4a;
    public static final int ASTORE_0        = 0x4b;
    public static final int ASTORE_1        = 0x4c;
    public static final int ASTORE_2        = 0x4d;
    public static final int ASTORE_3        = 0x4e;
    public static final int IASTORE         = 0x4f;
    public static final int LASTORE         = 0x50;
    public static final int FASTORE         = 0x51;
    public static final int DASTORE         = 0x52;
    public static final int AASTORE         = 0x53;
    public static final int BASTORE         = 0x54;
    public static final int CASTORE         = 0x55;
    public static final int SASTORE         = 0x56;
    public static final int POP             = 0x57;
    public static final int POP2            = 0x58;
    public static final int DUP             = 0x59;
    public static final int DUP_X1          = 0x5a;
    public static final int DUP_X2          = 0x5b;
    public static final int DUP2            = 0x5c;
    public static final int DUP2_X1         = 0x5d;
    public static final int DUP2_X2         = 0x5e;
    public static final int SWAP            = 0x5f;
    public static final int IADD            = 0x60;
    public static final int LADD            = 0x61;
    public static final int FADD            = 0x62;
    public static final int DADD            = 0x63;
    public static final int ISUB            = 0x64;
    public static final int LSUB            = 0x65;
    public static final int FSUB            = 0x66;
    public static final int DSUB            = 0x67;
    public static final int IMUL            = 0x68;
    public static final int LMUL            = 0x69;
    public static final int FMUL            = 0x6a;
    public static final int DMUL            = 0x6b;
    public static final int IDIV            = 0x6c;
    public static final int LDIV            = 0x6d;
    public static final int FDIV            = 0x6e;
    public static final int DDIV            = 0x6f;
    public static final int IREM            = 0x70;
    public static final int LREM            = 0x71;
    public static final int FREM            = 0x72;
    public static final int DREM            = 0x73;
    public static final int INEG            = 0x74;
    public static final int LNEG            = 0x75;
    public static final int FNEG            = 0x76;
    public static final int DNEG            = 0x77;
    public static final int ISHL            = 0x78;
    public static final int LSHL            = 0x79;
    public static final int ISHR            = 0x7a;
    public static final int LSHR            = 0x7b;
    public static final int IUSHR           = 0x7c;
    public static final int LUSHR           = 0x7d;
    public static final int IAND            = 0x7e;
    public static final int LAND            = 0x7f;
    public static final int IOR             = 0x80;
    public static final int LOR             = 0x81;
    public static final int IXOR            = 0x82;
    public static final int LXOR            = 0x83;
    public static final int IINC            = 0x84;
    public static final int I2L             = 0x85;
    public static final int I2F             = 0x86;
    public static final int I2D             = 0x87;
    public static final int L2I             = 0x88;
    public static final int L2F             = 0x89;
    public static final int L2D             = 0x8a;
    public static final int F2I             = 0x8b;
    public static final int F2L             = 0x8c;
    public static final int F2D             = 0x8d;
    public static final int D2I             = 0x8e;
    public static final int D2L             = 0x8f;
    public static final int D2F             = 0x90;
    public static final int I2B             = 0x91;
    public static final int I2C             = 0x92;
    public static final int I2S             = 0x93;
    public static final int LCMP            = 0x94;
    public static final int FCMPL           = 0x95;
    public static final int FCMPG           = 0x96;
    public static final int DCMPL           = 0x97;
    public static final int DCMPG           = 0x98;
    public static final int IFEQ            = 0x99;
    public static final int IFNE            = 0x9a;
    public static final int IFLT            = 0x9b;
    public static final int IFGE            = 0x9c;
    public static final int IFGT            = 0x9d;
    public static final int IFLE            = 0x9e;
    public static final int IF_ICMPEQ       = 0x9f;
    public static final int IF_ICMPNE       = 0xa0;
    public static final int IF_ICMPLT       = 0xa1;
    public static final int IF_ICMPGE       = 0xa2;
    public static final int IF_ICMPGT       = 0xa3;
    public static final int IF_ICMPLE       = 0xa4;
    public static final int IF_ACMPEQ       = 0xa5;
    public static final int IF_ACMPNE       = 0xa6;
    public static final int GOTO            = 0xa7;
    public static final int JSR             = 0xa8;
    public static final int RET             = 0xa9;
    public static final int TABLESWITCH     = 0xaa;
    public static final int LOOKUPSWITCH    = 0xab;
    public static final int IRETURN         = 0xac;
    public static final int LRETURN         = 0xad;
    public static final int FRETURN         = 0xae;
    public static final int DRETURN         = 0xaf;
    public static final int ARETURN         = 0xb0;
    public static final int RETURN          = 0xb1;
    public static final int GETSTATIC       = 0xb2;
    public static final int PUTSTATIC       = 0xb3;
    public static final int GETFIELD        = 0xb4;
    public static final int PUTFIELD        = 0xb5;
    public static final int INVOKEVIRTUAL   = 0xb6;
    public static final int INVOKESPECIAL   = 0xb7;
    public static final int INVOKESTATIC    = 0xb8;
    public static final int INVOKEINTERFACE = 0xb9;
    public static final int NEW             = 0xbb;
    public static final int NEWARRAY        = 0xbc;
    public static final int ANEWARRAY       = 0xbd;
    public static final int ARRAYLENGTH     = 0xbe;
    public static final int ATHROW          = 0xbf;
    public static final int CHECKCAST       = 0xc0;
    public static final int INSTANCEOF      = 0xc1;
    public static final int MONITORENTER    = 0xc2;
    public static final int MONITOREXIT     = 0xc3;
    public static final int WIDE            = 0xc4;
    public static final int MULTIANEWARRAY  = 0xc5;
    public static final int IFNULL          = 0xc6;
    public static final int IFNONNULL       = 0xc7;
    public static final int GOTO_W          = 0xc8;
    public static final int JSR_W           = 0xc9;

    /**
    * opcodes:  name by opcode value.
    */
    private static final String[] OPNAME = new String[0x100];
    static
        {
        OPNAME[0x00] = "nop";
        OPNAME[0x01] = "aconst_null";
        OPNAME[0x02] = "iconst_m1";
        OPNAME[0x03] = "iconst_0";
        OPNAME[0x04] = "iconst_1";
        OPNAME[0x05] = "iconst_2";
        OPNAME[0x06] = "iconst_3";
        OPNAME[0x07] = "iconst_4";
        OPNAME[0x08] = "iconst_5";
        OPNAME[0x09] = "lconst_0";
        OPNAME[0x0a] = "lconst_1";
        OPNAME[0x0b] = "fconst_0";
        OPNAME[0x0c] = "fconst_1";
        OPNAME[0x0d] = "fconst_2";
        OPNAME[0x0e] = "dconst_0";
        OPNAME[0x0f] = "dconst_1";
        OPNAME[0x10] = "bipush";
        OPNAME[0x11] = "sipush";
        OPNAME[0x12] = "ldc";
        OPNAME[0x13] = "ldc_w";
        OPNAME[0x14] = "ldc2_w";
        OPNAME[0x15] = "iload";
        OPNAME[0x16] = "lload";
        OPNAME[0x17] = "fload";
        OPNAME[0x18] = "dload";
        OPNAME[0x19] = "aload";
        OPNAME[0x1a] = "iload_0";
        OPNAME[0x1b] = "iload_1";
        OPNAME[0x1c] = "iload_2";
        OPNAME[0x1d] = "iload_3";
        OPNAME[0x1e] = "lload_0";
        OPNAME[0x1f] = "lload_1";
        OPNAME[0x20] = "lload_2";
        OPNAME[0x21] = "lload_3";
        OPNAME[0x22] = "fload_0";
        OPNAME[0x23] = "fload_1";
        OPNAME[0x24] = "fload_2";
        OPNAME[0x25] = "fload_3";
        OPNAME[0x26] = "dload_0";
        OPNAME[0x27] = "dload_1";
        OPNAME[0x28] = "dload_2";
        OPNAME[0x29] = "dload_3";
        OPNAME[0x2a] = "aload_0";
        OPNAME[0x2b] = "aload_1";
        OPNAME[0x2c] = "aload_2";
        OPNAME[0x2d] = "aload_3";
        OPNAME[0x2e] = "iaload";
        OPNAME[0x2f] = "laload";
        OPNAME[0x30] = "faload";
        OPNAME[0x31] = "daload";
        OPNAME[0x32] = "aaload";
        OPNAME[0x33] = "baload";
        OPNAME[0x34] = "caload";
        OPNAME[0x35] = "saload";
        OPNAME[0x36] = "istore";
        OPNAME[0x37] = "lstore";
        OPNAME[0x38] = "fstore";
        OPNAME[0x39] = "dstore";
        OPNAME[0x3a] = "astore";
        OPNAME[0x3b] = "istore_0";
        OPNAME[0x3c] = "istore_1";
        OPNAME[0x3d] = "istore_2";
        OPNAME[0x3e] = "istore_3";
        OPNAME[0x3f] = "lstore_0";
        OPNAME[0x40] = "lstore_1";
        OPNAME[0x41] = "lstore_2";
        OPNAME[0x42] = "lstore_3";
        OPNAME[0x43] = "fstore_0";
        OPNAME[0x44] = "fstore_1";
        OPNAME[0x45] = "fstore_2";
        OPNAME[0x46] = "fstore_3";
        OPNAME[0x47] = "dstore_0";
        OPNAME[0x48] = "dstore_1";
        OPNAME[0x49] = "dstore_2";
        OPNAME[0x4a] = "dstore_3";
        OPNAME[0x4b] = "astore_0";
        OPNAME[0x4c] = "astore_1";
        OPNAME[0x4d] = "astore_2";
        OPNAME[0x4e] = "astore_3";
        OPNAME[0x4f] = "iastore";
        OPNAME[0x50] = "lastore";
        OPNAME[0x51] = "fastore";
        OPNAME[0x52] = "dastore";
        OPNAME[0x53] = "aastore";
        OPNAME[0x54] = "bastore";
        OPNAME[0x55] = "castore";
        OPNAME[0x56] = "sastore";
        OPNAME[0x57] = "pop";
        OPNAME[0x58] = "pop2";
        OPNAME[0x59] = "dup";
        OPNAME[0x5a] = "dup_x1";
        OPNAME[0x5b] = "dup_x2";
        OPNAME[0x5c] = "dup2";
        OPNAME[0x5d] = "dup2_x1";
        OPNAME[0x5e] = "dup2_x2";
        OPNAME[0x5f] = "swap";
        OPNAME[0x60] = "iadd";
        OPNAME[0x61] = "ladd";
        OPNAME[0x62] = "fadd";
        OPNAME[0x63] = "dadd";
        OPNAME[0x64] = "isub";
        OPNAME[0x65] = "lsub";
        OPNAME[0x66] = "fsub";
        OPNAME[0x67] = "dsub";
        OPNAME[0x68] = "imul";
        OPNAME[0x69] = "lmul";
        OPNAME[0x6a] = "fmul";
        OPNAME[0x6b] = "dmul";
        OPNAME[0x6c] = "idiv";
        OPNAME[0x6d] = "ldiv";
        OPNAME[0x6e] = "fdiv";
        OPNAME[0x6f] = "ddiv";
        OPNAME[0x70] = "irem";
        OPNAME[0x71] = "lrem";
        OPNAME[0x72] = "frem";
        OPNAME[0x73] = "drem";
        OPNAME[0x74] = "ineg";
        OPNAME[0x75] = "lneg";
        OPNAME[0x76] = "fneg";
        OPNAME[0x77] = "dneg";
        OPNAME[0x78] = "ishl";
        OPNAME[0x79] = "lshl";
        OPNAME[0x7a] = "ishr";
        OPNAME[0x7b] = "lshr";
        OPNAME[0x7c] = "iushr";
        OPNAME[0x7d] = "lushr";
        OPNAME[0x7e] = "iand";
        OPNAME[0x7f] = "land";
        OPNAME[0x80] = "ior";
        OPNAME[0x81] = "lor";
        OPNAME[0x82] = "ixor";
        OPNAME[0x83] = "lxor";
        OPNAME[0x84] = "iinc";
        OPNAME[0x85] = "i2l";
        OPNAME[0x86] = "i2f";
        OPNAME[0x87] = "i2d";
        OPNAME[0x88] = "l2i";
        OPNAME[0x89] = "l2f";
        OPNAME[0x8a] = "l2d";
        OPNAME[0x8b] = "f2i";
        OPNAME[0x8c] = "f2l";
        OPNAME[0x8d] = "f2d";
        OPNAME[0x8e] = "d2i";
        OPNAME[0x8f] = "d2l";
        OPNAME[0x90] = "d2f";
        OPNAME[0x91] = "i2b";
        OPNAME[0x92] = "i2c";
        OPNAME[0x93] = "i2s";
        OPNAME[0x94] = "lcmp";
        OPNAME[0x95] = "fcmpl";
        OPNAME[0x96] = "fcmpg";
        OPNAME[0x97] = "dcmpl";
        OPNAME[0x98] = "dcmpg";
        OPNAME[0x99] = "ifeq";
        OPNAME[0x9a] = "ifne";
        OPNAME[0x9b] = "iflt";
        OPNAME[0x9c] = "ifge";
        OPNAME[0x9d] = "ifgt";
        OPNAME[0x9e] = "ifle";
        OPNAME[0x9f] = "if_icmpeq";
        OPNAME[0xa0] = "if_icmpne";
        OPNAME[0xa1] = "if_icmplt";
        OPNAME[0xa2] = "if_icmpge";
        OPNAME[0xa3] = "if_icmpgt";
        OPNAME[0xa4] = "if_icmple";
        OPNAME[0xa5] = "if_acmpeq";
        OPNAME[0xa6] = "if_acmpne";
        OPNAME[0xa7] = "goto";
        OPNAME[0xa8] = "jsr";
        OPNAME[0xa9] = "ret";
        OPNAME[0xaa] = "tableswitch";
        OPNAME[0xab] = "lookupswitch";
        OPNAME[0xac] = "ireturn";
        OPNAME[0xad] = "lreturn";
        OPNAME[0xae] = "freturn";
        OPNAME[0xaf] = "dreturn";
        OPNAME[0xb0] = "areturn";
        OPNAME[0xb1] = "return";
        OPNAME[0xb2] = "getstatic";
        OPNAME[0xb3] = "putstatic";
        OPNAME[0xb4] = "getfield";
        OPNAME[0xb5] = "putfield";
        OPNAME[0xb6] = "invokevirtual";
        OPNAME[0xb7] = "invokespecial";
        OPNAME[0xb8] = "invokestatic";
        OPNAME[0xb9] = "invokeinterface";
        OPNAME[0xbb] = "new";
        OPNAME[0xbc] = "newarray";
        OPNAME[0xbd] = "anewarray";
        OPNAME[0xbe] = "arraylength";
        OPNAME[0xbf] = "athrow";
        OPNAME[0xc0] = "checkcast";
        OPNAME[0xc1] = "instanceof";
        OPNAME[0xc2] = "monitorenter";
        OPNAME[0xc3] = "monitorexit";
        OPNAME[0xc4] = "wide";
        OPNAME[0xc5] = "multianewarray";
        OPNAME[0xc6] = "ifnull";
        OPNAME[0xc7] = "ifnonnull";
        OPNAME[0xc8] = "goto_w";
        OPNAME[0xc9] = "jsr_w";
        }

    }


class MarkedByteArrayInputStream extends ByteArrayInputStream
    {
    protected int posPrev;
    protected int posStart;

    public MarkedByteArrayInputStream(byte buf[])
        {
        this(buf, 0, buf.length);
        }

    public MarkedByteArrayInputStream(byte buf[], int offset, int length)
        {
        super(buf, offset, length);
        posPrev  = pos;
        posStart = offset;
        }

    public int getMarkedOffset()
        {
        return posPrev - posStart;
        }

    public int getCurrentOffset()
        {
        return pos - posStart;
        }

    public byte[] getMarkedBytes()
        {
        int     cb = pos - posPrev;
        byte[]  ab = new byte[cb];
        System.arraycopy(buf, posPrev, ab, 0, cb);
        posPrev = pos;
        return ab;
        }
    }
