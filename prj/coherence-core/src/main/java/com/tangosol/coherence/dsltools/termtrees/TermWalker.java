/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dsltools.termtrees;


/**
* TermWalker is a visitor class that provides a framework for walking
* Term Trees
*
* @author djl  2009.08.31
*/
public interface TermWalker
    {
    // ----- TermWalker API -------------------------------------------------

    /**
    * The receiver has been dispatched to from the given node.
    *
    * @param sFunctor  the node functor
    * @param term      the NodeTerm
    */
    public void acceptNode(String sFunctor, NodeTerm term);

    /**
    * The receiver has been dispatched to from the given atom.
    *
    * @param sFunctor    the node functor
    * @param atomicTerm  the AtomicTerm
    */
    public void acceptAtom(String sFunctor, AtomicTerm atomicTerm);

    /**
    * The receiver has been dispatched to from the given atom.
    *
    * @param sFunctor  the node functor
    * @param term     the Term
    */
    public void acceptTerm(String sFunctor, Term term);

    /**
     * Return the result of the previous TermTree walk.
     * This value could be null if no trees have been walked
     * or the last tree walk resulted in an undetermined state.
     *
     * @return the result of the previous TermTree walk
     */
    public Object getResult();

    /**
     * Return the result of the walking the specified TermTree.
     * This value could be null if the tree walk results in
     * an undetermined state.
     *
     * @param term the term tree to walk to obtain a result object
     *
     * @return the result of walking the specified TermTree
     */
    public Object walk(Term term);
    }
