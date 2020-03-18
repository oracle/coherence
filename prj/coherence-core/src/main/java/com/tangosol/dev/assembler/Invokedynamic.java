/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.dev.assembler;

import java.io.DataOutput;
import java.io.IOException;

/**
* The INVOKEDYNAMIC op invokes a method against a class bound at runtime
* via a MethodHandle.
* <p><code><pre>
* JASM op         :  INVOKEDYNAMIC    (0xba)
* JVM byte code(s):  INVOKEDYNAMIC    (0xba)
* Details         :
* </pre></code>
*
* @author hr  2012.08.06
*/
public class Invokedynamic
        extends OpConst
        implements Constants
    {

    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    *
    * @param constant  the InvokeDynamicConstant
    */
    public Invokedynamic(InvokeDynamicConstant constant)
        {
        super(INVOKEDYNAMIC, constant);
        }

    // ----- VMStructure operations -----------------------------------------

    /**
    * {@inheritDoc}
    */
    protected void assemble(DataOutput stream, ConstantPool pool)
            throws IOException
        {
        stream.writeByte(INVOKEDYNAMIC);
        stream.writeShort(pool.findConstant(super.getConstant()));
        stream.writeByte(0);
        stream.writeByte(0);
        }

    // ----- Op operations --------------------------------------------------

    /**
    * {@inheritDoc}
    */
    protected void calculateSize(ConstantPool pool)
        {
        setSize(5);
        }
    }
