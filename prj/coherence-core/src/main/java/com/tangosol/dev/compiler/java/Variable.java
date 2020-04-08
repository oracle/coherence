/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.compiler.java;


import com.tangosol.dev.assembler.CodeAttribute;
import com.tangosol.dev.assembler.Constants;
import com.tangosol.dev.assembler.OpDeclare;

import com.tangosol.dev.compiler.CompilerException;
import com.tangosol.dev.compiler.Context;

import com.tangosol.dev.component.DataType;

import com.tangosol.util.ErrorList;

import java.util.Set;
import java.util.Map;


/**
* A local variable.
*
* @version 1.00, 12/08/98
* @author  Cameron Purdy
*/
public class Variable
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a Variable.
    *
    * @param block   the owning block
    * @param sName   the name of the variable
    * @param dt      the data type of the variable
    * @param fParam  true if the variable is a parameter
    * @param fFinal  true if the variable is declared as final
    */
    protected Variable(Block block, String sName, DataType dt, boolean fParam, boolean fFinal)
        {
        m_block  = block;
        m_sName  = sName;
        m_dt     = dt;
        m_fParam = fParam;
        m_fFinal = fFinal;

        m_iHash = sm_iLastHash = (int) (((long) sm_iLastHash + BIGPRIME) % INTLIMIT);
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Determine the variable scope.
    *
    * @return  the block owning the variable
    */
    public Block getBlock()
        {
        return m_block;
        }

    /**
    * Determine the variable name.
    *
    * @return  the name of the variable
    */
    public String getName()
        {
        return m_sName;
        }

    /**
    * Determine the variable data type.
    *
    * @return  the data type of the variable
    */
    public DataType getType()
        {
        return m_dt;
        }

    /**
    * Determine if the variable is a parameter.
    *
    * @return  true if the variable is a parameter
    */
    public boolean isParameter()
        {
        return m_fParam;
        }

    /**
    * Determine if the variable is final.
    *
    * @return  true if the variable is final
    */
    public boolean isFinal()
        {
        return m_fFinal;
        }

    /**
    * Determine if the variable has a constant value.
    *
    * @return true if the expression results in a constant value
    */
    public boolean isConstant()
        {
        return m_fFinal && m_oVal != NO_VALUE && m_oVal != UNKNOWN_VALUE;
        }

    /**
    * Determine the constant value of the variable.
    *
    * @return the constant value of the variable
    */
    public Object getValue()
        {
        return m_oVal;
        }

    /**
    * Set the constant value of the variable.
    *
    * @param oVal  the constant value of the variable
    */
    protected void setValue(Object oVal)
        {
        if (m_oVal == NO_VALUE)
            {
            m_oVal = oVal;
            }
        else
            {
            // not considered constant if assigned multiple values
            m_oVal = UNKNOWN_VALUE;
            }
        }

    /**
    * Get the variable declaration op.
    *
    * @return the assembly op which refers to this variable
    */
    public OpDeclare getOp()
        {
        return m_op;
        }

    /**
    * Set the variable declaration op.
    *
    * @param op  the assembly op which refers to this variable
    */
    protected void setOp(OpDeclare op)
        {
        m_op = op;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Produce a fairly unique hash code.
    *
    * @return the hash code for this object
    */
    public int hashCode()
        {
        return m_iHash;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "Variable";

    /**
    * A big prime number.
    * @see <a href="http://www.utm.edu/research/primes/lists/small/small.html">
    * http://www.utm.edu/research/primes/lists/small/small.html</a>
    */
    private static final long BIGPRIME = 1500450271L;

    /**
    * A number just a little too big to be an int.
    */
    private static final long INTLIMIT = 0x80000000L;

    /**
    * The last hash code given out.
    */
    private static int sm_iLastHash;

    /**
    * Hash code.
    */
    private int m_iHash;

    /**
    * The variable's scope.
    */
    private Block m_block;

    /**
    * The variable's name.
    */
    private String m_sName;

    /**
    * The variable's data type.
    */
    private DataType m_dt;

    /**
    * Is the variable a parameter?
    */
    private boolean m_fParam;

    /**
    * Is the variable final?
    */
    private boolean m_fFinal;

    /**
    * A place-holder for no value.
    */
    public static final Object NO_VALUE = new Object();

    /**
    * A place-holder for multiple or unknown values.
    */
    public static final Object UNKNOWN_VALUE = new Object();

    /**
    * What is the variable's constant value?
    */
    private Object m_oVal = NO_VALUE;

    /**
    * The assembly variable.
    */
    private OpDeclare m_op;
    }
