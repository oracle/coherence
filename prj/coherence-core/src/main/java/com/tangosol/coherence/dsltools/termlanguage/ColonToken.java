/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dsltools.termlanguage;


import com.tangosol.coherence.dsltools.precedence.OPToken;
import com.tangosol.coherence.dsltools.precedence.OPParser;
import com.tangosol.coherence.dsltools.termtrees.Term;
import com.tangosol.coherence.dsltools.termtrees.Terms;
import com.tangosol.coherence.dsltools.termtrees.AtomicTerm;


/**
* Colon is used to make attributes in a list or bag.  The syntax is a:b
* where b is any Term.The results is the Term .attr.(a(b))
*
* @author djl  2009.08.31
*/
public class ColonToken
        extends OPToken
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a new ColonToken with the given parameters.
    *
    * @param sId  string identifier for this token
    * @param nBp  the binding power for this token
    */
    public ColonToken(String sId, int nBp)
        {
        super(sId, nBp);
        }


    // ----- Operator Presidence API ----------------------------------------

    /**
    * {@inheritDoc}
    */
     public Term led(OPParser p, Term leftNode)
        {
        Term rightNode = p.expression(getBindingPower() - 1);
        if (leftNode instanceof AtomicTerm)
            {
            String sKey = ((AtomicTerm)leftNode).getValue();
            return Terms.newTerm(".attr.",Terms.newTerm(sKey,rightNode));
            }
        else
            {
            return Terms.newTerm(".pair.",leftNode,rightNode);
            }
        }
    }
