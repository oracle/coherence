/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dsltools.precedence;


import com.tangosol.coherence.dsltools.base.LiteralBaseToken;

import com.tangosol.coherence.dsltools.termtrees.AtomicTerm;
import com.tangosol.coherence.dsltools.termtrees.Term;
import com.tangosol.coherence.dsltools.termtrees.Terms;


/**
* LiteralOpToken is used to implement literals.
*
* @author djl  2009.03.14
*/
public class LiteralOPToken
        extends OPToken
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a new LiteralOPToken with the given parameters.
    *
    * @param bt  the LiteralBaseToken that holds the literal info
    */
    public LiteralOPToken(LiteralBaseToken bt)
        {
        this(bt.getValue(), bt.getType());
        }

    /**
    * Construct a new LiteralOPToken with the given parameters.
    *
    * @param s            string representation of the literal
    * @param nTypeCode    the type code for this literal token
    * @param sNudASTName  the name to use for building an AST
    */
    public LiteralOPToken(String s, int nTypeCode, String sNudASTName)
        {
        this(s, nTypeCode);
        m_sNudASTName = sNudASTName;
        }

    /**
    * Construct a new LiteralOPToken with the given parameters.
    *
    * @param s          string representation of the literal
    * @param nTypeCode  the type code for this literal token
    */
     public LiteralOPToken(String s, int nTypeCode)
        {
        super(s);
        m_nType = nTypeCode;
        setBindingPower(OPToken.PRECEDENCE_IDENTIFIER);
        }

    // ----- Operator Presidence API ----------------------------------------

    /**
    * {@inheritDoc}
    */
    public Term nud(OPParser p)
        {
        String sNudASTName = getNudASTName();
        int    nType       = m_nType;

        if (sNudASTName == null)
            {
            return new AtomicTerm(getValue(), nType);
            }
        return Terms.newTerm(sNudASTName,
                new AtomicTerm(getValue(), nType));
        }

    /**
    * {@inheritDoc}
    */
    public Term led(OPParser parser, Term leftNode)
        {
        throw new OPException("Unexpected Literal " + getId() +
            " in operator position");
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Return a human-readable description for this token.
    *
    * @return a String description of the token
    */
    public String toString()
        {
        return "LiteralOPToken(" + getValue() + ")";
        }


    // ----- data members ---------------------------------------------------

    /**
    * The type code for this token
    */
    private int m_nType = 0;
    }

