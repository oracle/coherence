/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dsltools.precedence;


import com.tangosol.coherence.dsltools.termtrees.Term;


/**
* InfixOPToken is used to implement infix operators.  If enabled it will also
* do the right thing for unary operators such as + and - which are typically
* overloaded.
*
* @author djl  2009.03.14
*/
public class InfixOPToken
        extends OPToken
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a new InfixOPToken with the given parameters.
    *
    * @param sId  string identifier for this token
    * @param nBp  the binding power for this token
    */
    public InfixOPToken(String sId, int nBp)
        {
        super(sId, nBp);
        }

    /**
    * Construct a new InfixOPToken with the given parameters.
    *
    * @param sId       string representation of the token
    * @param nBp       the binding power for this token
    * @param sASTName  the name for this tokens AST
    */
    public InfixOPToken(String sId, int nBp, String sASTName)
        {
        super(sId, nBp, sASTName);
        }

    /**
    * Construct a new InfixOPToken with the given parameters.
    *
    * @param sId          string representation of the token
    * @param nBP          the binding power for this token
    * @param sLedASTName  the name for this tokens AST
    * @param sNudASTName  the name for this tokens AST
    */
    public InfixOPToken(String sId, int nBP, String sLedASTName,
            String sNudASTName)
        {
        super(sId, nBP, sLedASTName, sNudASTName);
        }


    // ----- Operator Precedence API ----------------------------------------

    /**
    * {@inheritDoc}
    */
    public Term led(OPParser p, Term leftNode)
        {
        return newAST(
                getLedASTName(),
                getId(),
                leftNode,
                p.expression(getBindingPower()));
        }

    /**
    * {@inheritDoc}
    */
    public Term nud(OPParser p)
        {
        if (!m_fPrefixAllowed)
            {
            throw new OPException("Infix operator " + getId() +
                " used in prefix position");
            }
        return newAST(
                getNudASTName(),
                getId(),
                p.expression(getBindingPower()));
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Get the flag that control whether the operator can be used as a prefix.
    *
    * @return the string representation
    */
    public boolean isPrefixAllowed()
        {
        return m_fPrefixAllowed;
        }

    /**
    * Set the flag that control whether the operator may be used as a prefix.
    *
    * @param fIsPrefix  the string representation for the token
    */
    public void setPrefixAllowed(boolean fIsPrefix)
        {
        m_fPrefixAllowed = fIsPrefix;
        }


    // ----- data members ---------------------------------------------------

    /**
    * Flag that control whether the operator can be used as a prefix.
    */
    protected boolean m_fPrefixAllowed = false;
   }
