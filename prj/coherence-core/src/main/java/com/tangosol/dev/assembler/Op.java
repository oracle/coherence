/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.assembler;


import java.io.IOException;
import java.io.EOFException;
import java.io.DataInput;
import java.io.DataOutput;

import java.util.Enumeration;
import java.util.Vector;

import com.tangosol.io.ByteArrayReadBuffer;
import com.tangosol.io.ReadBuffer;

import com.tangosol.util.NullImplementation;


/**
* Represents a Java Virtual Machine assembly (JASM) operation, which is
* disassembled from and/or assembled to a Java byte code.
*
* @version 0.50, 05/20/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public abstract class Op extends VMStructure implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the specified JASM op.
    *
    * @param iOp    the JASM op value
    */
    protected Op(int iOp)
        {
        m_iOp   = iOp;
        m_iHash = sm_iLastHash = (int) (((long) sm_iLastHash + BIGPRIME) % INTLIMIT);
        }


    // ----- Op operations --------------------------------------------------

    /**
    * Determine if the passed value is a legal JVM byte code.
    *
    * @param ub  the unsigned byte code value
    *
    * @return true if the value is a byte code defined by the JVM spec
    */
    public static boolean isByteCode(int ub)
        {
        // 0x00 (NOP) through 0xc9 (JSR_W) except for 0xba
        return ub >= NOP && ub <= JSR_W && ub != 0xba;
        }

    /**
    * For disassembly of ops, provide the requested variable.
    *
    * @param  iOp   the variable declaration op value (IVAR, ...)
    * @param  iVar  the variable index
    *
    * @return the requested variable
    */
    protected static OpDeclare getVariable(int iOp, int iVar, OpDeclare[][] aavar)
        {
        int       iType = iOp - IVAR;
        OpDeclare decl  = aavar[iType][iVar];

        if (decl == null)
            {
            switch (iOp)
                {
                case IVAR:
                    decl = new Ivar(iVar);
                    break;
                case LVAR:
                    decl = new Lvar(iVar);
                    break;
                case FVAR:
                    decl = new Fvar(iVar);
                    break;
                case DVAR:
                    decl = new Dvar(iVar);
                    break;
                case AVAR:
                    decl = new Avar(iVar);
                    break;
                case RVAR:
                    decl = new Rvar(iVar);
                    break;
                }

            aavar[iType][iVar] = decl;
            }

        return decl;
        }

    /**
    * For disassembly of ops, get the requested label.
    *
    * @param of      byte code offset
    * @param alabel  an array of labels per byte code offset
    *
    * @exception IOException
    */
    private static Label getLabel(int of, Op[] alabel)
            throws IOException
        {
        try
            {
            Label label = (Label) alabel[of];
            if (label == null)
                {
                label = new Label(String.valueOf(of));
                label.setOffset(of);
                alabel[of] = label;
                }
            return label;
            }
        catch (ArrayIndexOutOfBoundsException e)
            {
            throw new IOException(CLASS + ".getLabel:  Illegal label offset -- " + of);
            }
        }

    /**
    * For disassembly of ops, add the try at the specified offset.
    *
    * @param of        byte code offset
    * @param opTry     the try op
    * @param aopDefer  the deferred ops, by offset
    *
    * @exception IOException
    */
    private static void addTry(int of, Try opTry, Op[] aopDefer)
            throws IOException
        {
        // store the offset of the op
        opTry.setOffset(of);

        // see if any deferred ops are already at that offset
        Op op;
        try
            {
            op = aopDefer[of];
            }
        catch (ArrayIndexOutOfBoundsException e)
            {
            throw new IOException(CLASS + ".addTry:  Illegal Try offset!");
            }

        // place the try after any instances of catch and after any labels
        // but before any instances of try
        if (op == null || op instanceof Try)
            {
            // link the try in at the beginning
            aopDefer[of] = opTry;
            opTry.setNext(op);
            }
        else
            {
            // link the try in immediately after the catch's and label's
            Op opPrev = op;
            Op opNext = opPrev.getNext();
            while (opNext instanceof Catch || opNext instanceof Label)
                {
                opPrev = opNext;
                opNext = opPrev.getNext();
                }
            opPrev.setNext(opTry );
            opTry .setNext(opNext);
            }
        }

    /**
    * For disassembly of ops, add the catch at the specified offset.
    *
    * @param of        byte code offset
    * @param opCatch   the catch op
    * @param aopDefer  the deferred ops, by offset
    *
    * @exception IOException
    */
    private static void addCatch(int of, Catch opCatch, Op[] aopDefer)
            throws IOException
        {
        // store the offset of the op
        opCatch.setOffset(of);

        // see if any deferred ops are already at that offset
        Op op;
        try
            {
            op = aopDefer[of];
            }
        catch (ArrayIndexOutOfBoundsException e)
            {
            throw new IOException(CLASS + ".addCatch:  Illegal Catch offset!");
            }

        // see if the deferred ops are instances of Catch
        if (op instanceof Catch)
            {
            // link the try in after the catch's
            Op opPrev = op;
            Op opNext = opPrev.getNext();
            while (opNext instanceof Catch)
                {
                opPrev = opNext;
                opNext = opPrev.getNext();
                }
            opPrev .setNext(opCatch);
            opCatch.setNext(opNext );
            }
        else
            {
            // link the catch in at the beginning
            aopDefer[of] = opCatch;
            opCatch.setNext(op);
            }
        }


    /**
    * For disassembly of ops, update the LocalVariable/LocalVariableType
    * tables to point to the disassembled ops.
    *
    * @param attrTable  the local variable table attribute
    * @param aopLabel   labels by op offset
    */
    private static void disassembleVarTable(AbstractLocalVariableTableAttribute attrTable, Op[] aopLabel)
            throws IOException
        {
        for (Enumeration enmr = attrTable.ranges(); enmr.hasMoreElements(); )
            {
            AbstractLocalVariableTableAttribute.Range range =
                (AbstractLocalVariableTableAttribute.Range) enmr.nextElement();
            
            // determine byte code offsets of the range
            int ofInit = range.getOffset();
            int ofStop = ofInit + range.getLength();
            
            // translate offsets to ops
            range.setInit(getLabel(ofInit, aopLabel));
            range.setStop(getLabel(ofStop, aopLabel));
            }
        }

    
    /**
    * Based on the byte code, which is encountered first in the stream,
    * construct the correct Op class and disassemble it.
    *
    * @param abCode       the byte array containing the byte code portion of
    *                     the "Code" attribute
    * @param cVars        the number of local variable words
    * @param asParam      an array of JVM types for the method parameters
    * @param vectCatch    the table of try/catch data
    * @param attrLine     the line number table, if available, null otherwise
    * @param attrVar      the local variable table, if available
    * @param attrVarType  the local variable type table, if available
    * @param pool         the constant pool for the class
    * @param aopBounds    an array to return the first and last op in
    *
    * @exception IOException
    */
    protected static void disassembleOps(byte[] abCode, int cVars, String[] asParam, Vector vectCatch, LineNumberTableAttribute attrLine, LocalVariableTableAttribute attrVar, LocalVariableTypeTableAttribute attrVarType, ConstantPool pool, Op[] aopBounds)
            throws IOException
        {
        ReadBuffer.BufferInput stream = new ByteArrayReadBuffer(abCode).getBufferInput();

        // prime the line number enumeration
        Enumeration enmrLines = (attrLine == null ? NullImplementation.getEnumeration()
                                                  : attrLine.entries()         );
        LineNumberTableAttribute.Entry line =
                (LineNumberTableAttribute.Entry)
                (enmrLines.hasMoreElements() ? enmrLines.nextElement() : null);
        int iLine = 0;

        // build a table of variables by type/by slot
        OpDeclare[][] aavar = new OpDeclare[6][cVars];

        // labels by op offset (collected as necessary)
        Op[] aopDefer = new Op[abCode.length + 1];

        // support for switch parsing
        boolean  fSwitchOp = false;
        OpSwitch opSwitch  = null;
        int      iOpSwitch = 0;
        int      cCases    = 0;
        int      iCase     = 0;

        Op   opFirst  = null;
        Op   opLast   = null;
        while (true)
            {
            // get the offset of the byte code instruction
            int ofOp = stream.getOffset();
            Op  op   = null;

            if (fSwitchOp)
                {
                // lookupswitch cases have case values in the byte code
                // (tableswitch cases are ordered; their values are implied)
                if (iOpSwitch == LOOKUPSWITCH)
                    {
                    iCase = stream.readInt();
                    }

                // case branches to a label
                Label label = getLabel(((Op) opSwitch).m_of + stream.readInt(), aopDefer);

                // only create a case op if the case label is not the default
                if (label != opSwitch.getLabel())
                    {
                    op = new Case(iCase, label);
                    }

                ++iCase;
                if (--cCases <= 0)
                    {
                    fSwitchOp = false;
                    }
                }
            else
                {
                // check for line change
                if (line != null && ofOp >= line.getOffset())
                    {
                    iLine = line.getLine();
                    line  = (LineNumberTableAttribute.Entry)
                            (enmrLines.hasMoreElements() ? enmrLines.nextElement() : null);
                    }

                // read the byte code instruction
                int iOp;
                try
                    {
                    // read and validate the op
                    iOp = stream.readUnsignedByte();
                    }
                catch (EOFException e)
                    {
                    break;
                    }

                // create the op for the byte code
                switch (iOp)
                    {
                    case NOP:
                        op = new Nop();
                        break;

                    case ACONST_NULL:
                        op = new Aconst();
                        break;

                    case ICONST_M1:
                        op = new Iconst(CONSTANT_ICONST_M1);
                        break;

                    case ICONST_0:
                        op = new Iconst(CONSTANT_ICONST_0);
                        break;

                    case ICONST_1:
                        op = new Iconst(CONSTANT_ICONST_1);
                        break;

                    case ICONST_2:
                        op = new Iconst(CONSTANT_ICONST_2);
                        break;

                    case ICONST_3:
                        op = new Iconst(CONSTANT_ICONST_3);
                        break;

                    case ICONST_4:
                        op = new Iconst(CONSTANT_ICONST_4);
                        break;

                    case ICONST_5:
                        op = new Iconst(CONSTANT_ICONST_5);
                        break;

                    case LCONST_0:
                        op = new Lconst(CONSTANT_LCONST_0);
                        break;

                    case LCONST_1:
                        op = new Lconst(CONSTANT_LCONST_1);
                        break;

                    case FCONST_0:
                        op = new Fconst(CONSTANT_FCONST_0);
                        break;

                    case FCONST_1:
                        op = new Fconst(CONSTANT_FCONST_1);
                        break;

                    case FCONST_2:
                        op = new Fconst(CONSTANT_FCONST_2);
                        break;

                    case DCONST_0:
                        op = new Dconst(CONSTANT_DCONST_0);
                        break;

                    case DCONST_1:
                        op = new Dconst(CONSTANT_DCONST_1);
                        break;

                    case BIPUSH:
                        op = new Iconst(new IntConstant(stream.readByte()));
                        break;

                    case SIPUSH:
                        op = new Iconst(new IntConstant(stream.readShort()));
                        break;

                    case LDC:
                    case LDC_W:
                        {
                        int iConst = (iOp == LDC ? stream.readUnsignedByte()
                                                 : stream.readUnsignedShort());
                        Constant constant = pool.getConstant(iConst);
                        if (constant instanceof StringConstant)
                            {
                            op = new Aconst((StringConstant) constant);
                            }
                        else if (constant instanceof IntConstant)
                            {
                            op = new Iconst((IntConstant) constant);
                            }
                        else if (constant instanceof FloatConstant)
                            {
                            op = new Fconst((FloatConstant) constant);
                            }
                        else if (constant instanceof ClassConstant)
                            {
                            op = new Aconst((ClassConstant) constant);
                            }
                        else
                            {
                            throw new IOException(CLASS + ".disassembleOps:  " +
                                    "Invalid LDC/LDC_W constant type!");
                            }
                        }
                        break;

                    case LDC2_W:
                        {
                        Constant constant = pool.getConstant(stream.readUnsignedShort());
                        if (constant instanceof LongConstant)
                            {
                            op = new Lconst((LongConstant) constant);
                            }
                        else if (constant instanceof DoubleConstant)
                            {
                            op = new Dconst((DoubleConstant) constant);
                            }
                        else
                            {
                            throw new IOException(CLASS + ".disassembleOps:  " +
                                    "Invalid LDC2_W constant type!");
                            }
                        }
                        break;

                    case ILOAD:
                        op = new Iload((Ivar) getVariable(IVAR, stream.readUnsignedByte(), aavar));
                        break;

                    case LLOAD:
                        op = new Lload((Lvar) getVariable(LVAR, stream.readUnsignedByte(), aavar));
                        break;

                    case FLOAD:
                        op = new Fload((Fvar) getVariable(FVAR, stream.readUnsignedByte(), aavar));
                        break;

                    case DLOAD:
                        op = new Dload((Dvar) getVariable(DVAR, stream.readUnsignedByte(), aavar));
                        break;

                    case ALOAD:
                        op = new Aload((Avar) getVariable(AVAR, stream.readUnsignedByte(), aavar));
                        break;

                    case ILOAD_0:
                    case ILOAD_1:
                    case ILOAD_2:
                    case ILOAD_3:
                        op = new Iload((Ivar) getVariable(IVAR, iOp - ILOAD_0, aavar));
                        break;

                    case LLOAD_0:
                    case LLOAD_1:
                    case LLOAD_2:
                    case LLOAD_3:
                        op = new Lload((Lvar) getVariable(LVAR, iOp - LLOAD_0, aavar));
                        break;

                    case FLOAD_0:
                    case FLOAD_1:
                    case FLOAD_2:
                    case FLOAD_3:
                        op = new Fload((Fvar) getVariable(FVAR, iOp - FLOAD_0, aavar));
                        break;

                    case DLOAD_0:
                    case DLOAD_1:
                    case DLOAD_2:
                    case DLOAD_3:
                        op = new Dload((Dvar) getVariable(DVAR, iOp - DLOAD_0, aavar));
                        break;

                    case ALOAD_0:
                    case ALOAD_1:
                    case ALOAD_2:
                    case ALOAD_3:
                        op = new Aload((Avar) getVariable(AVAR, iOp - ALOAD_0, aavar));
                        break;

                    case IALOAD:
                        op = new Iaload();
                        break;

                    case LALOAD:
                        op = new Laload();
                        break;

                    case FALOAD:
                        op = new Faload();
                        break;

                    case DALOAD:
                        op = new Daload();
                        break;

                    case AALOAD:
                        op = new Aaload();
                        break;

                    case BALOAD:
                        op = new Baload();
                        break;

                    case CALOAD:
                        op = new Caload();
                        break;

                    case SALOAD:
                        op = new Saload();
                        break;

                    case ISTORE:
                        op = new Istore((Ivar) getVariable(IVAR, stream.readUnsignedByte(), aavar));
                        break;

                    case LSTORE:
                        op = new Lstore((Lvar) getVariable(LVAR, stream.readUnsignedByte(), aavar));
                        break;

                    case FSTORE:
                        op = new Fstore((Fvar) getVariable(FVAR, stream.readUnsignedByte(), aavar));
                        break;

                    case DSTORE:
                        op = new Dstore((Dvar) getVariable(DVAR, stream.readUnsignedByte(), aavar));
                        break;

                    case ASTORE:
                        op = new Astore((Avar) getVariable(AVAR, stream.readUnsignedByte(), aavar));
                        break;

                    case ISTORE_0:
                    case ISTORE_1:
                    case ISTORE_2:
                    case ISTORE_3:
                        op = new Istore((Ivar) getVariable(IVAR, iOp - ISTORE_0, aavar));
                        break;

                    case LSTORE_0:
                    case LSTORE_1:
                    case LSTORE_2:
                    case LSTORE_3:
                        op = new Lstore((Lvar) getVariable(LVAR, iOp - LSTORE_0, aavar));
                        break;

                    case FSTORE_0:
                    case FSTORE_1:
                    case FSTORE_2:
                    case FSTORE_3:
                        op = new Fstore((Fvar) getVariable(FVAR, iOp - FSTORE_0, aavar));
                        break;

                    case DSTORE_0:
                    case DSTORE_1:
                    case DSTORE_2:
                    case DSTORE_3:
                        op = new Dstore((Dvar) getVariable(DVAR, iOp - DSTORE_0, aavar));
                        break;

                    case ASTORE_0:
                    case ASTORE_1:
                    case ASTORE_2:
                    case ASTORE_3:
                        op = new Astore((Avar) getVariable(AVAR, iOp - ASTORE_0, aavar));
                        break;

                    case IASTORE:
                        op = new Iastore();
                        break;

                    case LASTORE:
                        op = new Lastore();
                        break;

                    case FASTORE:
                        op = new Fastore();
                        break;

                    case DASTORE:
                        op = new Dastore();
                        break;

                    case AASTORE:
                        op = new Aastore();
                        break;

                    case BASTORE:
                        op = new Bastore();
                        break;

                    case CASTORE:
                        op = new Castore();
                        break;

                    case SASTORE:
                        op = new Sastore();
                        break;

                    case POP:
                        op = new Pop();
                        break;

                    case POP2:
                        op = new Pop2();
                        break;

                    case DUP:
                        op = new Dup();
                        break;

                    case DUP_X1:
                        op = new Dup_x1();
                        break;

                    case DUP_X2:
                        op = new Dup_x2();
                        break;

                    case DUP2:
                        op = new Dup2();
                        break;

                    case DUP2_X1:
                        op = new Dup2_x1();
                        break;

                    case DUP2_X2:
                        op = new Dup2_x2();
                        break;

                    case SWAP:
                        op = new Swap();
                        break;

                    case IADD:
                        op = new Iadd();
                        break;

                    case LADD:
                        op = new Ladd();
                        break;

                    case FADD:
                        op = new Fadd();
                        break;

                    case DADD:
                        op = new Dadd();
                        break;

                    case ISUB:
                        op = new Isub();
                        break;

                    case LSUB:
                        op = new Lsub();
                        break;

                    case FSUB:
                        op = new Fsub();
                        break;

                    case DSUB:
                        op = new Dsub();
                        break;

                    case IMUL:
                        op = new Imul();
                        break;

                    case LMUL:
                        op = new Lmul();
                        break;

                    case FMUL:
                        op = new Fmul();
                        break;

                    case DMUL:
                        op = new Dmul();
                        break;

                    case IDIV:
                        op = new Idiv();
                        break;

                    case LDIV:
                        op = new Ldiv();
                        break;

                    case FDIV:
                        op = new Fdiv();
                        break;

                    case DDIV:
                        op = new Ddiv();
                        break;

                    case IREM:
                        op = new Irem();
                        break;

                    case LREM:
                        op = new Lrem();
                        break;

                    case FREM:
                        op = new Frem();
                        break;

                    case DREM:
                        op = new Drem();
                        break;

                    case INEG:
                        op = new Ineg();
                        break;

                    case LNEG:
                        op = new Lneg();
                        break;

                    case FNEG:
                        op = new Fneg();
                        break;

                    case DNEG:
                        op = new Dneg();
                        break;

                    case ISHL:
                        op = new Ishl();
                        break;

                    case LSHL:
                        op = new Lshl();
                        break;

                    case ISHR:
                        op = new Ishr();
                        break;

                    case LSHR:
                        op = new Lshr();
                        break;

                    case IUSHR:
                        op = new Iushr();
                        break;

                    case LUSHR:
                        op = new Lushr();
                        break;

                    case IAND:
                        op = new Iand();
                        break;

                    case LAND:
                        op = new Land();
                        break;

                    case IOR:
                        op = new Ior();
                        break;

                    case LOR:
                        op = new Lor();
                        break;

                    case IXOR:
                        op = new Ixor();
                        break;

                    case LXOR:
                        op = new Lxor();
                        break;

                    case IINC:
                        {
                        int      iVar = stream.readUnsignedByte();
                        Ivar     var  = (Ivar) getVariable(IVAR, iVar, aavar);
                        short    sInc = (short) stream.readByte();
                        op = new Iinc(var, sInc);
                        }
                        break;

                    case I2L:
                        op = new I2l();
                        break;

                    case I2F:
                        op = new I2f();
                        break;

                    case I2D:
                        op = new I2d();
                        break;

                    case L2I:
                        op = new L2i();
                        break;

                    case L2F:
                        op = new L2f();
                        break;

                    case L2D:
                        op = new L2d();
                        break;

                    case F2I:
                        op = new F2i();
                        break;

                    case F2L:
                        op = new F2l();
                        break;

                    case F2D:
                        op = new F2d();
                        break;

                    case D2I:
                        op = new D2i();
                        break;

                    case D2L:
                        op = new D2l();
                        break;

                    case D2F:
                        op = new D2f();
                        break;

                    case I2B:
                        op = new I2b();
                        break;

                    case I2C:
                        op = new I2c();
                        break;

                    case I2S:
                        op = new I2s();
                        break;

                    case LCMP:
                        op = new Lcmp();
                        break;

                    case FCMPL:
                        op = new Fcmpl();
                        break;

                    case FCMPG:
                        op = new Fcmpg();
                        break;

                    case DCMPL:
                        op = new Dcmpl();
                        break;

                    case DCMPG:
                        op = new Dcmpg();
                        break;

                    case IFEQ:
                        op = new Ifeq(getLabel(ofOp + stream.readShort(), aopDefer));
                        break;

                    case IFNE:
                        op = new Ifne(getLabel(ofOp + stream.readShort(), aopDefer));
                        break;

                    case IFLT:
                        op = new Iflt(getLabel(ofOp + stream.readShort(), aopDefer));
                        break;

                    case IFGE:
                        op = new Ifge(getLabel(ofOp + stream.readShort(), aopDefer));
                        break;

                    case IFGT:
                        op = new Ifgt(getLabel(ofOp + stream.readShort(), aopDefer));
                        break;

                    case IFLE:
                        op = new Ifle(getLabel(ofOp + stream.readShort(), aopDefer));
                        break;

                    case IF_ICMPEQ:
                        op = new If_icmpeq(getLabel(ofOp + stream.readShort(), aopDefer));
                        break;

                    case IF_ICMPNE:
                        op = new If_icmpne(getLabel(ofOp + stream.readShort(), aopDefer));
                        break;

                    case IF_ICMPLT:
                        op = new If_icmplt(getLabel(ofOp + stream.readShort(), aopDefer));
                        break;

                    case IF_ICMPGE:
                        op = new If_icmpge(getLabel(ofOp + stream.readShort(), aopDefer));
                        break;

                    case IF_ICMPGT:
                        op = new If_icmpgt(getLabel(ofOp + stream.readShort(), aopDefer));
                        break;

                    case IF_ICMPLE:
                        op = new If_icmple(getLabel(ofOp + stream.readShort(), aopDefer));
                        break;

                    case IF_ACMPEQ:
                        op = new If_acmpeq(getLabel(ofOp + stream.readShort(), aopDefer));
                        break;

                    case IF_ACMPNE:
                        op = new If_acmpne(getLabel(ofOp + stream.readShort(), aopDefer));
                        break;

                    case GOTO:
                        op = new Goto(getLabel(ofOp + stream.readShort(), aopDefer));
                        break;

                    case JSR:
                        op = new Jsr(getLabel(ofOp + stream.readShort(), aopDefer));
                        break;

                    case RET:
                        op = new Ret((Rvar) getVariable(RVAR, stream.readUnsignedByte(), aavar));
                        break;

                    case TABLESWITCH:
                    case LOOKUPSWITCH:
                        {
                        // skip allignment padding
                        int cbPad = 3 - ofOp % 4;
                        for (int i = 0; i < cbPad; ++i)
                            {
                            int iPad = stream.readUnsignedByte();
                            if (iPad != 0x00)
                                {
                                throw new IOException(CLASS + ".disassembleOps:  " +
                                    "Illegal padding! (" + iPad + ")");
                                }
                            }

                        // read the "default:" label
                        Label labelDefault = getLabel(ofOp + stream.readInt(), aopDefer);

                        // determine the number of cases
                        if (iOp == TABLESWITCH)
                            {
                            int iLow   = stream.readInt();
                            int iHigh  = stream.readInt();

                            cCases = iHigh - iLow + 1;
                            iCase  = iLow;
                            }
                        else
                            {
                            cCases = stream.readInt();
                            }

                        // create a generic switch op
                        op = opSwitch = new Switch(labelDefault);

                        // remember the switch type
                        iOpSwitch = iOp;

                        // change the parsing mode to parse the case ops
                        // 2001.05.03 cp  switch can have 0 cases, in which
                        //                case we are already done parsing it
                        fSwitchOp = cCases > 0;
                        }
                        break;

                    case IRETURN:
                        op = new Ireturn();
                        break;

                    case LRETURN:
                        op = new Lreturn();
                        break;

                    case FRETURN:
                        op = new Freturn();
                        break;

                    case DRETURN:
                        op = new Dreturn();
                        break;

                    case ARETURN:
                        op = new Areturn();
                        break;

                    case RETURN:
                        op = new Return();
                        break;

                    case GETSTATIC:
                        op = new Getstatic((FieldConstant)pool.getConstant(
                                stream.readUnsignedShort()));
                        break;

                    case PUTSTATIC:
                        op = new Putstatic((FieldConstant)pool.getConstant(
                                stream.readUnsignedShort()));
                        break;

                    case GETFIELD:
                        op = new Getfield((FieldConstant)pool.getConstant(
                                stream.readUnsignedShort()));
                        break;

                    case PUTFIELD:
                        op = new Putfield((FieldConstant)pool.getConstant(
                                stream.readUnsignedShort()));
                        break;

                    case INVOKEVIRTUAL:
                        op = new Invokevirtual((MethodConstant)pool.getConstant(
                                stream.readUnsignedShort()));
                        break;

                    case INVOKESPECIAL:
                        {
                        op = new Invokespecial((MethodConstant)pool.getConstant(
                                stream.readUnsignedShort()));
                        }
                        break;

                    case INVOKESTATIC:
                        op = new Invokestatic((MethodConstant)pool.getConstant(
                                stream.readUnsignedShort()));
                        break;

                    case INVOKEINTERFACE:
                        // the invoke interface construct has the byte code
                        // followed by a two-byte constant index, a single-byte
                        // "number of arguments" value, and a zero; the number
                        // of arguments is redundant and the zero is meaningless
                        op = new Invokeinterface((InterfaceConstant)pool.getConstant(
                                stream.readUnsignedShort()));
                        stream.readUnsignedByte(); // nargs is redundant
                        stream.readUnsignedByte(); // zero
                        break;

                    case INVOKEDYNAMIC:
                        op = new Invokedynamic((InvokeDynamicConstant)
                                pool.getConstant(stream.readUnsignedShort()));
                        stream.readUnsignedByte(); // zero
                        stream.readUnsignedByte(); // zero
                        break;

                    default:
                        throw new IOException(CLASS + ".disassembleOps:  " +
                            "Illegal byte code! (" + iOp + ")");

                    case NEW:
                        op = new New((ClassConstant)pool.getConstant(
                                stream.readUnsignedShort()));
                        break;

                    case NEWARRAY:
                        {
                        int iType = stream.readUnsignedByte();
                        switch (iType)
                            {
                            case 4:
                                op = new Znewarray();
                                break;
                            case 5:
                                op = new Cnewarray();
                                break;
                            case 6:
                                op = new Fnewarray();
                                break;
                            case 7:
                                op = new Dnewarray();
                                break;
                            case 8:
                                op = new Bnewarray();
                                break;
                            case 9:
                                op = new Snewarray();
                                break;
                            case 10:
                                op = new Inewarray();
                                break;
                            case 11:
                                op = new Lnewarray();
                                break;
                            default:
                                throw new IOException(CLASS + ".disassembleOps:  Unexpected NEWARRAY type (" + iType + ")");
                            }
                        }
                        break;

                    case ANEWARRAY:
                        op = new Anewarray((ClassConstant)pool.getConstant(
                                stream.readUnsignedShort()));
                        break;

                    case ARRAYLENGTH:
                        op = new Arraylength();
                        break;

                    case ATHROW:
                        op = new Athrow();
                        break;

                    case CHECKCAST:
                        op = new Checkcast((ClassConstant)pool.getConstant(
                                stream.readUnsignedShort()));
                        break;

                    case INSTANCEOF:
                        op = new Instanceof((ClassConstant)pool.getConstant(
                                stream.readUnsignedShort()));
                        break;

                    case MONITORENTER:
                        op = new Monitorenter();
                        break;

                    case MONITOREXIT:
                        op = new Monitorexit();
                        break;

                    case WIDE:
                        iOp = stream.readUnsignedByte();
                        switch (iOp)
                            {
                            case IINC:
                                {
                                int      iVar = stream.readUnsignedShort();
                                Ivar     var  = (Ivar) getVariable(IVAR, iVar, aavar);
                                short    sInc = (short) stream.readShort();
                                op = new Iinc(var, sInc);
                                }
                                break;

                            case ILOAD:
                                op = new Iload((Ivar) getVariable(IVAR, stream.readUnsignedShort(), aavar));
                                break;

                            case LLOAD:
                                op = new Lload((Lvar) getVariable(LVAR, stream.readUnsignedShort(), aavar));
                                break;

                            case FLOAD:
                                op = new Fload((Fvar) getVariable(FVAR, stream.readUnsignedShort(), aavar));
                                break;

                            case DLOAD:
                                op = new Dload((Dvar) getVariable(DVAR, stream.readUnsignedShort(), aavar));
                                break;

                            case ALOAD:
                                op = new Aload((Avar) getVariable(AVAR, stream.readUnsignedShort(), aavar));
                                break;

                            case ISTORE:
                                op = new Istore((Ivar) getVariable(IVAR, stream.readUnsignedShort(), aavar));
                                break;

                            case LSTORE:
                                op = new Lstore((Lvar) getVariable(LVAR, stream.readUnsignedShort(), aavar));
                                break;

                            case FSTORE:
                                op = new Fstore((Fvar) getVariable(FVAR, stream.readUnsignedShort(), aavar));
                                break;

                            case DSTORE:
                                op = new Dstore((Dvar) getVariable(DVAR, stream.readUnsignedShort(), aavar));
                                break;

                            case ASTORE:
                                op = new Astore((Avar) getVariable(AVAR, stream.readUnsignedShort(), aavar));
                                break;

                            case RET:
                                op = new Ret((Rvar) getVariable(RVAR, stream.readUnsignedShort(), aavar));
                                break;

                            default:
                                throw new IOException(CLASS + ".disassembleOps:  " +
                                    "Illegal byte code modified by WIDE! (" + iOp + ")");
                            }
                        break;

                    case MULTIANEWARRAY:
                        {
                        int iConst = stream.readUnsignedShort();
                        int cDims  = stream.readUnsignedByte();
                        ClassConstant constant = (ClassConstant) pool.getConstant(iConst);
                        op = new Multianewarray(constant, cDims);
                        }
                        break;

                    case IFNULL:
                        op = new Ifnull(getLabel(ofOp + stream.readShort(), aopDefer));
                        break;

                    case IFNONNULL:
                        op = new Ifnonnull(getLabel(ofOp + stream.readShort(), aopDefer));
                        break;

                    case GOTO_W:
                        op = new Goto(getLabel(ofOp + stream.readShort(), aopDefer));
                        break;

                    case JSR_W:
                        op = new Jsr(getLabel(ofOp + stream.readShort(), aopDefer));
                        break;
                    }
                }

            // determine how much was read from the stream (i.e. size of op)
            int cbOp = stream.getOffset() - ofOp;

            if (op == null)
                {
                // give this op's size to the previous op
                // (e.g. a case op which is the same as the switch default)
                opLast.m_cb += cbOp;
                }
            else
                {
                // op line number, offset and length
                op.m_iLine = iLine;
                op.m_of    = ofOp;
                op.m_cb    = cbOp;

                // add the op to the linked list of ops
                if (opFirst == null)
                    {
                    opFirst = op;
                    opLast  = op;
                    }
                else
                    {
                    // append op
                    opLast.m_opNext = op;
                    opLast = op;
                    }
                }
            }

        // the local variable table offsets/lengths need to be turned into ops;
        // the easiest way is to use labels since we keep labels by offset
        if (attrVar != null)
            {
            disassembleVarTable(attrVar, aopDefer);
            }
        if (attrVarType != null)
            {
            disassembleVarTable(attrVarType, aopDefer);
            }


        // the labels encounted in the code are already arranged by offset
        // (aopDefer) but the labels implied by guarded sections are not
        for (Enumeration enmr = vectCatch.elements(); enmr.hasMoreElements(); )
            {
            GuardedSection section = (GuardedSection) enmr.nextElement();
            section.setHandler(getLabel(section.getHandlerOffset(), aopDefer));
            }

        // using the guarded section informtion, add try and catch ops;
        // for each try, there is one or more catch;
        // for each guarded section, there is one catch;
        // multiple catch ops share a try op if and only if the guarded
        // section start and end PC's are identical;
        // guarded sections are arranged in order of the catch ops
        int ofTryPrev   = -1;
        int ofCatchPrev = -1;
        Try opTryPrev   = null;
        for (Enumeration enmr = vectCatch.elements(); enmr.hasMoreElements(); )
            {
            GuardedSection section = (GuardedSection) enmr.nextElement();
            int ofTry   = section.getTryOffset();
            int ofCatch = section.getCatchOffset();
            if (ofTry == ofTryPrev && ofCatch == ofCatchPrev)
                {
                // use previous try but create a new catch
                Try   opTry   = opTryPrev;
                Catch opCatch = new Catch(opTry, section.getException(), section.getHandler());
                // add the catch to the defered ops
                addCatch(ofCatch, opCatch, aopDefer);
                }
            else
                {
                // create a new try and catch
                Try   opTry   = new Try();
                Catch opCatch = new Catch(opTry, section.getException(), section.getHandler());
                // add the try and catch to the defered ops
                addTry  (ofTry  , opTry  , aopDefer);
                addCatch(ofCatch, opCatch, aopDefer);
                // remember the bounds of the guarded section in case the
                // next one is based on the same try
                ofTryPrev   = ofTry;
                ofCatchPrev = ofCatch;
                opTryPrev   = opTry;
                }
            }

        // ensure that all parameters are declared
        for (int iParam = 0, cParams = asParam.length, iVar = 0; iParam < cParams; ++iParam)
            {
            // determine the parameter type
            int  nOp;
            char chType = asParam[iParam].charAt(0);
            switch (chType)
                {
                case 'Z':
                case 'B':
                case 'C':
                case 'S':
                case 'I':
                    nOp = IVAR;
                    break;

                case 'J':
                    nOp = LVAR;
                    break;

                case 'F':
                    nOp = FVAR;
                    break;
            
                case 'D':
                    nOp = DVAR;
                    break;

                case '[':
                case 'L':
                    nOp = AVAR;
                    break;

                case 'V':
                    throw new IllegalStateException("Parameter cannot be void");

                default:
                    throw new IllegalStateException("JVM Type Signature cannot start with '"
                            + chType + "'");
                }

            // make sure that the parameter is declared
            getVariable(nOp, iVar, aavar);

            // iVar is the slot number, which reflects the width (1 or 2) of
            // the parameter
            iVar += OpDeclare.getJavaWidth(chType);
            }

        // preface:  open the global scope, add variable declarations
        // (including parameters)
        Op opPreInit = new Begin();
        Op opPreStop = opPreInit;
        for (int iVar = 0; iVar < cVars; ++iVar)
            {
            for (int iType = 0; iType < 6; ++iType)
                {
                OpDeclare decl = aavar[iType][iVar];
                if (decl != null)
                    {
                    opPreStop.m_opNext = decl;
                    opPreStop = decl;
                    }
                }
            }

        // postlogue:  close the global scope
        Op opPost = new End();
        opPost.m_of     = opLast.m_of + opLast.m_cb;
        opPost.m_iLine  = opLast.m_iLine;
        opLast.m_opNext = opPost;
        opLast = opPost;

        // insert labels, try's, catch's
        int ofPrev = -1;
        for (Op opPrev = null, op = opFirst; op != null; op = (opPrev = op).m_opNext)
            {
            int of = op.m_of;
            if (of > ofPrev)
                {
                // verify that no ops are being missed
                for (int ofSkip = ofPrev + 1; ofSkip < of; ++ofSkip)
                    {
                    if (aopDefer[ofSkip] != null)
                        {
                        Op opLabel = aopDefer[ofSkip];
                        StringBuffer sb = new StringBuffer();
                        while (opLabel != null)
                            {
                            sb.append("\n  ")
                              .append(opLabel.getClass().getName())
                              .append(' ')
                              .append(opLabel);
                            opLabel = opLabel.m_opNext;
                            }
                        throw new IOException(CLASS + ".disassembleOps:  " +
                                "Non-alligned label, try, or catch at offset " + ofSkip + ":" + sb
                                + "\n  opPrev=" + opPrev.getClass().getName() + " " + opPrev + " @" + opPrev.m_of
                                + "\n  op=" + op.getClass().getName() + " " + op + " @" + op.m_of);
                        }
                    }
                ofPrev = of;

                // check if any ops are present to insert
                Op opInsert = aopDefer[of];
                if (opInsert != null)
                    {
                    // find the last op in the linked list of ops to insert
                    // (also set offset for each op)
                    opInsert.m_of = of;
                    opInsert.m_iLine = op.m_iLine;
                    Op opLastInsert = opInsert;
                    while (opLastInsert.m_opNext != null)
                        {
                        opLastInsert = opLastInsert.m_opNext;
                        opLastInsert.m_of = of;
                        opLastInsert.m_iLine = op.m_iLine;
                        }

                    // link in the deferred ops
                    if (opPrev == null)
                        {
                        // insert at the front of the entire set of ops
                        opFirst = opInsert;
                        }
                    else
                        {
                        // insert before the current op
                        opPrev.m_opNext = opInsert;
                        }
                    opLastInsert.m_opNext = op;
                    }
                }
            }

        // link the preface code into the list of ops
        opPreStop.m_opNext = opFirst;
        opFirst = opPreInit;

        aopBounds[0] = opFirst;
        aopBounds[1] = opLast;
        }


    // ----- VMStructure operations -----------------------------------------

    /**
    * The disassemble method for an op is not used.  The logic is centralized
    * in the disassembleOps method.
    *
    * @param stream  the stream implementing java.io.DataInput from which
    *                to read the assembled VM structure
    * @param pool    the constant pool for the class which contains any
    *                constants referenced by this VM structure
    */
    protected void disassemble(DataInput stream, ConstantPool pool)
            throws IOException
        {
        }

    /**
    * The pre-assembly step collects the necessary entries for the constant
    * pool.  During this step, all constants used by this VM structure and
    * any sub-structures are registered with (but not yet bound by position
    * in) the constant pool.
    *
    * The implementation provided here is for a simple op.  A simple op is
    * a single-byte op that exists as both a JASM instruction and a Java
    * byte code.
    *
    * @param pool  the constant pool for the class which needs to be
    *              populated with the constants required to build this
    *              VM structure
    */
    protected void preassemble(ConstantPool pool)
        {
        }

    /**
    * The assembly process assembles and writes the structure to the passed
    * output stream, resolving any dependencies using the passed constant
    * pool.
    *
    * The implementation provided here is for simple ops and simple pseudo-
    * ops.  A simple op is a single-byte op that exists as both a JASM
    * instruction and a Java byte code.  A simple pseudo-op assembles to
    * nothing.
    *
    * @param stream  the stream implementing java.io.DataOutput to which to
    *                write the assembled VM structure
    * @param pool    the constant pool for the class which by this point
    *                contains the entire set of constants required to build
    *                this VM structure
    */
    protected void assemble(DataOutput stream, ConstantPool pool)
            throws IOException
        {
        // JSR_W is the last byte code; assume everything higher is a zero
        // length pseudo op and everything below is a one length byte code
        if (m_iOp <= JSR_W)
            {
            stream.write(m_iOp);
            }
        }


    // ----- Object operations ----------------------------------------------

    /**
    * Produce a human-readable string describing the byte code.
    *
    * @return a string describing the byte code
    */
    public String toString()
        {
        return format(null, OPNAME[m_iOp], null);
        }

    /**
    * Format a line of assembly.
    *
    * @return a formatted 3-column string (label, instruction, comment)
    */
    protected static String format(String sLabel, String sInstruction, String sComment)
        {
        StringBuffer sb = new StringBuffer();

        if (sLabel == null)
            {
            sLabel = BLANK;
            }
        if (sInstruction == null)
            {
            sInstruction = BLANK;
            }
        if (sComment == null)
            {
            sComment = BLANK;
            }

        boolean fLabel       = (sLabel      .length() > 0);
        boolean fInstruction = (sInstruction.length() > 0);
        boolean fComment     = (sComment    .length() > 0);

        boolean fOverflow = false;
        if (fLabel)
            {
            sb.append(sLabel)
              .append(':');
            fOverflow = (sb.length() > BLANK_LABEL.length());
            }

        if (fInstruction || fComment)
            {
            if (fOverflow)
                {
                sb.append('\n')
                  .append(BLANK_LABEL)
                  .append(SEPARATOR);
                }
            else
                {
                sb.append(BLANK_LABEL.substring(sb.length()))
                  .append(SEPARATOR);
                }

            fOverflow = false;
            if (fInstruction)
                {
                sb.append(sInstruction);
                fOverflow = (sInstruction.length() >= BLANK_INSTRUCTION.length());
                }

            if (fComment)
                {
                if (fOverflow)
                    {
                    sb.append('\n')
                      .append(BLANK_LABEL)
                      .append(SEPARATOR)
                      .append(BLANK_INSTRUCTION)
                      .append(SEPARATOR);
                    }
                else
                    {
                    sb.append(BLANK_INSTRUCTION.substring(sInstruction.length()))
                      .append(SEPARATOR);
                    }

                sb.append("// ")
                  .append(sComment);
                }
            }

        return sb.toString();
        }

    /**
    * Produce a human-readable string describing the byte code.
    *
    * @return a string describing the byte code
    */
    public String toJasm()
        {
        return OPNAME[m_iOp];
        }

    /**
    * Produce a fairly unique hash code.
    *
    * @return the hash code for this object
    */
    public int hashCode()
        {
        return m_iHash;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Access the JASM op value.
    *
    * @return the JASM op value
    */
    public int getValue()
        {
        return m_iOp;
        }

    /**
    * Set the JASM op value.
    *
    * @param iOp  the JASM op value
    */
    protected void setValue(int iOp)
        {
        m_iOp = iOp;
        }

    /**
    * Returns the offset of the byte code.  If the op was disassembled, the
    * offset reflects the location that the byte code was read from the
    * stream.  If the op was assembled, the offset is the location assigned
    * for the op to assemble to.  Otherwise the offset is meaningless.
    *
    * @return the op's byte code offset
    */
    public int getOffset()
        {
        return m_of;
        }

    /**
    * Sets the byte code offset for the op.
    *
    * @param  of  the op's new byte code offset
    */
    protected void setOffset(int of)
        {
        m_of = of;
        }

    /**
    * Determines if the op results in one or more byte codes.
    *
    * @return true if the op has a size when assembled
    */
    public boolean hasSize()
        {
        // JSR_W is the last byte code
        if (m_iOp <= JSR_W)
            {
            return true;
            }

        // some pseudo-ops don't produce code
        switch (m_iOp)
            {
            case BEGIN:
            case END:
            case IVAR:
            case LVAR:
            case FVAR:
            case DVAR:
            case AVAR:
            case RVAR:
            case CASE:
            case LABEL:
            case TRY:
            case CATCH:
                return false;

            default:
                return true;
            }
        }

    /**
    * Returns the size of the byte code.  If the op was disassembled, the
    * size is the number of bytes read from the stream.  If the op was
    * assembled, the size is the number of bytes written to the stream.
    * Otherwise, the size is meaningless.
    *
    * @return the size of the op
    */
    public int getSize()
        {
        return m_cb;
        }

    /**
    * Sets the byte code length for the op.
    *
    * @param  cb  the op's new byte code length
    */
    protected void setSize(int cb)
        {
        m_cb = cb;
        }

    /**
    * Calculate and set the size of the assembled op based on the offset of
    * the op and the constant pool which is passed.
    *
    * The implementation provided here supports simple ops and zero-length
    * pseudo-ops.  A simple op is a single-byte op that exists as both a
    * JASM instruction and a Java byte code.  A pseudo-op is an op which
    * does not have a one-to-one correlation to any Java byte code; it
    * either assembles to one of several byte codes or is used to build
    * non-byte-code structures contained within a Code attribute.
    *
    * @param pool    the constant pool for the class which by this point
    *                contains the entire set of constants required to build
    *                this VM structure
    */
    protected void calculateSize(ConstantPool pool)
        {
        // JSR_W is the last byte code; assume everything higher is a zero
        // length pseudo op and everything below is a one length byte code
        setSize(m_iOp > JSR_W ? 0 : 1);
        }

    /**
    * Returns the effect of the byte code on the height of the stack.  This
    * method is overridden by ops which dynamically calculate their effect
    * on the stack.
    *
    * @return the number of words pushed (if positive) or popped (if
    *         negative) from the stack by the op
    */
    public int getStackChange()
        {
        return OPEFFECT[m_iOp];
        }

    /**
    * Get the expected stack height for this op.  If the expected stack size
    * is still not calculated (UNKNOWN) after calculating the max stack,
    * then the op is considered "not reachable".
    */
    protected int getStackHeight()
        {
        return m_cwStack;
        }

    /**
    * Set the expected stack height for this op.
    *
    * @param cwStack the number of words in the stack when this op is
    *                executed
    */
    protected void setStackHeight(int cwStack)
        {
        // verify that this op always has the same stack size when it is
        // executed
        int cwStackPrev = m_cwStack;
        if (cwStackPrev != UNKNOWN && cwStackPrev != cwStack)
            {
            throw new IllegalStateException(CLASS + ".setStackHeight:  " +
                    "Height mismatch (" + cwStack + " vs " + cwStackPrev +
                    ") on " + toString() + "!");
            }

        // store the stack size
        m_cwStack = cwStack;
        }

    /**
    * Determine if the op is reachable; this is only valid after calculating
    * the max stack.
    *
    * @return true if the op was reached by the stack size calculating
    *         algorithm
    */
    protected boolean isReachable()
        {
        return m_cwStack != UNKNOWN;
        }

    /**
    * Determine if the op is discardable; by default, this is dependent on
    * determining if the op is reachable.
    *
    * @return true if the op can be discarded from assembly
    */
    protected boolean isDiscardable()
        {
        return !isReachable();
        }

    /**
    * Returns the line number of the source code which produced the op.
    *
    * @return the source code line number of the op
    */
    public int getLine()
        {
        return m_iLine;
        }

    /**
    * Sets the source code line number for the op.
    *
    * @param  iLine  the op's new source code line number
    */
    protected void setLine(int iLine)
        {
        m_iLine = iLine;
        }

    /**
    * Returns the name of the op.  This is for descriptive purposes only.
    *
    * @return the name of the op
    */
    public String getName()
        {
        return OPNAME[m_iOp];
        }

    /**
    * Get the op that follows this one.
    *
    * @return the next op
    */
    public Op getNext()
        {
        return m_opNext;
        }

    /**
    * Set the op that follows this one.  This is used internally by the
    * assembler.
    *
    * @param op  the next op
    */
    protected void setNext(Op op)
        {
        m_opNext = op;
        }

    /**
    * For debugging or listing purposes, print the op details.
    */
    public void print()
        {
        out('[' + String.valueOf(getOffset()) + "] (" + getLine() + ") " + toString());
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Op";

    /**
    * An empty byte array.
    */
    private static final byte[] NO_BYTES = new byte[0];

    /**
    * A big prime number.
    * @see <a href="http://www.utm.edu/research/primes/lists/small/small.html">
    * http://www.utm.edu/research/primes/lists/small/small.html</a>
    */
    private static final long BIGPRIME = 1500450271L;

    /**
    * A number just a little too big to be an int.
    */
    private static final long INTLIMIT = 0x80000000L;

    /**
    * A blank string.
    */
    private static final String BLANK               = "";

    /**
    * The formatted space reserved for a label.
    */
    private static final String BLANK_LABEL         = "                  ";

    /**
    * The formatted space reserved for an instruction.
    */
    private static final String BLANK_INSTRUCTION   = "                        ";

    /**
    * The formatted space between columns (label, instruction, comment).
    */
    private static final String SEPARATOR           = "  ";

    /**
    * The last hash code given out.
    */
    private static int sm_iLastHash;

    /**
    * The JASM op value.
    */
    private int m_iOp;

    /**
    * The offset of the byte code.  If the op is disassembled from byte code,
    * this value is set to the offset within the disassembled byte code of
    * the byte code instruction which disassembled to this op.  When the op
    * is assembled, this value is set to the expected offset where the op
    * will produce byte code.
    */
    private int m_of;

    /**
    * The length of the byte code.  If the op is disassembled from byte code,
    * this value is set to the number of bytes that were disassembled to make
    * this op.  When the op is assembled, this value is set to the expected
    * length of byte code that the op will produce.
    */
    private int m_cb;

    /**
    * The source code line number.
    */
    private int m_iLine;

    /**
    * The next op.
    */
    private Op m_opNext;

    /**
    * Count of words on the stack when this op is reached.
    */
    private int m_cwStack = UNKNOWN;

    /**
    * Hash code.
    */
    private int m_iHash;
    }
