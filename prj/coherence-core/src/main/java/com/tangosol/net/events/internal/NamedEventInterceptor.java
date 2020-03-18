/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.events.internal;

import com.tangosol.net.events.Event;
import com.tangosol.net.events.EventDispatcher;
import com.tangosol.net.events.EventInterceptor;
import com.tangosol.net.events.EventDispatcherAwareInterceptor;
import com.tangosol.net.events.annotation.Interceptor;
import com.tangosol.net.events.annotation.Interceptor.Order;

import com.tangosol.net.events.partition.PartitionedServiceDispatcher;
import com.tangosol.net.events.partition.TransactionEvent;
import com.tangosol.net.events.partition.TransferEvent;
import com.tangosol.net.events.partition.UnsolicitedCommitEvent;
import com.tangosol.net.events.partition.cache.CacheLifecycleEvent;
import com.tangosol.net.events.partition.cache.EntryEvent;
import com.tangosol.net.events.partition.cache.EntryProcessorEvent;
import com.tangosol.net.events.partition.cache.PartitionedCacheDispatcher;

import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.RegistrationBehavior;

import java.lang.reflect.Type;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.tangosol.util.Base.azzert;

/**
 * A wrapper for {@link EventInterceptor}s allowing additional metadata to be
 * associated with the interceptor without augmenting the interceptor contract.
 * <p>
 * This implementation will derive defaults for the values it is concerned
 * with. These defaults are based on the presence of the {@link Interceptor}
 * annotation or a generic type defined on the EventInterceptor class file.
 * Once this class has been initialized, via constructors, it is immutable.
 * <p>
 * There may be circumstances when the cost of deriving defaults may not be
 * required thus a {@link #NamedEventInterceptor(EventInterceptor) constructor}
 * is provided that avoids this initialization.
 *
 * @author hr/nsa/bo 2011.03.29
 * @since Coherence 12.1.2
 *
 * @param <E> the type of event
 */
