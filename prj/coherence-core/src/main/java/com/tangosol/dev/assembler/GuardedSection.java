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
* Represents a section of byte code which is guarded.  In Java, this is a
* the section of source after a try statement.  This class is publicly
* immutable.
* <p>
* The GuardedSection information is part of the Code Attribute of a method,
* and is defined by the JDK 1.1 documentation as:
* <p>
* <pre>
*   {
*   u2 start_pc;
*   u2 end_pc;
*   u2 handler_pc;
*   u2 catch_type;
*   }
* </pre>
* <p>
* The assembler builds guarded sections using the TRY and CATCH ops.  A TRY
* op is simply a place-holder; it assembles to a 0-length binary.  For a TRY,
* there are zero or more CATCH ops that reference the TRY, specify an
* exception to guard against, and provide an exception handler (via a LABEL).
* Like the TRY op, the CATCH op assembles to a 0-length binary, but each
* CATCH op encountered produces a GuardedSection structure.
* <p>
* Since TRY precedes the first guarded op and CATCH follows the last guarded
* op, the offset of the TRY is the offset of the guarded section of byte code
* and the offset of the CATCH is the offset of the first byte code following
* the guarded section of code.  This is very handy because the JVM spec for
* guarded sections specifies the start_pc and end_pc values (see structure
* above) in this exact manner.
* <p>
* The exception being guarded against can be null, which means "catch any".
* This is used to implement the finally keyword in the Java language, for
* example.  The guarded section information then specifies a zero for the
* catch_type value (see structure above).
*
* @version 0.50, 06/08/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class GuardedSection extends VMStructure implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor; typically used before disassembly.
    */
    protected GuardedSection()
        {
        }

    /**
    * Initializing constructor.
    *
    * @param opTry      the TRY op preceding the first guarded op
    * @param opCatch    the CATCH op following the last guarded op
    * @param clzExcept  the Java class deriving from java.lang.Throwable
    *                   which this section is guarded against; if null, all
    *                   exceptions are guarded against
    * @param opHandler  the LABEL to transfer execution to when the specified
    *                   exception occurs within the guarded section of code
    */
    protected GuardedSection(Try opTry, Catch opCatch, ClassConstant clzExcept, Label opHandler)
        {
        m_opTry     = opTry;
        m_opCatch   = opCatch;
        m_clzExcept = clzExcept;
        m_opHandler = opHandler;
        }


    // ----- VMStructure operations -----------------------------------------

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
    protected void disassemble(DataInput stream, ConstantPool pool)
            throws IOException
        {
        // read the guarded section information as integer offsets; defer
        // translation into op references until the byte code is disassembled
        m_ofTry     = stream.readUnsignedShort();
        m_ofCatch   = stream.readUnsignedShort();
        m_ofHandler = stream.readUnsignedShort();
        m_clzExcept = (ClassConstant) pool.getConstant(stream.readUnsignedShort());
        }

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
    protected void preassemble(ConstantPool pool)
        {
        pool.registerConstant(m_clzExcept);
        }

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
        stream.writeShort(m_opTry    .getOffset());
        stream.writeShort(m_opCatch  .getOffset());
        stream.writeShort(m_opHandler.getOffset());
        stream.writeShort(pool.findConstant(m_clzExcept));
        }


    // ----- Object operations ----------------------------------------------

    /**
    * Produce a human-readable string describing the attribute.
    *
    * @return a string describing the attribute
    */
    public String toString()
        {
        // by default, use the disassembled offsets
        int ofTry     = m_ofTry;
        int ofCatch   = m_ofCatch;
        int ofHandler = m_ofHandler;

        // if the ops are available (either because they were assembled as
        // opposed to being disassembled or because the byte code has been
        // fully disassembled), use the offsets from the ops instead
        if (m_opTry != null)
            {
            ofTry     = m_opTry    .getOffset();
            ofCatch   = m_opCatch  .getOffset();
            ofHandler = m_opHandler.getOffset();
            }

        return "(" + CLASS + ")->"
                + " on " + (m_clzExcept == null ? "any" : m_clzExcept.toString())
                + " in [" + ofTry + "," + ofCatch + ")"
                + " goto " + ofHandler;
        }

    /**
    * Compare this object to another object for equality.
    *
    * @param obj  the other object to compare to this
    *
    * @return true if this object equals that object
    */
    public boolean equals(Object obj)
        {
        try
            {
            GuardedSection that = (GuardedSection) obj;
            return  this            == that
                ||  this.getClass() == that.getClass()
                &&  this.m_clzExcept.equals(that.m_clzExcept)
                &&  this.m_opTry    .equals(that.m_opTry    )
                &&  this.m_opCatch  .equals(that.m_opCatch  )
                &&  this.m_opHandler.equals(that.m_opHandler);
            }
        catch (NullPointerException e)
            {
            // obj is null
            return false;
            }
        catch (ClassCastException e)
            {
            // obj is not of this class
            return false;
            }
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Determine the start of the guarded section of byte code.
    *
    * @return the offset in the byte code of the first guarded instruction
    */
    protected int getTryOffset()
        {
        return m_ofTry;
        }

    /**
    * Get the TRY op which starts the guarded section.
    *
    * @return the TRY op
    */
    public Try getTry()
        {
        return m_opTry;
        }

    /**
    * Set the TRY op.
    * (Used internally by the byte code disassembly process.)
    */
    protected void setTry(Try op)
        {
        m_opTry = op;
        }


    /**
    * Determine the end of the guarded section of byte code.
    *
    * @return the offset in the byte code of the first instruction following
    *         the guarded section
    */
    protected int getCatchOffset()
        {
        return m_ofCatch;
        }

    /**
    * Get the CATCH op which ends the guarded section.
    *
    * @return the CATCH op
    */
    public Catch getCatch()
        {
        return m_opCatch;
        }

    /**
    * Set the CATCH op.
    * (Used internally by the byte code disassembly process.)
    */
    protected void setCatch(Catch op)
        {
        m_opCatch = op;
        }


    /**
    * Get the exception which the section guards against.
    *
    * @return the caught exception or null for any
    */
    public ClassConstant getException()
        {
        return m_clzExcept;
        }


    /**
    * Determine the start of the guarded section of byte code.
    *
    * @return the offset in the byte code of the first guarded instruction
    */
    protected int getHandlerOffset()
        {
        return m_ofHandler;
        }

    /**
    * Get the LABEL op for the exception handler
    *
    * @return the LABEL op
    */
    public Label getHandler()
        {
        return m_opHandler;
        }

    /**
    * Set the LABEL op for the exception handler.
    * (Used internally by the byte code disassembly process.)
    */
    protected void setHandler(Label label)
        {
        m_opHandler = label;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "GuardedSection";


    /**
    * The TRY op.
    */
    private Try m_opTry;

    /**
    * The CATCH op past the last guarded op.
    */
    private Catch m_opCatch;

    /**
    * The exception class guarded against.
    */
    private ClassConstant m_clzExcept;

    /**
    * The label where the handler is located.
    */
    private Label m_opHandler;


    /**
    * The raw offset of the first guarded op.  This value is set by this
    * class's disassemble method and used by the byte code disassemble
    * method to determine the actual TRY op.
    */
    private int m_ofTry;

    /**
    * The raw offset of the op following the last guarded op.  This value
    * is set by this class's disassemble method and used by the byte code
    * disassemble method to determine the actual CATCH op.
    */
    private int m_ofCatch;

    /**
    * The raw offset of the exception handler.  This value is set by this
    * class's disassemble method and used by the byte code disassemble
    * method to determine the actual LABEL op for the exception handler.
    */
    private int m_ofHandler;
    }
