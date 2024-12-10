/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dsltools.precedence;


import com.tangosol.coherence.dsltools.termtrees.AtomicTerm;
import com.tangosol.coherence.dsltools.termtrees.Term;
import com.tangosol.coherence.dsltools.termtrees.Terms;


/**
* KeywordOPToken acts like a PunctuationOPToken when used in a led role
* and a Identifier when used as a nud.
*
* @author djl  2009.03.14
*/
public class KeywordOPToken
        extends OPToken
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a new KeywordOPToken with the given parameters.
    *
    * @param s string value of the identifier
    */
    public KeywordOPToken(String s)
        {
        super(s);
        m_sNudASTName = "identifier";
        setBindingPower(OPToken.PRECEDENCE_KEYWORD);
        }

    // ----- Operator Precedence API ----------------------------------------

    /**
    * {@inheritDoc}
    */
    public Term nud(OPParser p)
        {
        String sNudASTName = getNudASTName();

        if (sNudASTName == null)
            {
            return AtomicTerm.createSymbol(getValue());
            }
        return Terms.newTerm(
                sNudASTName,
                AtomicTerm.createSymbol(getValue()));
       }

    /**
    * {@inheritDoc}
    */
    public Term led(OPParser parser, Term leftNode)
        {
        return null;
        }

    }



