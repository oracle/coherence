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
* IdentifierOPToken is used to implement identifiers.
*
* @author djl  2009.03.14
*/
public class IdentifierOPToken
        extends OPToken
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a new IdentifierOPToken with the given parameters.
    *
    * @param s string value of the identifier
    */
    public IdentifierOPToken(String s)
        {
        super(s);
        setBindingPower(OPToken.PRECEDENCE_IDENTIFIER);
        }

    /**
    * Construct a new IdentifierOPToken with the given parameters.
    *
    * @param sIdentifier  string value of the identifier
    * @param sNudASTName  the ast name to use for constructing an ast
    */
    public IdentifierOPToken(String sIdentifier, String sNudASTName)
        {
        this(sIdentifier);
        m_sNudASTName = sNudASTName;
        }


    // ----- Operator Presidence API ----------------------------------------

    /**
    * {@inheritDoc}
    */
    public Term nud(OPParser parser)
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
        throw new OPException("Unexpected Identifier " + getId() +
            " in operator position");
        }
    }






