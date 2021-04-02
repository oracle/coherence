/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;

import java.util.NoSuchElementException;


/**
* A data structure resembling a long array indexed by long values.
* <p>
* While the PrimitiveSparseArray implements the LongArray interface, it stores
* all values as primitive longs, and as such any Object supplied to its
* LongArray interface must be provided as a java.lang.Long. The
* PrimitiveSparseArray provides additional methods whose signatures utilize
* primitive longs to avoid the creation of temporary Long objects.
*
* @see SparseArray
* @author mf 10.08.2007
*/
public class PrimitiveSparseArray
        extends AbstractSparseArray<Long>
        implements LongArray<Long>
    {
    // ----- constructor ----------------------------------------------------

    /**
    * Default constructor.
    */
    public PrimitiveSparseArray()
        {
        }

    /**
    * Copy constructor.
    *
    * @param array a PrimitiveSparseArray to copy from.
    */
    public PrimitiveSparseArray(PrimitiveSparseArray array)
        {
        for (Iterator iter = (Iterator) array.iterator(); iter.hasNext();)
            {
            long lValue = iter.nextPrimitive();
            long lIndex = iter.getIndex();

            setPrimitive(lIndex, lValue);
            }
        }

    // ----- LongArray API --------------------------------------------------

    /**
    * {@inheritDoc}
    */
    public Iterator iterator()
        {
        return (Iterator) super.iterator();
        }

    /**
    * {@inheritDoc}
    */
    public Iterator iterator(long lIndex)
        {
        return (Iterator) super.iterator(lIndex);
        }

    /**
    * {@inheritDoc}
    */
    public Iterator reverseIterator()
        {
        return (Iterator) super.reverseIterator();
        }

    /**
    * {@inheritDoc}
    */
    public Iterator reverseIterator(long lIndex)
        {
        return (Iterator) super.reverseIterator(lIndex);
        }

    // ----- PrimitiveSparseArray API ---------------------------------------

    /**
    * Return the value stored at the specified index.
    *
    * @param lIndex  a long index value
    *
    * @return the value stored at the specified index, or NOT_FOUND if
    * none exists
    */
    public long getPrimitive(long lIndex)
        {
        PrimitiveNode node = (PrimitiveNode) find(lIndex);
        return node == null ? NOT_FOUND : node.getPrimitiveValue();
        }

    /**
    * Remove the specified index from the PrimitiveSparseArray, returning its
    * associated value.
    *
    * @param lIndex  the index into the LongArray
    *
    * @return the associated value or NOT_FOUND if the specified index
    *         does not exist
    */
    public long removePrimitive(long lIndex)
        {
        PrimitiveNode node = (PrimitiveNode) find(lIndex);
        if (node == null)
            {
            return NOT_FOUND;
            }
        remove(node);
        return node.getPrimitiveValue();
        }

    /**
    * Add the passed item to the PrimitiveSparseArray at the specified index.
    * <p>
    * If the index is already used, the passed value will replace the current
    * value stored with the key, and the replaced value will be returned.
    * <p>
    * It is expected that LongArray implementations will "grow" as necessary
    * to support the specified index.
    *
    * @param lIndex  a long index value
    * @param lValue  the long value to store at the specified index
    *
    * @return the value that was stored at the specified index, or
    *         NOT_FOUND if the specified index does not exist
    */
    public long setPrimitive(long lIndex, long lValue)
        {
        PrimitiveNode node = (PrimitiveNode) findInsertionPoint(lIndex);
        if (node != null && node.getKey() == lIndex)
            {
            return node.setPrimitiveValue(lValue); // update
            }
        balancedInsertion(node, instantiateNode(lIndex, lValue));
        return NOT_FOUND;
        }


    // ----- inner class: PrimitiveNode -------------------------------------

    /**
    * {@inheritDoc}
    */
    protected Node instantiateNode(long lKey, Long value)
        {
        return instantiateNode(lKey, value.longValue());
        }

    /**
    * Create a new Node with the specified key and value.
    *
    * @param lKey    the long key
    * @param lValue  the long value
    *
    * @return the new node
    */
    protected Node instantiateNode(long lKey, long lValue)
        {
        return new PrimitiveNode(lKey, lValue);
        }

    /**
    * Node mapping long key to Object value.
    */
    protected static class PrimitiveNode
            extends Node<Long>
        {
        /**
        * Construct a new Node mapping a long key to a long value.
        *
        * @param lKey    the key
        * @param lValue  the long value
        */
        public PrimitiveNode(long lKey, long lValue)
            {
            key      = lKey;
            m_lValue = lValue;
            }

        /**
         * Return the key
         *
         * @return the key
         */
        public long getKey()
            {
            return key;
            }

        /**
        * {@inheritDoc}
        */
        public Long setValue(Long value)
            {
            return Long.valueOf(setPrimitiveValue(value.longValue()));
            }

        /**
        * {@inheritDoc}
        */
        public Long getValue()
            {
            return Long.valueOf(getPrimitiveValue());
            }

        /**
        * Set the long value for the Node.
        *
        * @param lValue the long value
        *
        * @return the prior long value for the node
        */
        public long setPrimitiveValue(long lValue)
            {
            long lOldValue = m_lValue;
            m_lValue = lValue;
            return lOldValue;
            }

        /**
        * Get the long value for the Node.
        *
        * @return the long value for the node
        */
        public long getPrimitiveValue()
            {
            return m_lValue;
            }

        // ----- data members -------------------------------------------

        /**
        * The Node's value.
        */
        protected long m_lValue;
        }


    // ----- inner class: PrimitiveCrawler ----------------------------------

    /**
    * {@inheritDoc}
    */
    protected Crawler instantiateCrawler(Node head, int fromdir, boolean fForward)
        {
        return new Iterator(head, fromdir, fForward);
        }

    /**
    * Iterator over long values stored in the tree.
    */
    public class Iterator
            extends Crawler
            implements LongArray.Iterator<Long>
        {
        // ----- constructor --------------------------------------------

        /**
        * Instantiate a new PrimitiveIterator at the specified location and
        * direction.
        *
        * @param head     the node at which to start the crawl
        * @param fromdir  the direction in which to start the crawl
        */
        protected Iterator(Node head, int fromdir, boolean fForward)
            {
            super(head, fromdir, fForward);
            }


        // ----- PrimitiveCrawler API -----------------------------------

        /**
        * Returns the next element (as a long) in the iteration.
        *
        * @return the next element (as a long) in the iteration
        *
        * @exception NoSuchElementException iteration has no more elements
        */
        public long nextPrimitive()
            {
            return ((PrimitiveNode) nextNode()).getPrimitiveValue();
            }

        /**
        * Returns the current value, which is the same value returned by the
        * most recent call to the <tt>nextPrimitive</tt> method, or the most
        * recent value passed to <tt>setPrimitiveValue</tt> if
        * <tt>setPrimitiveValue</tt> were called after the
        * <tt>nextPrimitive</tt> method.
        *
        * @return  the current value
        *
        * @exception IllegalStateException if the <tt>next</tt> method has
        *            not yet been called, or the <tt>remove</tt> method has
        *            already been called after the last call to the
        *            <tt>next</tt> method.
        */
        public long getPrimitiveValue()
            {
            return ((PrimitiveNode) currentNode()).getPrimitiveValue();
            }

        /**
        * Stores a new value at the current value index, returning the value
        * that was replaced. The index of the current value is obtainable by
        * calling the <tt>getIndex</tt> method.
        *
        * @return  the replaced value
        *
        * @exception IllegalStateException if the <tt>next</tt> method has
        *            not yet been called, or the <tt>remove</tt> method has
        *            already been called after the last call to the
        *            <tt>next</tt> method.
        */
        public long setPrimitiveValue(long lValue)
            {
            return ((PrimitiveNode) currentNode()).setPrimitiveValue(lValue);
            }
        }
    }
