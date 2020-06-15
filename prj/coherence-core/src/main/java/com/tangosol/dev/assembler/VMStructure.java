/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.assembler;


import com.tangosol.util.Base;

import java.io.IOException;
import java.io.DataInput;
import java.io.DataOutput;


/**
* Represents a Java Virtual Machine structure as defined by the Java Virtual
* Machine (JVM) Specification.  The JVM structures include the .class file
* structure and the structures which it can contain, such as fields, methods,
* and attributes.
*
* @version 0.50, 05/11/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public abstract class VMStructure
        extends Base
        implements Constants
    {
    /**
    * The disassembly process reads the structure from the passed input
    * stream and uses the constant pool to dereference any constant
    * references.
    *
    * @param stream  the stream implementing java.io.DataInput from which
    *                to read the assembled VM structure
    * @param pool    the constant pool for the class which contains any
    *                constants referenced by this VM structure
    */
    protected abstract void disassemble(DataInput stream, ConstantPool pool)
        throws IOException;

    /**
    * The pre-assembly step collects the necessary entries for the constant
    * pool.  During this step, all constants used by this VM structure and
    * any sub-structures are registered with (but not yet bound by position
    * in) the constant pool.
    *
    * @param pool  the constant pool for the class which needs to be
    *              populated with the constants required to build this
    *              VM structure
    */
    protected abstract void preassemble(ConstantPool pool);

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
    protected abstract void assemble(DataOutput stream, ConstantPool pool)
        throws IOException;

    /**
    * Determine the identity of the VM structure (if applicable).
    *
    * @return  the string identity of the VM structure
    */
    public String getIdentity()
        {
        return null;
        }

    /**
    * Determine if the VM structure (or any contained VM structure) has been
    * modified.
    *
    * @return true if the VM structure has been modified
    */
    public boolean isModified()
        {
        return false;
        }

    /**
    * Reset the modified state of the VM structure.
    */
    protected void resetModified()
        {
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "VMStructure";

    /**
    * Debug flag.
    */
    protected static final boolean DEBUG = false;
    }
