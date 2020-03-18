/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dsltools.precedence;


import com.tangosol.coherence.dsltools.termtrees.Term;
import com.tangosol.coherence.dsltools.termtrees.AtomicTerm;
import com.tangosol.coherence.dsltools.termtrees.Terms;


/**
* ParenOPToken is used to process expressions that are between "(" and ")".
* This can be an arithmetic expression such as (a+(b*c)+d). This class can
* also process the argument list to function calls such as f(a,b,c). Finally
* this class can process list literals such as (1,3,4,5).
*
* @author djl  2009.03.14
*/
public class ParenOPToken
        extends NestingOPToken
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a new ParenOpToken with the given parameters.
    *
    * @param sId  string identifier for this token
    * @param nBp  the binding power for this token
    */
    public ParenOPToken(String sId, int nBp)
        {
        super(sId, nBp);
        }

    /**
    * Construct a new ParenOpToken with the given parameters.
    *
    * @param sId          string representation of the token
    * @param nBp          the binding power for this token
    * @param sLedASTName  the name for this tokens AST
    * @param sNudASTName  the name for this tokens AST
    */
    public ParenOPToken(String sId, int nBp, String sLedASTName,
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
        String sFunctor;
        if (leftNode.isAtom())
            {
            sFunctor = ((AtomicTerm)leftNode).getValue();
            }
        else
            {
            sFunctor = ((AtomicTerm)leftNode.termAt(1)).getValue();
            }
        Term[] aLst = p.readNestedCommaSeparatedList(getNest());
        Term node = Terms.newTerm(sFunctor, aLst);
        String sLedASTName = getLedASTName();
        if (sLedASTName == null)
            {
            return node;
            }
        else
            {
            return newAST(sLedASTName, node);
            }
        }

    /**
    * {@inheritDoc}
    */
    public Term nud(OPParser p)
        {
         Term[] aLst = p.readNestedCommaSeparatedList(getNest());
         return aLst.length == 1 ? aLst[0]
                : Terms.newTerm((getNudASTName()== null ? getId() :
                     getNudASTName()),aLst);
        }
    }