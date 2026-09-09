/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.internal;

import com.tangosol.util.ExternalizableHelper;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Coherence global default ObjectInputFilter baseline. Product-owned Java
 * serialization bridge fields use expected-type filters from
 * {@link BridgeObjectInputFilter} or {@link SerializationBridgeFilters}; those
 * filters must intersect with this baseline through {@link #create(Object)} or
 * the local helper equivalent, so they narrow but never widen it.
 *
 * @author Aleks Seovic  2026.04.29
 * @since 26.04
 */
public final class DefaultObjectInputFilter
        implements InvocationHandler
    {
    /**
     * Return a filter that intersects the Coherence default filter with any
     * JVM-wide user-configured serial filter.
     *
     * @return the effective serial filter
     */
    public static Object create()
        {
        Object filterUser = ExternalizableHelper.getConfigSerialFilter();
        return filterUser == null ? INSTANCE.proxy() : new IntersectingFilter(INSTANCE.proxy(), filterUser).proxy();
        }

    /**
     * Return a filter that intersects the Coherence default filter, any
     * JVM-wide user-configured serial filter, and the specified bridge filter.
     *
     * @param filterBridge  the bridge-local filter
     *
     * @return the effective serial filter
     */
    public static Object create(Object filterBridge)
        {
        Object filterDefault = create();
        if (filterDefault == null)
            {
            return filterBridge;
            }
        return filterBridge == null ? filterDefault : new IntersectingFilter(filterDefault, filterBridge).proxy();
        }

    /**
     * Return a filter that intersects the Coherence default filter with the
     * specified user filter.
     *
     * @param filterUser  the user filter
     *
     * @return the effective serial filter
     */
    static Object forTesting(Object filterUser)
        {
        return filterUser == null ? INSTANCE.proxy() : new IntersectingFilter(INSTANCE.proxy(), filterUser).proxy();
        }

    /**
     * Apply a bridge-local filter to the current thread.
     *
     * @param filterBridge  the bridge-local filter
     *
     * @return a scope that restores the previous bridge filter when closed
     */
    public static Scope bridge(Object filterBridge)
        {
        final Object filterPrevious = s_filterBridge.get();
        Object       filterCurrent  = filterBridge == null || filterPrevious == null
                ? filterBridge
                : new IntersectingFilter(filterPrevious, filterBridge).proxy();

        s_filterBridge.set(filterCurrent);
        return new Scope()
            {
            @Override
            public void close()
                {
                if (filterPrevious == null)
                    {
                    s_filterBridge.remove();
                    }
                else
                    {
                    s_filterBridge.set(filterPrevious);
                    }
                }
            };
        }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
        {
        String sName = method.getName();
        if ("checkInput".equals(sName))
            {
            Class<?> clz = serialClass(args[0]);
            boolean  fAllowed = SerializationAllowlist.isAllowed(clz);
            if (!fAllowed)
                {
                SerializationTelemetry.recordFilterCheck("rejected", "serialization-allowlist-rejected", clz, null);
                return status("REJECTED");
                }

            Object filterBridge = s_filterBridge.get();
            if (filterBridge == null)
                {
                return status("ALLOWED");
                }

            Enum statusBridge = (Enum) method.invoke(filterBridge, args);
            return "REJECTED".equals(statusBridge.name()) ? statusBridge : status("ALLOWED");
            }
        else if ("toString".equals(sName))
            {
            return getClass().getName();
            }
        else if ("hashCode".equals(sName))
            {
            return System.identityHashCode(this);
            }
        else if ("equals".equals(sName))
            {
            return proxy == args[0];
            }
        return null;
        }

    /**
     * Return this handler's ObjectInputFilter proxy.
     *
     * @return this handler's ObjectInputFilter proxy
     */
    private Object proxy()
        {
        Object proxy = m_proxy;
        if (proxy == null && CLZ_FILTER != null)
            {
            proxy = Proxy.newProxyInstance(CLZ_FILTER.getClassLoader(), new Class<?>[] {CLZ_FILTER}, this);
            m_proxy = proxy;
            }
        return proxy;
        }

    // ----- inner interface: Scope ----------------------------------------

    /**
     * A bridge-filter scope.
     */
    public interface Scope
            extends AutoCloseable
        {
        @Override
        void close();
        }

    // ----- inner class: IntersectingFilter --------------------------------

    /**
     * Filter that rejects when either delegate rejects.
     */
    private static class IntersectingFilter
            implements InvocationHandler
        {
        IntersectingFilter(Object defaultFilter, Object userFilter)
            {
            f_defaultFilter = defaultFilter;
            f_userFilter    = userFilter;
            }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
            {
            String sName = method.getName();
            if ("checkInput".equals(sName))
                {
                Enum statusDefault = (Enum) method.invoke(f_defaultFilter, args);
                if ("REJECTED".equals(statusDefault.name()))
                    {
                    return statusDefault;
                    }

                Enum statusUser = (Enum) method.invoke(f_userFilter, args);
                if ("REJECTED".equals(statusUser.name()))
                    {
                    return statusUser;
                    }

                return "ALLOWED".equals(statusDefault.name()) || "ALLOWED".equals(statusUser.name())
                        ? status("ALLOWED")
                        : status("UNDECIDED");
                }
            else if ("toString".equals(sName))
                {
                return getClass().getName();
                }
            else if ("hashCode".equals(sName))
                {
                return System.identityHashCode(this);
                }
            else if ("equals".equals(sName))
                {
                return proxy == args[0];
                }
            return null;
            }

        /**
         * Return this handler's ObjectInputFilter proxy.
         *
         * @return this handler's ObjectInputFilter proxy
         */
        private Object proxy()
            {
            Object proxy = m_proxy;
            if (proxy == null && CLZ_FILTER != null)
                {
                proxy = Proxy.newProxyInstance(CLZ_FILTER.getClassLoader(), new Class<?>[] {CLZ_FILTER}, this);
                m_proxy = proxy;
                }
            return proxy;
            }

        private final Object f_defaultFilter;
        private final Object f_userFilter;
        private       Object m_proxy;
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Return the serial class from an ObjectInputFilter.FilterInfo proxy.
     *
     * @param filterInfo  the filter info
     *
     * @return the serial class
     *
     * @throws Exception if the serial class cannot be read
     */
    private static Class<?> serialClass(Object filterInfo) throws Exception
        {
        return (Class<?>) METHOD_SERIAL_CLASS.invoke(filterInfo);
        }

    /**
     * Return an ObjectInputFilter.Status enum value.
     *
     * @param sStatus  the status name
     *
     * @return an ObjectInputFilter.Status enum value
     */
    private static Enum status(String sStatus)
        {
        return Enum.valueOf((Class<Enum>) CLZ_STATUS, sStatus);
        }

    /**
     * Return an ObjectInputFilter class for the current runtime.
     *
     * @return an ObjectInputFilter class for the current runtime
     */
    private static Class<?> filterClass()
        {
        try
            {
            return Class.forName("java.io.ObjectInputFilter");
            }
        catch (ClassNotFoundException e)
            {
            try
                {
                return Class.forName("sun.misc.ObjectInputFilter");
                }
            catch (ClassNotFoundException ignored)
                {
                return null;
                }
            }
        }

    /**
     * Return an ObjectInputFilter.Status class for the current runtime.
     *
     * @return an ObjectInputFilter.Status class for the current runtime
     */
    private static Class<?> statusClass()
        {
        try
            {
            return Class.forName("java.io.ObjectInputFilter$Status");
            }
        catch (ClassNotFoundException e)
            {
            try
                {
                return Class.forName("sun.misc.ObjectInputFilter$Status");
                }
            catch (ClassNotFoundException ignored)
                {
                return null;
                }
            }
        }

    /**
     * Return an ObjectInputFilter.FilterInfo class for the current runtime.
     *
     * @return an ObjectInputFilter.FilterInfo class for the current runtime
     */
    private static Class<?> filterInfoClass()
        {
        try
            {
            return Class.forName("java.io.ObjectInputFilter$FilterInfo");
            }
        catch (ClassNotFoundException e)
            {
            try
                {
                return Class.forName("sun.misc.ObjectInputFilter$FilterInfo");
                }
            catch (ClassNotFoundException ignored)
                {
                return null;
                }
            }
        }

    /**
     * Return the serialClass method for the current runtime FilterInfo type.
     *
     * @return the serialClass method
     */
    private static Method serialClassMethod()
        {
        try
            {
            return CLZ_FILTER_INFO == null ? null : CLZ_FILTER_INFO.getMethod("serialClass");
            }
        catch (NoSuchMethodException e)
            {
            throw new IllegalStateException(e);
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * Singleton default filter.
     */
    private static final DefaultObjectInputFilter INSTANCE = new DefaultObjectInputFilter();

    /**
     * The runtime ObjectInputFilter class.
     */
    private static final Class<?> CLZ_FILTER = filterClass();

    /**
     * The runtime ObjectInputFilter.Status class.
     */
    private static final Class<?> CLZ_STATUS = statusClass();

    /**
     * The runtime ObjectInputFilter.FilterInfo class.
     */
    private static final Class<?> CLZ_FILTER_INFO = filterInfoClass();

    /**
     * The FilterInfo.serialClass method.
     */
    private static final Method METHOD_SERIAL_CLASS = serialClassMethod();

    /**
     * Bridge filter scoped to the current deserialization operation.
     */
    private static final ThreadLocal<Object> s_filterBridge = new ThreadLocal<>();

    // ----- data members ---------------------------------------------------

    /**
     * This handler's ObjectInputFilter proxy.
     */
    private Object m_proxy;
    }
