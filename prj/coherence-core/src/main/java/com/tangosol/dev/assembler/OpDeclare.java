/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.assembler;


import com.tangosol.java.type.Type;


/**
* This abstract class implements IVAR, LVAR, FVAR, DVAR, AVAR, and RVAR.
*
* @version 0.50, 06/18/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public abstract class OpDeclare extends Op implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    *
    * @param iOp    the op value
    * @param sName  the variable name or null if no name or temporary
    * @param sSig   the JVM signature for debugging information
    */
    protected OpDeclare(int iOp, String sName, String sSig)
        {
        this(iOp, sName, sSig, UNKNOWN);
        }

    /**
    * Construct the op.
    *
    * @param iOp    the op value
    * @param sName  the variable name or null if no name or temporary
    * @param sSig   the JVM signature for debugging information
    * @param iVar   the variable index or UNKNOWN
    */
    protected OpDeclare(int iOp, String sName, String sSig, int iVar)
        {
        super(iOp);

        if (sSig == null)
            {
            sSig = SIGS[iOp - IVAR];
            }

        if (sName != null && sName.length() == 0)
            {
            sName = null;
            }

        m_sName = sName;
        m_sSig  = sSig;
        m_iVar  = iVar;
        }


    // ----- Object operations ----------------------------------------------

    /**
    * Produce a human-readable string describing the attribute.
    *
    * @return a string describing the attribute
    */
    public String toString()
        {
        StringBuffer sbInstruction = new StringBuffer(getName());

        // only show non-default signatures
        if (m_sSig != null && !m_sSig.equals(SIGS[getValue() - IVAR]))
            {
            sbInstruction.append(' ')
                         .append(m_sSig);
            }

        sbInstruction.append(' ')
                     .append(format());

        String sInstruction = sbInstruction.toString();

        String sComment = null;
        if (m_iVar != UNKNOWN)
            {
            sComment = "reg[" + m_iVar + "]";
            }

        return format(null, sInstruction, sComment);
        }

    /**
    * Produce a human-readable string describing the attribute.
    *
    * @return a string describing the attribute
    */
    public String toJasm()
        {
        StringBuffer sb = new StringBuffer(getName());

        // only show non-default signatures
        if (m_sSig != null && !m_sSig.equals(SIGS[getValue() - IVAR]))
            {
            sb.append(' ')
              .append(m_sSig);
            }

        sb.append(' ')
          .append(format());

        return sb.toString();
        }

    /**
    * Get the variable name or make one up.
    *
    * @return a displayable variable name (prefixed with # if made up)
    */
    public String format()
        {
        String sName = m_sName;

        if (sName == null)
            {
            sName = new StringBuffer()
                    .append('#')
                    .append(getName().charAt(0))
                    .append(m_iVar == UNKNOWN ? hashCode() : m_iVar)
                    .toString();
            }

        return sName;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Determine the variable type declared by this op.
    *
    * @return the variable type ('I', 'L', 'F', 'D', 'A', 'R')
    */
    public char getType()
        {
        return TYPES[getValue() - IVAR];
        }

    /**
    * Determine the variable name declared by this op.
    *
    * @return the variable name or null if a temporary
    */
    public String getVariableName()
        {
        return m_sName;
        }

    /**
    * Determine the variable JVM signature for this op.
    *
    * @return the variable signature
    */
    public String getSignature()
        {
        return m_sSig;
        }

    /**
    * Determine if the variable will show up in a debug table.
    *
    * @return true if the variable is named and not an explicit temporary
    */
    public boolean hasDebugInfo()
        {
        String sName = m_sName;
        return sName != null && sName.charAt(0) != '#';
        }


    // ----- variable ("register") slot

    /**
    * Determine the variable slot for the variable declared by this op.
    *
    * @return the variable slot for this variable declaration
    */
    public int getSlot()
        {
        return m_iVar;
        }

    /**
    * Set the variable slot for the variable declared by this op.
    *
    * @param iVar the variable slot for this variable declaration
    */
    protected void setSlot(int iVar)
        {
        m_iVar = iVar;
        }

    /**
    * Get the variable width.  The width of a variable is defined as the
    * number of slots used by the variable in the local variable storage
    * or the affect on the stack by pushing the variable.
    *
    * @return  the number of words used by this variable
    */
    public int getWidth()
        {
        int iOp = getValue();
        return (iOp == LVAR || iOp == DVAR ? 2 : 1);
        }

    /**
    * Get the variable width.  The width of a variable is defined as the
    * number of slots used by the variable in the local variable storage
    * or the affect on the stack by pushing the variable.
    *
    * @param ch  the variable type
    *
    * @return  the number of words used by the variable type
    */
    public static int getWidth(char ch)
        {
        return (ch == 'L' || ch == 'D' ? 2 : 1);
        }

    /**
    * Get the variable width.  The width of a variable is defined as the
    * number of slots used by the variable in the local variable storage
    * or the affect on the stack by pushing the variable.
    *
    * @param ch  the variable type (J for Long, L for reference, etc.)
    *
    * @return  the number of words used by the variable type
    */
    public static int getJavaWidth(char ch)
        {
        return (ch == 'J' || ch == 'D' ? 2 : 1);
        }


    // ----- variable scope

    /**
    * Determine the beginning of the scope.
    *
    * @return  the op representing the beginning of the scope
    */
    protected Begin getBegin()
        {
        return m_begin;
        }

    /**
    * Set the scope within which the variable is declared by this op.
    *
    * @param begin  the op representing the beginning of the scope
    */
    protected void setBegin(Begin begin)
        {
        m_begin = begin;
        }

    /**
    * Determine the end of the scope.
    *
    * @return  the op representing the end of the scope
    */
    protected End getEnd()
        {
        return m_begin.getEnd();
        }


    // ----- code context

    /**
    * Determine the code context of the variable.
    *
    * @return the label that starts the code context within which the
    *         variable is declared
    */
    protected Label getContext()
        {
        return m_labelContext;
        }

    /**
    * Specify the code context for the variable.
    *
    * @param label  the label that starts the code context within which the
    *               variable is declared
    */
    protected void setContext(Label label)
        {
        m_labelContext = label;
        }

    /**
    * Determine the subroutine depth of the variable.
    *
    * @return the maximum depth in the subroutine call chain that this
    *         variable will be declared in
    */
    protected int getDepth()
        {
        Label labelContext = m_labelContext;
        return (labelContext == null ? 0 : labelContext.getDepth());
        }


    // ----- helpers --------------------------------------------------------

    /**
    * Get a new variable for the specified Java signature type 
    *
    * @param sType  a JVM type signature
    * @param sName  variable name
    */
    public static OpDeclare getDeclareVar(String sType, String sName)
        {
        switch (sType.charAt(0))
            {
            case 'Z':
                return new Ivar(sName, "Z");
            case 'B':
                return new Ivar(sName, "B");
            case 'C':
                return new Ivar(sName, "C");
            case 'S':
                return new Ivar(sName, "S");
            case 'I':
                return new Ivar(sName, "I");
            case 'J':
                return new Lvar(sName);
            case 'F':
                return new Fvar(sName);
            case 'D':
                return new Dvar(sName);
            default:
                return new Avar(sName, sType);
            }
        }

    // Note: the following methods should be "virtualized", but since
    // for now is coded in one place due to the simplicity
    /**
    * Get a "load variable" op for this variable
    */
    public Op getLoadOp()
        {
        return getLoadOp(this);
        }

    // Note: the following methods should be "virtualized", but since
    // for now is coded in one place due to the simplicity
    /**
    * Get a "load variable" op for this variable
    */
    public static OpLoad getLoadOp(OpDeclare opDeclare)
        {
        switch (opDeclare.getSignature().charAt(0))
            {
            case 'Z':
            case 'B':
            case 'C':
            case 'S':
            case 'I':
                return new Iload((Ivar) opDeclare);
            case 'J':
                return new Lload((Lvar) opDeclare);
            case 'F':
                return new Fload((Fvar) opDeclare);
            case 'D':
                return new Dload((Dvar) opDeclare);
            default:
                return new Aload((Avar) opDeclare);
            }
        }

    /**
    * Get a "store variable" op for this variable
    */
    public Op getStoreOp()
        {
        switch (getSignature().charAt(0))
            {
            case 'Z':
            case 'B':
            case 'C':
            case 'S':
            case 'I':
                return new Istore((Ivar) this);
            case 'J':
                return new Lstore((Lvar) this);
            case 'F':
                return new Fstore((Fvar) this);
            case 'D':
                return new Dstore((Dvar) this);
            default:
                return new Astore((Avar) this);
            }
        }
    
    /**
    * Get a "return" op for this variable
    */
    public Op getReturnOp()
        {
        return getReturnOp(getSignature());
        }

    /**
    * Get a "return" op for this variable
    */
    public static Op getReturnOp(String sSig)
        {
        switch (sSig.charAt(0))
            {
            case 'Z':
            case 'B':
            case 'C':
            case 'S':
            case 'I':
                return new Ireturn();
            case 'J':
                return new Lreturn();
            case 'F':
                return new Freturn();
            case 'D':
                return new Dreturn();
            case 'V':
                return new Return();
            default:
                return new Areturn();
            }
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "OpDeclare";

    /**
    * Variable types.
    */
    private static final char[] TYPES = {'I','L','F','D','A','R'};

    /**
    * Default JVM signature per type.
    */
    private static final String[] SIGS =
        {
        Type.INT   .getSignature(),
        Type.LONG  .getSignature(),
        Type.FLOAT .getSignature(),
        Type.DOUBLE.getSignature(),
        Type.OBJECT.getSignature(),
        null,
        };

    /**
    * Variable name.
    */
    private String m_sName;

    /**
    * The JVM signature of the variable.
    */
    private String m_sSig;

    /**
    * The variable slot for the variable declared by this op.
    */
    private int m_iVar = UNKNOWN;

    /**
    * The scope creation op.
    */
    private Begin m_begin;

    /**
    * The scope destruction op.
    */
    private End m_end;

    /**
    * The code context (main or subroutine) this variable is declared in.
    */
    private Label m_labelContext;
    }
