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

import java.util.TreeSet;


/**
* The SWITCH op implements the general multi-case switch.  It assembles to
* either a TABLESWITCH or a LOOKUPSWITCH depending on which is more
* "efficient".  Since a TABLESWITCH is always more efficient for performance,
* it is always selected unless the amount of additional code generated for
* the TABLESWITCH compared to the LOOKUPSWITCH is considered to be too
* expensive in terms of size.
* <p><code><pre>
* JASM op         :  SWITCH        (0xfc)
*                    TABLESWITCH   (0xaa)
*                    LOOKUPSWITCH  (0xab)
* JVM byte code(s):  TABLESWITCH   (0xaa)
*                    LOOKUPSWITCH  (0xab)
* Details         :  The Java byte code TABLESWITCH has the following format:
*                       ub   TABLESWITCH      (0xaa)
*                       ub[] [0-3 byte pad]   (0x00..)
*                       s4   default offset
*                       s4   low case value
*                       s4   high case value
*                       s4[] branch offsets
*                       
*                    The byte code LOOKUPSWITCH has the following format:
*                       ub   LOOKUPSWITCH     (0xab)
*                       ub[] [0-3 byte pad]   (0x00..)
*                       s4   default offset
*                       u4   number of cases
*                       struct[]
*                           {
*                           s4  case value
*                           s4  branch offset
*                           }
* </pre></code>
*
* @version 0.50, 06/17/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public abstract class OpSwitch extends OpBranch implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    *
    * @param label  the default label to branch to if no cases match
    */
    protected OpSwitch(int iOp, Label label)
        {
        super(iOp, label);
        setSwitch(iOp);
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
        // write the byte code
        int iOp = getSwitch();
        stream.writeByte(iOp);

        // align to a 4-byte boundary
        int ofOp  = getOffset();
        int cbPad = 3 - ofOp % 4;
        for (int i = 0; i < cbPad; ++i)
            {
            stream.writeByte(0x00);
            }

        // default branch
        int ofDefault = getLabel().getOffset() - ofOp;
        stream.writeInt(ofDefault);

        // cases
        Case[] acase  = getCases();
        int    cCases = acase.length;
        switch (iOp)
            {
            case TABLESWITCH:
                {
                if (cCases == 0)
                    {
                    throw new IllegalStateException(CLASS + ".assemble:  "
                            + "TABLESWITCH requires at least one case!");
                    }

                int iLow = acase[0].getCase();
                stream.writeInt(iLow);

                int iHigh = acase[cCases - 1].getCase();
                stream.writeInt(iHigh);

                for (int i = iLow, iCase = 0; i <= iHigh; ++i)
                    {
                    Case op = acase[iCase];
                    if (i == op.getCase())
                        {
                        // this particular case is specified; use its label
                        stream.writeInt(op.getLabel().getOffset() - ofOp);
                        ++iCase;
                        }
                    else
                        {
                        // this particular case value is not specified; go to
                        // the default label
                        stream.writeInt(ofDefault);
                        }
                    }
                }
                break;

            case LOOKUPSWITCH:
                stream.writeInt(cCases);
                for (int i = 0; i < cCases; ++i)
                    {
                    Case op = acase[i];
                    stream.writeInt(op.getCase());
                    stream.writeInt(op.getLabel().getOffset() - ofOp);
                    }
                break;

            default:
                throw new IllegalStateException(CLASS + ".assemble:  "
                        + "Illegal byte code! (" + iOp + ")");
            }
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
        Case[] acase  = getCases();
        int    cCases = acase.length;

        // determine padding which aligns the 4-byte values in the complex
        // byte code structure to a 4-byte boundary
        int cbPad = 3 - getOffset() % 4;

        int iOp = getSwitch();
        switch (iOp)
            {
            case TABLESWITCH:
                if (cCases > 1)
                    {
                    cCases = acase[cCases-1].getCase() - acase[0].getCase() + 1;
                    }
                setSize(cbPad + 13 + 4 * cCases);
                break;

            case LOOKUPSWITCH:
                setSize(cbPad + 9 + 8 * cCases);
                break;

            default:
                throw new IllegalStateException(CLASS + ".calculateSize:  "
                        + "Illegal byte code! (" + iOp + ")");
            }
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Produce a human-readable string describing the byte code.
    *
    * @return a string describing the byte code
    */
    public String toJasm()
        {
        return getName() + " default: goto " + getLabel().getOffset();
        }

    /**
    * Get the complete set of cases.
    *
    * @return an ordered array of case ops
    */
    protected Case[] getCases()
        {
        Case[] acase = m_acase;
        if (acase == null)
            {
            TreeSet setCase = new TreeSet();
            for (Op op = this.getNext(); op instanceof Case; op = op.getNext())
                {
                // only keep non-default case's
                Case opCase = (Case) op;
                if (opCase.getLabel() != this.getLabel())
                    {
                    setCase.add(opCase);
                    }
                }

            m_acase = acase = (Case[]) setCase.toArray(CASE_ARRAY);
            }

        return acase;
        }

    /**
    * Gets the op used to implement the switch.
    *
    * @return  one of SWITCH, TABLESWITCH, LOOKUPSWITCH
    */
    protected int getSwitch()
        {
        return m_iSwitchOp;
        }

    /**
    * Sets the op used to implement the switch.  By the time this op
    * assembles, it must be set to TABLESWITCH or LOOKUPSWITCH.
    *
    * @param iOp  one of SWITCH, TABLESWITCH, LOOKUPSWITCH
    */
    protected void setSwitch(int iOp)
        {
        switch (iOp)
            {
            case SWITCH:
            case TABLESWITCH:
            case LOOKUPSWITCH:
                m_iSwitchOp = iOp;
                break;
            default:
                throw new IllegalArgumentException(CLASS + ".setSwitch:  Illegal op (" + iOp + ")");
            }
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "OpSwitch";

    /**
    * An array of type Case[].
    */
    private static final Case[] CASE_ARRAY = new Case[0];

    /**
    * Which byte code will be used to assemble the op; SWITCH means "not yet
    * decided".
    */
    private int m_iSwitchOp;

    /**
    * Cached list of cases.
    */
    private Case[] m_acase;
    }
