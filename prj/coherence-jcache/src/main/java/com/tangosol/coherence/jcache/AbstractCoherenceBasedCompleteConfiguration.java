/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.internal.util.ObjectFormatter;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

import javax.cache.configuration.CacheEntryListenerConfiguration;
import javax.cache.configuration.CompleteConfiguration;
import javax.cache.configuration.Factory;

import javax.cache.expiry.EternalExpiryPolicy;
import javax.cache.expiry.ExpiryPolicy;

import javax.cache.integration.CacheLoader;
import javax.cache.integration.CacheWriter;

/**
 * A base implementation of a {@link CoherenceBasedCompleteConfiguration}.
 *
 * @author bo  2013.11.12
 * @since Coherence 12.1.3
 *
 * @param <K>  the type of the keys
 * @param <V>  the type of the values
 */
public abstract class AbstractCoherenceBasedCompleteConfiguration<K, V>
        implements CoherenceBasedCompleteConfiguration<K, V>, ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs an {@link AbstractCoherenceBasedCompleteConfiguration}.
     */
    public AbstractCoherenceBasedCompleteConfiguration()
        {
        m_clzKey                     = (Class<K>) Object.class;
        m_clzValue                   = (Class<V>) Object.class;
        m_listListenerConfigurations = new ArrayList<CacheEntryListenerConfiguration<K, V>>();
        m_factoryCacheLoader         = null;
        m_factoryCacheWriter         = null;
        setExpiryPolicyFactory(null);
        m_fReadThrough       = false;
        m_fWriteThrough      = false;
        m_fStatisticsEnabled = false;
        m_fStoreByValue      = true;
        m_fManagementEnabled = false;
        }

    /**
     * Constructs an {@link AbstractCoherenceBasedCompleteConfiguration} based on
     * another {@link CompleteConfiguration}.
     *
     * @param cfgComplete  the {@link CompleteConfiguration}
     */
    public AbstractCoherenceBasedCompleteConfiguration(CompleteConfiguration<K, V> cfgComplete)
        {
        m_clzKey                     = cfgComplete.getKeyType();
        m_clzValue                   = cfgComplete.getValueType();

        m_listListenerConfigurations = new ArrayList<CacheEntryListenerConfiguration<K, V>>();

        for (CacheEntryListenerConfiguration<K, V> cfgListener : cfgComplete.getCacheEntryListenerConfigurations())
            {
            addCacheEntryListenerConfiguration(cfgListener);
            }

        m_factoryCacheLoader = cfgComplete.getCacheLoaderFactory();
        m_factoryCacheWriter = cfgComplete.getCacheWriterFactory();
        setExpiryPolicyFactory(cfgComplete.getExpiryPolicyFactory());

        m_fReadThrough       = cfgComplete.isReadThrough();
        m_fWriteThrough      = cfgComplete.isWriteThrough();

        m_fStatisticsEnabled = cfgComplete.isStatisticsEnabled();

        m_fStoreByValue      = cfgComplete.isStoreByValue();

        m_fManagementEnabled = cfgComplete.isManagementEnabled();
        }

    // ----- Configuration interface ----------------------------------------

    @Override
    public Class<K> getKeyType()
        {
        return m_clzKey;
        }

    @Override
    public Class<V> getValueType()
        {
        return m_clzValue;
        }

    @Override
    public boolean isStoreByValue()
        {
        return m_fStoreByValue;
        }

    @Override
    public List<CacheEntryListenerConfiguration<K, V>> getCacheEntryListenerConfigurations()
        {
        return m_listListenerConfigurations;
        }

    @Override
    public Factory<CacheLoader<K, V>> getCacheLoaderFactory()
        {
        return m_factoryCacheLoader;
        }

    @Override
    public Factory<CacheWriter<? super K, ? super V>> getCacheWriterFactory()
        {
        return m_factoryCacheWriter;
        }

    @Override
    public Factory<ExpiryPolicy> getExpiryPolicyFactory()
        {
        return m_factoryExpiryPolicy;
        }

    @Override
    public boolean isReadThrough()
        {
        return m_fReadThrough;
        }

    @Override
    public boolean isWriteThrough()
        {
        return m_fWriteThrough;
        }

    @Override
    public boolean isStatisticsEnabled()
        {
        return m_fStatisticsEnabled;
        }

    @Override
    public boolean isManagementEnabled()
        {
        return m_fManagementEnabled;
        }

    // ----- CoherenceBasedCompleteConfiguration interface ------------------

    @Override
    public void setTypes(Class<K> clzKey, Class<V> clzValue)
        {
        if (clzKey == null || clzValue == null)
            {
            throw new NullPointerException("The key and/or value type can't be null");
            }
        else
            {
            m_clzKey   = clzKey;
            m_clzValue = clzValue;
            }
        }

    @Override
    public void addCacheEntryListenerConfiguration(CacheEntryListenerConfiguration<K, V> cfgListener)
        {
        if (cfgListener == null)
            {
            throw new NullPointerException("CacheEntryListenerConfiguration can't be null");
            }

        if (m_listListenerConfigurations.contains(cfgListener))
            {
            throw new IllegalArgumentException("A CacheEntryListenerConfiguration can " + "be registered only once");
            }
        else
            {
            m_listListenerConfigurations.add(cfgListener);
            }
        }

    @Override
    public void setCacheLoaderFactory(Factory<? extends CacheLoader<K, V>> factory)
        {
        m_factoryCacheLoader = (Factory<CacheLoader<K, V>>) factory;
        }

    @Override
    public void setCacheWriterFactory(Factory<? extends CacheWriter<? super K, ? super V>> factory)
        {
        m_factoryCacheWriter = (Factory<CacheWriter<? super K, ? super V>>) factory;
        }

    @Override
    public void setExpiryPolicyFactory(Factory<? extends ExpiryPolicy> factory)
        {
        m_factoryExpiryPolicy = factory == null ? EternalExpiryPolicy.factoryOf() : (Factory<ExpiryPolicy>) factory;
        }

    @Override
    public void setReadThrough(boolean fReadThrough)
        {
        m_fReadThrough = fReadThrough;
        }

    @Override
    public void setWriteThrough(boolean fWriteThrough)
        {
        m_fWriteThrough = fWriteThrough;
        }

    @Override
    public void setStoreByValue(boolean fStoreByValue)
        {
        m_fStoreByValue = fStoreByValue;
        }

    @Override
    public void setStatisticsEnabled(boolean fStatisticsEnabled)
        {
        m_fStatisticsEnabled = fStatisticsEnabled;
        }

    @Override
    public void setManagementEnabled(boolean fManagementEnabled)
        {
        m_fManagementEnabled = fManagementEnabled;
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public int hashCode()
        {
        final int prime  = 31;
        int       result = 1;

        result = prime * result + m_clzKey.hashCode();
        result = prime * result + m_clzValue.hashCode();
        result = prime * result
                 + ((m_listListenerConfigurations == null) ? 0 : m_listListenerConfigurations.hashCode());
        result = prime * result + ((m_factoryCacheLoader == null) ? 0 : m_factoryCacheLoader.hashCode());
        result = prime * result + ((m_factoryCacheWriter == null) ? 0 : m_factoryCacheWriter.hashCode());
        result = prime * result + ((m_factoryExpiryPolicy == null) ? 0 : m_factoryExpiryPolicy.hashCode());
        result = prime * result + (m_fReadThrough ? 1231 : 1237);
        result = prime * result + (m_fStatisticsEnabled ? 1231 : 1237);
        result = prime * result + (m_fStoreByValue ? 1231 : 1237);
        result = prime * result + (m_fWriteThrough ? 1231 : 1237);

        return result;
        }

    @Override
    public boolean equals(Object object)
        {
        if (this == object)
            {
            return true;
            }

        if (object == null)
            {
            return false;
            }

        if (!(object instanceof AbstractCoherenceBasedCompleteConfiguration))
            {
            return false;
            }

        AbstractCoherenceBasedCompleteConfiguration other = (AbstractCoherenceBasedCompleteConfiguration) object;

        if (!m_clzKey.equals(other.m_clzKey))
            {
            return false;
            }

        if (!m_clzValue.equals(other.m_clzValue))
            {
            return false;
            }

        if (m_listListenerConfigurations == null)
            {
            if (other.m_listListenerConfigurations != null)
                {
                return false;
                }
            }
        else if (!m_listListenerConfigurations.equals(other.m_listListenerConfigurations))
            {
            return false;
            }

        if (m_factoryCacheLoader == null)
            {
            if (other.m_factoryCacheLoader != null)
                {
                return false;
                }
            }
        else if (!m_factoryCacheLoader.equals(other.m_factoryCacheLoader))
            {
            return false;
            }

        if (m_factoryCacheWriter == null)
            {
            if (other.m_factoryCacheWriter != null)
                {
                return false;
                }
            }
        else if (!m_factoryCacheWriter.equals(other.m_factoryCacheWriter))
            {
            return false;
            }

        if (m_factoryExpiryPolicy == null)
            {
            if (other.m_factoryExpiryPolicy != null)
                {
                return false;
                }
            }
        else if (!m_factoryExpiryPolicy.equals(other.m_factoryExpiryPolicy))
            {
            return false;
            }

        if (m_fReadThrough != other.m_fReadThrough)
            {
            return false;
            }

        if (m_fStatisticsEnabled != other.m_fStatisticsEnabled)
            {
            return false;
            }

        if (m_fStoreByValue != other.m_fStoreByValue)
            {
            return false;
            }

        if (m_fWriteThrough != other.m_fWriteThrough)
            {
            return false;
            }

        return true;
        }

    @Override
    public String toString()
        {
        return new ObjectFormatter().format(this.getClass().getCanonicalName(), this);
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Helper to load class field in read serializers.
     *
     * @param loader loader to use to load <tt>className</tt>
     * @param className the fully-qualified canonical className to load
     * @param description description of field being loaded.
     *
     * @return loaded class
     */
    private Class loadClass(ClassLoader loader, String className, String description)
        {
        Class result = Object.class;

        if (className != null)
            {
            try
                {
                result = (Class<K>) loader.loadClass(className);
                }
            catch (ClassNotFoundException e)
                {
                Logger.warn(description + " not found: " + className);
                }

            }

        return result;
        }

    // ----- ExternalizableLite interface -----------------------------------

    @Override
    public void readExternal(DataInput in)
            throws IOException
        {
        ClassLoader loader    = Base.getContextClassLoader();
        String      className = ExternalizableHelper.readUTF(in);

        m_clzKey   = loadClass(loader, className, "keyClass");
        className  = ExternalizableHelper.readUTF(in);
        m_clzValue = loadClass(loader, className, "valueClass");
        ExternalizableHelper.readCollection(in, m_listListenerConfigurations, loader);
        m_factoryCacheLoader = (Factory<CacheLoader<K, V>>) ExternalizableHelper.readObject(in);
        m_factoryCacheWriter = (Factory<CacheWriter<? super K, ? super V>>) ExternalizableHelper.readObject(in);
        setExpiryPolicyFactory((Factory<? extends ExpiryPolicy>) ExternalizableHelper.readObject(in));
        m_fReadThrough       = in.readBoolean();
        m_fWriteThrough      = in.readBoolean();
        m_fStatisticsEnabled = in.readBoolean();
        m_fStoreByValue      = in.readBoolean();
        m_fManagementEnabled = in.readBoolean();
        }

    @Override
    public void writeExternal(DataOutput out)
            throws IOException
        {
        ExternalizableHelper.writeUTF(out, m_clzKey.getCanonicalName());
        ExternalizableHelper.writeUTF(out, m_clzValue.getCanonicalName());
        ExternalizableHelper.writeCollection(out, m_listListenerConfigurations);
        ExternalizableHelper.writeObject(out, m_factoryCacheLoader);
        ExternalizableHelper.writeObject(out, m_factoryCacheWriter);
        ExternalizableHelper.writeObject(out, m_factoryExpiryPolicy);
        out.writeBoolean(m_fReadThrough);
        out.writeBoolean(m_fWriteThrough);
        out.writeBoolean(m_fStatisticsEnabled);
        out.writeBoolean(m_fStoreByValue);
        out.writeBoolean(m_fManagementEnabled);
        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader in)
            throws IOException
        {
        ClassLoader loader       = Base.getContextClassLoader();
        String      keyClassName = in.readString(0);

        m_clzKey = loadClass(loader, keyClassName, "keyClass");

        String valueClassName = in.readString(1);

        m_clzValue = loadClass(loader, valueClassName, "valueClass");

        in.readCollection(2, m_listListenerConfigurations);

        m_factoryCacheLoader = in.readObject(3);
        m_factoryCacheWriter = in.readObject(4);
        setExpiryPolicyFactory(in.readObject(5));
        m_fReadThrough       = in.readBoolean(6);
        m_fWriteThrough      = in.readBoolean(7);
        m_fStatisticsEnabled = in.readBoolean(8);
        m_fStoreByValue      = in.readBoolean(9);
        m_fManagementEnabled = in.readBoolean(10);
        }

    @Override
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeString(0, m_clzKey.getCanonicalName());
        out.writeString(1, m_clzValue.getCanonicalName());
        out.writeCollection(2, m_listListenerConfigurations);
        out.writeObject(3, m_factoryCacheLoader);
        out.writeObject(4, m_factoryCacheWriter);
        out.writeObject(5, m_factoryExpiryPolicy);
        out.writeBoolean(6, m_fReadThrough);
        out.writeBoolean(7, m_fWriteThrough);
        out.writeBoolean(8, m_fStatisticsEnabled);
        out.writeBoolean(9, m_fStoreByValue);
        out.writeBoolean(10, m_fManagementEnabled);
        }

    // ------ data members --------------------------------------------------

    /**
     * The type of keys for {@link javax.cache.Cache}s.
     */
    protected Class<K> m_clzKey;

    /**
     * The type of values for {@link javax.cache.Cache}s.
     */
    protected Class<V> m_clzValue;

    /**
     * The {@link CacheEntryListenerConfiguration}s.
     */
    protected List<CacheEntryListenerConfiguration<K, V>> m_listListenerConfigurations;

    /**
     * The {@link Factory} for the {@link CacheLoader}.
     */
    protected Factory<CacheLoader<K, V>> m_factoryCacheLoader;

    /**
     * The {@link Factory} for the {@link CacheWriter}.
     */
    protected Factory<CacheWriter<? super K, ? super V>> m_factoryCacheWriter;

    /**
     * The {@link Factory} for the {@link ExpiryPolicy}.
     */
    protected Factory<ExpiryPolicy> m_factoryExpiryPolicy;

    /**
     * Is "read-through" enabled?
     */
    protected boolean m_fReadThrough;

    /**
     * Is "write-through" enabled?
     */
    protected boolean m_fWriteThrough;

    /**
     * Are statistics enabled?
     */
    protected boolean m_fStatisticsEnabled;

    /**
     * Is store-by-reference enabled?
     */
    protected boolean m_fStoreByValue;

    /**
     * Is JMX management enabled?
     */
    protected boolean m_fManagementEnabled;
    }
