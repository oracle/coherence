/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.events;

/**
 * An {@link Event} object captures the necessary information required to
 * adequately describe some activity that has occurred. Events can fire before
 * a change has been committed to memory (pre-committed) or after the change
 * has been committed (post-commit). Semantically {@link EventInterceptor}s
 * listening to pre-committed events will have the opportunity to change the
 * state of the request before it has been committed.
 * <p>
 * Note that an Event object itself is immutable and is only valid in
 * the context of a single chain of {@link EventInterceptor#onEvent(Event)}
 * calls. Holding a reference to the Event outside of that scope is unsafe.
 *
 * @author bo, nsa, rhan, rhl, hr 2011.03.29
 * @since Coherence 12.1.2
 *
 * @param <T> the type of event
 */
public interface Event<T extends Enum<T>>
    {
    /**
     * Return the {@link Event}'s type.
     *
     * @return the Event's type
     */
    public T getType();

    /**
     * Return the {@link EventDispatcher} this event was raised by.
     *
     * @return the EventDispatcher this event was raised by
     */
    public EventDispatcher getDispatcher();

    /**
     * Dispatch this event to the next {@link EventInterceptor} in the chain
     * if one exists. After each subsequent interceptor has run, this method
     * will return giving the caller the opportunity to observe any side effects
     * caused by down stream EventInterceptors. EventInterceptors that do not
     * explicitly call this method will not prevent other interceptors from
     * being executed, but rather will not have the opportunity to see any
     * side effects of those interceptors.
     * <p>
     * In the following example an interceptor looks for an <tt>INSERTING</tt>
     * storage event, and calls {@code nextInterceptor}.  This allows "more
     * application logic" to look at the effects of other interceptors down
     * stream.  If the event is not an <tt>INSERTING</tt> storage event, the
     * interceptor is not interested in the side effects, and simply returns.
     *
     * <pre><code>
     * public void onEvent(Event event)
     *   {
     *   if (event.getType() == StorageEntryEvent.INSERTING)
     *     {
     *     // application logic
     *
     *     event.nextInterceptor();
     *
     *     // more application logic
     *     }
     *   }
     * </code></pre>
     *
     * If an Exception is thrown by an interceptor's {@link
     * EventInterceptor#onEvent onEvent} method and this event is pre-committed,
     * the processing of further interceptors will be terminated, the exception
     * is re-thrown and the operation that generated the event will fail.
     * If this event is immutable however, the exception will be caught and
     * logged and normal processing of subsequent interceptors will continue.
     */
    public void nextInterceptor() throws RuntimeException;
    }