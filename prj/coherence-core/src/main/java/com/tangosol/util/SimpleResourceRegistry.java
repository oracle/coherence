/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

import com.oracle.coherence.common.base.Disposable;
import com.tangosol.net.security.LocalPermission;

import static com.tangosol.util.BuilderHelper.using;

import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;

/**
 * A {@link SimpleResourceRegistry} is a basic implementation of a {@link ResourceRegistry}.
 *
 * @author bo  2011.06.22
 * @author pp  2012.02.29
 * @since Coherence 12.1.2
 */
public class SimpleResourceRegistry
        implements ResourceRegistry
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a {@link SimpleResourceRegistry}.
     */
    public SimpleResourceRegistry()
        {
        m_mapResource = new ConcurrentHashMap<>();
        }

    // ----- SimpleResourceRegistry methods ---------------------------------

    /**
     * Determine if the {@link ResourceRegistry} is empty (contains no resource registrations).
     *
     * @return  true if the registry contains no resource registrations
     */
    public boolean isEmpty()
        {
        return m_mapResource.isEmpty();
        }

    /**
     * Register all the resources contained in this registry with the target registry.
     *
     * @param registry  the target {@link ResourceRegistry} to register resources with
     */
    public void registerResources(ResourceRegistry registry)
        {
        registerResources(registry, RegistrationBehavior.FAIL);
        }

    /**
     * Register all the resources contained in this registry with the target registry.
     *
     * @param registry  the target {@link ResourceRegistry} to register resources with
     * @param behavior  the {@link RegistrationBehavior} to use
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void registerResources(ResourceRegistry registry, RegistrationBehavior behavior)
        {
        for (Map.Entry<RegistryKey, RegistryValue> entry : m_mapResource.entrySet())
            {
            RegistryKey   key   = entry.getKey();
            RegistryValue value = entry.getValue();
            Class         clz   = key.getResourceClass();

            registry.registerResource(clz, key.getName(), using(value.getResource()), behavior, value.getObserver());
            }
        }

    // ----- ResourceRegistry interface -------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void dispose()
        {
        Map<RegistryKey, RegistryValue> mapResource = m_mapResource;

        for (Map.Entry<RegistryKey, RegistryValue> entry : mapResource.entrySet())
            {
            RegistryValue value = entry.getValue();

            try
                {
                value.dispose();
                }
            catch (RuntimeException e)
                {
                Base.log("Exception while disposing the " + entry.getKey().getName() + " resource: " + e);
                Base.log(e);
                }
            }

        mapResource.clear();
        }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public <R> R getResource(Class<R> clzResource)
        {
        RegistryValue value = m_mapResource.get(new RegistryKey(clzResource));

        return value == null ? null : (R) value.getResource();
        }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public <R> R getResource(Class<R> clzResource, String sResourceName)
        {
        RegistryValue value = m_mapResource.get(new RegistryKey(clzResource, sResourceName));

        return value == null ? null : (R) value.getResource();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public <R> String registerResource(Class<R> clzResource, R resource)
        {
        return registerResource(clzResource, clzResource.getName(), using(resource), RegistrationBehavior.FAIL, null);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public <R> String registerResource(Class<R> clzResource, String sResourceName, R resource)
        {
        return registerResource(clzResource, sResourceName, using(resource), RegistrationBehavior.FAIL, null);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public <R> String registerResource(Class<R> clzResource, Builder<? extends R> bldrResource,
                                       RegistrationBehavior behavior, ResourceLifecycleObserver<R> observer)
        {
        return registerResource(clzResource, clzResource.getName(), bldrResource, behavior, observer);
        }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public <R> String registerResource(Class<R> clzResource, String sResourceName, Builder<? extends R> bldrResource,
                                       RegistrationBehavior behavior, ResourceLifecycleObserver<R> observer)
        {
        SecurityManager security = System.getSecurityManager();
        if (security != null)
            {
            security.checkPermission(
                new LocalPermission("Service.registerResource"));
            }

        synchronized (clzResource)
            {
            // attempt to get an existing resource registration for the key
            RegistryKey   key   = new RegistryKey(clzResource, sResourceName);
            RegistryValue value = m_mapResource.get(key);

            if (value == null)
                {
                // register the resource as it's not in the registry
                value = new RegistryValue(bldrResource.realize(), (ResourceLifecycleObserver<Object>) observer);
                m_mapResource.put(key, value);
                }
            else
                {
                switch (behavior)
                    {
                    case IGNORE :
                        // SKIP: there's nothing to do if the resource is already
                        // registered
                        break;

                    case REPLACE :
                        // realize the resource to register
                        R resource = bldrResource.realize();

                        // when the existing resource different from the one
                        // we've just built, dispose of the old one and register
                        // the new one
                        if (resource != value.getResource() || !resource.equals(value.getResource()))
                            {
                            value.dispose();
                            value = new RegistryValue(resource, (ResourceLifecycleObserver<Object>) observer);
                            m_mapResource.put(key, value);
                            }

                        break;

                    case FAIL :
                        // check to see if the resource being registered is the
                        // same as the one we already have registered.  if it is
                        // we don't throw an exception
                        resource = bldrResource.realize();

                        if (resource != value.getResource() || !resource.equals(value.getResource()))
                            {
                            throw new IllegalArgumentException(String.format(
                                "Can not register resource [%s] as resource [%s] is it already registered with [%s] as [%s]",
                                sResourceName, value.getResource(), key.getResourceClass(), key.getName()));
                            }

                        break;

                    case ALWAYS :
                        // create a unique key to register the resource
                        int    i = 1;
                        String sGeneratedResourceName;

                        do
                            {
                            sGeneratedResourceName = String.format("%s-%d", sResourceName, i++);
                            key                    = new RegistryKey(clzResource, sGeneratedResourceName);
                            }
                        while (m_mapResource.containsKey(key));

                        // realize the resource to register
                        resource = bldrResource.realize();

                        value    = new RegistryValue(resource, (ResourceLifecycleObserver<Object>) observer);
                        m_mapResource.put(key, value);

                        sResourceName = sGeneratedResourceName;
                        break;
                    }
                }

            return sResourceName;
            }
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public <R> void unregisterResource(Class<R> clzResource, String sResourceName)
        {
        SecurityManager security = System.getSecurityManager();
        if (security != null)
            {
            security.checkPermission(
                new LocalPermission("Service.registerResource"));
            }

        m_mapResource.remove(new RegistryKey(clzResource, sResourceName));
        }

    // ----- inner classes --------------------------------------------------

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
        public RegistryKey(Class<?> clz)
            {
            this(clz, clz.getName());
            }

        /**
         * Construct a {@link RegistryKey}.
         *
         * @param clz    class of registered resource
         * @param sName  name of registered resource
         */
        public RegistryKey(Class<?> clz, String sName)
            {
            if (clz == null)
                {
                throw new NullPointerException("Resource class cannot be null");
                }

            if (sName == null)
                {
                throw new NullPointerException("Resource name cannot be null");
                }

            m_clz   = clz;
            m_sName = sName;
            }

        // ----- accessors --------------------------------------------------

        /**
         * Return the resource class.
         *
         * @return  the resource class
         */
        public Class<?> getResourceClass()
            {
            return m_clz;
            }

        /**
         * Return the resource name.
         *
         * @return  the resource name
         */
        public String getName()
            {
            return m_sName;
            }

        // ----- Object methods ---------------------------------------------

        /**
         * {@inheritDoc}
         */
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

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode()
            {
            int result = m_clz.hashCode();

            result = 31 * result + m_sName.hashCode();

            return result;
            }

        // ----- data members -----------------------------------------------

        /**
         * The resource class.
         */
        private Class<?> m_clz;

        /**
         * The resource name.
         */
        private String m_sName;
        }

    /**
     * A holder for resource objects and their (optional) respective
     * {@link ResourceRegistry.ResourceLifecycleObserver ResourceLifecycleObservers}.
     * The {@link ResourceRegistry.ResourceLifecycleObserver#onRelease(Object)}
     * method will be invoked when {@link #dispose()} is invoked on this object. Furthermore,
     * if the provided resource implements {@link Disposable}, its {@link #dispose()} method will
     * be invoked.
     */
    protected class RegistryValue
            implements Disposable
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a {@link RegistryValue} object.
         *
         * @param oResource the registered resource
         * @param observer  the lifecycle observer
         */
        public RegistryValue(Object oResource, ResourceLifecycleObserver<Object> observer)
            {
            if (oResource == null)
                {
                throw new NullPointerException("Resource cannot be null");
                }

            m_oResource = oResource;
            m_observer  = observer;
            }

        // ----- accessors --------------------------------------------------

        /**
         * Return the registered resource.
         *
         * @return the registered resource
         */
        public Object getResource()
            {
            return m_oResource;
            }

        /**
         * Return the lifecycle observer for the registered resource.
         *
         * @return the lifecycle observer for the registered resource (may be null)
         */
        public ResourceLifecycleObserver<Object> getObserver()
            {
            return m_observer;
            }

        // ----- interface Disposable ---------------------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        public void dispose()
            {
            Object oResource = m_oResource;

            if (oResource instanceof Disposable)
                {
                ((Disposable) oResource).dispose();
                }

            ResourceLifecycleObserver<Object> observer = m_observer;

            if (observer != null)
                {
                observer.onRelease(oResource);
                }
            }

        // ----- data members -----------------------------------------------

        /**
         * The registered resource.
         */
        private Object m_oResource;

        /**
         * The lifecycle observer for the registered resource.
         */
        private ResourceLifecycleObserver<Object> m_observer;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The map of resources keyed by class and name.
     */
    private final ConcurrentHashMap<RegistryKey, RegistryValue> m_mapResource;
    }
