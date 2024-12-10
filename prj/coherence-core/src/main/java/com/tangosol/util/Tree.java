/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import java.io.Serializable;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.TreeMap;


/**
* A thread-safe balanced binary search tree.
*
* <p>
* As of Coherence 3.3 this class is a wrapper around {@link TreeMap}.
* <p>
* Note:  Where practical, use <code>java.util.TreeMap</code>; including
* external syncronization as necessary.
*
* @author cp 1997.09.05; mf 2007.10.16
*/
public class Tree
        extends Base
        implements Cloneable, Serializable
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public Tree()
        {
        m_tree = new TreeMap();
        }


    // ----- Tree interface -------------------------------------------------

    /**
    * Add the passed key to the tree.
    *
    * @param key the key to add to the tree
    */
    public void add(Comparable key)
        {
        put(key, null);
        }

    /**
    * Add the passed key to the tree and associate the passed value with the
    * key.  If the key is already in the tree, the passed value will replace
    * the current value stored with the key.
    *
    * @param key   the key to add to the tree
    * @param value the object to associate with the key
    */
    public synchronized void put(Comparable key, Object value)
        {
        m_tree.put(key, value);
        }

    /**
    * Find the specified key and return its value.
    *
    * @param key the key to look for in the tree
    *
    * @return the associated value or null if the key is not in the tree
    */
    public synchronized Object get(Comparable key)
        {
        return m_tree.get(key);
        }

    /**
    * Determine if the passed key is in the tree.
    *
    * @param key the key to look for in the tree
    *
    * @return true if the key is in the tree, false otherwise
    */
    public synchronized boolean contains(Comparable key)
        {
        return m_tree.containsKey(key);
        }

    /**
    * Remove the specified key from the tree, returning its associated value.
    *
    * @param key the key to look for in the tree
    *
    * @return the associated value (which can be null) or null if the key is
    *         not in the tree
    */
    public synchronized Object remove(Comparable key)
        {
        return m_tree.remove(key);
        }

    /**
    * Remove all nodes from the tree.
    */
    public synchronized void clear()
        {
        m_tree.clear();
        }

    /**
    * Determine the size of the tree.
    *
    * @return the number of nodes in the tree
    */
    public int getSize()
        {
        return m_tree.size();
        }

    /**
    * Test for empty tree.
    *
    * @return true if tree has no nodes
    */
    public boolean isEmpty()
        {
        return m_tree.isEmpty();
        }

    /**
    * Create an in-order enumerator for the tree's keys.
    *
    * @return an enumerator of the tree's keys
    */
    public synchronized Enumeration keys()
        {
        if (m_tree.isEmpty())
            {
            return NullImplementation.getEnumeration();
            }

        Object[] aoKey     = new Comparable[m_tree.size()];
        int      cElements = 0;

        for (Iterator iter = m_tree.keySet().iterator(); iter.hasNext(); )
            {
            aoKey[cElements++] = iter.next();
            }

        return new SimpleEnumerator(aoKey);
        }

    /**
    * Create an enumerator for the tree's values.
    *
    * @return an enumerator of the tree's values (in the same order that the
    *         keys were returned)
    */
    public synchronized Enumeration elements()
        {
        if (m_tree.isEmpty())
            {
            return NullImplementation.getEnumeration();
            }

        Object[] aoValue   = new Object[m_tree.size()];
        int      cElements = 0;

        for (Iterator iter = m_tree.values().iterator(); iter.hasNext(); )
            {
            aoValue[cElements++] = iter.next();
            }

        return new SimpleEnumerator(aoValue);
        }

    /**
    * Adds all of the nodes in the specified Tree to this Tree if they are not
    * already present.  This operation effectively modifies this Tree
    * so that its value is the <em>union</em> of the two Trees.  The behavior
    * of this operation is unspecified if the specified Tree is
    * modified while the operation is in progress.
    *
    * @return true if this Tree changed as a result of the call.
    * @see java.util.Collection#addAll(Collection)
    */
    public boolean addAll(Tree that)
        {
        TreeMap mapDelta = (TreeMap) that.m_tree.clone();
        TreeMap mapThis  = m_tree;
        mapDelta.keySet().removeAll(mapThis.keySet());
        if (mapDelta.isEmpty())
            {
            return false;
            }
        mapThis.putAll(mapDelta);
        return true;
        }

    /**
    * Puts all of the nodes in the specified Tree to this Tree (including the ones
    * that are already present).  This operation effectively modifies this Tree
    * so that its value is the <em>union</em> of the two Trees.  The behavior
    * of this operation is unspecified if the specified Tree is
    * modified while the operation is in progress.
    */
    public void putAll(Tree that)
        {
        m_tree.putAll(that.m_tree);
        }

    /**
     * Retains only the nodes in this Tree that are contained in the specified
     * Tree.  In other words, removes from this Tree all of its nodes that are not
     * contained in the specified Tree.  This operation effectively modifies this
     * Tree so that its value is the <em>intersection</em> of the two Trees.
     *
     * @return true if this Collection changed as a result of the call.
     * @see java.util.Collection#retainAll(Collection)
     */
    public boolean retainAll(Tree that)
        {
        return m_tree.keySet().retainAll(that.m_tree.keySet());
        }

    /**
     * Removes from this Tree all of its nodes that are contained in the
     * specified Tree.  This operation effectively modifies this Tree so that
     * its value is the <em>asymmetric set difference</em> of the two Trees.
     *
     * @return true if this Tree changed as a result of the call.
     * @see java.util.Collection#removeAll(Collection)
     */
    public boolean removeAll(Tree that)
        {
        return m_tree.keySet().removeAll(that.m_tree.keySet());
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Provide a string representation of the tree.
    */
    public synchronized String toString()
        {
        return "Tree" + m_tree.toString();
        }

    /**
    * Test for tree equality.
    */
    public synchronized boolean equals(Object obj)
        {
        if (obj instanceof Tree)
            {
            Tree that = (Tree) obj;
            synchronized (that)
                {
                return m_tree.equals(that.m_tree);
                }
            }
        return false;
        }


    // ----- cloneable interface --------------------------------------------

    /**
    * Make a shallow copy of the tree and its nodes.  Note that cloning does
    * make new copies of each node.
    */
    public synchronized Object clone()
        {
        Tree that = new Tree();
        that.m_tree = (TreeMap) m_tree.clone();
        return that;
        }


    // ----- debugging methods ----------------------------------------------

    /**
    * In-order printing of the contents of the tree.
    */
    public synchronized void print()
        {
        System.out.println(toString());
        }


    // ----- internal methods -----------------------------------------------

    /**
    * Get an enumerator of the nodes in the tree.  This enumerator is not in
    * any way thread-safe, so the tree should be synchronized for as long as
    * this enumerator is in use.
    *
    * Note:  Purposefully package private; used by StringTable
    */
    protected Enumeration getUnsynchronizedKeyEnumerator()
        {
        return new IteratorEnumerator(m_tree.keySet().iterator());
        }

    /**
    * Get an enumerator of the nodes in the tree.  This enumerator is not in
    * any way thread-safe, so the tree should be synchronized for as long as
    * this enumerator is in use.
    *
    * Note:  Purposefully package private; used by StringTable
    */
    protected Enumeration getUnsynchronizedKeyEnumerator(Comparable key)
        {
        return new IteratorEnumerator(m_tree.tailMap(key).keySet().iterator());
        }


    // ----- data members ---------------------------------------------------

    /**
    * The delgate tree.
    */
    protected TreeMap m_tree;
    }
