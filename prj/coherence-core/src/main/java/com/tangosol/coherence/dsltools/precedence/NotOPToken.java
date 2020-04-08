/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dsltools.precedence;


import com.tangosol.coherence.dsltools.termtrees.Term;


/**
* NotOPToken is used to implement not operators.  It will also work for
* sql like infix not.
*
* @author djl  2009.03.14
*/
public class NotOPToken
        extends OPToken
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a new NotOPToken with the given parameters.
    *
    * @param sId  string identifier for this token
    * @param nBp  the binding power for this token
    */
    public NotOPToken(String sId, int nBp)
        {
        super(sId, nBp);
        }

    /**
    * Construct a new NotOPToken with the given parameters.
    *
    * @param sId          string representation of the token
    * @param nBp          the binding power for this token
    * @param sLedASTName  the name for this tokens AST
    * @param sNudASTName  the name for this tokens AST
    */
    public NotOPToken(String sId, int nBp, String sLedASTName,
            String sNudASTName)
        {
        super(sId, nBp, sLedASTName, sNudASTName);
        }


    // ----- Operator Presidence API ----------------------------------------

    /**
    * {@inheritDoc}
    */
    public Term led(OPParser p, Term leftNode)
        {
        // Not is really a prefix operator but
        // It is possible for ! to be after a left value and before the
        // operator like "x not in (1,2,3)"
        // so we deal with it in a funny led rather that corrupt our
        //  simple parser loop
        OPToken tok = p.getScanner().getCurrent();
        p.getScanner().next();
        Term t = tok.led(p, leftNode);
        return newAST(getLedASTName(), getId(), t);
        }

    /**
    * {@inheritDoc}
    */
    public Term nud(OPParser p)
        {
        Term t = p.expression(getBindingPower());
        return newAST(getNudASTName(), getId(), t);
        }
    }
