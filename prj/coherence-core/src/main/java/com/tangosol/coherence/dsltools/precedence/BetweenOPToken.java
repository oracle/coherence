/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dsltools.precedence;


import com.tangosol.coherence.dsltools.termtrees.Term;
import com.tangosol.coherence.dsltools.termtrees.Terms;


/**
* BetweenOPToken is used to parse a SQl like between statment.
* Example "x between 5 and 10.
*
* @author djl  2009.03.14
*/
public class BetweenOPToken
        extends OPToken
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a new BetweenOPToken with the given parameters.
    *
    * @param sId  string identifier for this token
    * @param nBp  the binding power for this token
    */
    public BetweenOPToken(String sId, int nBp)
        {
        super(sId, nBp);
        }

    /**
    * Construct a new BetweenOPToken with the given parameters.
    *
    * @param sId       string representation of the token
    * @param nBp       the binding power for this token
    * @param sAstName  the name for this tokens AST
    */
    public BetweenOPToken(String sId, int nBp, String sAstName)
        {
        super(sId, nBp, sAstName);
        }


    // ----- Operator Presidence API ----------------------------------------

    /**
    * {@inheritDoc}
    */
    public Term led(OPParser p, Term leftNode)
        {
        Term[] at = new Term[2];

        at[0] = p.expression(getBindingPower() + 1);
        p.m_scanner.advance("and");  // eat the and
        at[1] = p.expression(getBindingPower() + 1);
        return newAST(getLedASTName(),
                getId(),
                leftNode,
                Terms.newTerm("listNode",at));
        }
    }
