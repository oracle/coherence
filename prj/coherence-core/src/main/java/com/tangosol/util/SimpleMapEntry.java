/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import java.util.Map;


/**
* A map entry (key-value pair).  The <tt>Map.entrySet</tt> method returns
* a collection-view of the map, whose elements are of this class.  The
* <i>only</i> way to obtain a reference to a map entry is from the
* iterator of this collection-view.  These <tt>Map.Entry</tt> objects are
* valid <i>only</i> for the duration of the iteration; more formally,
* the behavior of a map entry is undefined if the backing map has been
* modified after the entry was returned by the iterator, except through
* the iterator's own <tt>remove</tt> operation, or through the
* <tt>setValue</tt> operation on a map entry returned by the iterator.
*
* @author cp 2002.02.07
*/
public class SimpleMapEntry<K, V>
        extends Base
        implements MapTrigger.Entry<K, V>, Cloneable, Serializable
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    protected SimpleMapEntry()
        {
        this(null, null, (V) NO_VALUE);
        }

    /**
    * Construct a SimpleMapEntry with just a key.
    *
    * @param key  an object for the key
    */
    protected SimpleMapEntry(K key)
        {
        this(key, null, (V) NO_VALUE);
        }

    /**
    * Copy constructor.
    *
    * @param entry  an entry to copy from
    */
    public SimpleMapEntry(Map.Entry<K, V> entry)
        {
        this(entry.getKey(), entry.getValue(),
             entry instanceof MapTrigger.Entry
                && ((MapTrigger.Entry) entry).isOriginalPresent() ?
                   ((MapTrigger.Entry<K, V>) entry).getOriginalValue() : (V) NO_VALUE);
        }

    /**
    * Construct a SimpleMapEntry with a key and a value.
    *
    * @param key    an object for the key
    * @param value  an object for the value
    */
    public SimpleMapEntry(K key, V value)
        {
        this(key, value, (V) NO_VALUE);
        }

    /**
    * Construct a SimpleMapEntry with a key, value and original value.
    *
    * @param key        an object for the key
    * @param value      an object for the value
    * @param origValue  an object for the original value
    *
    * @since Coherence 3.6
    */
    public SimpleMapEntry(K key, V value, V origValue)
        {
        m_oKey       = key;
        m_oValue     = value;
        m_oOrigValue = origValue;
        }


    // ----- Map.Entry interface --------------------------------------------

    /**
     * {@inheritDoc}
     */
    public K getKey()
        {
        return m_oKey;
        }

    /**
    * Returns the value corresponding to this entry.  If the mapping
    * has been removed from the backing map (by the iterator's
    * <tt>remove</tt> operation), the results of this call are undefined.
    *
    * @return the value corresponding to this entry.
    */
    public V getValue()
        {
        return m_oValue;
        }

    /**
    * Replaces the value corresponding to this entry with the specified
    * value (optional operation).  (Writes through to the map.)  The
    * behavior of this call is undefined if the mapping has already been
    * removed from the map (by the iterator's <tt>remove</tt> operation).
    *
    * @param value new value to be stored in this entry
    *
    * @return old value corresponding to the entry
    */
    public V setValue(V value)
        {
        V oPrev = m_oValue;
        m_oValue = value;
        return oPrev;
        }


    // ----- MapTrigger.Entry interface -------------------------------------

    /**
     * {@inheritDoc}
     */
    public V getOriginalValue()
        {
        V oOrig = m_oOrigValue;
        return oOrig == NO_VALUE ? null : oOrig;
        }

    /**
     * {@inheritDoc}
     */
    public boolean isOriginalPresent()
        {
        return m_oOrigValue != NO_VALUE;
        }

    /**
     * {@inheritDoc}
     */
    public void setValue(V oValue, boolean fSynthetic)
        {
        setValue(oValue);
        }

    /**
     * {@inheritDoc}
     */
    public <U> void update(ValueUpdater<V, U> updater, U value)
        {
        InvocableMapHelper.updateEntry(updater, this, value);
        }

    /**
     * {@inheritDoc}
     */
    public boolean isPresent()
        {
        return true;
        }

    /**
     * {@inheritDoc}
     */
    public boolean isSynthetic()
        {
        return false;
        }

    /**
     * @throws UnsupportedOperationException  by default
     */
    public void remove(boolean fSynthetic)
        {
        throw new UnsupportedOperationException();
        }

    /**
     * {@inheritDoc}
     */
    public <T, E> E extract(ValueExtractor<T, E> extractor)
        {
        return InvocableMapHelper.extractFromEntry(extractor, this);
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Compares the specified object with this entry for equality.
    * Returns <tt>true</tt> if the given object is also a map entry and
    * the two entries represent the same mapping.  More formally, two
    * entries <tt>e1</tt> and <tt>e2</tt> represent the same mapping
    * if<pre>
    *     (e1.getKey()==null ?
    *      e2.getKey()==null : e1.getKey().equals(e2.getKey()))  &amp;&amp;
    *     (e1.getValue()==null ?
    *      e2.getValue()==null : e1.getValue().equals(e2.getValue()))
    * </pre>
    * This ensures that the <tt>equals</tt> method works properly across
    * different implementations of the <tt>Map.Entry</tt> interface.
    *
    * @param o object to be compared for equality with this map entry
    *
    * @return <tt>true</tt> if the specified object is equal to this map
    *         entry
    */
    public boolean equals(Object o)
        {
        if (!(o instanceof Map.Entry))
            {
            return false;
            }

        Map.Entry that = (Map.Entry) o;
        if (this == that)
            {
            return true;
            }

        return equals(this.getKey()  , that.getKey()  )
            && equals(this.getValue(), that.getValue());
        }

    /**
    * Returns the hash code value for this map entry.  The hash code
    * of a map entry <tt>e</tt> is defined to be: <pre>
    *     (e.getKey()==null   ? 0 : e.getKey().hashCode()) ^
    *     (e.getValue()==null ? 0 : e.getValue().hashCode())
    * </pre>
    * This ensures that <tt>e1.equals(e2)</tt> implies that
    * <tt>e1.hashCode()==e2.hashCode()</tt> for any two Entries
    * <tt>e1</tt> and <tt>e2</tt>, as required by the general
    * contract of <tt>Object.hashCode</tt>.
    *
    * @return the hash code value for this map entry.
    */
    public int hashCode()
        {
        Object oKey   = getKey();
        Object oValue = getValue();
        return (oKey   == null ? 0 : oKey  .hashCode()) ^
               (oValue == null ? 0 : oValue.hashCode());
        }

    /**
    * Render the map entry as a String.
    *
    * @return the details about this entry
    */
    public String toString()
        {
        StringBuilder sb = new StringBuilder();
        sb.append("Entry{Key=\"")
          .append(getKey())
          .append("\", Value=\"")
          .append(getValue());

        if (isOriginalPresent())
            {
            sb.append("\", OrigValue=\"")
               .append(getOriginalValue());
            }

        sb.append("\"}");
        return sb.toString();
        }


    // ----- Cloneable interface --------------------------------------------

    /**
    * Clone the Entry.
    *
    * @return a clone of this Entry
    */
    public Object clone()
        {
        try
            {
            return super.clone();
            }
        catch (CloneNotSupportedException e)
            {
            throw ensureRuntimeException(e);
            }
        }


    // ----- Serializable interface -----------------------------------------

    /**
    * Write this object to an ObjectOutputStream.
    *
    * @param out  the ObjectOutputStream to write this object to
    *
    * @throws IOException  thrown if an exception occurs writing this object
    */
    private void writeObject(ObjectOutputStream out)
            throws IOException
        {
        out.writeObject(m_oKey);
        out.writeObject(m_oValue);
        if (isOriginalPresent())
            {
            out.writeBoolean(true);
            out.writeObject(m_oOrigValue);
            }
        else
            {
            out.writeBoolean(false);
            }
        }

    /**
    * Read this object from an ObjectInputStream.
    *
    * @param in  the ObjectInputStream to read this object from
    *
    * @throws IOException  if an exception occurs reading this object
    * @throws ClassNotFoundException  if the class for an object being
    *         read cannot be found
    */
    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException
        {
        m_oKey   = (K) in.readObject();
        m_oValue = (V) in.readObject();
        if (in.readBoolean())
            {
            m_oOrigValue = (V) in.readObject();
            }
        else
            {
            m_oOrigValue = (V) NO_VALUE;
            }
        }

    // ----- data members ---------------------------------------------------

    /**
    * Constant used to indicate that the original value does not exist.
    */
    protected static final Object NO_VALUE = new Object();

    /**
    * The key. This object reference will not change for the life of
    * the Entry.
    */
    protected K m_oKey;

    /**
    * The value. This object reference can change within the life of
    * the Entry.
    */
    protected V m_oValue;

    /**
    * The original value. This object reference will not change within the life
    * of the Entry.
    */
    protected V m_oOrigValue;
    }
