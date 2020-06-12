/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.events;

/**
 * EventDispatcherAwareInterceptor is an {@link EventInterceptor} implementation
 * that takes responsibility for registering itself with the {@link EventDispatcher}.
 * This allows the EventInterceptor to determine applicability to the
 * dispatcher in addition to choosing the appropriate registration call
 * exposed by the EventDispatcher. The following shows an example of a
 * {@link #introduceEventDispatcher(String, EventDispatcher) introduceEventDispatcher}
 * implementation that explicitly states its interest in being the first interceptor
 * called and to be notified on EntryEvent.Type.INSERTING events exclusively:
 * <pre><code>
 *     public void introduceEventDispatcher(String sIdentifier, EventDispatcher dispatcher)
 *         {
 *         dispatcher.addEventInterceptor(sIdentifier, this,
 *             new HashSet(Arrays.asList(EntryEvent.Type.INSERTING)), true);
 *         }
 * </code></pre>
 * This interface accommodates for those EventInterceptor implementations
 * that require a custom registration mechanism outside of the provided
 * mechanisms; annotation and/or generics.
 *
 * @author hr  2012.09.19
 * @since Coherence 12.1.2
 *
 * @param <E>  the type of {@link Event} this interceptor accepts
 *
 * @see EventInterceptor
 */
public interface EventDispatcherAwareInterceptor<E extends Event<?>>
        extends EventInterceptor<E>
    {
    /**
     * Introduce and possibly bind this {@link EventInterceptor} to the
     * specified {@link EventDispatcher}.
     * <p>
     * Note that EventInterceptors are responsible for determining
     * whether they should be registered with an EventDispatcher by calling
     * {@link EventDispatcher#addEventInterceptor}.
     *
     * @param sIdentifier  the unique name identifying this interceptor
     * @param dispatcher   the dispatcher being introduced
     */
    public void introduceEventDispatcher(String sIdentifier, EventDispatcher dispatcher);
    }
