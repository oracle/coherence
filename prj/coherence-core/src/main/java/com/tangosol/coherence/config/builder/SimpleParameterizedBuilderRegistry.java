/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder;

import com.oracle.coherence.common.base.Disposable;

import com.tangosol.net.security.LocalPermission;

import com.tangosol.util.Base;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;

/**
 * A basic implementation of a {@link ParameterizedBuilderRegistry}.
 *
 * @author bo  2014.10.27
 *
 * @since Coherence 12.1.3
 */
public class SimpleParameterizedBuilderRegistry
        implements ParameterizedBuilderRegistry
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link SimpleParameterizedBuilderRegistry}.
     */
    public SimpleParameterizedBuilderRegistry()
        {
        f_mapBuilders = new ConcurrentHashMap<>();
        }

    /**
     * Constructs a {@link SimpleParameterizedBuilderRegistry} given another one.
     *
     * @param registry  the registry to copy
     */
    public SimpleParameterizedBuilderRegistry(ParameterizedBuilderRegistry registry)
        {
        this();

        for (Registration reg : registry)
            {
            registerBuilder(reg.getInstanceClass(), reg.getName(), reg.getBuilder());
            }
        }

    // ----- SimpleParameterizedBuilderRegistry methods ---------------------

    /**
     * Determine if the {@link ParameterizedBuilderRegistry} is empty (contains no registrations).
     *
     * @return  true if the registry contains no registrations
     */
    public boolean isEmpty()
        {
        return f_mapBuilders.isEmpty();
        }

    // ----- ParameterizedBuilderRegistry interface -------------------------

    @Override
    public synchronized void dispose()
        {
        Map<RegistryKey, RegistryValue> mapResource = f_mapBuilders;

        for (Map.Entry<RegistryKey, RegistryValue> entry : mapResource.entrySet())
            {
            RegistryValue value = entry.getValue();

            try
                {
                value.dispose();
                }
            catch (RuntimeException e)
                {
                Base.log("Exception while disposing the " + entry.getKey().getName() + " builder: " + e);
                Base.log(e);
                }
            }

        mapResource.clear();
        }

    @Override
    public <T> ParameterizedBuilder<T> getBuilder(Class<T> clzInstance)
        {
        RegistryValue value = f_mapBuilders.get(new RegistryKey(clzInstance));

        return value == null ? null : (ParameterizedBuilder<T>) value.getBuilder();
        }

    @Override
    public <T> ParameterizedBuilder<T> getBuilder(Class<T> clzInstance, String sBuilderName)
        {
        RegistryValue value = f_mapBuilders.get(new RegistryKey(clzInstance, sBuilderName));

        return value == null ? null : (ParameterizedBuilder<T>) value.getBuilder();
        }

    @Override
    public <T> String registerBuilder(Class<T> clzInstance, ParameterizedBuilder<? extends T> builder)
            throws IllegalArgumentException
        {
        return registerBuilder(clzInstance, DEFAULT_NAME, builder);
        }

    @Override
    public <T> String registerBuilder(Class<T> clzInstance, String sBuilderName,
                                      ParameterizedBuilder<? extends T> builder)
            throws IllegalArgumentException
        {
        SecurityManager security = System.getSecurityManager();
        if (security != null)
            {
            security.checkPermission(new LocalPermission("Service.registerResource"));
            }

        synchronized (clzInstance)
            {
            // attempt to get an existing registration for the key
            RegistryKey   key   = new RegistryKey(clzInstance, sBuilderName);
            RegistryValue value = f_mapBuilders.get(key);

            if (value == null)
                {
                // register the builder as it's not in the registry
                value = new RegistryValue(builder);

                f_mapBuilders.put(key, value);

                return sBuilderName;
                }
            else
                {
                throw new IllegalArgumentException(String.format(
                    "Can not register builder [%s] as [%s] of type [%s] is it already registered [%s]", builder,
                    sBuilderName, key.getInstanceClass(), value.getBuilder()));
                }
            }
        }

    @Override
    public Iterator<Registration> iterator()
        {
        ArrayList<Registration> listRegistrations = new ArrayList<>(f_mapBuilders.size());

        for (Map.Entry<RegistryKey, RegistryValue> entry : f_mapBuilders.entrySet())
            {
            RegistryKey   key   = entry.getKey();
            RegistryValue value = entry.getValue();
            BuilderRegistration registration = new BuilderRegistration(key.getName(), key.getInstanceClass(),
                                                   value.getBuilder());

            listRegistrations.add(registration);
            }

        return listRegistrations.iterator();
        }

    // ----- inner classes --------------------------------------------------

    /**
     * An internal {@link Registration} implementation.
     */
    protected class BuilderRegistration
            implements Registration
        {
        /**
         * Constructs a {@link BuilderRegistration}
         *
         * @param sBuilderName  the name of the builder
         * @param clzInstance   the class of instances constructed by the builder
         * @param bldr          the builder
         */
        public BuilderRegistration(String sBuilderName, Class<?> clzInstance, ParameterizedBuilder<?> bldr)
            {
            m_sBuilderName = sBuilderName;
            m_clzInstance  = clzInstance;
            m_bldr         = bldr;
            }

        @Override
        public String getName()
            {
            return m_sBuilderName;
            }

        @Override
        public Class<?> getInstanceClass()
            {
            return m_clzInstance;
            }

        @Override
        public ParameterizedBuilder<?> getBuilder()
            {
            return m_bldr;
            }

        /**
         * The name of the builder.
         */
        private String m_sBuilderName;

        /**
         * The class of instances produced by the builder.
         */
        private Class<?> m_clzInstance;

        /**
         * The builder.
         */
        private ParameterizedBuilder<?> m_bldr;
        }

    /**
     * Key class for a registered resource.
     */
    protected class RegistryKey
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a {@link RegistryKey}. Instances created
         * with this constructor will return {@link Class#getName()}
         * for {@link #getName()}.
         *
         * @param clz  class of registered resource
         */
        public RegistryKey(Class clz)
            {
            this(clz, clz.getName());
            }

        /**
         * Construct a {@link RegistryKey}.
         *
         * @param clz    class of instances produced by the registered builder
         * @param sName  name of registered builder
         */
        public RegistryKey(Class clz, String sName)
            {
            if (clz == null)
                {
                throw new NullPointerException("ParameterizedBuilder class cannot be null");
                }

            if (sName == null)
                {
                throw new NullPointerException("ParameterizedBuilder name cannot be null");
                }

            m_clz   = clz;
            m_sName = sName;
            }

        // ----- accessors --------------------------------------------------

        /**
         * Return the class of the instance produced by the builder.
         *
         * @return  the instance class
         */
        public Class getInstanceClass()
            {
            return m_clz;
            }

        /**
         * Return the builder name.
         *
         * @return  the builder name
         */
        public String getName()
            {
            return m_sName;
            }

        // ----- Object methods ---------------------------------------------

        @Override
        public boolean equals(Object o)
            {
            if (this == o)
                {
                return true;
                }

            if (o == null || !Base.equals(getClass(), o.getClass()))
                {
                return false;
                }

            RegistryKey that = (RegistryKey) o;

            return Base.equals(m_clz, that.m_clz) && Base.equals(m_sName, that.m_sName);
            }

        @Override
        public int hashCode()
            {
            int result = m_clz.hashCode();

            result = 31 * result + m_sName.hashCode();

            return result;
            }

        // ----- data members -----------------------------------------------

        /**
         * The instance class.
         */
        private Class m_clz;

        /**
         * The builder name.
         */
        private String m_sName;
        }

    /**
     * A holder for a {@link ParameterizedBuilder}.
     */
    protected class RegistryValue
            implements Disposable
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a {@link RegistryValue}.
         *
         * @param builder  the registered builder
         */
        public RegistryValue(ParameterizedBuilder<?> builder)
            {
            if (builder == null)
                {
                throw new NullPointerException("Resource cannot be null");
                }

            m_builder = builder;
            }

        // ----- accessors --------------------------------------------------

        /**
         * Return the registered builder.
         *
         * @return the registered builder
         */
        public ParameterizedBuilder<?> getBuilder()
            {
            return m_builder;
            }

        // ----- interface Disposable ---------------------------------------

        @Override
        public void dispose()
            {
            ParameterizedBuilder<?> builder = m_builder;

            if (builder instanceof Disposable)
                {
                ((Disposable) builder).dispose();
                }
            }

        // ----- data members -----------------------------------------------

        /**
         * The registered resource.
         */
        private ParameterizedBuilder<?> m_builder;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The map of builders keyed by class and name.
     */
    private final ConcurrentHashMap<RegistryKey, RegistryValue> f_mapBuilders;
    }
