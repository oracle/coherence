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
* The INVOKEINTERFACE op invokes a method (virtually) using an interface
* reference.
* <p><code><pre>
* JASM op         :  INVOKEINTERFACE    (0xb9)
* JVM byte code(s):  INVOKEINTERFACE    (0xb9)
* Details         :
* </pre></code>
*
* @version 0.50, 06/15/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class Invokeinterface extends OpConst implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    *
    * @param constant  the InterfaceConstant
    */
    public Invokeinterface(InterfaceConstant constant)
        {
        super(INVOKEINTERFACE, constant);
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
        // the byte code that invokes an interface method is really
        // non-standard -- it sticks out like a sore thumb in the JVM
        // specification because it reserves space for an optimization
        // done only in Sun's JVM and it also carries redundant data
        // (the number of arguments)
        InterfaceConstant constant = (InterfaceConstant) super.getConstant();
        String[]          asType   = Method.toTypes(constant.getType());
        int               cTypes   = asType.length;

        // calculate the number of arguments, including the reference,
        // in "words", where long and double types use two words and
        // all other types use one word
        int cArgs = 1;  // "this"
        for (int i = 1; i < cTypes; ++i)
            {
            char chType = asType[i].charAt(0);
            cArgs += OpDeclare.getJavaWidth(chType);
            }

        stream.writeByte(INVOKEINTERFACE);
        stream.writeShort(pool.findConstant(constant));
        stream.writeByte(cArgs);
        stream.writeByte(0);
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
        setSize(5);
        }

    /**
    * Returns the effect of the byte code on the height of the stack.
    *
    * @return the number of words pushed (if positive) or popped (if
    *         negative) from the stack by the op
    */
    public int getStackChange()
        {
        // pops reference and parameters
        // pushes return value
        InterfaceConstant constant = (InterfaceConstant) getConstant();
        return -1 - constant.getTotalParameterSize() + constant.getVariableSize(0);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Invokeinterface";
    }
