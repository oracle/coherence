/*
 * Copyright (c) 2016, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;

import com.tangosol.net.cache.KeyAssociation;

import com.tangosol.util.ExternalizableHelper;

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
    @SuppressWarnings({"unchecked", "rawtypes"})
    public ClusteredProperties(String sTaskId, CacheService service, TaskProperties properties)
        {
        m_sTaskId = sTaskId;
        m_service = service;

        NamedCache<PropertyKey, PropertyValue> propertyCache = service.ensureCache(CACHE_NAME, null);
        for (Object o : properties.getProperties().entrySet())
            {
            Map.Entry entry = (Map.Entry) o;
            propertyCache.put(new PropertyKey(sTaskId, entry.getKey()), new PropertyValue(sTaskId, entry.getValue()));
            }
        }

    // ----- Task.Properties interface --------------------------------------

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public <V extends Serializable> V get(String sKey)
        {
        if (m_service != null)
            {
            NamedCache propertyCache = m_service.ensureCache(CACHE_NAME, null);
            PropertyValue value = (PropertyValue) propertyCache.get(new PropertyKey(m_sTaskId, sKey));

            return value == null ? null : (V) value.getValue();
            }

        return null;
        }

    @SuppressWarnings({"unchecked"})
    @Override
    public <V extends Serializable> V put(String sKey, V value)
        {
        if (m_service != null)
            {
            NamedCache<PropertyKey, PropertyValue> propertyCache = m_service.ensureCache(CACHE_NAME, null);

            PropertyValue oldValue = propertyCache.put(new PropertyKey(m_sTaskId, sKey),
                    new PropertyValue(m_sTaskId, value));

            return oldValue == null ? null : (V) oldValue.getValue();
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

    // ----- inner class: PropertyKey ---------------------------------------

    /**
     * Property key.
     */
    @SuppressWarnings("rawtypes")
    public static class PropertyKey
            implements ExternalizableLite, KeyAssociation, PortableObject
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
        public PropertyKey(String sTaskId, Object oKey)
            {
            m_sTaskId = sTaskId;
            m_oKey    = oKey;
            }

        // ----- KeyAssociation interface -----------------------------------

        /**
         * {@inheritDoc}
         */
        public Object getAssociatedKey()
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
         * Return task key.
         *
         * @return task key
         */
        public String getTaskId()
            {
            return m_sTaskId;
            }

        /**
         * Return property key.
         *
         * @return property key
         */
        public Object getKey()
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
        protected Object m_oKey;
        }

    // ----- inner class: PropertyValue -------------------------------------

    /**
     * Property value.
     */
    public static class PropertyValue
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
        public PropertyValue(String sTaskId, Object oValue)
            {
            m_sTaskId = sTaskId;
            m_oValue = oValue;
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
        public Object getValue()
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
        protected Object m_oValue;
        }

    // ----- constants ------------------------------------------------------

    /**
     * The {@link NamedCache} in which the properties will be place in.
     */
    public static String CACHE_NAME = "executor-properties";

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
