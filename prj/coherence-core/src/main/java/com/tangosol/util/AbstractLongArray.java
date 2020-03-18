/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import com.oracle.coherence.common.base.NaturalHasher;


/**
* An implementation of portions of the LongArray interface.
*
* @author cp
* @version 1.00 2002.04.24
*/
public abstract class AbstractLongArray<V>
        implements LongArray<V>
    {
    /**
    * Return the value stored at the specified index.
    *
    * @param lIndex  a long index value
    *
    * @return the object stored at the specified index, or null
    */
    public V get(long lIndex)
        {
        Iterator<V> iter = iterator(lIndex);
        if (iter.hasNext())
            {
            V value = iter.next();
            return iter.getIndex() == lIndex ? value : null;
            }
        else
            {
            return null;
            }
        }

    /**
    * Add the passed element value to the LongArray and return the index at
    * which the element value was stored.
    *
    * @param oValue  the object to add to the LongArray
    *
    * @return  the long index value at which the element value was stored
    */
    public long add(V oValue)
        {
        long lIndex = getLastIndex() + 1L;
        set(lIndex, oValue);
        return lIndex;
        }

    /**
    * Determine if the specified index is in use.
    *
    * @param lIndex  a long index value
    *
    * @return true if a value (including null) is stored at the specified
    *         index, otherwise false
    */
    public boolean exists(long lIndex)
        {
        Iterator iter = iterator(lIndex);
        if (iter.hasNext())
            {
            iter.next();
            return iter.getIndex() == lIndex;
            }
        else
            {
            return false;
            }
        }

    /**
    * Remove the specified key from the LongArray, returning its associated
    * value.
    *
    * @param lIndex the index of the key to look for in the LongArray
    *
    * @return the associated value (which can be null) or null if the key is
    *         not in the LongArray
    */
    public V remove(long lIndex)
        {
        Iterator<V> iter = iterator(lIndex);
        if (iter.hasNext())
            {
            V value = iter.next();
            if (iter.getIndex() == lIndex)
                {
                iter.remove();
                return value;
                }
            }

        return null;
        }

    /**
     * Remove all nodes in the specified range.
     *
     * @param lIndexFrom  the floor index
     * @param lIndexTo    the ceiling index (exclusive)
     */
    public void remove(long lIndexFrom, long lIndexTo)
        {
        for (Iterator iter = iterator(lIndexFrom); iter.hasNext(); )
            {
            iter.next();
            if (iter.getIndex() < lIndexTo)
                {
                iter.remove();
                }
            else
                {
                break;
                }
            }
        }

    /**
    * Determine if the LongArray contains the specified element.
    * <p>
    * More formally, returns <tt>true</tt> if and only if this LongArray
    * contains at least one element <tt>e</tt> such that
    * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>.
    *
    * @param value element whose presence in this list is to be tested
    *
    * @return <tt>true</tt> if this list contains the specified element
    */
    public boolean contains(V value)
        {
        Iterator iter = iterator();
        while (iter.hasNext())
            {
            if (NaturalHasher.INSTANCE.equals(value, iter.next()))
                {
                return true;
                }
            }
        return false;
        }

    /**
    * Remove all nodes from the LongArray.
    */
    public void clear()
        {
        Iterator iter = iterator();
        while (iter.hasNext())
            {
            iter.next();
            iter.remove();
            }
        }

    /**
    * Test for empty LongArray.
    *
    * @return true if LongArray has no nodes
    */
    public boolean isEmpty()
        {
        return getSize() == 0;
        }

    /**
    * Determine the size of the LongArray.
    *
    * @return the number of nodes in the LongArray
    */
    public int getSize()
        {
        int cItems = 0;
        Iterator iter = iterator();
        while (iter.hasNext())
            {
            iter.next();
            ++cItems;
            }
        return cItems;
        }

    /**
    * Determine the first index that exists in the LongArray.
    *
    * @return the lowest long value that exists in this LongArray,
    *         or NOT_FOUND if the LongArray is empty
    */
    public long getFirstIndex()
        {
        Iterator iter = iterator();
        if (iter.hasNext())
            {
            iter.next();
            return iter.getIndex();
            }
        else
            {
            return NOT_FOUND;
            }
        }

    /**
    * Determine the last index that exists in the LongArray.
    *
    * @return the highest long value that exists in this LongArray,
    *         or NOT_FOUND if the LongArray is empty
    */
    public long getLastIndex()
        {
        Iterator iter = reverseIterator();
        if (iter.hasNext())
            {
            iter.next();
            return iter.getIndex();
            }
        else
            {
            return NOT_FOUND;
            }
        }

    /**
    * Return the index in this LongArray of the first occurrence of
    * the specified element, or NOT_FOUND if this LongArray does not
    * contain the specified element.
    */
    public long indexOf(V oValue)
        {
        return indexOf(oValue, getFirstIndex());
        }

    /**
    * Return the index in this LongArray of the first occurrence of
    * the specified element such that <tt>(index is greater or equal to lIndex)</tt>, or
    * NOT_FOUND if this LongArray does not contain the specified
    * element.
    */
    public long indexOf(V oValue, long lIndex)
        {
        Iterator iter = iterator(lIndex);
        while(iter.hasNext())
            {
            if (NaturalHasher.INSTANCE.equals(oValue, iter.next()))
                {
                return iter.getIndex();
                }
            }
        return NOT_FOUND;
        }

    /**
    * Return the index in this LongArray of the last occurrence of the
    * specified element, or NOT_FOUND if this LongArray does not
    * contain the specified element.
    */
    public long lastIndexOf(V oValue)
        {
        return lastIndexOf(oValue, getLastIndex());
        }

    /**
    * Return the index in this LongArray of the last occurrence of the
    * specified element such that <tt>(index less than or equal to lIndex)</tt>, or
    * NOT_FOUND if this LongArray does not contain the specified
    * element.
    */
    public long lastIndexOf(V oValue, long lIndex)
        {
        Iterator iter = reverseIterator(lIndex);
        while(iter.hasNext())
            {
            if (NaturalHasher.INSTANCE.equals(oValue, iter.next()))
                {
                return iter.getIndex();
                }
            }
        return NOT_FOUND;
        }

    // ----- Object methods -------------------------------------------------

    /**
    * Provide a string representation of the LongArray.
    *
    * @return a human-readable String value describing the LongArray instance
    */
    public String toString()
        {
        String sClass = getClass().getName();
        sClass = sClass.substring(sClass.lastIndexOf('.') + 1);

        StringBuffer sb = new StringBuffer();
        sb.append(sClass)
          .append('{');

        boolean fFirst = true;
        Iterator iter = iterator();
        while (iter.hasNext())
            {
            if (fFirst)
                {
                fFirst = false;
                }
            else
                {
                sb.append(", ");
                }

            Object o = iter.next();
            sb.append('[')
              .append(iter.getIndex())
              .append("]=")
              .append(o);
            }

        sb.append('}');

        return sb.toString();
        }

    /**
    * Test for LongArray equality.
    *
    * @param o  an Object to compare to this LongArray for equality
    *
    * @return true if the passed Object is a LongArray containing the same
    *         indexes and whose elements at those indexes are equal
    */
    public boolean equals(Object o)
        {
        if (o instanceof LongArray)
            {
            LongArray that = (LongArray) o;
            if (this.getSize() == that.getSize())
                {
                // short-cut:  both are empty?
                if (this.isEmpty())
                    {
                    return true;
                    }

                // perform an in-order traversal, comparing each element
                Iterator iterThis = this.iterator();
                Iterator iterThat = that.iterator();

                while (iterThis.hasNext() && iterThat.hasNext())
                    {
                    if (!NaturalHasher.INSTANCE.equals(iterThis.next(), iterThat.next())
                        || iterThis.getIndex() != iterThat.getIndex())
                        {
                        return false;
                        }
                    }

                return iterThis.hasNext() == iterThat.hasNext();
                }
            }

        return false;
        }

    /**
    * {@inheritDoc}
    */
    public int hashCode()
        {
        int nHash = 0;
        for (Iterator iter = this.iterator(); iter.hasNext(); )
            {
            nHash += NaturalHasher.INSTANCE.hashCode(iter.next());
            }
        return nHash;
        }


    // ----- cloneable interface --------------------------------------------

    /**
    * Make a clone of the LongArray. The element values are not deep-cloned.
    *
    * @return a clone of this LongArray object
    */
    public AbstractLongArray<V> clone()
        {
        try
            {
            return (AbstractLongArray<V>) super.clone();
            }
        catch (CloneNotSupportedException e)
            {
            throw new RuntimeException(e);
            }
        }
    }