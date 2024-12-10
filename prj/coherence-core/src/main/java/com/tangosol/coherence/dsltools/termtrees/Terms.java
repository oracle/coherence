/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dsltools.termtrees;


import com.tangosol.coherence.dslquery.CoherenceQueryLanguage;
import com.tangosol.coherence.dsltools.precedence.OPParser;
import com.tangosol.coherence.dsltools.termlanguage.TermLanguage;


/**
* Terms is a utility class that provides static convenience methods for
* the construction of Terms.  Terms also provides a convenience interface
* for converting String in the TermLanguage to trees of Terms
*
* @author djl  2009.08.31
*/
public class Terms
    {
    // ----- Factory API ---------------------------------------------------_

    /**
    * Construct a new TermNode with the given functor.
    *
    * @param sFunctor  the functor for the Term
    *
    * @return a term tree
    */
    public static Term newTerm(String sFunctor)
        {
        return new NodeTerm(sFunctor);
        }

    /**
    * Construct a new TermNode with the given functor and given Terms.
    *
    * @param sFunctor  the functor for the Term
    * @param aTerms    an children of the node
    *
    * @return a term tree
    */
    public static Term newTerm(String sFunctor, Term[] aTerms)
        {
        return new NodeTerm(sFunctor, aTerms);
        }

    /**
    * Construct a new TermNode with the given functor and given Terms.
    *
    * @param sFunctor  the functor for the Term
    * @param t1        a child term
    *
    * @return a term tree
    */
    public static Term newTerm(String sFunctor, Term t1)
        {
        return new NodeTerm(sFunctor, t1);
        }

    /**
    * Construct a new TermNode with the given functor and given Terms.
    *
    * @param sFunctor  the functor for the Term
    * @param t1        a child term
    * @param t2        a child term
    *
    * @return a term tree
    */
    public static Term newTerm(String sFunctor, Term t1, Term t2)
        {
        return new NodeTerm(sFunctor, t1,t2);
        }

    /**
    * Construct a new TermNode with the given functor and given Terms.
    *
    * @param sFunctor  the functor for the Term
    * @param t1        a child term
    * @param t2        a child term
    * @param t3        a child term
    *
    * @return a term tree
    */
    public static Term newTerm(String sFunctor, Term t1, Term t2, Term t3)
        {
        return new NodeTerm(sFunctor, t1,t2,t3);
        }

    /**
    * Construct a new TermNode with the given functor and given Terms.
    *
    * @param sFunctor  the functor for the Term
    * @param t1        a child term
    * @param t2        a child term
    * @param t3        a child term
    * @param t4        a child term
    *
    * @return a term tree
    */
    public static Term newTerm(String sFunctor,
            Term t1, Term t2, Term t3, Term t4)
        {
        return new NodeTerm(sFunctor, t1,t2,t3,t4);
        }

    /**
    * Construct a new TermNode with the given functor and given Terms.
    *
    * @param sFunctor  the functor for the Term
    * @param t1        a child term
    * @param t2        a child term
    * @param t3        a child term
    * @param t4        a child term
    * @param t5        a child term
    *
    * @return a term tree
    */
    public static Term newTerm(String sFunctor,
            Term t1, Term t2, Term t3, Term t4, Term t5)
        {
        return new NodeTerm(sFunctor, t1,t2,t3,t4,t5);
        }

    /**
    * Create a Tree of Terms using the Term Language in the given String
    *
    * @param s  String representing a Term tree
    *
    * @return a term tree
    */
    public static Term create(String s)
         {
         return create(s, f_language);
         }

    /**
    * Create a Tree of Terms using the Term Language in the given String
    *
    * @param s  String representing a Term tree
    *
    * @return a term tree
    */
    public static Term create(String s, CoherenceQueryLanguage language)
         {
         if (language == null)
             {
             language = f_language;
             }

         OPParser p = new OPParser(s, TermLanguage.tokenTable(), language.getOperators());
         p.getScanner().setStrictness(false);
         return p.parse();
         }

    /**
     * The default {@link CoherenceQueryLanguage} used by this QueryHelper when no language
     * is provided to methods.
     */
    protected static final CoherenceQueryLanguage f_language = new CoherenceQueryLanguage();
    }
