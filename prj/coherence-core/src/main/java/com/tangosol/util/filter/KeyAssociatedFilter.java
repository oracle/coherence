/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.filter;


import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import javax.json.bind.annotation.JsonbProperty;


/**
* Filter which limits the scope of another filter according to the key
* association information.
* <p>
* This filter is intended to be used to optimize queries for partitioned
* caches that utilize any of the key association algorithms (by implementing
* either {@link com.tangosol.net.partition.KeyAssociator} or
* {@link com.tangosol.net.cache.KeyAssociation}) to ensure placement of
* all associated entries in the same distributed cache partition (and
* therefore in the same storage-enabled cluster node). Using the
* KeyAssociatedFilter will instruct the distributed cache to apply the
* wrapped filter only to the entries stored at the cache service node that
* owns the specified host key.
* <p>
* <b>Note 1:</b> This filter must be the outermost filter and cannot be used
* as a part of any composite filter (AndFilter, OrFilter, etc.)
* <p>
* <b>Note 2:</b> This filter is intended to be processed only on the client
* side of the partitioned cache service.
* <p>
* For example, consider two classes called <i>Parent</i> and <i>Child</i>
* that are stored in separate caches using <i>ParentKey</i> and
* <i>ChildKey</i> objects respectively. The Parent and Child classes have a
* <i>getId</i> method that returns a Long value that uniquely identifies the
* object. Similarly, the ParentKey and ChildKey classes have a <i>getId</i>
* method that uniquely identifies the corresponding cached object.
* Futhermore, the Child and ChildKey classes include a <i>getParentId</i>
* method that returns the Long identifier of the Parent object.
* <p>
* There are two ways to ensure that Child objects are collocated with their
* Parent objects (in the same storage-enabled cluster node).
* <ol>
* <li>Make the ChildKey class implement
* {@link com.tangosol.net.cache.KeyAssociation} as follows:
* <pre>
* public Object getAssociatedKey()
*     {
*     return getParentId();
*     }</pre>
* and the ParentKey class implement
* {@link com.tangosol.net.cache.KeyAssociation} as follows:
* <pre>
* public Object getAssociatedKey()
*     {
*     return getId();
*     }</pre></li>
* <li>Implement a custom {@link com.tangosol.net.partition.KeyAssociator}
* as follows:
* <pre>
* public Object getAssociatedKey(Object oKey)
*     {
*     if (oKey instanceof ChildKey)
*         {
*         return ((ChildKey) oKey).getParentId();
*         }
*     else if (oKey instanceof ParentKey)
*         {
*         return ((ParentKey) oKey).getId();
*         }
*     else
*         {
*         return null;
*         }
*     }</pre></li>
* </ol>
* The first approach requires a trivial change to the ChildKey and ParentKey
* classes, whereas the second requires a new class and a configuration
* change, but no changes to existing classes.
* <p>
* Now, to retrieve all the Child objects of a given Parent using an optimized
* query you would do the following:
* <pre>
* ParentKey parentKey = new ParentKey(...);
* Long      parentId  = parentKey.getId();
*
* // this Filter will be applied to all Child objects in order to fetch those
* // for which getParentId() returns the specified Parent identifier
* Filter filterEq = new EqualsFilter("getParentId", parentId);
*
* // this Filter will direct the query to the cluster node that currently
* // owns the Parent object with the given identifier
* Filter filterAsc = new KeyAssociatedFilter(filterEq, parentId);
*
* // run the optimized query to get the ChildKey objects
* Set setChildKeys = cacheChildren.keySet(filterAsc);
*
* // get all the Child objects at once
* Set setChildren = cacheChildren.getAll(setChildKeys);
* </pre>
* To remove the Child objects you would then do the following:
* <pre>
* cacheChildren.keySet().removeAll(setChildKeys);</pre>
*
* @author gg 2005.06.09
* @author jh 2005.11.02
*/
public class KeyAssociatedFilter<T>
        extends    ExternalizableHelper
        implements Filter<T>, ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (required by ExternalizableLite interface).
    */
    public KeyAssociatedFilter()
        {
        }

    /**
    * Construct a key associated filter.
    *
    * @param filter    the underlying (wrapped) filter
    * @param oHostKey  the host key that serves as an associated key for all
    *                  keys that the wrapped filter will be applied to
    */
    public KeyAssociatedFilter(Filter<T> filter, Object oHostKey)
        {
        if (filter == null || filter instanceof KeyAssociatedFilter)
            {
            throw new IllegalArgumentException("Invalid filter: " + filter);
            }

        if (oHostKey == null)
            {
            throw new IllegalArgumentException("Host key must be specified");
            }

        m_filter   = filter;
        m_oHostKey = oHostKey;
        }


    // ----- Filter interface -----------------------------------------------

   /**
   * {@inheritDoc}
   */
   public boolean evaluate(T o)
        {
        return m_filter.evaluate(o);
        }


    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        m_filter   = (Filter<T>) readObject(in);
        m_oHostKey = readObject(in);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        writeObject(out, m_filter);
        writeObject(out, m_oHostKey);
        }


    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        m_filter   = (Filter<T>) in.readObject(0);
        m_oHostKey = in.readObject(1);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeObject(0, m_filter);
        out.writeObject(1, m_oHostKey);
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Obtain the wrapped Filter.
    *
    * @return the wrapped filter object
    */
    public Filter<T> getFilter()
        {
        return m_filter;
        }

    /**
    * Obtain the host key that serves as an associated key for all keys that
    * the wrapped filter will be applied to.
    *
    * @return the host key
    */
    public Object getHostKey()
        {
        return m_oHostKey;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Compare the KeyAssociatedFilter with another object to determine
    * equality. Two KeyAssociatedFilter objects are considered equal iff the
    * wrapped filters and host keys are equal.
    *
    * @return true iff this KeyAssociatedFilter and the passed object are
    *         equivalent KeyAssociatedFilter objects
    */
    public boolean equals(Object o)
        {
        if (o instanceof KeyAssociatedFilter)
            {
            KeyAssociatedFilter that = (KeyAssociatedFilter) o;
            return equals(this.m_filter,   that.m_filter)
                && equals(this.m_oHostKey, that.m_oHostKey);
            }

        return false;
        }

    /**
    * Determine a hash value for the KeyAssociatedFilter object according to
    * the general {@link Object#hashCode()} contract.
    *
    * @return an integer hash value for this KeyAssociatedFilter object
    */
    public int hashCode()
        {
        return hashCode(m_filter) + hashCode(m_oHostKey);
        }

    /**
    * Return a human-readable description for this Filter.
    *
    * @return a String description of the Filter
    */
    public String toString()
        {
        String sClass = getClass().getName();
        return sClass.substring(sClass.lastIndexOf('.') + 1) +
            '(' + m_filter + ", " + m_oHostKey + ')';
        }


    // ----- data members ---------------------------------------------------

    /**
    * The underlying filter.
    */
    @JsonbProperty("filter")
    private Filter<T> m_filter;

    /**
    * The association host key.
    */
    @JsonbProperty("hostKey")
    private Object m_oHostKey;
    }
