/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dsltools.precedence;


import com.tangosol.coherence.dsltools.termtrees.Term;
import com.tangosol.coherence.dsltools.termtrees.Terms;


/**
* LikeOPToken is used to parse a SQL like statement.
* Example "key() like 'key\_%' escape '\'
*
* @author bbc  2011.05.23
*/
public class LikeOPToken
        extends InfixOPToken
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a new LikeOPToken with the given parameters.
    *
    * @param sId  string identifier for this token
    * @param nBp  the binding power for this token
    */
    public LikeOPToken(String sId, int nBp)
        {
        super(sId, nBp);
        }

    /**
    * Construct a new LikeOPToken with the given parameters.
    *
    * @param sId       string representation of the token
    * @param nBp       the binding power for this token
    * @param sAstName  the name for this tokens AST
    */
    public LikeOPToken(String sId, int nBp, String sAstName)
        {
        super(sId, nBp, sAstName);
        }


    // ----- Operator Precedence API ----------------------------------------

    /**
    * {@inheritDoc}
    */
    public Term led(OPParser p, Term leftNode)
        {
        Term[] at = new Term[2];

        at[0] = p.expression(getBindingPower() + 1);
        if (p.m_scanner.advanceWhenMatching("escape"))
            {
            at[1] = p.expression(getBindingPower() + 1);
            return newAST(getLedASTName(),
                getId(), leftNode,
                Terms.newTerm("listNode", at));
            }
        else
            {
            return newAST(getLedASTName(),
                getId(), leftNode, at[0]);
            }
        }
    }
