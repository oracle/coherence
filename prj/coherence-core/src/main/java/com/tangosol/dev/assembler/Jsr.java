/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.assembler;


import java.io.IOException;
import java.io.DataInput;
import java.io.DataOutput;


/**
* The JSR op branches to the label such that a RET statement returns to the
* byte code following the JSR statement.  The JSR byte code causes a return
* address to be pushed onto the stack; this behavior is extraordinary when
* contrasted with other byte codes.
* <p><code><pre>
* JASM op         :  JSR    (0xa8)
* JVM byte code(s):  JSR    (0xa8)
*                    JSR_W  (0xc9)
* Details         :  The JSR_W byte code is currently not produced by the
*                    assembler.
* </pre></code>
*
* @version 0.50, 06/14/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class Jsr extends OpBranch implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    *
    * @param label  the label to branch to
    */
    public Jsr(Label label)
        {
        super(JSR, label);
        }


    // ----- VMStructure operations -----------------------------------------

    /**
    * The assembly process assembles and writes the structure to the passed
    * output stream, resolving any dependencies using the passed constant
    * pool.
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
        // if a JSR does not reach a RET, then assemble it as a GOTO
        Label label = getLabel();
        int   iOp   = (label.getRet() == null ? GOTO : JSR);

        int ofBranch = label.getOffset() - this.getOffset();
        if (ofBranch < Short.MIN_VALUE || ofBranch > Short.MAX_VALUE)
            {
            throw new IllegalStateException(CLASS +
                    ".assemble:  Branch offset out of range!");
            }

        stream.writeByte(iOp);
        stream.writeShort(ofBranch);
        }


    // ----- Op operations --------------------------------------------------

    /**
    * Returns the effect of the byte code on the height of the stack.
    *
    * @return the number of words pushed (if positive) or popped (if
    *         negative) from the stack by the op
    */
    public int getStackChange()
        {
        // JSR is the worst exception to the rule ... the stack size after
        // the JSR byte code executes has no relation to the stack size
        // before it executes because the subroutine can modify the stack;
        // when this method is called, the JSR expected stack height has been
        // set up already and the LABEL has already been traced to its RET
        // with the net change from the LABEL to the RET being stamped into
        // the LABEL
        int cwBefore = getStackHeight();
        int cwAfter  = getLabel().getRetHeight();

        if (cwBefore == UNKNOWN || cwAfter == UNKNOWN)
            {
            return UNKNOWN;
            }

        return cwAfter - cwBefore;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Produce a human-readable string describing the byte code.
    *
    * @return a string describing the byte code
    */
    public String toJasm()
        {
        return getName() + ' ' + getLabel().getOffset();
        }


    // ----- context

    /**
    * Determine the code context of the JSR.
    *
    * @return the label that starts the code context within which the JSR is
    *         reached
    */
    protected Label getContext()
        {
        return m_labelContext;
        }

    /**
    * Specify the code context for the JSR op.
    *
    * @param label  the label that starts the code context within which the
    *               JSR is reached
    */
    protected void setContext(Label label)
        {
        m_labelContext = label;
        }

    /**
    * Determine the subroutine depth of the subroutine called by the JSR.
    *
    * @return the maximum depth in the subroutine call chain that the
    *         subroutine invoked by the JSR op will be found
    */
    protected int getDepth()
        {
        Label labelContext = m_labelContext;
        return (labelContext == null ? 0 : labelContext.getDepth() + 1);
        }


    // ----- for variable allocation

    /**
    * Get the first available variable slot.
    *
    * @return the first available variable slot
    */
    protected int getFirstSlot()
        {
        return m_iSlot;
        }

    /**
    * Set the first available variable slot.
    *
    * @param iSlot  the first available variable slot
    */
    protected void setFirstSlot(int iSlot)
        {
        m_iSlot = iSlot;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Jsr";

    /**
    * The main/subroutine context containing this JSR.
    */
    private Label m_labelContext;

    /**
    * The JSR remembers how many variables were assigned when it was reached;
    * this is used to determine the register (var slot) base for subroutines.
    */
    private int m_iSlot = UNKNOWN;
    }
