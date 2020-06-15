/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dsltools.precedence;


import com.tangosol.coherence.dsltools.termtrees.Term;


/**
* InfixRightOPToken is used to implement infix operators that like to bind
 * to the right which is typical of exponentiation rules.
*
* @author djl  2009.03.14
*/
public class InfixRightOPToken
        extends OPToken
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a new InfixRightOPToken with the given parameters.
    *
    * @param sId  string identifier for this token
    * @param nBP  the binding power for this token
    */
    public InfixRightOPToken(String sId, int nBP)
        {
        super(sId, nBP);
        }

    /**
    * Construct a new InfixRightOPToken with the given parameters.
    *
    * @param sId          string representation of the token
    * @param nBp          the binding power for this token
    * @param sLedASTName  the name for this tokens AST
    */
    public InfixRightOPToken(String sId, int nBp, String sLedASTName)
        {
        super(sId, nBp, sLedASTName);
        }

    // ----- Operator Presidence API ----------------------------------------

    /**
    * {@inheritDoc}
    */
    public Term led(OPParser p, Term leftNode)
        {
        return newAST(getLedASTName(),
                getId(),
                leftNode, p.expression(getBindingPower() -1));
        }
    }
