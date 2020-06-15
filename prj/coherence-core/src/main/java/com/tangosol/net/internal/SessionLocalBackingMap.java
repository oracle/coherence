/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.internal;


import com.tangosol.net.BackingMapManagerContext;

import com.tangosol.net.cache.ConfigurableCacheMap;
import com.tangosol.net.cache.LocalCache;

import com.tangosol.util.Binary;
import com.tangosol.util.ConverterCollections;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.MapEvent;
import com.tangosol.util.SafeHashMap;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
* SessionLocalBackingMap is a backing map that stores HttpSession models in
* Object format and is used by Coherence*Web to provide direct access to those
* values from client threads as well as custom EntryProcessors. The caveats are:
* <ul>
*  <li>There is additional conversion cost for get, put and remove, so failback
*      and backup transfer operation will be less efficient;
*  <li>Null values are not supported;
*  <li>For mutating operations to be backed up the "putObject" must ne called
*      explicitly'
*  <li>It cannot be used with the standard BINARY unit calculator
* </ul>
* Note: the keys are still in internal format.
*
* @author gg 2011.05.18
*/
public class SessionLocalBackingMap
        extends LocalCache
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a SessionLocalBackingMap with the specified context.
    */
    public SessionLocalBackingMap(BackingMapManagerContext ctx)
        {
        m_ctx = ctx;
        }


    // ----- direct (raw session object) access -----------------------------

    /**
    * Get the value in external (Object) format directly from the backing map.
    *
    * @param oKey  the key; could be in either internal (Binary) or external
    *              format
    */
    public Object getObject(Object oKey)
        {
        return super.get(
            oKey instanceof Binary ? oKey : convertKeyToInternal(oKey));
        }

    /**
    * Put the value in external (Object) format directly int the backing map.
    *
    * @param oKey    the key; could be in either internal (Binary) or external
    *                format
    * @param oValue  the value in external (Object) format
    *
    * @return  the old value in external format
    */
    public Object putObject(Object oKey, Object oValue)
        {
        return super.put(
            oKey instanceof Binary ? oKey : convertKeyToInternal(oKey),
            oValue, 0L);
        }


    // ----- overridden LocalCache methods that require conversion ----------

    @Override
    public Object remove(Object oKey)
        {
        return convertValueToInternal(super.remove(oKey));
        }

    @Override
    public Object get(Object oKey)
        {
        return convertValueToInternal(super.get(oKey));
        }

    @Override
    public Map getAll(Collection colKeys)
        {
        return convertMapToInternal(super.getAll(colKeys));
        }

    @Override
    public Object put(Object oKey, Object oValue)
        {
        return convertValueToInternal(
            super.put(oKey, convertValueFromInternal((Binary) oValue), 0L));
        }

    @Override
    public void putAll(Map map)
        {
        super.putAll(convertMapFromInternal(map));
        }

    @Override
    public Collection values()
        {
        throw new UnsupportedOperationException();
        }

    @Override
    public Object put(Object oKey, Object oValue, long cMillis)
        {
        return convertValueToInternal(
            super.put(oKey, convertValueFromInternal((Binary) oValue), cMillis));
        }

    @Override
    public ConfigurableCacheMap.Entry getCacheEntry(Object oKey)
        {
        return convertEntryToInternal(super.getCacheEntry(oKey));
        }

    @Override
    protected void dispatchEvent(MapEvent evt)
        {
        super.dispatchEvent(convertMapEventToInternal(evt));
        }


    // ----- Factory pattern -----------------------------------------------

    @Override
    protected SafeHashMap.EntrySet instantiateEntrySet()
        {
        // Set<Map.Entry<Binary, Object>> -> Set<Map.Entry<Binary, Binary>>
        return new SessionLocalBackingMap.EntrySet();
        }


    // ----- inner class: EntrySet -----------------------------------------

    protected class EntrySet
            extends LocalCache.EntrySet
        {
        @Override
        public Object[] toArray(Object ao[])
            {
            throw new UnsupportedOperationException();
            }

        @Override
        protected Iterator instantiateIterator()
            {
            return new SessionLocalBackingMap.EntrySet.EntrySetIterator();
            }

        @Override
        public boolean addAll(Collection c)
            {
            throw new UnsupportedOperationException();
            }

        @Override
        public boolean containsAll(Collection c)
            {
            throw new UnsupportedOperationException();
            }

        @Override
        public boolean contains(Object o)
            {
            throw new UnsupportedOperationException();
            }

        /**
        * An Iterator over the EntrySet.
        */
        protected class EntrySetIterator
                extends LocalCache.EntrySet.EntrySetIterator
            {
            @Override
            public Object next()
                {
                // Map.Entry<Binary, Object>> -> Map.Entry<Binary, Binary>>
                BackingMapManagerContext ctx = m_ctx;
                return new ConverterCollections.ConverterEntry(
                        (Map.Entry) super.next(),
                        NullImplementation.getConverter(),
                        ctx.getValueToInternalConverter(),
                        ctx.getValueFromInternalConverter()
                    ).getEntry();
                }
            }
        }

    // ----- conversion helpers ---------------------------------------------

    protected Binary convertKeyToInternal(Object oKey)
        {
        // Object (key) -> Binary
        return (Binary) m_ctx.getKeyToInternalConverter().convert(oKey);
        }

    protected Object convertKeyFromInternal(Binary bKey)
        {
        // Binary - > Object (key)
        return m_ctx.getKeyFromInternalConverter().convert(bKey);
        }

    protected Binary convertValueToInternal(Object oValue)
        {
        // Object (value) -> Binary
        return (Binary) m_ctx.getValueToInternalConverter().convert(oValue);
        }

    protected ConfigurableCacheMap.Entry convertEntryToInternal(ConfigurableCacheMap.Entry entry)
        {
        // Entry<Binary, Object> -> Entry<Binary, Binary>
        BackingMapManagerContext ctx = m_ctx;
        return new ConverterCollections.ConverterCacheEntry(entry,
                NullImplementation.getConverter(),
                ctx.getValueToInternalConverter(),
                ctx.getValueFromInternalConverter());
        }

    protected Set convertEntrySetToInternal(Set setEntries)
        {
        // Set<Binary> -> Set<Object>
        BackingMapManagerContext ctx = m_ctx;
        return ConverterCollections.getSet(setEntries,
                ctx.getValueToInternalConverter(),
                ctx.getValueFromInternalConverter());
        }

    protected Map convertMapToInternal(Map map)
        {
        // Map<Binary, Object> -> Map<Binary, Binary>
        BackingMapManagerContext ctx = m_ctx;
        return ConverterCollections.getMap(map,
                NullImplementation.getConverter(),
                NullImplementation.getConverter(),
                ctx.getValueToInternalConverter(),
                ctx.getValueFromInternalConverter());
        }

    protected Object convertValueFromInternal(Binary binValue)
        {
        // Binary -> Object
        return m_ctx.getValueFromInternalConverter().convert(binValue);
        }

    protected Map convertMapFromInternal(Map map)
        {
        // Map<Binary, Binary> -> Map<Binary, Object>
        BackingMapManagerContext ctx = m_ctx;
        return ConverterCollections.getMap(map,
                NullImplementation.getConverter(),
                NullImplementation.getConverter(),
                ctx.getValueFromInternalConverter(),
                ctx.getValueToInternalConverter());
        }

    protected MapEvent convertMapEventToInternal(MapEvent event)
        {
        // MapEvent<Binary, Object, Object> -> MapEvent<Binary, Binary, Binary>
        BackingMapManagerContext ctx = m_ctx;
        return ConverterCollections.getMapEvent(event.getMap(), event,
                NullImplementation.getConverter(),
                ctx.getValueToInternalConverter());
        }


    // ----- fields ---------------------------------------------------------

    /**
    * The corresponding context.
    */
    protected BackingMapManagerContext m_ctx;
    }