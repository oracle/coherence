/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dsltools.precedence;


import com.tangosol.coherence.dsltools.termtrees.Term;


/**
* PrefixOPToken is used to implement prefix operators.  Prefix operators
* are things like not, new, or "~".
*
* @author djl  2009.03.14
*/
public class PrefixOPToken
        extends OPToken
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a new PrefixOPToken with the given parameters.
    *
    * @param sId  string identifier for this token
    * @param nBP  the binding power for this token
    */
    public PrefixOPToken(String sId, int nBP)
        {
        super(sId, nBP);
        }

    /**
    * Construct a new PrefixOPToken with the given parameters.
    *
    * @param sId       string representation of the token
    * @param nBp       the binding power for this token
    * @param sASTName  the name for this tokens AST
    */
    public PrefixOPToken(String sId, int nBp, String sASTName)
        {
        this(sId, nBp);
        m_sNudASTName = sASTName;
        }


    // ----- Operator Presidence API ----------------------------------------

    /**
    * {@inheritDoc}
    */
    public Term nud(OPParser p)
        {
        return newAST(
                getNudASTName(),
                getId(),
                p.expression(getBindingPower()));
        }
    }