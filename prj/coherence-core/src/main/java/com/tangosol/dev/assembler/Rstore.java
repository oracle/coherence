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
* The RSTORE variable-size op stores a return address.
* <p><code><pre>
* JASM op         :  RSTORE    (0x3a)
* JVM byte code(s):  ASTORE    (0x3a)
*                    ASTORE_0  (0x4b)
*                    ASTORE_1  (0x4c)
*                    ASTORE_2  (0x4d)
*                    ASTORE_3  (0x4e)
* Details         :
* </pre></code>
*
* @version 0.50, 06/17/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class Rstore extends OpStore implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    *
    * @param var  the variable to push
    */
    public Rstore(Rvar var)
        {
        super(RSTORE, var);
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
        // if the label context (a sub-routine) does not have a reachable
        // RET op, then assemble to nothing (since the JSR will assemble
        // to a GOTO)
        if (getVariable().getContext().getRet() == null)
            {
            return;
            }

        super.assemble(stream, pool);
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
        // see comment in assemble()
        if (getVariable().getContext().getRet() == null)
            {
            setSize(0);
            }
        else
            {
            super.calculateSize(pool);
            }
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Rstore";
    }
