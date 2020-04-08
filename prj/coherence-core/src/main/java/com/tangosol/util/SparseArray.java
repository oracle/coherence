/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

/**
* A data structure resembling an Object array indexed by long values.
*
* This data structure is not thread safe and if there exists the possibility that some thread may perform
* a mutation while other thread(s) are reading/iterating then some form of locking is required to protect
* the SparseArray.  Note that concurrent readers are allowed, thus protecting the SparseArray with a
* read-write would be appropriate.  See {@link SafeLongArray}, {@link ReadHeavyLongArray}, and
* {@link CopyOnWriteLongArray} for thread-safe LongArray implementations.
*
* @author cp 2007.10.08
*/
public class SparseArray<V>
        extends AbstractSparseArray<V>
    {
    // ----- inner class: ObjectNode ----------------------------------------

    /**
    * {@inheritDoc}
    */
    protected Node<V> instantiateNode(long lKey, V oValue)
        {
        return new ObjectNode<>(lKey, oValue);
        }

    @Override
    public SparseArray<V> clone()
        {
        return (SparseArray<V>) super.clone();
        }

    /**
    * Node mapping long key to Object value.
    */
    protected static class ObjectNode<V>
            extends Node<V>
        {
        public ObjectNode(long lKey, V oValue)
            {
            key      = lKey;
            m_oValue = oValue;
            }

        /**
        * {@inheritDoc}
        */
        public V setValue(V oValue)
            {
            V oldValue = m_oValue;
            m_oValue = oValue;
            return oldValue;
            }

        /**
        * {@inheritDoc}
        */
        public V getValue()
            {
            return m_oValue;
            }

        // ----- data members -----------------------------------------------

        /**
        * The Node's value.
        */
        protected V m_oValue;
        }
    }