public class NamedEventInterceptor<E extends Event<?>>
        implements EventDispatcherAwareInterceptor<E>
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Construct a NamedEventInterceptor for the specified interceptor.
     *
     * @param interceptor  the {@link EventInterceptor}
     */
    public NamedEventInterceptor(EventInterceptor<E> interceptor)
        {
        this(null, interceptor);
        }

    /**
     * Construct a NamedEventInterceptor for the specified interceptor and
     * unique identifier.
     *
     * @param sName        the registered name of the wrapped interceptor
     * @param interceptor  the {@link EventInterceptor}
     */
    public NamedEventInterceptor(String sName, EventInterceptor<E> interceptor)
        {
        this(sName, interceptor, null);
        }

    /**
     * Construct a NamedEventInterceptor for the specified interceptor and
     * unique identifier.
     *
     * @param sName        the registered name of the wrapped interceptor
     * @param interceptor  the {@link EventInterceptor}
     * @param behavior     the behavior enacted upon discovering duplicate
     *                     interceptors
     */
    public NamedEventInterceptor(String sName, EventInterceptor<E> interceptor, RegistrationBehavior behavior)
        {
        this(sName, interceptor, null, null, null, behavior);
        }

    /**
     * Construct a NamedEventInterceptor for the specified interceptor.
     *
     * @param sName          the registered name of the wrapped interceptor
     * @param interceptor    the {@link EventInterceptor}
     * @param sCacheName     the name of the cache this interceptor is
     *                       targeted to
     * @param sServiceName   the name of the service this interceptor is
     *                       targeted to
     * @param order          whether this interceptor should be first in the
     *                       chain of interceptors
     * @param behavior       the behavior enacted upon discovering duplicate
     *                       interceptors
     */
    public NamedEventInterceptor(String sName, EventInterceptor<E> interceptor, String sCacheName,
                                 String sServiceName, Order order, RegistrationBehavior behavior)
        {
        this(sName, interceptor, sCacheName, sServiceName, order, behavior, null);
        }

    /**
     * Construct a NamedEventInterceptor for the specified interceptor using
     * the provided NamedEventInterceptor as the source to clone from.
     *
     * @param interceptor  the {@link EventInterceptor}
     * @param incptrNamed  the NamedEventInterceptor to clone from
     */
    public NamedEventInterceptor(EventInterceptor<E> interceptor, NamedEventInterceptor<E> incptrNamed)
        {
        this(incptrNamed.getRegisteredName(),
             interceptor,
             incptrNamed.getCacheName(),
             incptrNamed.getServiceName(),
             incptrNamed.getOrder(),
             incptrNamed.getBehavior(),
             incptrNamed.getEventTypes());
        }

    /**
     * Construct a NamedEventInterceptor for the specified interceptor.
     *
     * @param sName          the registered name of the wrapped interceptor
     * @param interceptor    the {@link EventInterceptor}
     * @param sCacheName     the name of the cache this interceptor is
     *                       targeted to
     * @param sServiceName   the name of the service this interceptor is
     *                       targeted to
     * @param order          whether this interceptor should be first in the
     *                       chain of interceptors
     * @param setEventTypes  the set of event types the interceptor is
     *                       interested in
     * @param behavior       the behavior enacted upon discovering duplicate
     *                       interceptors
     */
    protected NamedEventInterceptor(String sName, EventInterceptor<E> interceptor, String sCacheName,
                                    String sServiceName, Order order, RegistrationBehavior behavior,
                                    Set<Enum> setEventTypes)
        {
        azzert(interceptor != null, "interceptor can not be null");

        m_sName         = sName;
        m_interceptor   = interceptor;
        m_sCacheName    = sCacheName   != null && sCacheName.isEmpty()   ? null : sCacheName;
        m_sServiceName  = sServiceName != null && sServiceName.isEmpty() ? null : sServiceName;
        m_order         = order;
        m_setEventTypes = setEventTypes == null || setEventTypes.isEmpty() ? null : setEventTypes;
        m_behavior      = behavior;

        ensureInitialized();
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Return registered name of {@link EventInterceptor}.
     *
     * @return registered name of {@link EventInterceptor}
     */
    public String getRegisteredName()
        {
        return m_sName;
        }

    /**
     * Return the wrapped interceptor.
     *
     * @return the wrapped interceptor
     */
    public EventInterceptor<E> getInterceptor()
        {
        return m_interceptor;
        }

    /**
     * Return the set of event types this interceptor is concerned with.
     *
     * @return the set of event types this interceptor is concerned with
     */
    public Set<Enum> getEventTypes()
        {
        return m_setEventTypes == null
                ? null : Collections.unmodifiableSet(m_setEventTypes);
        }

    /**
     * Return the name of the service this interceptor should receive events
     * from.
     *
     * @return the name of the service this interceptor should receive events
     *         from
     */
    public String getServiceName()
        {
        return m_sServiceName;
        }

    /**
     * Return the name of the cache this interceptor should receive events
     * from.
     *
     * @return the name of the cache this interceptor should receive events
     *         from
     */
    public String getCacheName()
        {
        return m_sCacheName;
        }

    /**
     * Return whether this interceptor should request to be first in the
     * chain of interceptors.
     *
     * @return whether this interceptor should request to be first in the
     *         chain of interceptors
     */
    public boolean isFirst()
        {
        return m_order == Interceptor.Order.HIGH;
        }

    /**
     * Return the priority of this interceptor in the chain of interceptors.
     *
     * @return the priority of this interceptor in the chain of interceptors
     */
    public Order getOrder()
        {
        return m_order;
        }

    /**
     * Return the {@link RegistrationBehavior} associated with this interceptor.
     *
     * @return the RegistrationBehavior associated with this interceptor
     */
    public RegistrationBehavior getBehavior()
        {
        return m_behavior;
        }

    // ----- EventInterceptor interface -------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void onEvent(E event)
        {
        getInterceptor().onEvent(event);
        }

    // ----- EventDispatcherAwareInterceptor interface ----------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void introduceEventDispatcher(String sIdentifier, EventDispatcher dispatcher)
        {
        EventInterceptor interceptor = getInterceptor();
        if (interceptor instanceof EventDispatcherAwareInterceptor && isAcceptable(dispatcher))
            {
            // Note: we restrict EventDispatcherAwareInterceptor implementations
            // by their scope in the configuration as known by this NamedEventInterceptor
            ((EventDispatcherAwareInterceptor) interceptor).introduceEventDispatcher(sIdentifier, dispatcher);
            }
        else
            {
            dispatcher.addEventInterceptor(sIdentifier, this);
            }
        }

    /**
     * Determine whether the {@link EventDispatcher} should be accepted by
     * this interceptor.
     *
     * @param dispatcher  the EventDispatcher being introduced
     */
    public boolean isAcceptable(EventDispatcher dispatcher)
        {
        // perform service name and/or cache name filtering
        String sServiceName       = m_sServiceName;
        String sCacheName         = m_sCacheName;
        String sActualServiceName = null;
        String sActualCacheName   = null;

        // fast short-circuit
        if (sServiceName == null && sCacheName == null)
            {
            return true;
            }

        if (dispatcher instanceof PartitionedCacheDispatcher)
            {
            PartitionedCacheDispatcher bmd = (PartitionedCacheDispatcher) dispatcher;
            sActualServiceName = bmd.getBackingMapContext().getManagerContext().getCacheService().getInfo().getServiceName();
            sActualCacheName   = bmd.getBackingMapContext().getCacheName();
            }
        else if (dispatcher instanceof PartitionedServiceDispatcher)
            {
            sActualServiceName = ((PartitionedServiceDispatcher) dispatcher).getService().getInfo().getServiceName();
            }

        // predicate
        boolean fMatch = sServiceName == null || sServiceName.equals(sActualServiceName);
        if (fMatch && sCacheName != null && sActualCacheName != null)
            {
            fMatch = sCacheName.endsWith("*")
                    ? sActualCacheName.startsWith(sCacheName.substring(0, sCacheName.length() - 1))
                    : sActualCacheName.equals(sCacheName);
            }

        return fMatch;
        }

    /**
     * Generates a unique name for the interceptor. The unique name will be
     * generated by appending a {@code '$'} following by an increasing integer.
     *
     * @return a unique name for the interceptor
     */
    public String generateName()
        {
        String sName = m_sName;

        if (sName == null)
            {
            sName = (m_sCacheName   == null || m_sCacheName.isEmpty()   ? "" : m_sCacheName   + ":") +
                    (m_sServiceName == null || m_sServiceName.isEmpty() ? "" : m_sServiceName + ":") +
                    m_interceptor.getClass().getName();
            }
        else
            {
            int iPos = sName.lastIndexOf('$');
            if (iPos == -1)
                {
                sName += "$1";
                }
            else
                {
                int nCurrent = 0;
                try
                    {
                    nCurrent = Integer.parseInt(sName.substring(iPos + 1));
                    }
                catch (NumberFormatException e)
                    {
                    // $ may exist in the name; resort to assuming we have not
                    // generated before
                    sName += "$";
                    iPos  = sName.length() - 1;
                    }

                sName = sName.substring(0, iPos + 1) + ++nCurrent;
                }
            }

        return m_sName = sName;
        }

    // ----- Object methods -------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public String toString()
        {
        return "<" + m_sName + ", " + getInterceptor().getClass().getName() + ">";
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o)
        {
        return this == o || (o instanceof NamedEventInterceptor
                ? Base.equals(m_interceptor, ((NamedEventInterceptor) o).m_interceptor)
                : o instanceof EventInterceptor && Base.equals(m_interceptor, o));
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
        {
        return m_interceptor != null ? m_interceptor.hashCode() : 0;
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Based on the state of the NamedEventInterceptor populate any
     * uninitialized values based on either an annotation being present or
     * the class file's generic types.
     */
    protected final void ensureInitialized()
        {
        EventInterceptor incptr = m_interceptor;

        if (incptr == null)
            {
            throw new IllegalArgumentException("EventInterceptor can not be null");
            }

        // process annotation
        String      sName         = m_sName;
        Order       order         = Order.LOW;
        Class<?>    clzIncptr     = incptr.getClass();
        Interceptor anno          = clzIncptr.getAnnotation(Interceptor.class);
        Set<Enum>   setEventTypes = new HashSet<Enum>(m_setEventTypes == null
                        ? Collections.<Enum>emptySet() : m_setEventTypes);

        if (anno != null)
            {
            sName = m_sName = sName == null ? anno.identifier() : sName;
            order = anno.order();

            if (setEventTypes.isEmpty())
                {
                setEventTypes.addAll(Arrays.asList(anno.entryEvents()));
                setEventTypes.addAll(Arrays.asList(anno.entryProcessorEvents()));
                setEventTypes.addAll(Arrays.asList(anno.transactionEvents()));
                setEventTypes.addAll(Arrays.asList(anno.transferEvents()));
                setEventTypes.addAll(Arrays.asList(anno.unsolicitedEvents()));
                setEventTypes.addAll(Arrays.asList(anno.cacheLifecycleEvents()));
                }
            }

        // process generics iff there are no events specified in the annotation
        Map<String, Type[]> mapTypes = setEventTypes.isEmpty()
                ? ClassHelper.getReifiedTypes(clzIncptr, EventInterceptor.class)
                : Collections.<String, Type[]>emptyMap();

        //Note: we could improve the interceptor acceptance by validating the
        //      generic bounds with the annotation event types at the cost of
        //      always calling ClassHelper.getReifiedTypes

        if (mapTypes.containsKey("E"))
            {
            Type[] aTypes = mapTypes.get("E");
            if (aTypes[0] instanceof Type)
                {
                Class clzEvent = (Class) ClassHelper.getClass(aTypes[0]);

                // there are special cases for event grouping interfaces as we
                // explicitly reference the children and their associated event
                // types.
                // Note: we avoid a check for the root interface as null translates
                //       to listening to all event types
                if (clzEvent == com.tangosol.net.events.partition.Event.class)
                    {
                    setEventTypes.addAll(Arrays.asList(TransactionEvent.Type.values()));
                    setEventTypes.addAll(Arrays.asList(TransferEvent.Type.values()));
                    setEventTypes.addAll(Arrays.asList(UnsolicitedCommitEvent.Type.values()));
                    }
                else if (clzEvent == com.tangosol.net.events.partition.cache.Event.class)
                    {
                    setEventTypes.addAll(Arrays.asList(EntryEvent.Type.values()));
                    setEventTypes.addAll(Arrays.asList(EntryProcessorEvent.Type.values()));
                    setEventTypes.addAll(Arrays.asList(CacheLifecycleEvent.Type.values()));
                    }
                else
                    {
                    // we assume events follow the model of having the event type
                    // enum defined as a child of the event class thus can generically
                    // extract the event type enum information.
                    for (Class<?> clzInner : clzEvent.getDeclaredClasses())
                        {
                        if (clzInner.isEnum())
                            {
                            setEventTypes.addAll(Arrays.asList((Enum[]) clzInner.getEnumConstants()));
                            break;
                            }
                        }
                    }
                }
            }

        if (!setEventTypes.isEmpty())
            {
            m_setEventTypes = setEventTypes;
            }

        // ensure the identifier, order and behavior are populated
        if (sName == null || sName.isEmpty())
            {
            generateName();
            m_behavior = m_behavior == null ? RegistrationBehavior.ALWAYS : m_behavior;
            }
        else if (m_behavior == null)
            {
            // if a name has been specified by either being provided or via
            // an annotation we should fail on duplicate registration attempt
            m_behavior = RegistrationBehavior.FAIL;
            }
        m_order = m_order == null ? order : m_order;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The event interceptor.
     */
    private final EventInterceptor<E> m_interceptor;

    /**
     * The cache name this {@link EventInterceptor} is concerned with.
     */
    private final String m_sCacheName;

    /**
     * The service name this {@link EventInterceptor} is concerned with.
     */
    private final String m_sServiceName;

    /**
     * The registered name associated with the {@link EventInterceptor}.
     */
    private String m_sName;

    /**
     * Enum indicating whether this {@link EventInterceptor} should be added
     * at the head of the {@link EventDispatcher}'s interceptor chain
     * ({@link Order#HIGH}).
     */
    private Order m_order;

    /**
     * The set of event types that this {@link EventInterceptor} subscribes
     * to.
     */
    private Set<Enum> m_setEventTypes;

    /**
     * Specifies the behavior upon duplicate registration.
     */
    private RegistrationBehavior m_behavior;
    }
