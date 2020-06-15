/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dsltools.precedence;


import com.tangosol.coherence.dsltools.base.NestedBaseTokens;


/**
* NestingOPToken is an abstract classused to implement parsing situation
* where some nesting is implied.  Typical uses are for processing between
* bracked symbols like "(" and ")". This class supports processing a nested
* collection of BaseTokens.
*
* @author djl  2009.03.14
*/
public class NestingOPToken
        extends OPToken
        implements Cloneable

    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a new NestingOPToken with the given parameters.
     *
     * @param sId  string identifier for this token
     * @param nBp  the binding power for this token
     */
     public NestingOPToken(String sId, int nBp)
         {
         super(sId, nBp);
         }
    /**
    * Construct a new NestingOPToken with the given parameters.
    *
    * @param sId          string representation of the token
    * @param nBp          the binding power for this token
    * @param sLedASTName  the name for this tokens AST
    * @param sNudASTName  the name for this tokens AST
    */
    public NestingOPToken(String sId, int nBp, String sLedASTName,
            String sNudASTName)
        {
        super(sId, nBp, sLedASTName, sNudASTName);
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Obtain the NestedBaseTokens that this token will process.
    *
    * @return the NestedBaseTokens that this token will process
    */
     public NestedBaseTokens getNest()
        {
        return m_nest;
        }

    /**
    * Set the NestedBaseTokens to process.
    *
    * @param nest  the NestedBaseTokens object to process
    * @return the receiver
    */
     public NestingOPToken setNest(NestedBaseTokens nest)
        {
        m_nest = nest;
        return this;
        }


    // ----- Object methods -------------------------------------------------

    public Object clone() throws CloneNotSupportedException
        {
        return super.clone();
        }


    // ----- data members ---------------------------------------------------

    /**
    * The nested collection of base tokens used for processing
    */
    NestedBaseTokens m_nest = null;
    }
