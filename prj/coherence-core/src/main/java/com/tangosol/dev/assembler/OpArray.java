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
* This abstract class implements:
* <ul>
* <li> ZNEWARRAY   (0xf3)
* <li> CNEWARRAY   (0xf5)
* <li> FNEWARRAY   (0xf9)
* <li> DNEWARRAY   (0xfa)
* <li> BNEWARRAY   (0xf4)
* <li> SNEWARRAY   (0xf6)
* <li> INEWARRAY   (0xf7)
* <li> LNEWARRAY   (0xf8)
* </ul>
*
* @version 0.50, 06/12/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public abstract class OpArray extends Op implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    *
    * @param iOp  the op value
    */
    public OpArray(int iOp)
        {
        super(iOp);
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
        int iOp = super.getValue();

        stream.writeByte(NEWARRAY);
        stream.writeByte(TYPES[iOp-ZNEWARRAY]);
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
        setSize(2);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "OpArray";

    /**
    * Array types converted to JVM immediate values.
    */
    private static final char[] TYPES = {4, 8, 5, 9, 10, 11, 6, 7};
    }
