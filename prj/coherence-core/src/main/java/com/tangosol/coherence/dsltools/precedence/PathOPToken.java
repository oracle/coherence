/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dsltools.precedence;


import com.tangosol.coherence.dsltools.termtrees.Term;


/**
* PathOPToken is used to implement dereferencing paths where you have
* a sequence of identifiers or function calls seperated by a path separator.
*
* @author djl  2009.03.14
*/
public class PathOPToken
        extends OPToken
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a new PathOPToken with the given parameters.
    *
    * @param sId  string identifier for this token
    * @param nBp  the binding power for this token
    */
    public PathOPToken(String sId, int nBp)
        {
        super(sId, nBp);
        }

    /**
    * Construct a new PathOPToken with the given parameters.
    *
    * @param sId          string representation of the token
    * @param nBp          the binding power for this token
    * @param sLedASTName  the name for this tokens AST
    */
    public PathOPToken(String sId, int nBp, String sLedASTName)
        {
        super(sId, nBp, sLedASTName);
        }


    // ----- Operator Presidence API ----------------------------------------

    /**
    * {@inheritDoc}
    */
    public Term led(OPParser p, Term leftNode)
       {
       // Coaless Terms that are the same into one Term that represents
       // the path
       Term   rightNode   = p.expression(getBindingPower() - 1);
       Term   t           = leftNode;
       String sLedASTName = getLedASTName();
       String sFunctor    = sLedASTName == null ? getId() : sLedASTName;

       if (!t.getFunctor().equals(sFunctor))
           {
           t = newAST(sFunctor, leftNode);
           }
       if (rightNode.getFunctor().equals(sFunctor))
           {
           for (int i = 1, c = rightNode.length() ; i <= c; ++i)
               {
               t = t.withChild(rightNode.termAt(i));
               }
           }
       else
           {
           t = t.withChild(rightNode);
           }
       return t;
       }
    }
