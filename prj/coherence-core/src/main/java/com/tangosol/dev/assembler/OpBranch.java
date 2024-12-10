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
* This abstract class implements IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE,
* IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE,
* IF_ACMPEQ, IF_ACMPNE, GOTO (including the wide version), JSR (including
* the wide version), IFNULL, and IFNONNULL.
* <p>
* Currently, this class does not assemble to either GOTO_W and JSR_W,
* which means that the assembler can reliably produce 32K of byte code
* per method.  Due to the JVM architecture, method code is limited to 64k
* anyway.  To support 32k up to 64k of code, additional "macro" support
* would have to be added to each of the branching instructions, since only
* GOTO and JSR have wide versions.
*
* @version 0.50, 06/14/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public abstract class OpBranch extends Op implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    *
    * @param iOp    the op value
    * @param label  the Label to push
    */
    protected OpBranch(int iOp, Label label)
        {
        super(iOp);
        m_label = label;

        if (label == null)
            {
            throw new IllegalArgumentException(CLASS +
                    ":  Label must not be null!");
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
        return format(null, getName() + ' ' + m_label.format(), null);
        }

    /**
    * Produce a human-readable string describing the byte code.
    *
    * @return a string describing the byte code
    */
    public String toJasm()
        {
        return getName() + " goto " + m_label.getOffset();
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
        int iOp      = super.getValue();
        int ofBranch = m_label.getOffset() - this.getOffset();

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
    * Calculate and set the size of the assembled op based on the offset of
    * the op and the constant pool which is passed.
    *
    * @param pool    the constant pool for the class which by this point
    *                contains the entire set of constants required to build
    *                this VM structure
    */
    protected void calculateSize(ConstantPool pool)
        {
        // byte code plus 2-byte signed offset of label
        setSize(3);
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Access the label which the branching op branches to.
    *
    * @return the label branched to
    */
    public Label getLabel()
        {
        return m_label;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "OpBranch";

    /**
    * The label branched to by this op.
    */
    private Label m_label;
    }
