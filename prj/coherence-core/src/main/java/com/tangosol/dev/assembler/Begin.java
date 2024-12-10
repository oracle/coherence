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

import java.util.Vector;
import java.util.Enumeration;
import java.util.HashSet;


/**
* The BEGIN pseudo op opens a variable scope.
* <p><code><pre>
* JASM op         :  BEGIN (0xea)
* JVM byte code(s):  n/a
* Details         :
* </pre></code>
*
* @version 0.50, 06/17/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class Begin extends Op implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    */
    public Begin()
        {
        super(BEGIN);
        }


    // ----- Op operations --------------------------------------------------

    /**
    * Determine if the op is discardable.  Begin and End ops are never
    * considered discardable since they may come before labels and after
    * returns/unconditional branches and since they do not affect execution.
    *
    * @return false always
    */
    protected boolean isDiscardable()
        {
        return false;
        }


    // ----- accessors ------------------------------------------------------

    // ----- end of scope

    /**
    * Get the End for this Begin.
    *
    * @return  the End op for this scope
    */
    protected End getEnd()
        {
        return m_end;
        }

    /**
    * Set the End for this Begin.
    *
    * @param end  the end of this scope
    */
    protected void setEnd(End end)
        {
        m_end = end;
        }


    // ----- nested scope

    /**
    * Get the begin that this begin is within.
    *
    * @return  the Begin op of the outer scope
    */
    protected Begin getOuterScope()
        {
        return m_outer;
        }

    /**
    * Set the begin that this begin is within.
    *
    * @param begin  the start of the outer scope
    */
    protected void setOuterScope(Begin begin)
        {
        m_outer = begin;
        }


    // ----- variable declarations

    /**
    * Add a variable to this scope.
    *
    * @param var  the variable being declared
    */
    protected void addDeclaration(OpDeclare var)
        {
        m_vectVar.addElement(var);
        }

    /**
    * Enumerate all variables declared within this immediate scope.
    *
    * @return an enumeration of this scope's variables
    */
    protected Enumeration getDeclarations()
        {
        return m_vectVar.elements();
        }


    // ----- variable "pool"

    /**
    * Get the variable pool state that existed when this scope was entered.
    *
    * @return  the variable pool
    */
    protected int[] getVariablePool()
        {
        return m_aiVar;
        }

    /**
    * Set the variable pool state as it exists when this scope is entered.
    *
    * @param  aiVar  the variable pool
    */
    protected void setVariablePool(int[] aiVar)
        {
        m_aiVar = aiVar;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Begin";

    /**
    * The End for this Begin.
    */
    private End m_end;

    /**
    * The Begin from this Begin's outer scope.
    */
    private Begin m_outer;

    /**
    * All variables declared within this immediate scope.
    */
    private Vector m_vectVar = new Vector();

    /**
    * The scope holds on to the entering state of the variable pool for the
    * assembler.
    */
    private int[] m_aiVar;
    }
