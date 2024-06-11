/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.assembler;

import java.io.IOException;
import java.io.DataInput;
import java.io.DataOutput;

/**
* Represent a Java Virtual Machine constant.
*
* @version 0.50, 05/12/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public abstract class Constant extends VMStructure implements Constants, Comparable
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct with a specific constant tag.
    *
    * @param nTag  the constant tag
    */
    protected Constant(int nTag)
        {
        m_nTag = nTag;
        }


    // ----- Constant operations --------------------------------------------

    /**
    * Based on the tag of the constant, which is encountered first in the
    * stream, construct the correct constant class and disassemble it.
    *
    * @param stream  the stream implementing java.io.DataInput from which
    *                to read the constant information
    * @param pool    the constant pool for the class which does not yet
    *                contain the constants referenced by this constant
    */
    protected static Constant loadConstant(DataInput stream, ConstantPool pool)
            throws IOException
        {
        int      nTag     = stream.readUnsignedByte();
        Constant constant;

        switch (nTag)
            {
            case CONSTANT_UTF8:
                constant = new UtfConstant();
                break;
            case CONSTANT_INTEGER:
                constant = new IntConstant();
                break;
            case CONSTANT_FLOAT:
                constant = new FloatConstant();
                break;
            case CONSTANT_LONG:
                constant = new LongConstant();
                break;
            case CONSTANT_DOUBLE:
                constant = new DoubleConstant();
                break;
            case CONSTANT_CLASS:
                constant = new ClassConstant();
                break;
            case CONSTANT_STRING:
                constant = new StringConstant();
                break;
            case CONSTANT_FIELDREF:
                constant = new FieldConstant();
                break;
            case CONSTANT_METHODREF:
                constant = new MethodConstant();
                break;
            case CONSTANT_INTERFACEMETHODREF:
                constant = new InterfaceConstant();
                break;
            case CONSTANT_NAMEANDTYPE:
                constant = new SignatureConstant();
                break;
            case CONSTANT_METHODHANDLE:
                ClassFile cf = pool.getClassFile();
                constant = new MethodHandleConstant(
                        cf != null && cf.getMajorVersion() >= 52);
                break;
            case CONSTANT_METHODTYPE:
                constant = new MethodTypeConstant();
                break;
            case CONSTANT_DYNAMIC:
                constant = new DynamicConstant();
                break;
            case CONSTANT_INVOKEDYNAMIC:
                constant = new InvokeDynamicConstant();
                break;
            case CONSTANT_MODULE:
                constant = new ModuleConstant();
                break;
            case CONSTANT_PACKAGE:
                constant = new PackageConstant();
                break;
            default:
                throw new IOException("Invalid constant tag " + nTag);
            }

        constant.disassemble(stream, pool);
        return constant;
        }

    /**
    * Determine the enumerated JVM constant classification.
    *
    * @return the JVM constant tag
    */
    public final int getTag()
        {
        return m_nTag;
        }

    /**
    * Determine the number of constant pool slots used by the constant.
    *
    * @return the number of constant pool slots used by the constant
    */
    protected final int getElementSize()
        {
        return CONSTANT_SIZE[m_nTag];
        }


    // ----- VMStructure operations -----------------------------------------

    /**
    * Read the constant information from the stream.  Since constants can be
    * inter-related, the dependencies are not derefenced until all constants
    * are disassembled; at that point, the constants are resolved using the
    * postdisassemble method.
    *
    * @param stream  the stream implementing java.io.DataInput from which
    *                to read the constant information
    * @param pool    the constant pool for the class which does not yet
    *                contain the constants referenced by this constant
    */
    protected abstract void disassemble(DataInput stream, ConstantPool pool)
        throws IOException;

    /**
    * Resolve any referenced constants.  When stored persistently, those
    * constant types which refer to other constants do so by integer index;
    * this method dereferences the integer index to access the referred-to
    * constants directly.  This method is overridden by constant types which
    * reference other constants.
    *
    * @param pool  the constant pool for the class which now contains all
    *              constants referenced by this constant
    */
    protected void postdisassemble(ConstantPool pool)
        {
        }

    /**
    * Register any referenced constants.  This method is overridden by
    * constant types which reference other constants.
    *
    * The pre-assembly step collects the necessary entries for the constant
    * pool.  During this step, all constants used by this VM structure and
    * any sub-structures are registered with (but not yet bound by position
    * in) the constant pool.
    *
    * @param pool  the constant pool for the class which needs to be
    *              populated with the constants required to build this
    *              VM structure
    */
    protected void preassemble(ConstantPool pool)
        {
        }

    /**
    * The assembly process assembles and writes the constant to the passed
    * output stream.  The particular constant implementation should super
    * to this generic implementation before assembling its own data.
    *
    * @param stream  the stream implementing java.io.DataOutput to which to
    *                write the assembled constant
    * @param pool    the constant pool for the class which by this point
    *                contains the entire set of constants required to build
    *                this VM structure
    */
    protected void assemble(DataOutput stream, ConstantPool pool)
            throws IOException
        {
        stream.writeByte(m_nTag);
        }


    // ----- Comparable operations ------------------------------------------

    /**
    * Compares this Object with the specified Object for order.  Returns a
    * negative integer, zero, or a positive integer as this Object is less
    * than, equal to, or greater than the given Object.
    *
    * @param   obj the <code>Object</code> to be compared.
    *
    * @return  a negative integer, zero, or a positive integer as this Object
    *          is less than, equal to, or greater than the given Object.
    *
    * @exception ClassCastException the specified Object's type prevents it
    *            from being compared to this Object.
    */
    public abstract int compareTo(Object obj);


    // ----- Object operations ----------------------------------------------

    /**
    * Produce a human-readable string describing the constant.
    *
    * @return a string describing the constant
    */
    public abstract String toString();

    /**
    * Format the constant as it would appear in JASM code.
    *
    * @return the constant as it would appear in JASM code
    */
    public abstract String format();

    /**
    * Compare this object to another object for equality.
    *
    * @param obj  the other object to compare to this
    *
    * @return true if this object equals that object
    */
    public abstract boolean equals(Object obj);


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Constant";

    /**
    * The JVM constant classification tag.
    */
    private int m_nTag;

    /**
    * The cached location of the constant in the pool; used as an
    * optimization to avoid repeatedly searching for the same constant if
    * it isn't actually moving around in the pool.
    */
    protected transient int m_iLastKnownLocation;
    }
