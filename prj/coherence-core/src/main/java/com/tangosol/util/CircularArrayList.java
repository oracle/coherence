/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import java.util.AbstractList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.RandomAccess;


/**
* Resizable-array implementation of the <tt>List</tt> interface.  Implements
* all optional list operations, and permits all elements, including
* <tt>null</tt>. (This class is roughly equivalent to
* <tt>ArrayList</tt>, except that it is optimized for removing elements at
* the front and back of the list to facilitate use as a queue or deque.<p>
*
* The <tt>size</tt>, <tt>isEmpty</tt>, <tt>get</tt>, <tt>set</tt>,
* <tt>iterator</tt>, and <tt>listIterator</tt> operations run in constant
* time.  The <tt>add</tt> operation runs in <i>amortized constant time</i>,
* that is, adding n elements requires O(n) time.  All of the other
* operations run in linear time (roughly speaking).  The constant factor is
* low compared to that for the <tt>LinkedList</tt> implementation.<p>
*
* Each <tt>CircularArrayList</tt> instance has a <i>capacity</i>.
* The capacity is the size of the array used to store the elements in the
* list.  It is always at least as large as the list size.  As elements are
* added to an CircularArrayList, its capacity grows automatically.  The
* details of the growth policy are not specified beyond the fact that adding
* an element has constant amortized time cost.<p>
*
* An application can increase the capacity of an <tt>CircularArrayList</tt>
* instance before adding a large number of elements using the
* <tt>ensureCapacity</tt> operation.  This may reduce the amount of
* incremental reallocation.<p>
*
* <strong>Note that this implementation is not synchronized.</strong> If
* multiple threads access a <tt>CircularArrayList</tt> concurrently, and at
* least one of the threads modifies the list structurally, it <i>must</i> be
* synchronized externally.  (A structural modification is any operation that
* adds or deletes one or more elements, or explicitly resizes the backing
* array; merely setting the value of an element is not a structural
* modification.)  This is typically accomplished by synchronizing on some
* object that naturally encapsulates the list.  If no object exists, the
* list should be "wrapped" using the <tt>Collections.synchronizedList</tt>
* method.  This is best done at creation time, to prevent accidental
* unsynchronized access to the list:
* <pre>
*   List list = Collections.synchronizedList(new CircularArrayList(...));
* </pre><p>
*
* The iterators returned by this class's <tt>iterator</tt> and
* <tt>listIterator</tt> methods are fail-fast: if list is structurally
* modified at any time after the iterator is created, in any way except
* through the iterator's own remove or add methods, the iterator will throw
* a ConcurrentModificationException.  Thus, in the face of concurrent
* modification, the iterator fails quickly and cleanly, rather than risking
* arbitrary, non-deterministic behavior at an undetermined time in the
* future.<p>
*
* Note that the fail-fast behavior of an iterator cannot be guaranteed as
* it is, generally speaking, impossible to make any hard guarantees in the
* presence of unsynchronized concurrent modification.  Fail-fast iterators
* throw <tt>ConcurrentModificationException</tt> on a best-effort basis.
* Therefore, it would be wrong to write a program that depended on this
* exception for its correctness: <i>the fail-fast behavior of iterators
* should be used only to detect bugs.</i><p>
*
* @author djl  2008.10.22
*
* @since Coherence 3.5
*/
public class CircularArrayList
        extends AbstractList
        implements List, RandomAccess, Cloneable, Serializable
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Create a new CircularArrayList with default settings.
    */
    public CircularArrayList()
        {
        this(16);
        }

    /**
    * Create a new CircularArrayList with the specified initial capacity.
    *
    * @param cInitialElements  the initial capacity of the list
    *
    * @throws IllegalArgumentException if the specified initial capacity
    *            is negative
    */
    public CircularArrayList(int cInitialElements)
        {
        if (cInitialElements < 0)
            {
            throw new IllegalArgumentException("Illegal Capacity: "+
                                               cInitialElements);
            }

        m_aoData = new Object[cInitialElements];
        }

    /**
    * Construct a CircularArrayList containing the elements of the specified
    * collection in the order they are returned by the collection's iterator.
    *
    * @param c  the collection whose elements are to be placed into this list
    */
    public CircularArrayList(Collection c)
        {
        m_aoData = new Object[c.size() + 1];
        addAll(c);
        }


    // ----- CircularArrayList interface ------------------------------------

    /**
    * Trim the capacity of this list instance to be as small as possible.
    */
    public void trimToSize()
        {
        modCount++;
        int cElements = m_cElements;
        if (cElements + 1 < m_aoData.length)
            {
            Object[] aoNewData = new Object[cElements + 1];
            toArray(aoNewData);
            m_aoData = aoNewData;
            m_iFirst = 0;
            m_iLast = cElements;
            }
        }

    /**
    * Increase the capacity of this list instance, if necessary, to ensure
    * that it can hold at least the specified number of elements.
    *
    * @param cMin  the minimum allowable capacity
    *
    * @return true if the capacity was increased
    */
    public boolean ensureCapacity(int cMin)
        {
        int cOld = m_aoData.length;
        if (cMin > cOld)
            {
            int cNew = Math.max(cMin, (cOld * 3) / 2 + 1);

            m_aoData = toArray(new Object[cNew]);
            m_iLast  = m_cElements;
            m_iFirst = 0;

            ++modCount;
            return true;
            }

        return false;
        }


    // ----- List interface -------------------------------------------------

    /**
    * {@inheritDoc}
    */
    public int size()
        {
        return m_cElements;
        }

    /**
    * {@inheritDoc}
    */
    public boolean isEmpty()
        {
        return m_cElements == 0;
        }

    /**
    * {@inheritDoc}
    */
    public boolean contains(Object o)
        {
        return indexOf(o) >= 0;
        }

    /**
    * Search for the first occurence of the given argument, testing
    * for equality using the equals method.
    *
    * @param o  the element to search for
    *
    * @return the index of the first occurrence of the argument in this
    *         list; returns -1 if the object is not found
    */
    public int indexOf(Object o)
        {
        Object[] aoData = m_aoData;
        int      cSlots = aoData.length;
        int      iFirst = m_iFirst;

        for (int i = 0, c = m_cElements; i < c; ++i)
            {
            if (Base.equals(o, aoData[(iFirst + i) % cSlots]))
                {
                return i;
                }
            }

        return -1;
        }

    /**
    * Returns the index of the last occurrence of the specified object in
    * this CycicArrayList.
    *
    * @param o  the element to search for
    *
    * @return the index of the last occurrence of the specified object in
    *         this list; returns -1 if the object is not found
    */
    public int lastIndexOf(Object o)
        {
        int      cElements = m_cElements;
        int      iFirst    = m_iFirst;
        Object[] aoData    = m_aoData;
        int      cSlots    = aoData.length;
        for (int i = cElements-1; i >= 0; i--)
            {
            if (Base.equals(o, aoData[(i+iFirst) % cSlots]))
                {
                return i;
                }
            }
        return -1;
        }

    /**
    * {@inheritDoc}
    */
    public Object[] toArray()
        {
        return toArray(new Object[m_cElements]);
        }

    /**
    * {@inheritDoc}
    */
    public Object[] toArray(Object ao[])
        {
        Object[] aoData    = m_aoData;
        int      cSlots    = aoData.length;
        int      iFirst    = m_iFirst;
        int      iLast     = m_iLast;
        int      cElements = m_cElements;
        int      co        = ao.length;

        if (co < cElements)
            {
            ao = (Object[])java.lang.reflect.Array.newInstance(
                ao.getClass().getComponentType(), cElements);
            }
        else if (co > cElements)
            {
            ao[cElements] = null;
            }

        if (cElements > 0)
            {
            if (iFirst < iLast)
                {
                System.arraycopy(aoData, iFirst, ao, 0, cElements);
                }
            else
                {
                System.arraycopy(aoData, iFirst, ao, 0, cSlots  - iFirst);
                System.arraycopy(aoData, 0, ao, cSlots - iFirst, iLast);
                }
            }

        return ao;
        }

    /**
    * {@inheritDoc}
    */
    public Object get(int index)
        {
        return m_aoData[ensureEffectiveIndex(index)];
        }

    /**
    * {@inheritDoc}
    */
    public Object set(int index, Object o)
        {
        Object[] aoData    = m_aoData;
        int      iEff      = ensureEffectiveIndex(index);
        Object   oOldValue = aoData[iEff];

        aoData[iEff] = o;
        return oOldValue;
        }

    /**
    * {@inheritDoc}
    */
    public boolean add(Object o)
        {
        // ensureCapacity may increments modCount
        ensureCapacity(1 + m_cElements + 1); // must have one extra slot

        Object[] aoData = m_aoData;
        int      iLast  = m_iLast;

        aoData[iLast] = o;
        m_iLast       = (iLast + 1) % aoData.length;

        ++m_cElements;
        return true;
        }

    /**
    * Insert the element at the specified position in this list. Shifts the
    * element currently at that position (if any) and any subsequent
    * elements to the right (adds one to their indices).
    *
    * @param index  the index at which the specified element will be inserted
    * @param o      the element to be inserted
    *
    * @throws IndexOutOfBoundsException if index is out of range
    */
    public void add(int index, Object o)
        {
        int cElements = m_cElements;
        if (index == cElements)
            {
            // append to tail
            add(o);
            return;
            }

        rangeCheck(index);
        ensureCapacity(1 + cElements + 1); // must have one empty slot

        Object[] aoData = m_aoData;
        int      cSlots = aoData.length;
        int      iFirst = m_iFirst;
        int      iLast  = m_iLast;
        int      iEff   = effectiveIndex(index);

        if (iEff == iFirst)
            {
            // insert before head
            m_iFirst = iFirst = (iFirst - 1 + cSlots) % cSlots;
            aoData[iFirst] = o;
            }
        else if (((iFirst > iLast) && (iEff > iFirst)) || iLast == cSlots - 1)
            {
            // ... m_iFirst o o o indx o o o m_iLast
            // o o o m_iLast ... m_iFirst o o o indx o o o
            // so we have space in front of m_iFirst

            // shift head to the right
            System.arraycopy(aoData, iFirst,
                    aoData, iFirst - 1, iEff - iFirst);
            --m_iFirst;
            aoData[iEff - 1] = o;
            ++modCount;
            }
        else // iEff < iLast
            {
            // m_iFirst o o o indx o o o m_iLast ...
            // ... m_iFirst o o o indx o o o m_iLast ...
            // or o o o indx o o o m_iLast ... m_iFirst o o o

            // shift tail to the left
            System.arraycopy(aoData, iEff, aoData, iEff + 1, iLast - iEff);
            m_iLast = (iLast + 1) % cSlots;
            aoData[iEff] = o;
            ++modCount;
            }
        ++m_cElements;
        }

    /**
    * Removes the element at the specified position in this list.
    * Shifts any subsequent elements to the left (subtracts one from their
    * indices).
    *
    * @param index  the index of the element to removed
    *
    * @return the element that was removed from the list
    *
    * @throws IndexOutOfBoundsException if index out of range
    */
    public Object remove(int index)
        {
        Object[] aoData    = m_aoData;
        int      iEff      = ensureEffectiveIndex(index);
        int      iFirst    = m_iFirst;
        int      iLast     = m_iLast;
        Object   oOldValue = aoData[iEff];

        // optimized for removing from front.  This test and code block
        // are really the only reason we wrote this class
        if (iEff == iFirst)
            {
            aoData[iEff] = null;
            m_iFirst     = (iFirst + 1) % aoData.length;
            }
        else if ((iFirst > iLast) && (iEff > iFirst))
            {
            // o o o m_LastIndex ... m_iFirst o o o iEff o o o

            // shift head to the right (overwritting iEff)
            System.arraycopy(aoData, iFirst,
                    aoData, iFirst + 1, iEff - iFirst);
            aoData[m_iFirst++] = null;
            }
        else // iEff < iLast
            {
            // ... m_iFirst o o o indx o o o m_iLast
            // m_iFirst o o o indx o o o m_iLast ...
            // ... m_iFirst o o o indx o o o m_iLast ...
            // o o o indx o o o m_iLast ... m_iFirst o o o

            // shift tail to the left (overitting iEff)
            System.arraycopy(aoData, iEff + 1,
                    aoData, iEff, iLast - iEff - 1);
            aoData[--m_iLast] = null;
            }

        --m_cElements;
        ensureCompactness();
        ++modCount;

        return oOldValue;
        }

    /**
    * {@inheritDoc}
    */
    public void clear()
        {
        Object[] aoData = m_aoData;
        int      cSlots = aoData.length;
        int      iFirst = m_iFirst;
        int      iLast  = m_iLast;

        m_iFirst = m_iLast = m_cElements = 0;

        ensureCompactness();

        // if ensure didn't create a new array, null out old data
        if (m_aoData == aoData)
            {
            for (int i = iFirst; i != iLast; i = (i + 1) % cSlots)
                {
                aoData[i] = null;
                }
            }
        ++modCount;
        }

    /**
    * Appends all of the elements in the specified Collection to the end of
    * this list, in the order that they are returned by the
    * specified Collection's Iterator.  The behavior of this operation is
    * undefined if the specified Collection is modified while the operation
    * is in progress.  (This implies that the behavior of this call is
    * undefined if the specified Collection is this list, and this
    * list is nonempty.)
    *
    * @param c  the elements to be inserted into this list
    *
    * @return true if this list changed as a result of the call
    *
    * @throws NullPointerException if the specified collection is null.
    */
    public boolean addAll(Collection c)
        {
        // we could have called addAll(m_cElements, c)
        // but this is a common case and the size is optimizable friendly
        int cNew = c.size();
        if (cNew == 0)
            {
            return false;
            }

        // ensureCapacity increments modCount
        ensureCapacity(1 + m_cElements + cNew); // must have one extra slot

        Object[] aoData = m_aoData;
        int      cSlots = aoData.length;
        int      iLast  = m_iLast;
        int      iNew   = 0;
        Iterator iter   = c.iterator();

        for (; iNew < cNew && iter.hasNext(); ++iNew)
            {
            aoData[iLast] = iter.next();
            iLast         = (iLast + 1) % cSlots;
            }

        m_iLast      = iLast;
        m_cElements += iNew;
        return true;
        }

    /**
    * Inserts all of the elements in the specified Collection into this
    * list, starting at the specified position.  Shift the element
    * currently at that position (if any) and any subsequent elements to
    * the right (increases their indices).  The new elements will appear
    * in the list in the order that they are returned by the
    * specified Collection's iterator.
    *
    * @param index  the index at which to insert first element
    *               from the specified collection
    * @param c      the elements to be inserted into this list
    *
    * @return true if this list changed as a result of the call
    *
    * @throws IndexOutOfBoundsException if index out of range
    * @throws NullPointerException if the specified Collection is null
    */
    public boolean addAll(int index, Collection c)
        {
        int cElements = m_cElements;
        if (index == cElements)
            {
            addAll(c);
            return true;
            }

        rangeCheck(index);

        int cNew = c.size();
        ensureCapacity(1 + cElements + cNew); // must one empty slot

        Object[] aoData = m_aoData;
        int      cSlots = aoData.length;
        int      iFirst = m_iFirst;
        int      iLast  = m_iLast;
        int      iEff   = effectiveIndex(index);
        int      iNew   = 0;
        Iterator iter   = c.iterator();

        if (iEff == iFirst)
            {
            // insert at head (no shifting required)
            m_iFirst = (iFirst - cNew + cSlots) % cSlots;
            }
        else if ((iFirst > iLast && iEff > iFirst) ||
                 (iFirst < iLast && iFirst - cNew >= 0))
            {
            // o o o m_iLast ... m_iFirst o o o iEff o o o
            // or ... (lots of room) ... iFirst o o o iLast ...
            // there is guaranteed room before m_iFirst
            // shift head to the left
            System.arraycopy(aoData, iFirst,
                    aoData, iFirst - cNew, iEff - iFirst);
            m_iFirst = (iFirst - cNew + cSlots) % cSlots;
            }
        // ... (too small) ... m_iFirst o o o iEff o o o m_iLast ...
        // or o o o iEff o o o m_iLast ... m_iFirst o o o
        else if (iLast + cNew <= cSlots) // || (iFirst > iLast)
            {
            // if o o o iEff o o o m_iLast ... m_iFirst o o o
            //     there is guaranteed room after m_iLast
            // if ... m_iFirst o o o iEff o o o m_iLast ...
            //     room in this case too otherwise test would have failed

            // shift tail to the right
            System.arraycopy(aoData, iEff,
                   aoData, iEff + cNew, iLast - iEff);
            m_iLast = (iLast + cNew) % cSlots;
            }
        else // must be ... m_iFirst o o o iEff o o o m_iLast ...
            {
            // shoot, we already knew there was no room after iLast,
            // therefore we must do two moves
            // shift head to the left; tail to the right
            System.arraycopy(aoData, iFirst,
                  aoData, 0, iEff - iFirst);
            int cRight = cNew - iFirst;
            System.arraycopy(aoData, iEff,
                  aoData, iEff + cRight, iLast - iEff);

            m_iFirst = 0;
            m_iLast  = iLast + cRight;
            }

        iEff = effectiveIndex(index); // m_iFirst might have moved
        for (; iNew < cNew && iter.hasNext(); ++iNew)
            {
            aoData[iEff] = iter.next();
            iEff         = (iEff + 1) % cSlots;
            }
        m_cElements += cNew;
        ++modCount;

        if (iNew != cNew) // collection lost elements somehow
            {
            removeRange(index + iNew, index + cNew + 1);
            }

        return true;
        }

    /**
    * Removes from this list all of the elements whose indexes are
    * between fromIndex, inclusive and toIndex, exclusive.  Shifts any
    * succeeding elements to the left (reduces their index).
    * This call shortens the list by (toIndex - fromIndex) elements.
    * (If toIndex==fromIndex, this operation has no effect.)
    *
    * @param fromIndex  the index of first element to be removed
    * @param toIndex    the index after last element to be removed.
    */
    protected void removeRange(int fromIndex, int toIndex)
        {
        rangeCheck(fromIndex);
        rangeCheck(toIndex - 1);

        if (fromIndex >= toIndex)
            {
            return;
            }

        Object[] aoData   = m_aoData;
        int      cSlots   = aoData.length;
        int      iFrom    = effectiveIndex(fromIndex);
        int      iTo      = effectiveIndex(toIndex);
        int      iFirst   = m_iFirst;
        int      iLast    = m_iLast;
        int      cRemoved = iTo - iFrom;

        if (iFirst < iLast || (iFrom < iLast && iTo < iLast))
            {
            // ... iFirst o o o [from o o o to) o o o iLast ...
            // or
            // o o o [from o o o to) o o o iLast ... iFirst o o o
            int cMoved   = iLast - iTo;
            int iNewLast = iLast - cRemoved;
            System.arraycopy(aoData, iTo, aoData, iFrom, cMoved);
            while (iLast != iNewLast)
                {
                aoData[--iLast] = null;
                }
            m_iLast = iLast;
            }
        else if (iFrom >= iFirst && iTo > iFirst)
            {
            // o o o iLast ... iFirst o o o [from o o o to) o o o
            int cMoved    = iFrom - iFirst;
            int iNewFirst = iTo   - cMoved;
            System.arraycopy(aoData, iFirst,
                    aoData, iNewFirst, cMoved);
            while (iFirst != iNewFirst)
                {
                aoData[iFirst++] = null;
                }
            m_iFirst = iFirst;
            }
        else // if (iFrom >= iFirst && iTo <= iLast)
            {
            // o o o to) o o o iLast ... iFirst o o o [from o o o

            // shift head to the right
            int cMovedEnd = iFrom  - iFirst;
            int iNewFirst = cSlots - cMovedEnd;

            if (cMovedEnd > 0)
                {
                System.arraycopy(aoData, iFirst,
                        aoData, iNewFirst, cMovedEnd);
                }

            while (iFirst != iNewFirst)
                {
                aoData[iFirst++] = null;
                }

            // iFrom could have been == iFirst
            m_iFirst = iNewFirst % cSlots;

            if (iTo != 0)
                {
                // shift tail to the left
                int iNewLast = iLast - iTo;

                System.arraycopy(aoData, iTo, aoData, 0, iNewLast);

                while (iLast != iNewLast)
                    {
                    aoData[--iLast] = null;
                    }
                m_iLast = iLast;
                }
            }

        m_cElements -= cRemoved;
        ensureCompactness();
        ++modCount;
        }


    // ----- Serializable Interface -----------------------------------------

    /**
    * Save the state of the list instance to a stream (that
    * is, serialize it).
    *
    * @param s  The stream to write to
    *
    * @throws IOException
    */
    private void writeObject(ObjectOutputStream s)
            throws IOException
        {
        int      preMod = modCount;
        int      iFirst = m_iFirst;
        int      iLast  = m_iLast;
        Object[] aoData = m_aoData;
        int      cSlots = aoData.length;
        s.writeInt(m_cElements);
        for (int i = iFirst; i != iLast; i = (i+1) % cSlots)
            {
            s.writeObject(aoData[i]);
            }

        if (preMod != modCount)
            {
            throw new ConcurrentModificationException();
            }
        }

    /**
    * Reconstitute the list instance from a stream (that is,
    * deserialize it).
    *
    * @param s  The stream to read from
    *
    * @throws IOException            if an I/O error occurs reading this object
    * @throws ClassNotFoundException if the class for an object being
    *                                read cannot be found
    */
    private void readObject(ObjectInputStream s)
            throws IOException, ClassNotFoundException
        {
        Object[] aoData = readObjectArray(s);

        m_iFirst    = 0;
        m_iLast     = aoData.length;
        m_cElements = aoData.length - 1;
        m_aoData    = aoData;
        }


    // ----- Clonable Interface  --------------------------------------------

    /**
    * Returns a shallow copy of this list instance. The
    * elements themselves are not copied.
    *
    * @return a clone of this list instance
    */
    public Object clone()
        {
        try
            {
            int preMod = modCount;

            CircularArrayList cal = (CircularArrayList) super.clone();

            cal.m_aoData = (Object[]) m_aoData.clone();

            if (preMod != modCount)
                {
                throw new ConcurrentModificationException();
                }
            return cal;
            }
        catch (CloneNotSupportedException e)
            {
            // this shouldn't happen, since we are Cloneable
            throw Base.ensureRuntimeException(e);
            }
        }


    // ----- helpers --------------------------------------------------------

    /**
     * Read an array of objects from a DataInput stream.
     *
     * @param in  a ObjectInputStream stream to read from
     *
     * @return an array of objects
     *
     * @throws IOException if an I/O exception occurs
     *
     * @since 22.09
     */
    private static Object[] readObjectArray(ObjectInputStream in)
            throws IOException, ClassNotFoundException
        {
        int c = in.readInt();

        // JEP-290 - ensure we can allocate this array
        ExternalizableHelper.validateLoadArray(Object[].class, c, in);

        return c <= 0
               ? new Object[0]
               : c < ExternalizableHelper.CHUNK_THRESHOLD >> 4
                   ? readObjectArray(in, c, true)
                   : readLargeObjectArray(in, c);
        }

    /**
     * Read an array of the specified number of objects from a DataInput stream.
     *
     * @param in       a DataInput stream to read from
     * @param cLength  length to read
     * @param fPad     if the returned array should be padded by one
     *
     * @return an array of objects
     *
     * @throws IOException if an I/O exception occurs
     *
     * @since 22.09
     */
    private static Object[] readObjectArray(ObjectInputStream in, int cLength, boolean fPad)
            throws IOException, ClassNotFoundException
        {
        Object[] ao = new Object[cLength + (fPad ? 1 : 0)];
        for (int i = 0; i < cLength; i++)
            {
            ao[i] = in.readObject();
            }

        return ao;
        }

    /**
     * Read an array of objects with length larger than {@link ExternalizableHelper#CHUNK_THRESHOLD} {@literal >>} 4.
     *
     * @param in       a DataInput stream to read from
     * @param cLength  length to read
     *
     * @return an array of objects
     *
     * @throws IOException if an I/O exception occurs
     *
     * @since 22.09
     */
    private static Object[] readLargeObjectArray(ObjectInputStream in, int cLength)
            throws IOException, ClassNotFoundException
        {
        int      cBatchMax = ExternalizableHelper.CHUNK_SIZE >> 4;
        int      cBatch    = cLength / cBatchMax + 1;
        Object[] aMerged   = null;
        int      cRead     = 0;
        int      cAllocate = cBatchMax;

        Object[] ao;
        for (int i = 0; i < cBatch && cRead < cLength; i++)
            {
            ao      = readObjectArray(in, cAllocate, cAllocate < cBatchMax);
            aMerged = ExternalizableHelper.mergeArray(aMerged, ao);
            cRead  += ao.length;

            cAllocate = Math.min(cLength - cRead, cBatchMax);
            }

        return aMerged;
        }

    /**
    * Calculate the effective index taking into account offsets and the
    * circular nature of CircularArrayList.
    *
    * @param index  the index to transform
    *
    * @return the effective index in the physical storage array
    */
    protected int effectiveIndex(int index)
        {
        return (m_iFirst + index ) % m_aoData.length;
        }

    /**
    * Check if the given index is in range.  If not, throw an appropriate
    * runtime exception.
    *
    * @param index  the index to be checked for being between
    *               size() and 0 inclusive
    *
    * @throws IndexOutOfBoundsException if the index is not in the range
    */
    protected void rangeCheck(int index)
        {
        if (index >= m_cElements || index < 0)
            {
            throw new IndexOutOfBoundsException(
                "Index: " + index + ", Size: " + m_cElements);
            }
        }

    /**
    * After range checking Calculate the effective index while taking into
    * account the offsets and the circular nature of the list.
    *
    * @param index  the index to transform
    *
    * @return the effective index in the physical storage array
    * @throws IndexOutOfBoundsException if the index is not in the range
    */
    protected int ensureEffectiveIndex(int index)
        {
        if (index >= m_cElements || index < 0)
            {
            throw new IndexOutOfBoundsException(
                "Index: " + index + ", Size: " + m_cElements);
            }
        return (m_iFirst + index) % m_aoData.length;
        }

    /**
    * Ensure the representation of this list is appropriatly compact
    * by shrinking if necessary.
    *
    * @return true if an actual compaction happened; false otherwise
    */
    protected boolean ensureCompactness()
        {
        return false;
        }

    /**
    * Outputs information to standard output about representation
    * for debugging purposes.
    */
    public void dump()
        {
        System.out.println ("\niFirst = " + m_iFirst +
                " iLast= " + m_iLast + " count= " + m_cElements);
        for (int i = 0; i < m_aoData.length; i++)
            {
            System.out.print(m_aoData[i]);
            if (i != m_aoData.length - 1)
                {
                System.out.print(", ");
                }
            }
        System.out.println();
        }


   // ----- data members ----------------------------------------------------

    /**
    * The array into which the elements of the list are stored.
    * The capacity of the list is the length of this array buffer.
    */
    protected Object[] m_aoData = null;

    /**
    * The offset to the first element.
    */
    protected int m_iFirst = 0;

    /**
    * The offset to one past the last element.
    */
    protected int m_iLast = 0;

    /**
    * The size of the list (the number of elements it contains).
    */
    protected int m_cElements = 0;
    }

