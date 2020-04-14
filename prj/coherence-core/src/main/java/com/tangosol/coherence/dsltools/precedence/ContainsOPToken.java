/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dsltools.precedence;


import com.tangosol.coherence.dsltools.termtrees.Term;


/**
* ContainsOPToken is used to implement a contains operation that checks for
* membership in a list.  Example "x contains 5". The parsing can support
* the additional keywors "any" or "all" as in "x contains any (4,8,9)".
*
* @author djl  2009.03.14
*/
public class ContainsOPToken
        extends OPToken
    {
    // ----- constructors ---------------------------------------------------

     /**
     * Construct a new ContainsOPToken with the given parameters.
     *
     * @param sId  string identifier for this token
     * @param nBp  the binding power for this token
     */
     public ContainsOPToken(String sId, int nBp)
        {
        super(sId, nBp);
        }


    // ----- Operator Presidence API ----------------------------------------

    /**
    * {@inheritDoc}
    */
    public Term led(OPParser p, Term leftNode)
        {
        OPToken tok     = p.getScanner().getCurrent();
        String  sMyName = getId();

        if (tok instanceof IdentifierOPToken)
            {
            String sTok = tok.getValue();
            if (sTok.equalsIgnoreCase("all") || sTok.equalsIgnoreCase("any"))
                {
                sMyName = sMyName + "_" + sTok;
                p.getScanner().next();
                }
            }
        return newAST(getLedASTName(),
                sMyName,
                leftNode,
                p.expression(getBindingPower() - 1));
        }
    }
