/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dsltools.termtrees;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Term is the abstract class used to represent trees of Terms (term-trees).
 * A term (a node of a term tree) consists of a special label called a
 * functor and a sequence of child terms (called arguments). Strings, Numbers,
 * and Symbols are atomic-terms and are the leaves of a term-tree, i.e. they
 * have no children.
 *
 * Trees of symbols lie midway between sequences of symbols and graphs of
 * symbols in expressiveness and therefor represent somewhat of a sweet-spot
 * in representation space. Historically, term-trees can be found in Prolog
 * and Mathematica and are used for many of the same purposes that others use
 * S-Expressions or XML. All are generic means for representing trees of
 * symbols, and are useful for representing a great variety of kinds of data
 *
 * There exist encodings that freely go  between S-Expressions or XML into
 * term-trees. Powerful matching techniques exist that operate over
 * term-trees and at this point in time are 50 years old. Techniques that are
 * 60 years old show how powerful term-trees can be as the implementation
 * vehicle for interpreters and compilers. Finally,
 * "Term Language" (TL), the literal expression of term-trees is much more
 * readable and writable than either S-expressions or XML and is an excelent.
 * way to test the output of a parser by comparing a simple literal.
 *
 * The Term protocol distunguishes between atoms (such as literals) and nodes
 * with children, and leaf nodes that are atoms or nodes without children.
 * All Terms have a functor which acts somewhat like a type or classifier.
 *
 * Term-trees are especially useful as Abstract Syntax Trees (AST) and in
 * implementing expression languages.
 *
 * @author djl  2009.08.31
 */
