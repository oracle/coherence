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
* ListOPToken is used to process expressions between bracketing characters
* such as are between "[" and "]" which should result in a list (e.g.
* [1,3,4,5]).  This class does nothing to limit nesting hence
* nested lists are possible.
*
* @author djl  2009.03.14
*/
public class ListOpToken
        extends NestingOPToken
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a new InfixOPToken with the given parameters.
    *
    * @param sId  string identifier for this token
    * @param nBp  the binding power for this token
    */
    public ListOpToken(String sId, int nBp)
        {
        super(sId, nBp);
        }

    /**
    * Construct a new InfixOPToken with the given parameters.
    *
    * @param sId          string representation of the token
    * @param nBp          the binding power for this token
    * @param sNudASTName  the name for this tokens AST
    */
    public ListOpToken(String sId, int nBp, String sNudASTName)
        {
        super(sId, nBp, null, sNudASTName);
        }


    // ----- Operator Presidence API ----------------------------------------

    /**
    * {@inheritDoc}
    */
    public Term nud(OPParser p)
        {
        String sNudASTName  = getNudASTName();
        Term[] aLst         = p.readNestedCommaSeparatedList(getNest());
        String sFunctor     = sNudASTName == null ? getId() : sNudASTName;

        return Terms.newTerm(sFunctor, aLst);
        }
    }