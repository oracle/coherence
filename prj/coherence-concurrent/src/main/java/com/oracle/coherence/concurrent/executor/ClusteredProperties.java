/*
 * Copyright (c) 2016, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor;

import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.concurrent.executor.internal.ExecutorTrace;
import com.oracle.coherence.concurrent.executor.util.Caches;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;

import com.tangosol.net.cache.KeyAssociation;

import com.tangosol.util.BinaryEntry;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

import java.util.Map;
import java.util.Objects;

/**
 * A clustered implementation of a {@link Task.Properties}.
 *
 * @author bo, lh
 * @since 21.12
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class ClusteredProperties
        implements Task.Properties, ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs an {@link ClusteredProperties} (required for serialization).
     */
    @SuppressWarnings("unused")
    public ClusteredProperties()
        {
        }

    /**
     * Constructs a {@link ClusteredProperties} given the task ID and cache service.
     *
     * @param sTaskId   the {@link Task} ID
     * @param service  the {@link CacheService} used by the {@link Task}
     */
    public ClusteredProperties(String sTaskId, CacheService service)
        {
        m_sTaskId = sTaskId;
        m_service = service;
        }

    /**
     * Constructs a {@link ClusteredProperties} given the task ID, cache service, and initial properties.
     *
     * @param sTaskId      the {@link Task} ID
     * @param service     the {@link CacheService} used by the {@link Task}
     * @param properties  the properties to initialize
     */
    public ClusteredProperties(String sTaskId, CacheService service, TaskProperties properties)
        {
        m_sTaskId = sTaskId;
        m_service = service;

        NamedCache<PropertyKey<Serializable>, PropertyValue<Serializable>> propertyCache = Caches.properties(service);
        for (Object o : properties.getProperties().entrySet())
            {
            Map.Entry entry = (Map.Entry) o;
            propertyCache.put(new PropertyKey(sTaskId, (Serializable) entry.getKey()),
                              new PropertyValue(sTaskId, (Serializable) entry.getValue()));
            }
        }

    // ----- Task.Properties interface --------------------------------------

    @Override
    public <V extends Serializable> V get(String sKey)
        {
        if (m_service != null)
            {
            NamedCache<PropertyKey, PropertyValue> propertyCache = Caches.properties(getCacheService());
            PropertyValue value = propertyCache.get(new PropertyKey(m_sTaskId, sKey));

            return value == null ? null : (V) value.getValue();
            }

        return null;
        }

    @Override
    public <V extends Serializable> V put(String sKey, V value)
        {
        if (m_service != null)
            {
            NamedCache<PropertyKey, PropertyValue> propertyCache = Caches.properties(getCacheService());

            PropertyValue<V> oldValue = (PropertyValue<V>) propertyCache
                    .invoke(new PropertyKey(m_sTaskId, sKey),
                            new SetPropertyValueProcessor(new PropertyValue(m_sTaskId, value)));

            return oldValue == null ? null : oldValue.getValue();
            }
        else
            {
            Logger.warn("Failed to put a property; the cache service is null.");
            }

        return null;
        }

    // ----- ExternalizableLite interface -----------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_sTaskId = ExternalizableHelper.readUTF(in);
        m_service = ExternalizableHelper.readObject(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeUTF(out, m_sTaskId);
        ExternalizableHelper.writeObject(out, m_service);
        }


    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_sTaskId = in.readString(0);
        m_service = in.readObject(1);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeString(0, m_sTaskId);
        out.writeObject(1, m_service);
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Return the {@link CacheService} used by this executor service.
     *
     * @return the {@link CacheService} used by this executor service
     */
    protected CacheService getCacheService()
        {
        return m_service;
        }

    // ----- inner class: PropertyKey ---------------------------------------

    /**
     * Property key.
     *
     * @param <T>  the key type
     */
    @SuppressWarnings("rawtypes")
    public static class PropertyKey<T extends Serializable>
            implements ExternalizableLite, KeyAssociation<String>, PortableObject
        {
        // ----- constructors -----------------------------------------------

        /**
         * Deserialization constructor.
         */
        @SuppressWarnings("unused")
        public PropertyKey()
            {
            }

        /**
         * Construct a PropertyKey instance.
         *
         * @param sTaskId  task ID
         * @param oKey     property key
         */
        public PropertyKey(String sTaskId, T oKey)
            {
            m_sTaskId = sTaskId;
            m_oKey    = oKey;
            }

        // ----- KeyAssociation interface -----------------------------------

        /**
         * {@inheritDoc}
         */
        public String getAssociatedKey()
            {
            return m_sTaskId;
            }

        // ----- Object methods ---------------------------------------------

        @Override
        public boolean equals(Object o)
            {
            if (this == o)
                {
                return true;
                }

            if (o == null || getClass() != o.getClass())
                {
                return false;
                }

            PropertyKey that = (PropertyKey) o;

            if (!Objects.equals(m_sTaskId, that.m_sTaskId))
                {
                return false;
                }

            return Objects.equals(m_oKey, that.m_oKey);
            }

        @Override
        public int hashCode()
            {
            int result = m_sTaskId != null ? m_sTaskId.hashCode() : 0;

            result = 31 * result + (m_oKey != null ? m_oKey.hashCode() : 0);

            return result;
            }

        @Override
        public String toString()
            {
            return m_sTaskId + ":" + m_oKey;
            }

        // ----- accessors --------------------------------------------------

        /**
         * Return the task ID.
         *
         * @return task ID
         */
        public String getTaskId()
            {
            return m_sTaskId;
            }

        /**
         * Return the property key.
         *
         * @return property key
         */
        public T getKey()
            {
            return m_oKey;
            }

        // ----- ExternalizableLite interface -------------------------------

        @Override
        public void readExternal(DataInput in) throws IOException
            {
            m_sTaskId = ExternalizableHelper.readUTF(in);
            m_oKey    = ExternalizableHelper.readObject(in);
            }

        @Override
        public void writeExternal(DataOutput out) throws IOException
            {
            ExternalizableHelper.writeUTF(out, m_sTaskId);
            ExternalizableHelper.writeObject(out, m_oKey);
            }

        // ----- PortableObject interface -----------------------------------

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            m_sTaskId = in.readString(0);
            m_oKey    = in.readObject(1);
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeString(0, m_sTaskId);
            out.writeObject(1, m_oKey);
            }

        // ----- data members -----------------------------------------------

        /**
         * Task ID.
         */
        protected String m_sTaskId;

        /**
         * Property key.
         */
        protected T m_oKey;
        }

    // ----- inner class: PropertyValue -------------------------------------

    /**
     * Property value.
     *
     * @param <T>  the property value type
     */
    public static class PropertyValue<T extends Serializable>
            implements ExternalizableLite, PortableObject
        {
        // ----- constructors -----------------------------------------------

        /**
         * Deserialization constructor.
         */
        @SuppressWarnings("unused")
        public PropertyValue()
            {
            }

        /**
         * Construct a PropertyValue instance.
         *
         * @param sTaskId  task ID
         * @param oValue   property value
         */
        public PropertyValue(String sTaskId, T oValue)
            {
            m_sTaskId = sTaskId;
            m_oValue  = oValue;
            }

        // ----- Object methods ---------------------------------------------

        @Override
        public boolean equals(Object o)
            {
            if (this == o)
                {
                return true;
                }

            if (o == null || getClass() != o.getClass())
                {
                return false;
                }

            PropertyValue that = (PropertyValue) o;

            if (!Objects.equals(m_sTaskId, that.m_sTaskId))
                {
                return false;
                }

            return Objects.equals(m_oValue, that.m_oValue);
            }

        @Override
        public int hashCode()
            {
            int result = m_sTaskId != null ? m_sTaskId.hashCode() : 0;

            result = 31 * result + (m_oValue != null ? m_oValue.hashCode() : 0);

            return result;
            }

        @Override
        public String toString()
            {
            return m_sTaskId + ':' + m_oValue;
            }

        // ----- accessors --------------------------------------------------

        /**
         * Return task key.
         *
         * @return task key
         */
        public String getTaskId()
            {
            return m_sTaskId;
            }

        /**
         * Return property value.
         *
         * @return property value
         */
        public T getValue()
            {
            return m_oValue;
            }

        // ----- ExternalizableLite interface -------------------------------

        @Override
        public void readExternal(DataInput in) throws IOException
            {
            m_sTaskId = ExternalizableHelper.readUTF(in);
            m_oValue  = ExternalizableHelper.readObject(in);
            }

        @Override
        public void writeExternal(DataOutput out) throws IOException
            {
            ExternalizableHelper.writeUTF(out, m_sTaskId);
            ExternalizableHelper.writeObject(out, m_oValue);
            }

        // ----- PortableObject interface -----------------------------------

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            m_sTaskId = in.readString(0);
            m_oValue  = in.readObject(1);
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeString(0, m_sTaskId);
            out.writeObject(1, m_oValue);
            }

        // ----- data members -----------------------------------------------

        /**
         * Task ID.
         */
        protected String m_sTaskId;

        /**
         * Property value.
         */
        protected T m_oValue;
        }

    // ----- inner class: SetPropertyValueProcessor -------------------------

    /**
     * An {@link InvocableMap.EntryProcessor} for inserting/updating
     * {@link PropertyKey}/{@link PropertyValue} mappings.
     *
     * @param <K>  the property key type
     * @param <V>  the property value type
     *
     * @since 22.06.1
     */
    public static class SetPropertyValueProcessor<K extends Serializable, V extends Serializable>
            extends PortableAbstractProcessor<PropertyKey<K>, PropertyValue<V>, PropertyValue<V>>
            implements ExternalizableLite
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs a new {@code SetPropertyValueProcessor}
         * (required for serialization)
         */
        @SuppressWarnings("unused")
        public SetPropertyValueProcessor()
            {
            }

        /**
         * Constructs a new {@code SetPropertyValueProcessor}
         *
         * @param oValue  the property value
         */
        public SetPropertyValueProcessor(PropertyValue<V> oValue)
            {
            m_oValue = oValue;
            }

        // ----- EntryProcessor interface -----------------------------------

        @Override
        public PropertyValue<V> process(InvocableMap.Entry<PropertyKey<K>, PropertyValue<V>> entry)
            {
            String               sTaskId = entry.getKey().getTaskId();
            ClusteredTaskManager manager = null;

            ExecutorTrace.entering(SetPropertyValueProcessor.class, "process", sTaskId);

            BinaryEntry managerEntry = ((BinaryEntry) entry)
                    .getAssociatedEntry(Caches.TASKS_CACHE_NAME, sTaskId);

            PropertyValue<V> result = null;

            if (managerEntry != null)
                {
                manager = (ClusteredTaskManager) managerEntry.getValue();
                }

            if (manager != null && !manager.isCompleted())
                {
                if (entry.isPresent())
                    {
                    result = entry.getValue();
                    }

                entry.setValue(m_oValue);
                }
            else
                {
                // task is no longer preset or has been completed; this is a no-op
                ExecutorTrace.log(() -> String.format("Ignoring attempt to set property [%s]"
                                                      + " for task [%s] as it has been completed or no longer exists",
                                                      m_oValue, sTaskId));
                }

            ExecutorTrace.exiting(SetPropertyValueProcessor.class, "process", sTaskId);

            return result;
            }

        // ----- PortableObject interface -----------------------------------

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            m_oValue = in.readObject(0);
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeObject(0, m_oValue);
            }

        // ----- ExternalizableHelper interface -----------------------------

        @Override
        public void readExternal(DataInput in) throws IOException
            {
            m_oValue = ExternalizableHelper.readObject(in);
            }

        @Override
        public void writeExternal(DataOutput out) throws IOException
            {
            ExternalizableHelper.writeObject(out, m_oValue);
            }

        // ----- data members -----------------------------------------------

        /**
         * The property value.
         */
        protected PropertyValue<V> m_oValue;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link Task} ID.
     */
    protected String m_sTaskId;

    /**
     * The {@link CacheService} used by the {@link Task}, hence the property cache.
     */
    protected CacheService m_service;
    }
