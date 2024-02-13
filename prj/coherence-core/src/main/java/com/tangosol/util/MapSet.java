/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import com.oracle.coherence.common.base.Nullable;
import com.tangosol.io.ExternalizableLite;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.Externalizable;
import java.io.IOException;
import java.io.NotActiveException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
* An ExternalizableLite implementation of java.util.Set that uses an
* underlying Map object to store its data in, just as the Java HashSet
* implementation uses an underlying HashMap for its element storage.
*
* @author cp 09/16/05 Originally SafeHashSet (gg)
* @since Coherence 3.2
*/
public class MapSet
        extends AbstractSet
        implements Cloneable, Externalizable, ExternalizableLite
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor. By default, HashMap is the underlying Map. If that
    * is not the desired behavior, then pass an explicit Map to the
    * {@link #MapSet(java.util.Map) MapSet(Map)} constructor.
    * To change the default Map implementation, sub-class the MapSet
    * and override the {@link #instantiateMap() instantiateMap()} method.
    */
    public MapSet()
        {
        m_map = instantiateMap();
        }

    /**
    * Construct a MapSet that uses the given Map for its underlying storage.
    *
    * @param map  the underlying Map object
    */
    public MapSet(Map map)
        {
        Base.azzert(map != null);
        m_map = map;
        }


    // ----- Set interface --------------------------------------------------

    /**
    * Returns the number of elements in this Set.
    *
    * @return the number of elements in this Set.
    */
    public int size()
        {
        return m_map.size();
        }

    /**
    * Returns <tt>true</tt> if this Set contains the specified element.
    *
    * @return <tt>true</tt> if this Set contains the specified element
    */
    public boolean contains(Object o)
        {
        return m_map.containsKey(o);
        }

    /**
    * Returns an iterator over the elements in this Set.
    * The elements are returned in no particular order.
    *
    * @return an iterator over the elements in this Set
    */
    public Iterator iterator()
        {
        return m_map.keySet().iterator();
        }

    /**
    * Returns an array containing all of the elements in this Set.
    * Obeys the general contract of the {@link java.util.Collection#toArray}
    * method.
    *
    * @return an array containing all of the elements in this Set
    */
    public Object[] toArray()
        {
        return m_map.keySet().toArray();
        }

    /**
    * Returns an array containing all of the elements in this set whose
    * runtime type is that of the specified array.  Obeys the general
    * contract of the {@link java.util.Collection#toArray(Object[])} method.
    *
    * @param ao  the array into which the elements of this Set are to
    *            be stored, if it is big enough; otherwise, a new array of the
    *            same runtime type is allocated for this purpose
    *
    * @return   an array containing the elements of this Set
    *
    * @throws   ArrayStoreException the runtime type of a is not a supertype
    *           of the runtime type of every element in this Set
    */
    public Object[] toArray(Object[] ao)
        {
        return m_map.keySet().toArray(ao);
        }

    /**
    * Ensures that this Set contains the specified element.
    * Returns <tt>true</tt> if the Set changed as a result of the call.
    * Returns <tt>false</tt> if this Set already contains the specified
    * element.
    *
    * @param o element to be added to this Set
    *
    * @return <tt>true</tt> if this Set did not already contain the specified
    *         element
    */
    public boolean add(Object o)
        {
        return m_map.put(o, MapSet.NO_VALUE) == null;
        }

    /**
    * Removes the specified element from this Set if it is present.
    * Returns <tt>true</tt> if the Set contained the specified element
    * (or equivalently, if the Set changed as a result of the call).
    * The Set will not contain the specified element once the call returns.
    *
    * @param o object to be removed from this Set, if present
    *
    * @return true if the Set contained the specified element
    */
    public boolean remove(Object o)
        {
        return m_map.remove(o) != null;
        }

    /**
    * Returns <tt>true</tt> if this Set contains all of the elements
    * in the specified Collection.
    *
    * @param coll  Collection to be checked for containment in this Set
    *
    * @return <tt>true</tt> if this Set contains all of the elements
    *         in the specified Collection
    *
    * @see #contains(Object)
    */
    public boolean containsAll(Collection coll)
        {
        return m_map.keySet().containsAll(coll);
        }

    /**
    * Adds all of the elements in the specified Collection to this Set if
    * they're not already present.
    *
    * @param coll Collection whose elements are to be added to this Set
    *
    * @return <tt>true</tt> if this Set changed as a result of the call
    *
    * @see #add(Object)
    */
    public boolean addAll(Collection coll)
        {
        Map     map      = m_map;
        boolean fChanged = false;

        for (Iterator iter = coll.iterator(); iter.hasNext();)
            {
            Object o = iter.next();
            if (map.put(o, MapSet.NO_VALUE) == null)
                {
                fChanged = true;
                }
            }
        return fChanged;
        }

    /**
    * Retains only the elements in this Set that are contained in the
    * specified Collection.
    *
    * @param coll Collection that defines which elements this Set will retain
    *
    * @return <tt>true</tt> if this Set changed as a result of the call
    */
    public boolean retainAll(Collection coll)
        {
        return m_map.keySet().retainAll(coll);
        }

    /**
    * Removes from this Set all of its elements that are contained in the
    * specified Collection.
    *
    * @param coll Collection that defines which elements will be removed
    *             from this Set
    *
    * @return <tt>true</tt> if this Set changed as a result of the call
    *
    * @see #remove(Object)
    */
    public boolean removeAll(Collection coll)
        {
        return m_map.keySet().removeAll(coll);
        }

    /**
    * Removes all of the elements from this Set.
    */
    public void clear()
        {
        m_map.clear();
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Obtain the underlying Map for purposes of synchronization or read-only
    * access; the caller must never directly modify the returned Map.
    *
    * @return the underlying Map
    */
    public Map getMap()
        {
        return m_map;
        }


    // ----- Cloneable interface --------------------------------------------

    /**
    * Create a clone of this MapSet.
    *
    * @return a clone of this MapSet
    */
    public Object clone()
        {
        // shallow clone
        MapSet that;
        try
            {
            that = (MapSet) super.clone();
            }
        catch (CloneNotSupportedException e)
            {
            throw Base.ensureRuntimeException(e);
            }

        // clone the underlying map
        Map mapThat = that.instantiateMap();
        mapThat.putAll(this.m_map);
        that.m_map = mapThat;

        return that;
        }


    // ----- Externalizable interface ---------------------------------------

    /**
    * Initialize this object from the contents of the passed object stream.
    *
    * @param in  the stream to read data from in order to restore the object
    *
    * @exception IOException  if an I/O exception occurs
    */
    public void readExternal(ObjectInput in)
            throws IOException, ClassNotFoundException
        {
        Map map = m_map;
        if (!map.isEmpty())
            {
            throw new NotActiveException();
            }

        int c = in.readInt();
        for (int i = 0; i < c; ++i)
            {
            map.put(in.readObject(), NO_VALUE);
            }
        }

    /**
    * Write the contents of this object into the passed object stream.
    *
    * @param out  the stream to write the object to
    *
    * @exception IOException if an I/O exception occurs
    */
    public void writeExternal(ObjectOutput out)
            throws IOException
        {
        Map map = m_map;

        int c = map.size();
        out.writeInt(c);

        for (Iterator iter = map.keySet().iterator(); iter.hasNext(); )
            {
            out.writeObject(iter.next());
            --c;
            }

        if (c != 0)
            {
            throw new IOException("wrote " + Math.abs(c) + " too " +
                    (c > 0 ? "few" : "many") + " elements");
            }
        }


    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        Map map = m_map;
        if (!map.isEmpty())
            {
            throw new NotActiveException();
            }

        final Object NO_VALUE = MapSet.NO_VALUE;
        boolean fLite = in.readBoolean();
        if (fLite)
            {
            int c = ExternalizableHelper.readInt(in);
            for (int i = 0; i < c; ++i)
                {
                map.put(ExternalizableHelper.readObject(in), NO_VALUE);
                }
            }
        else
            {
            Object[] ao = (Object[]) ExternalizableHelper.readObject(in);
            for (int i = 0, c = ao.length; i < c; ++i)
                {
                map.put(ao[i], NO_VALUE);
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        // scan through the contents searching for anything that cannot be
        // streamed to a DataOutput (i.e. anything that requires Java Object
        // serialization); note that the toArray() also resolves concerns
        // related to the synchronization of the data structure itself during
        // serialization
        boolean  fLite = true;
        Object[] ao    = m_map.keySet().toArray();
        int      c     = ao.length;
        final int FMT_OBJ_SER = ExternalizableHelper.FMT_OBJ_SER;
        for (int i = 0; i < c; ++i)
            {
            if (ExternalizableHelper.getStreamFormat(ao[i]) == FMT_OBJ_SER)
                {
                fLite = false;
                break;
                }
            }

        out.writeBoolean(fLite);
        if (fLite)
            {
            ExternalizableHelper.writeInt(out, c);
            for (int i = 0; i < c; ++i)
                {
                ExternalizableHelper.writeObject(out, ao[i]);
                }
            }
        else
            {
            ExternalizableHelper.writeObject(out, ao);
            }
        }


    // ----- internal -------------------------------------------------------

    /**
    * Factory pattern: Provide an underlying Map for this Set implementation
    * to store its contents in.
    *
    * @return a new Map instance
    */
    protected Map instantiateMap()
        {
        Map map = m_map;
        if (map == null)
            {
            // default Map implementation
            return new HashMap();
            }

        try
            {
            return (Map) m_map.getClass().newInstance();
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }


    // ----- constants ------------------------------------------------------

    /**
    * A no-value object.
    */
    protected static final Object NO_VALUE = Nullable.of(true);


    // ----- data members ---------------------------------------------------

    /**
    * The underlying Map.
    */
    protected transient Map m_map;
    }
