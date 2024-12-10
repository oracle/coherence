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
* The CATCH pseudo-op closes a guarded section, specifies an exception to
* guard against, and specifies an exception handler.
* <p><code><pre>
* JASM op         :  CATCH      (0xff)
* JVM byte code(s):  n/a
* Details         :
* </pre></code>
*
* @version 0.50, 06/19/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class Catch extends Op implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    *
    * @param opTry  the Try op that started the guarded section that this op
    *               is closing
    * @param clz    a class constant to guard against or null to catch all
    * @param label  the label for the exception handler
    */
    public Catch(Try opTry, ClassConstant clz, Label label)
        {
        super(CATCH);

        if (opTry == null || label == null)
            {
            throw new IllegalArgumentException(CLASS +
                    ":  The Try op and exception handler label arguments are required!");
            }

        m_opTry = opTry;
        m_clz   = clz;
        m_label = label;

        opTry.addCatch(this);
        }


    // ----- Object operations ----------------------------------------------

    /**
    * Produce a human-readable string describing the byte code.
    *
    * @return a string describing the byte code
    */
    public String toString()
        {
        String sName   = getName();
        String sExcept = (m_clz == null ? "*" : m_clz.format());
        String sLabel  = m_label.format();
        return format(null, sName + ' ' + sExcept + ' ' + sLabel, null);
        }

    /**
    * Produce a human-readable string describing the byte code.
    *
    * @return a string describing the byte code
    */
    public String toJasm()
        {
        String sName   = getName();
        String sExcept = (m_clz == null ? "*" : m_clz.format());
        String sLabel  = String.valueOf(m_label.getOffset());
        return sName + ' ' + sExcept + " goto " + sLabel;
        }


    // ----- VMStructure operations -----------------------------------------

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
        pool.registerConstant(m_clz);
        }


    // ----- Op operations --------------------------------------------------

    /**
    * Determine if the op is reachable; this is only valid after calculating
    * the max stack.
    *
    * @return true if the op was reached by the stack size calculating
    *         algorithm
    */
    protected boolean isReachable()
        {
        // if try is reachable then keep the catch
        return m_opTry.isReachable();
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Get the Try op corresponding to this op.
    *
    * @return the Try op
    */
    public Try getTry()
        {
        return m_opTry;
        }

    /**
    * Get the ClassConstant that this op catches.
    *
    * @return the exception class guarded against or null if all
    */
    public ClassConstant getExceptionClass()
        {
        return m_clz;
        }

    /**
    * Get the label for the exception handler.
    *
    * @return the label for the exception handler
    */
    public Label getLabel()
        {
        return m_label;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Catch";

    /**
    * The Try op corresponding to this Catch op.
    */
    private Try m_opTry;

    /**
    * The class constant guarded against by this Catch op.
    */
    private ClassConstant m_clz;

    /**
    * The label of the exception handler.
    */
    private Label m_label;
    }
