/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.events;

/**
 * An EventDispatcherRegistry manages the registration of
 * {@link EventDispatcher}s. The registration of an EventDispatcher results
 * in all known and future {@link EventInterceptor}s to be informed of the new
 * EventDispatcher. Unregistering an EventDispatcher causes it to no longer
 * be informed of any registered EventInterceptors.
 *
 * @author hr  2012.09.21
 * @since Coherence 12.1.2
 */
public interface EventDispatcherRegistry
    {
    /**
     * Add the specified {@link EventDispatcher} to the list of known
     * EventDispatchers that are informed about registered
     * {@link EventInterceptor}s.
     * <p>
     * When the EventDispatcher is added, it is introduced to all known
     * EventInterceptors.
     *
     * @param dispatcher  the EventDispatcher to register
     */
    public void registerEventDispatcher(EventDispatcher dispatcher);

    /**
     * Remove the specified {@link EventDispatcher} from the list of known
     * EventDispatchers.
     *
     * @param dispatcher  the EventDispatcher to be removed
     */
    public void unregisterEventDispatcher(EventDispatcher dispatcher);
    }