public abstract class Term
        implements Iterable<Term>
    {

    // ----- Term API -------------------------------------------------------

    /**
     * Obtain the functor representation of the Term.
     *
     * @return the functor
     */
    public abstract String getFunctor();

    /**
     * Obtain the child term at the given index. The index is 1 based
     * for children and with at(0) returning the functor as an AtomicTerm.
     * Beware, your 0 based habits can cause problems but 1 based indexing
     * is useful since the functor is an interesting part of the information
     * space.  We are bowing here to the wisdom of Mathematica Expressions.
     *
     * @param index  index of the child or functor to return
     *
     * @return the child Term or functor as AtomicTerm if index is 0
     */
    public abstract Term termAt(int index);

    /**
     * Obtain the childern Terms
     *    
     * @return the children of the receiver
     */
    public abstract Term[] children();

    /**
     * Join the receiver with the given child Term. AtomicTerms will
     * construct a general list term (functor .list.) and NodeTerms
     * may be mutated.
     *
     * @param t  the term to join with
     *
     * @return the Term resulting from joining.
     */
    public abstract Term withChild(Term t);

    /**
     * Answer whether the receiver is equal to the given Term.
     * Terms are equal if their functors are equal and their children
     * are termEqual to the children of the given term.
     *
     * @param t  the Term to check for termEqual
     *
     * @return the boolean result of the comparison
     */
    public abstract boolean termEqual(Term t);

    /**
     * Answer a String representation of the Term that is allowed to
     * show more internal details than toString()
     * which does not compress information. Similar to Object.toString().
     *
     * @return a String representation of the receiver
     */
    public abstract String fullFormString();

    /**
     * Answer whether the receiver has children.
     *    
     * @return the boolean result of the isLeaf() test
     */
    public boolean isLeaf()
        {
        return true;
        }

    /**
     * Answer whether the receiver is an Atomic Term.
     *    
     * @return the boolean result of the isAtom() test
     */
    public boolean isAtom()
        {
        return true;
        }

    /**
     * Answer whether the receiver is an Atomic Term representing a Number.
     *    
     * @return the boolean result of the isNumber() test
     */
    public boolean isNumber()
        {
        return false;
        }

    /**
     * Answer whether the length of the receivers children
     *    
     * @return the length
     */
    public int length()
        {
        return children().length;
        }

    // ----- Term search and matching api -----------------------------------

    /**
     * Find the Term amoungst the children whose functor equals the
     * given functor.
     *
     * @param sFunctor  the functor to search for
     *
     * @return the found Term or null if not found
     */
    public Term findChild(String sFunctor)
        {
        Term[] aTerms = children();

        for (int i = 0, c = aTerms.length; i < c; ++i)
            {
            Term t = aTerms[i];
            if (sFunctor.equals(t.getFunctor()))
                {
                return t;
                }
            }

        return null;
        }

    /**
     * Find the Term amoungst the children whose functor equals the
     * given functor that has a singleton child.
     *
     * @param sFunctor  the functor to search for
     *
     * @return the found Term's first child or null if not found
     */
    public Term findAttribute(String sFunctor)
        {
        Term[] aTerms = children();

        for (int i = 0, c = aTerms.length; i < c; i++)
            {
            Term t = aTerms[i];
            if (sFunctor.equals(t.getFunctor()))
                {
                if (t.length() == 1)
                    {
                    return t.termAt(1);
                    }
                }
            }

        return null;
        }

    /**
     * Answer whether the receiver's children is equal to the given Terms
     * children. Terms are equal if their functors are equal and their children
     * are termEqual to the children of the given term.
     *
     * @param t  term whose children are to be checked for equality
     *
     * @return the found Term or null if not found
     */
    public boolean childrenTermEqual(Term t)
        {
        if (t == null)
            {
            return false;
            }

        Term[] aMyTerms    = children();
        int    count       = aMyTerms.length;
        Term[] aOtherTerms = t.children();

        if (count != aOtherTerms.length)
            {
            return false;
            }

        if (count == 0)
            {
            return true;
            }

        for (int i = 0; i < count; ++i)
            {
            if (!aMyTerms[i].termEqual(aOtherTerms[i]))
                {
                return false;
                }
            }

        return true;
        }

    /**
     * Find the Term amongst the children whose functor equals the
     * given functor.
     *
     * @param t  the functor to search for
     *
     * @return the found Term or null if not found
     */
    public boolean headChildrenTermEqual(Term t)
        {
        if (t == null)
            {
            return false;
            }

        if (t.isAtom())
            {
            return false;
            }

        Term[] aMyTerms    = children();
        int    count       = aMyTerms.length;
        Term[] aOtherTerms = t.children();

        if (count > aOtherTerms.length)
            {
            return false;
            }

        if (count == 0)
            {
            return true;
            }

        for (int i = 0; i < count; ++i)
            {
            if (!aMyTerms[i].termEqual(aOtherTerms[i]))
                {
                return false;
                }
            }

        return true;
        }

    // ----- TermWalker methods ---------------------------------------------

    /**
     * Do a dispatch back to the given walker.
     *
     * @param walker  the TermWalker that implements the visitor for Terms
     *
     */
    public void accept(TermWalker walker)
        {
        walker.acceptTerm(getFunctor(), this);
        }

    // ----- Iterable interface ---------------------------------------------

    @Override
    public Iterator<Term> iterator()
        {
        return new TermIterator(this);
        }

    // ----- inner class: TermIterator --------------------------------------

    /**
     * This {@link Iterator} implementation iterates over
     * the child {@link Term}s of a given {@link Term}.
     *
     * @author jk 2014.02.25
     * @since Coherence 12.2.1
     */
    public class TermIterator
            implements Iterator<Term>
        {

        // ----- constructors -----------------------------------------------

        /**
         * Construct an {@link Iterator} that will iterate over the child
         * {@link Term}s of the specified {@link Term}.
         *
         * @param termParent  the term to iterate over
         */
        public TermIterator(Term termParent)
            {
            f_termParent = termParent;
            m_nIndex = 1;
            }

        // ----- Iterator interface -----------------------------------------

        @Override
        public boolean hasNext()
            {
            return m_nIndex <= f_termParent.length();
            }

        @Override
        public Term next()
            {
            if (!hasNext())
                {
                throw new NoSuchElementException("Attempt to read past end of Term Iterator");
                }

            return f_termParent.termAt(m_nIndex++);
            }

        /**
         * @throws UnsupportedOperationException as this iterator does not support removal
         * of elements.
         */
        @Override
        public void remove()
            {
            throw new UnsupportedOperationException("Term Iterator does not support remove");
            }

        // ----- data members -----------------------------------------------

        /**
         * The Term who's children this {@link TermIterator} iterates over.
         */
        private final Term f_termParent;

        /**
         * A pointer to the next child term the iterator will return.
         */
        private int m_nIndex;
        }
    }
