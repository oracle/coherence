/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import com.oracle.coherence.common.collections.AbstractStableIterator;

import java.lang.reflect.Array;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
* AbstractKeySetBasedMap is an extension to the AbstractKeyBasedMap that
* has a full awareness of the set of keys upon which the Map is based. As
* a result, it is possible to optimize the implementation of a number of
* methods that benefit from a knowledge of the entire set of keys.
* <p>
* Read-only implementations must implement {@link #getInternalKeySet()} and
* {@link #get(Object)}. Read/write implementations must additionally
* implement {@link #put(Object, Object)} and {@link #remove(Object)}. If the
* implementation has any cost of returning an "old value", such as is done
* by the {@link java.util.Map#put} and {@link java.util.Map#remove(Object)},
* then the {@link #putAll(java.util.Map)} and {@link #removeBlind(Object)}
* methods should also be implemented. The only other obvious method for
* optimization is {@link #clear()}, if the implementation is able to do it
* in bulk.
*
* @author 2005.09.20  cp
*/
public abstract class AbstractKeySetBasedMap<K, V>
        extends AbstractKeyBasedMap<K, V>
    {
    // ----- Map interface --------------------------------------------------

    /**
    * {@inheritDoc}
    */
    public boolean containsKey(Object oKey)
        {
        return getInternalKeySet().contains(oKey);
        }

    /**
    * {@inheritDoc}
    */
    public boolean isEmpty()
        {
        return getInternalKeySet().isEmpty();
        }

    /**
    * {@inheritDoc}
    */
    public int size()
        {
        return getInternalKeySet().size();
        }


    // ----- AbstractKeyBasedMap methods ------------------------------------

    /**
    * Create an iterator over the keys in this Map. Note that this
    * implementation delegates back to the key Set, while the super class
    * delegates from the key Set to this method.
    *
    * @return a new instance of an Iterator over the keys in this Map
    */
    protected Iterator<K> iterateKeys()
        {
        return instantiateKeyIterator();
        }


    // ----- internal methods -----------------------------------------------

    /**
    * Obtain a set of keys that are represented by this Map.
    * <p>
    * The AbstractKeySetBasedMap only utilizes the internal key set as a
    * read-only resource.
    *
    * @return an internal Set of keys that are contained by this Map
    */
    protected abstract Set<K> getInternalKeySet();

    /**
    * Determine if this Iterator should remove an iterated item by calling
    * remove on the internal key Set Iterator, or by calling removeBlind on
    * the map itself.
    *
    * @return true to remove using the internal key Set Iterator or false to
    *         use the {@link AbstractKeyBasedMap#removeBlind(Object)} method
    */
    protected boolean isInternalKeySetIteratorMutable()
        {
        return false;
        }


    // ----- inner class: KeySet --------------------------------------------

    /**
    * {@inheritDoc}
    */
    protected Set<K> instantiateKeySet()
        {
        return new KeySet();
        }

    /**
    * A set of keys backed by this map.
    */
    protected class KeySet
            extends AbstractKeyBasedMap.KeySet
        {
        // ----- Set interface ------------------------------------------

        /**
        * {@inheritDoc}
        */
        public Object[] toArray()
            {
            return AbstractKeySetBasedMap.this.getInternalKeySet().toArray();
            }

        /**
        * {@inheritDoc}
        */
        public Object[] toArray(Object ao[])
            {
            return AbstractKeySetBasedMap.this.getInternalKeySet().toArray(ao);
            }
        }


    // ----- inner class: EntrySet ------------------------------------------

    /**
    * {@inheritDoc}
    */
    protected Set<Map.Entry<K, V>> instantiateEntrySet()
        {
        return new EntrySet();
        }

    /**
    * A set of entries backed by this map.
    */
    public class EntrySet
            extends AbstractKeyBasedMap.EntrySet
        {
        // ----- Set interface ------------------------------------------

        /**
        * {@inheritDoc}
        */
        public Object[] toArray()
            {
            AbstractKeySetBasedMap map = AbstractKeySetBasedMap.this;
            Object[] ao = map.keySet().toArray();
            for (int i = 0, c = ao.length; i < c; ++i)
                {
                K key = (K) ao[i];
                ao[i] = instantiateEntry(key, map.get(key)); // snapshot!
                }

            return ao;
            }

        /**
        * {@inheritDoc}
        */
        public Object[] toArray(Object ao[])
            {
            Object[] aoRaw = toArray();
            if (ao == null)
                {
                ao = aoRaw;
                }
            else
                {
                int cRaw  = aoRaw.length;
                int cDest = ao.length;
                if (cDest < cRaw)
                    {
                    ao = (Object[]) Array.newInstance(
                            ao.getClass().getComponentType(), cRaw);
                    }
                System.arraycopy(aoRaw, 0, ao, 0, cRaw);
                if (cDest > cRaw)
                    {
                    ao[cRaw] = null;
                    }
                }
            return ao;
            }
        }


    // ----- inner class: ValuesCollection ----------------------------------

    /**
    * {@inheritDoc}
    */
    protected Collection<V> instantiateValues()
        {
        return new ValuesCollection();
        }

    /**
    * A Collection of values backed by this map.
    */
    protected class ValuesCollection
            extends AbstractKeyBasedMap.ValuesCollection

        {
        // ----- Set interface ------------------------------------------

        /**
        * {@inheritDoc}
        */
        public Object[] toArray()
            {
            AbstractKeySetBasedMap map = AbstractKeySetBasedMap.this;
            Object[] ao = map.keySet().toArray();
            for (int i = 0, c = ao.length; i < c; ++i)
                {
                ao[i] = map.get(ao[i]);
                }

            return ao;
            }

        /**
        * {@inheritDoc}
        */
        public Object[] toArray(Object ao[])
            {
            Object[] aoRaw = toArray();
            if (ao == null)
                {
                ao = aoRaw;
                }
            else
                {
                int cRaw  = aoRaw.length;
                int cDest = ao.length;
                if (cDest < cRaw)
                    {
                    ao = (Object[]) Array.newInstance(
                            ao.getClass().getComponentType(), cRaw);
                    }
                System.arraycopy(aoRaw, 0, ao, 0, cRaw);
                if (cDest > cRaw)
                    {
                    ao[cRaw] = null;
                    }
                }
            return ao;
            }
        }


    // ----- inner class: KeyIterator ---------------------------------------

    /**
    * Factory pattern: Create a mutable Iterator over the keys in the Map
    *
    * @return a new instance of Iterator that iterates over the keys in the
    *         Map and supports element removal
    */
    protected Iterator<K> instantiateKeyIterator()
        {
        Iterator<K> iter = getInternalKeySet().iterator();
        if (!isInternalKeySetIteratorMutable())
            {
            iter = new KeyIterator(iter);
            }
        return iter;
        }

    /**
    * An iterator over the keys from the internal key Set that implements
    * element removal via the Map's removeBlind method.
    */
    public class KeyIterator
            extends AbstractStableIterator<K>
        {
        /**
        * Construct a KeyIterator.
        *
        * @param iter  the underlying Iterator from the internal key Set
        */
        protected KeyIterator(Iterator<K> iter)
            {
            m_iter = iter;
            }

        /**
        * {@inheritDoc}
        */
        protected void advance()
            {
            Iterator<K> iter = m_iter;
            if (iter.hasNext())
                {
                setNext(iter.next());
                }
            }

        /**
        * {@inheritDoc}
        */
        protected void remove(Object oPrev)
            {
            AbstractKeySetBasedMap.this.removeBlind(oPrev);
            }

        /**
        * The underlying iterator from the internal key Set.
        */
        private Iterator<K> m_iter;
        }
    }
