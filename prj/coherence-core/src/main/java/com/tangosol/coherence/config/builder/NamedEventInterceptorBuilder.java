/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder;

import com.tangosol.coherence.config.ParameterList;

import com.tangosol.config.annotation.Injectable;
import com.tangosol.config.expression.Parameter;
import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.net.events.EventInterceptor;
import com.tangosol.net.events.NamedEventInterceptor;
import com.tangosol.net.events.annotation.Interceptor;
import com.tangosol.net.events.annotation.Interceptor.Order;

import com.tangosol.util.RegistrationBehavior;

/**
 * An NamedEventInterceptorBuilder facilitates the construction of a
 * {@link NamedEventInterceptor}, which wraps an {@link EventInterceptor}. The
 * wrapped EventInterceptor will be constructed based on its <tt>instance /
 * class-scheme</tt> XML declaration, honoring any <tt>init-params</tt>
 * defined.
 * <p>
 * The construction of a NamedEventInterceptor allows metadata associated
 * with the EventInterceptor to be held. This metadata is used to determine
 * eligibility against {@link com.tangosol.net.events.EventDispatcher}s and
 * for registration purposes.
 * <p>
 * In addition to XML being used as input in defining an EventInterceptor and
 * its metadata the presence of an annotation ({@link Interceptor}) can also
 * contribute. This annotation can define the identifier for this interceptor,
 * the event types to subscribe to and whether to be placed first in the chain
 * of EventInterceptors ({@link Order#HIGH}.
 * <p>
 * NamedEventInterceptor objects also use class level generic type
 * definitions as input for configuration. The generic type definition allows
 * EventInterceptor implementations to restrict on event types by narrowing
 * the type definition within the reference to the EventInterceptor interface.
 * For example, the following interceptor restricts to only accept transfer
 * events:
 * <pre><code>
 *     class MyInterceptor
 *             implements EventInterceptor&lt;TransferEvent&gt;
 *         {
 *         public void onEvent(TransferEvent event);
 *         }
 * </code></pre>
 * The precedence, in highest order first reading left to right, for
 * configuration is as follows:
 * <pre><code>
 *     XML -&gt; Annotation -&gt; Generic Type Bounds
 * </code></pre>
 *
 * @author hr  2011.10.05
 * @since Coherence 12.1.2
 */
public class NamedEventInterceptorBuilder
        implements ParameterizedBuilder<NamedEventInterceptor>, BuilderCustomization<EventInterceptor>
    {
    // ----- ParameterizedBuilder interface ---------------------------------

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    public NamedEventInterceptor realize(ParameterResolver resolver, ClassLoader loader, ParameterList listParameters)
        {
        // ensure we have a classloader
        loader = loader == null ? getClass().getClassLoader() : loader;

        ParameterizedBuilder<EventInterceptor> bldr        = getCustomBuilder();

        EventInterceptor                       interceptor = null;

        try
            {
            interceptor = bldr.realize(resolver, loader, listParameters);
            }
        catch (Exception e)
            {
            throw new IllegalArgumentException("Unable to build an EventInterceptor based on the specified class: "
                                               + String.valueOf(bldr), e);
            }

        // determine the service and cache name for the event interceptor
        Parameter paramServiceName = resolver.resolve("service-name");
        String    sServiceName = paramServiceName == null ? null : paramServiceName.evaluate(resolver).as(String.class);
        Parameter paramCacheName   = resolver.resolve("cache-name");
        String    sCacheName       = paramCacheName == null ? null : paramCacheName.evaluate(resolver).as(String.class);

        // instantiate the NamedEventInterceptor which provides a store for the
        // metadata used in determining the interceptor's applicability to an
        // event dispatcher
        // if a RegistrationBehavior is prescribed in the XML we will honor it
        // otherwise we make an appropriate decision on the correct behavior in
        // NamedEventInterceptor.ensureInitialized()
        NamedEventInterceptor incptrNamed = new NamedEventInterceptor(getName(), interceptor, sCacheName, sServiceName,
                                                getOrder(), getRegistrationBehavior());

        return incptrNamed;
        }

    // ----- BuilderCustomization interface ---------------------------------

    /**
     * {@inheritDoc}
     */
    public ParameterizedBuilder<EventInterceptor> getCustomBuilder()
        {
        return m_bldrEventInterceptor;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCustomBuilder(ParameterizedBuilder<EventInterceptor> bldr)
        {
        m_bldrEventInterceptor = bldr;
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Get the logical name / identifier for this {@link EventInterceptor}.
     *
     * @return the logical name / identifier for this {@link EventInterceptor}
     */
    public String getName()
        {
        return m_sName;
        }

    /**
     * Set the logical name / identifier for this {@link EventInterceptor}.
     *
     * @param sName  logical name / identifier for this {@link EventInterceptor}.
     *
     * @return a reference to {@code this} builder
     */
    @Injectable
    public NamedEventInterceptorBuilder setName(String sName)
        {
        m_sName = sName;

        return this;
        }

    /**
     * Whether this {@link EventInterceptor} should be head of the stack.
     *
     * @return whether this {@link EventInterceptor} should be head of the stack
     */
    public boolean isFirst()
        {
        return m_order != null && Interceptor.Order.HIGH.equals(m_order);
        }

    /**
     * Return the {@link Order} of this interceptor.
     *
     * @return the {@link Order} of this interceptor
     */
    public Order getOrder()
        {
        return m_order;
        }

    /**
     * Set the {@link EventInterceptor}'s order (<tt>HIGH || LOW)</tt>, hence
     * whether it should be at the start of the chain of interceptors.
     *
     * @param order  whether this {@link EventInterceptor} should be
     *               head of the stack based on the values {@link Order#HIGH}
     *               or {@link Order#LOW}
     *
     * @return a reference to {@code this} builder
     */
    @Injectable
    public NamedEventInterceptorBuilder setOrder(Order order)
        {
        m_order = order;

        return this;
        }

    /**
     * Returns the behavior upon duplicate registration.
     *
     * @return the behavior upon duplicate registration.
     */
    public RegistrationBehavior getRegistrationBehavior()
        {
        return m_behavior;
        }

    /**
     * Specifies the behavior upon duplicate registration.
     *
     * @param behavior  the behavior upon duplicate registration
     *
     * @return a reference to {@code this} builder
     */
    @Injectable
    public NamedEventInterceptorBuilder setRegistrationBehavior(RegistrationBehavior behavior)
        {
        m_behavior = behavior;

        return this;
        }

    // ----- Object methods -------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
        {
        return "NamedEventInterceptorBuilder{" + "Builder = " + m_bldrEventInterceptor + ", name = '" + m_sName + '\''
               + ", priority = '" + isFirst() + '}';
        }

    // ---- data members ----------------------------------------------------

    /**
     * The logical name / identifier to be used for the realized {@link EventInterceptor}.
     */
    private String m_sName;

    /**
     * The {@link ParameterizedBuilder} responsible for realizing an instance
     * of an {@link EventInterceptor} class.
     */
    private ParameterizedBuilder<EventInterceptor> m_bldrEventInterceptor;

    /**
     * Whether this {@link EventInterceptor} should be head of the stack, with
     * default behaviour being the tail/end.
     */
    private Order m_order;

    /**
     * Specifies the behavior upon duplicate registration.
     */
    private RegistrationBehavior m_behavior;
    }
