/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dsltools.termlanguage;


import com.tangosol.coherence.dsltools.precedence.NestingOPToken;
import com.tangosol.coherence.dsltools.precedence.OPParser;
import com.tangosol.coherence.dsltools.termtrees.Term;
import com.tangosol.coherence.dsltools.termtrees.AtomicTerm;
import com.tangosol.coherence.dsltools.termtrees.Terms;


/**
* CurlyToken is used to process expressions between bracketing characters
*  such as are between "{" and "}" which should result in a bag
* such as {1,3,4,5}.  It can be used as a literal or with a functor.
*
* @author djl  2009.08.31
*/
public class CurlyToken
         extends NestingOPToken
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a new CurlyToken with the given parameters.
    *
    * @param sId  string identifier for this token
    * @param nBp  the binding power for this token
    */
    public CurlyToken(String sId, int nBp)
        {
        super(sId, nBp);
        }


    // ----- Operator Presidence API ----------------------------------------

    /**
    * {@inheritDoc}
    */
    public Term led(OPParser p, Term leftNode)
        {
        Term[] aLst = p.readNestedCommaSeparatedList(getNest());
        if (leftNode instanceof AtomicTerm)
            {
            String sFunctor = ((AtomicTerm)leftNode).getValue();
            return Terms.newTerm(sFunctor, Terms.newTerm(".bag.",aLst));
            }
        else
            {
            return Terms.newTerm(".object.",leftNode,Terms.newTerm(".bag.",aLst));            
            }
        }

    /**
    * {@inheritDoc}
    */
    public Term nud(OPParser p)
        {
        Term[] aLst = p.readNestedCommaSeparatedList(getNest());
        return Terms.newTerm(".bag.",aLst);
        }
    }