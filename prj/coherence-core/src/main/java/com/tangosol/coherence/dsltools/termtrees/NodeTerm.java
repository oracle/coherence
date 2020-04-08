/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dsltools.termtrees;


/**
* NodeTerm is the  class used to represent trees of Terms that can have
* children.
*
* @author djl  2009.08.31
*/
public class NodeTerm
        extends Term
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a new TermNode with the given functor.
    *
    * @param sFunctor  the functor for the Term
     */
    public NodeTerm(String sFunctor)
        {
        this(sFunctor, new Term[0]);
        }

    /**
    * Construct a new TermNode with the given functor and given Terms.
    *
    * @param sFunctor  the functor for the Term
    * @param t1        a child term
    */
    public NodeTerm(String sFunctor, Term t1)
        {
        this(sFunctor, new Term[] {t1});
        }

    /**
    * Construct a new TermNode with the given functor and given Terms.
    *
    * @param sFunctor  the functor for the Term
    * @param t1        a child term
    * @param t2        a child term
    */
    public NodeTerm(String sFunctor, Term t1, Term t2)
        {
        this(sFunctor, new Term[] {t1, t2});
        }

    /**
    * Construct a new TermNode with the given functor and given Terms.
    *
    * @param sFunctor  the functor for the Term
    * @param t1       a child term
    * @param t2       a child term
    * @param t3       a child term
    */
    public NodeTerm(String sFunctor, Term t1, Term t2, Term t3)
        {
        this(sFunctor, new Term[] {t1, t2, t3});
        }

    /**
    * Construct a new TermNode with the given functor and given Terms.
    *
    * @param sFunctor  the functor for the Term
    * @param t1        a child term
    * @param t2        a child term
    * @param t3        a child term
    * @param t4        a child term
    */
     public NodeTerm(String sFunctor, Term t1, Term t2, Term t3, Term t4)
        {
        this(sFunctor, new Term[] {t1, t2, t3, t4});
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
    */
    public NodeTerm(String sFunctor, Term t1, Term t2, Term t3, Term t4,
            Term t5)
        {
        this(sFunctor, new Term[] {t1, t2, t3, t4, t5});
        }

    /**
    * Construct a new TermNode with the given functor and given Terms.
    *
    * @param sFunctor  the functor for the Term
    * @param aTerms     children of the node
    */
    public NodeTerm(String sFunctor, Term[] aTerms)
       {
       m_sFunctor = sFunctor;
       m_aTerms   = aTerms;
       }

    /**
    * Construct a new TermNode with the given functor and given Terms.
    *
    * @param sFunctor  the functor for the Term
    * @param term     a term
    * @param aTerms    children of the node
    */
    public NodeTerm(String sFunctor, Term term, Term[] aTerms)
       {
       m_sFunctor = sFunctor;
       m_aTerms   = new Term[aTerms.length +1];
       m_aTerms[0] = term;
       for (int i =1, c = m_aTerms.length; i < c; i++)
           {
           m_aTerms[i] = aTerms[i-1];
           }
       }

    /**
    * Construct a new TermNode with the given functor and given Terms.
    *
    * @param sFunctor  the functor for the Term
    * @param aTerms    children of the node
    * @param term     a term
    */
    public NodeTerm(String sFunctor, Term[] aTerms, Term term)
       {
       int c                       = aTerms.length;
           m_sFunctor              = sFunctor;
           m_aTerms                = new Term[aTerms.length +1 ];
           m_aTerms[aTerms.length] = term;

       for (int i =0; i < c; i++)
           {
           m_aTerms[i] = aTerms[i];
           }
       }


    // ----- Term API -------------------------------------------------------

    /**
    * {@inheritDoc}
    */
    public String getFunctor()
        {
        return m_sFunctor;
        }

    /**
    * {@inheritDoc}
    */
    public boolean isLeaf()
        {
        return m_aTerms.length > 0;
        }

    /**
    * {@inheritDoc}
    */
    public boolean isAtom()
        {
        return false;
        }

    /**
    * {@inheritDoc}
    */
    public  Term[] children()
        {
        return m_aTerms;
        }

    /**
    * {@inheritDoc}
    */
    public int length()
        {
        return m_aTerms.length;
        }

    /**
    * {@inheritDoc}
    */
    public Term termAt(int index)
        {
        if (index == 0)
            {
            return AtomicTerm.createSymbol(getFunctor());
            }
        return m_aTerms[index-1];
        }

    /**
    * {@inheritDoc}
    */
    public Term withChild(Term t)
        {
        Term[] temp = new Term[m_aTerms.length + 1];
        for (int i = 0, c = m_aTerms.length; i< c; i++)
            {
            temp[i] = m_aTerms[i];
            }
        temp[m_aTerms.length] = t;
        m_aTerms = temp;
        return this;
        }

    /**
    * {@inheritDoc}
    */
    public boolean termEqual(Term t)
        {
        if (t == null)
            {
            return false;
            }
        if (t.isAtom())
            {
            return false;
            }
        NodeTerm nt = (NodeTerm)t;
        if (!m_sFunctor.equals(nt.m_sFunctor))
            {
            return false;
            }
        int count = m_aTerms.length;
        Term[] aMyTerms = m_aTerms;
        Term[] aOtherTerms = nt.m_aTerms;
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
    * {@inheritDoc}
    */
     public String fullFormString()
        {
        StringBuffer ans = new StringBuffer();
        ans.append(m_sFunctor);
        ans.append("(");
        for (int i = 0, c = m_aTerms.length; i < c; i++)
            {
            ans.append(m_aTerms[i].fullFormString());
            if (i != m_aTerms.length-1)
                {
                ans.append(", ");
                }
            }
        ans.append(")");
        return ans.toString();
        }


    // ----- TermWalker methods ---------------------------------------------

    public void accept(TermWalker walker)
        {
        walker.acceptNode(getFunctor(),this);
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Return a human-readable description for this Node.
    *
    * @return a String description of the Node
    */
     public String toString()
        {
        StringBuffer ans = new StringBuffer();

        if (m_sFunctor.equals(".attr."))
            {
            Term t = m_aTerms[0];
            ans.append(t.getFunctor());
            ans.append(":");
            ans.append(t.termAt(1).toString());
            return ans.toString();
              }
        if (m_sFunctor.equals(".pair."))
            {
            Term t  = m_aTerms[0];
            Term t2 = m_aTerms[1];
            ans.append(t.toString());
            ans.append(":");
            ans.append(t2.toString());
            return ans.toString();
            }
        if (!m_sFunctor.equals(".bag") && length() == 1
                && m_aTerms[0].getFunctor().equals(".bag."))
            {
            ans.append(m_sFunctor);
            ans.append(m_aTerms[0].toString());
            return ans.toString();
            }
        if (m_sFunctor.equals(".sequence."))
            {
            for (int i = 0; i < m_aTerms.length; i++)
                {
                ans.append(m_aTerms[i].toString());
                if (i != m_aTerms.length-1)
                    {
                    ans.append("; ");
                     }
                }
            return ans.toString();
            }

        if (m_sFunctor.equals(".list."))
            {
            ans.append("[");
            }
        else if (m_sFunctor.equals(".bag."))
            {
            ans.append("{");
            }
        else
            {
            ans.append(m_sFunctor);
            ans.append("(");
            }
        for (int i = 0; i < m_aTerms.length; i++)
            {
            ans.append(m_aTerms[i].toString());
            if (i != m_aTerms.length-1)
                {
                ans.append(", ");
                }
            }
        if (m_sFunctor.equals(".list."))
            {
            ans.append("]");
            }
        else if (m_sFunctor.equals(".bag."))
            {
            ans.append("}");
            }
        else
            {
            ans.append(")");
            }
        return ans.toString();
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Set the receiver's functor to be the given String
    *
    * @param sFunctor  the new functor for this Term
    *
    */
    public void setFunctor(String sFunctor)
        {
        m_sFunctor = sFunctor;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The functor for this node.
    */
    String m_sFunctor;

    /**
    * The children Terms of this node.
    */
    Term[] m_aTerms;

    }
