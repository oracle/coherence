/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.compiler.java;


import com.tangosol.dev.assembler.CodeAttribute;

import com.tangosol.dev.compiler.CompilerException;
import com.tangosol.dev.compiler.Context;

import com.tangosol.dev.component.DataType;

import com.tangosol.util.ErrorList;
import com.tangosol.util.NullImplementation;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.AbstractSet;
import java.util.NoSuchElementException;


/**
* This class implements a variable scope in a Java script.  There are several
* language elements that have a variable scope, including the StatementBlock,
* the ForStatement, and the CatchClause.
*
* @version 1.00, 09/14/98
* @author  Cameron Purdy
*/
public abstract class Block extends Statement
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a Java code block.
    *
    * @param outer  the enclosing Java statement
    * @param token  the token starting the block (which is typically a
    *               left curly brace except for the "for" construct)
    */
    protected Block(Statement outer, Token token)
        {
        super(outer, token);
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Add a statement to this block.
    *
    * @param stmt  the statement to add
    */
    public void addStatement(Statement stmt)
        {
        if (last == null)
            {
            setInnerStatement(stmt);
            }
        else
            {
            last.setNextStatement(stmt);
            }

        last = stmt;
        }

    /**
    * Register the declaration of a variable with this block.
    *
    * @param var  the variable to register
    *
    * @return  false if the name hides another variable
    */
    protected boolean registerVariable(Variable var)
            throws CompilerException
        {
        String  sName  = var.getName();
        boolean fHides = (getVariable(sName) != null);

        // add the variable; in the case of variables which hide other
        // variables, register them anyway in order to prevent multiple
        // errors from being logged if code references this variable name
        // assuming it is of the type declared here (this appears to be
        // how JAVAC works)
        mapVars.put(sName, var);

        return !fHides;
        }

    /**
    * Look up a variable by its name.
    *
    * @param sName  the name of the variable
    *
    * @return  the variable with the specified name or null if no such
    *          variable has been declared
    */
    protected Variable getVariable(String sName)
        {
        Variable var = (Variable) mapVars.get(sName);

        if (var == null)
            {
            Block block = getBlock();
            if (block != null)
                {
                var = getBlock().getVariable(sName);
                }
            }

        return var;
        }

    /**
    * Get an immutable set of variables declared (thus far) within this
    * block.
    */
    protected Set getVariables()
        {
        if (SET_VARS == null)
            {
            // get the outer block's set of variables
            Block block    = getBlock();
            Set   setOuter = (block == null ? NullImplementation.getSet() : block.getVariables());

            SET_VARS = new VariableSet(mapVars, setOuter);
            }

        return SET_VARS;
        }


    // ----- inner classes --------------------------------------------------

    /**
    * Implements an immutable singleton set which represents the variables
    * declared by this block.
    *
    * @version 1.00, 12/09/98
    * @author Cameron Purdy
    */
    private static class VariableSet extends AbstractSet
        {
        // ----- constructors ---------------------------------------------------

        /**
        * Construct the variable set.
        */
        public VariableSet(Map mapVars, Set setOuter)
            {
            this.mapVars  = mapVars;
            this.setOuter = setOuter;
            }


        // ----- Set interface --------------------------------------------------

        /**
        * Returns an Iterator over the elements contained in this Collection.
        *
        * @return an Iterator over the elements contained in this Collection
        */
        public Iterator iterator()
            {
            Iterator iter = setOuter.iterator();

            if (!mapVars.isEmpty())
                {
                iter = new VariableIterator(mapVars.values().iterator(), iter);
                }

            return iter;
            }

        /**
        * Returns the number of elements in this Collection.
        *
        * @return the number of elements in this Collection
        */
        public int size()
            {
            return mapVars.size() + setOuter.size();
            }

        /**
        * Returns true if this Collection contains the specified element.  More
        * formally, returns true if and only if this Collection contains at least
        * one element <code>e</code> such that <code>(o==null ? e==null :
        * o.equals(e))</code>.
        *
        * @param o  the object to search for in the set
        *
        * @return true if this set contains the specified object
        */
        public boolean contains(Object o)
            {
            try
                {
                return o == mapVars.get(((Variable) o).getName()) || setOuter.contains(o);
                }
            catch (ClassCastException e)
                {
                return false;
                }
            }

        /**
        * Ensures that this Collection contains the specified element.
        *
        * @param o element whose presence in this Collection is to be ensured
        *
        * @return true if the Collection changed as a result of the call
        */
        public boolean add(Object o)
            {
            throw new UnsupportedOperationException();
            }

        /**
        * Removes a single instance of the specified element from this Collection,
        * if it is present (optional operation).  More formally, removes an
        * element <code>e</code> such that <code>(o==null ? e==null :
        * o.equals(e))</code>, if the Collection contains one or more such
        * elements.  Returns true if the Collection contained the specified
        * element (or equivalently, if the Collection changed as a result of the
        * call).
        *
        * @param o element to be removed from this Collection, if present
        *
        * @return true if the Collection contained the specified element
        */
        public boolean remove(Object o)
            {
            throw new UnsupportedOperationException();
            }

        /**
        * Removes all of the elements from this Collection.
        */
        public void clear()
            {
            throw new UnsupportedOperationException();
            }


        // ----- inner classes --------------------------------------------------

        /**
        * Iterator for the variables in the set.
        */
        private static class VariableIterator implements Iterator
            {
            /**
            * Construct the iterator.
            */
            protected VariableIterator(Iterator iterCurrent, Iterator iterOuter)
                {
                this.iterVars  = iterCurrent;
                this.iterOuter = iterOuter;
                }

            /**
            * Returns true if the iteration has more elements.
            */
            public boolean hasNext()
                {
                boolean fNext = iterVars.hasNext();

                if (!fNext && iterOuter != null)
                    {
                    // all done iterating this block's variables, so now do
                    // the same with the outer block's variables
                    iterVars  = iterOuter;
                    iterOuter = null;

                    fNext = iterVars.hasNext();
                    }

                return fNext;
                }

            /**
            * Returns the next element in the interation.
            *
            * @exception NoSuchElementException iteration has no more elements.
            */
            public Object next()
                {
                try
                    {
                    return iterVars.next();
                    }
                catch (NoSuchElementException e)
                    {
                    // it is possible that this block's variables have all
                    // been iterated, but that the outer block's variables
                    // have not been
                    if (hasNext())
                        {
                        return next();
                        }
                    else
                        {
                        throw e;
                        }
                    }
                }

            /**
            * Removes from the underlying Collection the last element returned by the
            * Iterator .  This method can be called only once per call to next  The
            * behavior of an Iterator is unspecified if the underlying Collection is
            * modified while the iteration is in progress in any way other than by
            * calling this method.  Optional operation.
            *
            * @exception IllegalStateException next has not yet been called,
            *            or remove has already been called after the last call
            *            to next.
            */
            public void remove()
                {
                throw new UnsupportedOperationException();
                }

            /**
            * The variable iterator currently being iterated by this iterator.
            * Initially, this is the iterator for this block's variables, but
            * when exhausted, it becomes the iterator of the outer block's
            * variables.
            */
            private Iterator iterVars;

            /**
            * If the outer block's variables have not yet been iterated, then
            * this is the outer block's variable iterator, otherwise it is null.
            */
            private Iterator iterOuter;
            }


        // ----- data members ---------------------------------------------------

        /**
        * The map of vars for this block.
        */
        private Map mapVars;

        /**
        * The immutable set of the outer block.
        */
        private Set setOuter;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "Block";

    /**
    * A linked list of statements within this block.
    */
    private Statement last;

    /**
    * A set of in-scope variables
    */
    private final Map mapVars = new HashMap();

    /**
    * A set view of all in-scope variables (including outer blocks).
    */
    private Set SET_VARS;
    }
